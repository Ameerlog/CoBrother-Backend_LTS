package com.cobrother.web.service.domain;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.cobranding.VerificationMethod;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.DomainRepository;
import com.cobrother.web.service.auth.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DomainVerificationService {

    @Autowired private DomainRepository domainRepository;
    @Autowired private MailService mailService;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── Step 1: Init verification — generate token, return instructions ───────
    public ResponseEntity<?> initVerification(long domainId, String method, AppUser user) {
        Domain domain = domainRepository.getDomainById(domainId);
        if (domain == null)
            return ResponseEntity.notFound().build();
        if (!domain.getListedBy().getId().equals(user.getId()))
            return ResponseEntity.status(403).body("Not your domain");
        if (domain.isVerified()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "This domain is already verified. No further action needed."
            ));
        }
        VerificationMethod vm;
        try {
            vm = VerificationMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid method. Use DNS_TXT, META_TAG, or WHOIS_EMAIL"));
        }

        // Generate a fresh token
        // ✅ Only regenerate token for WHOIS_EMAIL (it expires in 30 mins)
// DNS_TXT and META_TAG reuse the existing token assigned at listing time
        String token;
        if (vm == VerificationMethod.WHOIS_EMAIL) {
            token = "cobrother-verify=" + UUID.randomUUID().toString().replace("-", "");
            domain.setVerificationToken(token);
        } else {
            // Reuse existing token — if somehow null (legacy record), generate once
            if (domain.getVerificationToken() == null) {
                token = "cobrother-verify=" + UUID.randomUUID().toString().replace("-", "");
                domain.setVerificationToken(token);
            } else {
                token = domain.getVerificationToken();
            }
        }
        domain.setVerificationMethod(vm);
        domain.setVerified(false);

        String fullDomain = domain.getDomainName() + domain.getDomainExtension();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("method", vm.name());
        response.put("token", token);
        response.put("domain", fullDomain);

        switch (vm) {
            case DNS_TXT -> {
                domain.setWhoisEmail(null);
                domainRepository.save(domain);
                response.put("instructions", List.of(
                        "Log in to your domain registrar (GoDaddy, Namecheap, etc.)",
                        "Navigate to DNS Management for " + fullDomain,
                        "Add a new TXT record with these exact values:",
                        "  Host / Name: @ (or leave blank)",
                        "  Value: " + token,
                        "  TTL: 3600 (or default)",
                        "Click Save, then come back and click 'Check Verification'",
                        "DNS propagation can take a few minutes — if it fails, wait 5 mins and retry"
                ));
                response.put("recordType", "TXT");
                response.put("recordHost", "@");
                response.put("recordValue", token);
            }
            case META_TAG -> {
                domain.setWhoisEmail(null);
                domainRepository.save(domain);
                response.put("instructions", List.of(
                        "Add the following <meta> tag inside the <head> section of your homepage at " + fullDomain,
                        "<meta name=\"cobrother-verification\" content=\"" + token + "\">",
                        "OR upload a text file to: http://" + fullDomain + "/.well-known/cobrother-verification.txt",
                        "The file should contain only: " + token,
                        "Once done, click 'Check Verification'"
                ));
                response.put("metaTag", "<meta name=\"cobrother-verification\" content=\"" + token + "\">");
                response.put("filePath", "/.well-known/cobrother-verification.txt");
                response.put("fileContent", token);
            }
            case WHOIS_EMAIL -> {
                // Look up WHOIS email before saving
                String whoisEmail = lookupWhoisEmail(fullDomain);
                if (whoisEmail == null) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Could not find a public registrant email for " + fullDomain +
                                    ". The WHOIS record may be private. Please try DNS TXT or Meta Tag instead."
                    ));
                }

                // Send verification email
                try {
                    mailService.sendDomainVerificationEmail(whoisEmail, fullDomain, token);
                } catch (Exception e) {
                    return ResponseEntity.internalServerError().body(Map.of(
                            "error", "Found WHOIS email but failed to send verification email. Please try another method."
                    ));
                }

                String maskedEmail = maskEmail(whoisEmail);
                domain.setWhoisEmail(maskedEmail);
                domainRepository.save(domain);

                response.put("instructions", List.of(
                        "A verification code has been sent to the registrant email: " + maskedEmail,
                        "Check your email and enter the 6-digit code below",
                        "The code expires in 30 minutes"
                ));
                response.put("maskedEmail", maskedEmail);
            }
        }

        return ResponseEntity.ok(response);
    }

    // ── Step 2: Check / confirm verification ──────────────────────────────────
    public ResponseEntity<?> checkVerification(long domainId, String otpCode, AppUser user) {
        Domain domain = domainRepository.getDomainById(domainId);
        if (domain == null) return ResponseEntity.notFound().build();
        // Already verified by this user
        if (domain.isVerified()) {
            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "message", "This domain is already verified."
            ));
        }
        if (!domain.getListedBy().getId().equals(user.getId()))
            return ResponseEntity.status(403).body("Not your domain");
        if (domain.isVerified())
            return ResponseEntity.ok(Map.of("verified", true, "message", "Already verified"));
        if (domain.getVerificationToken() == null || domain.getVerificationMethod() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "No verification in progress. Please initiate first."));

        String fullDomain = domain.getDomainName() + domain.getDomainExtension();
        String token = domain.getVerificationToken();
        boolean success = false;
        String failReason = "";

        switch (domain.getVerificationMethod()) {
            case DNS_TXT -> {
                try {
                    success = checkDnsTxt(fullDomain, token);
                    if (!success) failReason = "TXT record not found yet. DNS may still be propagating — wait a few minutes and try again.";
                } catch (Exception e) {
                    failReason = "DNS lookup failed: " + e.getMessage();
                }
            }
            case META_TAG -> {
                try {
                    success = checkMetaTagOrFile(fullDomain, token);
                    if (!success) failReason = "Verification token not found on your website. Make sure the meta tag or file is correctly placed.";
                } catch (Exception e) {
                    failReason = "Could not reach your website: " + e.getMessage();
                }
            }
            case WHOIS_EMAIL -> {
                // otpCode is the 6-digit code from email
                if (otpCode == null || otpCode.isBlank())
                    return ResponseEntity.badRequest().body(Map.of("error", "Please enter the verification code from your email"));
                // Token stores the full UUID — last 6 chars used as the code
                String expectedCode = token.substring(token.length() - 6).toUpperCase();
                success = otpCode.trim().toUpperCase().equals(expectedCode);
                if (!success) failReason = "Invalid verification code. Please check your email and try again.";
            }
        }

        if (!success) {
            return ResponseEntity.ok(Map.of("verified", false, "message", failReason));
        }

        // Mark verified
        domain.setVerified(true);
        domain.setVerifiedAt(LocalDateTime.now());
        domain.setVerificationToken(null); // clear token after use
        domainRepository.save(domain);

        return ResponseEntity.ok(Map.of(
                "verified", true,
                "message", "Domain verified successfully! Your listing now shows a verified badge."
        ));
    }

    // ── DNS TXT check using dnsjava ───────────────────────────────────────────
    private boolean checkDnsTxt(String domainName, String expectedToken) throws Exception {
        // Strip leading dot if present e.g. ".com" case — domainName is already full
        Lookup lookup = new Lookup(domainName, Type.TXT);
        lookup.setResolver(new SimpleResolver("8.8.8.8")); // Google DNS
        lookup.setCache(null); // no cache — always fresh

        Record[] records = lookup.run();
        if (records == null) return false;

        for (Record record : records) {
            TXTRecord txt = (TXTRecord) record;
            for (String s : txt.getStrings()) {
                if (s.equals(expectedToken)) return true;
            }
        }
        return false;
    }

    // ── Meta tag + file check ─────────────────────────────────────────────────
    private boolean checkMetaTagOrFile(String domainName, String expectedToken) {
        // Try file first (simpler check)
        try {
            String fileUrl = "http://" + domainName + "/.well-known/cobrother-verification.txt";
            String content = restTemplate.getForObject(fileUrl, String.class);
            if (content != null && content.trim().equals(expectedToken)) return true;
        } catch (Exception ignored) {}

        // Try https file
        try {
            String fileUrl = "https://" + domainName + "/.well-known/cobrother-verification.txt";
            String content = restTemplate.getForObject(fileUrl, String.class);
            if (content != null && content.trim().equals(expectedToken)) return true;
        } catch (Exception ignored) {}

        // Try meta tag on homepage
        for (String scheme : List.of("https://", "http://")) {
            try {
                String html = restTemplate.getForObject(scheme + domainName, String.class);
                if (html != null) {
                    // Look for <meta name="cobrother-verification" content="TOKEN">
                    Pattern p = Pattern.compile(
                            "<meta[^>]+name=[\"']cobrother-verification[\"'][^>]+content=[\"']([^\"']+)[\"']",
                            Pattern.CASE_INSENSITIVE
                    );
                    Matcher m = p.matcher(html);
                    if (m.find() && m.group(1).equals(expectedToken)) return true;

                    // Also handle reversed attribute order
                    Pattern p2 = Pattern.compile(
                            "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+name=[\"']cobrother-verification[\"']",
                            Pattern.CASE_INSENSITIVE
                    );
                    Matcher m2 = p2.matcher(html);
                    if (m2.find() && m2.group(1).equals(expectedToken)) return true;
                }
            } catch (Exception ignored) {}
        }

        return false;
    }

    // ── WHOIS email lookup ────────────────────────────────────────────────────
    private String lookupWhoisEmail(String domainName) {
        try {
            // Use IANA WHOIS via plain socket — most reliable fallback
            String whoisData = queryWhois(domainName, "whois.iana.org");

            // Try to find a registrar WHOIS server
            String registrarWhois = extractWhoisServer(whoisData);
            if (registrarWhois != null) {
                whoisData = queryWhois(domainName, registrarWhois);
            }

            // Extract email from WHOIS data
            return extractEmail(whoisData);
        } catch (Exception e) {
            return null;
        }
    }

    private String queryWhois(String domain, String whoisServer) throws Exception {
        try (java.net.Socket socket = new java.net.Socket(whoisServer, 43);
             java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
             java.io.BufferedReader in = new java.io.BufferedReader(
                     new java.io.InputStreamReader(socket.getInputStream()))) {
            socket.setSoTimeout(5000);
            out.println(domain);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        }
    }

    private String extractWhoisServer(String whoisData) {
        Pattern p = Pattern.compile("(?i)whois:\\s*(.+\\.\\S+)");
        Matcher m = p.matcher(whoisData);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractEmail(String whoisData) {
        // Look for registrant/admin/tech email
        Pattern p = Pattern.compile(
                "(?i)(?:registrant|admin|tech).*?email:\\s*([\\w.+-]+@[\\w.-]+\\.[a-z]{2,})"
        );
        Matcher m = p.matcher(whoisData);
        if (m.find()) return m.group(1).trim();

        // Generic email fallback
        Pattern generic = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}");
        Matcher gm = generic.matcher(whoisData);
        // Skip noreply/example emails
        while (gm.find()) {
            String email = gm.group();
            if (!email.contains("noreply") && !email.contains("example")
                    && !email.contains("abuse") && !email.contains("privacy")) {
                return email;
            }
        }
        return null;
    }

    // ── Mask email for display: vittal@gmail.com → v*****@g*****.com ─────────
    private String maskEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length != 2) return email;
        String user   = parts[0];
        String domain = parts[1];
        String maskedUser   = user.charAt(0) + "*".repeat(Math.max(1, user.length() - 1));
        String[] domainParts = domain.split("\\.");
        String maskedDomain = domainParts[0].charAt(0)
                + "*".repeat(Math.max(1, domainParts[0].length() - 1))
                + "." + domainParts[domainParts.length - 1];
        return maskedUser + "@" + maskedDomain;
    }
}