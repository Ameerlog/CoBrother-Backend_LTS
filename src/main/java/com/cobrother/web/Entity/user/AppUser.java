package com.cobrother.web.Entity.user;


import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.coventure.Venture;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_verification_token", columnList = "verificationToken")
        }
)
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;


    private String firstname;
    private String lastname;

    @Column(nullable = false)
    private String password;

    // Optional but highly recommended
    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean emailVerified = false;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    // Email verification token
    private String verificationToken;
    private LocalDateTime verificationTokenExpiry;


    private String otp;
    private LocalDateTime otpExpiry;

    @Column(nullable = false)
    private LocalDateTime lastModified;

    // For JWT refresh tokens
    private String refreshToken;


    @OneToMany(mappedBy = "listedBy")
    private List<Domain> listedDomains;

    @OneToMany(mappedBy = "purchasedBy")
    private List<Domain> purchasedDomains;

    @OneToMany(mappedBy = "listedBy")
    private List<Venture> listedVentures;

    @OneToMany(mappedBy = "purchasedBy")
    private List<Venture> purchasedVentures;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.lastModified = LocalDateTime.now();
    }


    public AppUser() {}


    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getVerificationTokenExpiry() {
        return verificationTokenExpiry;
    }

    public void setVerificationTokenExpiry(LocalDateTime verificationTokenExpiry) {
        this.verificationTokenExpiry = verificationTokenExpiry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp(){
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(LocalDateTime otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public List<Domain> getListedDomains() {
        return listedDomains;
    }

    public void setListedDomains(List<Domain> listedDomains) {
        this.listedDomains = listedDomains;
    }

    public List<Domain> getPurchasedDomains() {
        return purchasedDomains;
    }

    public void setPurchasedDomains(List<Domain> purchasedDomains) {
        this.purchasedDomains = purchasedDomains;
    }

    public List<Venture> getListedVentures() {
        return listedVentures;
    }

    public void setListedVentures(List<Venture> listedVentures) {
        this.listedVentures = listedVentures;
    }

    public List<Venture> getPurchasedVentures() {
        return purchasedVentures;
    }

    public void setPurchasedVentures(List<Venture> purchasedVentures) {
        this.purchasedVentures = purchasedVentures;
    }
}
