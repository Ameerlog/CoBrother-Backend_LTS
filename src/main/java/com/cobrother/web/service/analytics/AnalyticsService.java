package com.cobrother.web.service.analytics;

import com.cobrother.web.Entity.analytics.ProfileView;
import com.cobrother.web.Entity.analytics.VentureView;
import com.cobrother.web.Entity.coventure.CoVenture;
import com.cobrother.web.Entity.coventure.Venture;
import com.cobrother.web.Entity.community.Community;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.*;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired private VentureViewRepository ventureViewRepository;
    @Autowired private ProfileViewRepository profileViewRepository;
    @Autowired private VentureRepository ventureRepository;
    @Autowired private CoVentureRepository coVentureRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private NotificationService notificationService;


    // ── Track a venture view ──────────────────────────────────────────────────
    public void trackVentureView(Venture venture, AppUser viewer) {
        // Don't track owner viewing their own venture
        if (viewer != null && viewer.getId().equals(venture.getListedBy().getId())) return;

        VentureView view = new VentureView();
        view.setVenture(venture);
        view.setViewer(viewer);

        if (viewer != null && viewer.getCommunityProfile() != null) {
            Community cp = viewer.getCommunityProfile();
            if (cp.getIndustry() != null) view.setViewerIndustry(cp.getIndustry().name());
            if (cp.getRole() != null) view.setViewerRole(cp.getRole().name());
        }

        ventureViewRepository.save(view);

        // Also increment the existing views counter on Venture
        venture.setViews(venture.getViews() + 1);
        ventureRepository.save(venture);
    }

    // ── Track a profile view ──────────────────────────────────────────────────
    public void trackProfileView(Community profile, AppUser viewer) {
        // skip self-view
        if (viewer != null && viewer.getId().equals(profile.getAppUser().getId())) return;

        ProfileView pv = new ProfileView();
        pv.setProfile(profile);      // ✅ set the entity, NOT pv.setProfileId(...)
        pv.setViewer(viewer);        // ✅ set the entity, NOT pv.setViewerId(...)

        // pull industry/role from the viewer's community profile if available
        communityRepository.findByAppUser(viewer).ifPresent(c -> {
            pv.setViewerIndustry(c.getIndustry() != null ? c.getIndustry().toString() : null);
            pv.setViewerRole(c.getRole() != null ? c.getRole().toString() : null);
        });

        profileViewRepository.save(pv);

        if (viewer != null) {
            String viewerName = viewer.getFirstname() != null
                    ? viewer.getFirstname() + " " + viewer.getLastname()
                    : "Someone";
            notificationService.notifyProfileViewed(
                    profile.getAppUser(),
                    viewerName
            );
        }

        profile.setViews(profile.getViews() + 1);
        communityRepository.save(profile);
    }

    // ── Venture Analytics ─────────────────────────────────────────────────────
    public Map<String, Object> getVentureAnalytics(Long ventureId) {
        AppUser me = currentUserService.getCurrentUser();
        Venture venture = ventureRepository.findById(ventureId)
                .orElseThrow(() -> new RuntimeException("Venture not found"));

        if (!venture.getListedBy().getId().equals(me.getId()))
            throw new RuntimeException("Access denied");

        List<VentureView> views = ventureViewRepository.findByVentureId(ventureId);
        List<CoVenture> applications = coVentureRepository.findByVentureListedById(me.getId())
                .stream().filter(a -> a.getVenture().getId().equals(ventureId))
                .collect(Collectors.toList());

        // Views over last 30 days grouped by day
        Map<String, Long> viewsByDay = buildDailyTimeline(
                views.stream().map(VentureView::getViewedAt).collect(Collectors.toList()), 30
        );

        // Viewer industry breakdown
        Map<String, Long> byIndustry = views.stream()
                .filter(v -> v.getViewerIndustry() != null)
                .collect(Collectors.groupingBy(VentureView::getViewerIndustry, Collectors.counting()));

        // Viewer role breakdown
        Map<String, Long> byRole = views.stream()
                .filter(v -> v.getViewerRole() != null)
                .collect(Collectors.groupingBy(VentureView::getViewerRole, Collectors.counting()));

        // Applicant skills breakdown
        Map<String, Long> skillsMap = new HashMap<>();
        applications.forEach(app -> {
            Community cp = app.getApplicant().getCommunityProfile();
            if (cp != null && cp.getSkills() != null) {
                Arrays.stream(cp.getSkills().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .forEach(skill -> skillsMap.merge(skill, 1L, Long::sum));
            }
        });

        // Application status breakdown
        Map<String, Long> byStatus = applications.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));

        // Conversion rate
        long totalViews = views.size();
        long totalApps = applications.size();
        double conversionRate = totalViews > 0 ? (double) totalApps / totalViews * 100 : 0;

        // Average time from view to application (in hours)
        double avgHoursToApply = 0;
        // Match viewers who became applicants
        List<Long> applicantIds = applications.stream()
                .map(a -> a.getApplicant().getId()).collect(Collectors.toList());
        List<Double> durations = new ArrayList<>();
        for (VentureView view : views) {
            if (view.getViewer() != null && applicantIds.contains(view.getViewer().getId())) {
                applications.stream()
                        .filter(a -> a.getApplicant().getId().equals(view.getViewer().getId()))
                        .findFirst().ifPresent(app -> {
                            // We don't have application timestamp yet — use a placeholder
                            durations.add(24.0); // placeholder until you add appliedAt to CoVenture
                        });
            }
        }
        if (!durations.isEmpty()) {
            avgHoursToApply = durations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ventureId", ventureId);
        result.put("ventureName", venture.getBrandDetails().getBrandName());
        result.put("totalViews", totalViews);
        result.put("totalApplications", totalApps);
        result.put("conversionRate", Math.round(conversionRate * 10.0) / 10.0);
        result.put("avgHoursToApply", Math.round(avgHoursToApply * 10.0) / 10.0);
        result.put("viewsByDay", viewsByDay);
        result.put("byIndustry", byIndustry);
        result.put("byRole", byRole);
        result.put("applicantSkills", skillsMap);
        result.put("byStatus", byStatus);
        return result;
    }

    // ── Profile Analytics ─────────────────────────────────────────────────────
    public Map<String, Object> getProfileAnalytics() {
        AppUser me = currentUserService.getCurrentUser();
        Community profile = communityRepository.findByAppUserId(me.getId())
                .orElseThrow(() -> new RuntimeException("No community profile found"));

        List<ProfileView> views = profileViewRepository.findByProfileId(profile.getId());
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        List<ProfileView> thisWeek = views.stream()
                .filter(v -> v.getViewedAt().isAfter(oneWeekAgo))
                .collect(Collectors.toList());

        Map<String, Long> viewsByDay = buildDailyTimeline(
                views.stream().map(ProfileView::getViewedAt).collect(Collectors.toList()), 30
        );

        Map<String, Long> byIndustry = views.stream()
                .filter(v -> v.getViewerIndustry() != null)
                .collect(Collectors.groupingBy(ProfileView::getViewerIndustry, Collectors.counting()));

        Map<String, Long> byRole = views.stream()
                .filter(v -> v.getViewerRole() != null)
                .collect(Collectors.groupingBy(ProfileView::getViewerRole, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalViews", views.size());
        result.put("viewsThisWeek", thisWeek.size());
        result.put("viewsByDay", viewsByDay);
        result.put("byIndustry", byIndustry);
        result.put("byRole", byRole);
        return result;
    }

    // ── Helper: build last N days timeline ────────────────────────────────────
    private Map<String, Long> buildDailyTimeline(List<LocalDateTime> timestamps, int days) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        Map<String, Long> timeline = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = days - 1; i >= 0; i--) {
            timeline.put(now.minusDays(i).format(fmt), 0L);
        }
        timestamps.forEach(ts -> {
            String key = ts.format(fmt);
            if (timeline.containsKey(key)) timeline.merge(key, 1L, Long::sum);
        });
        return timeline;
    }
}