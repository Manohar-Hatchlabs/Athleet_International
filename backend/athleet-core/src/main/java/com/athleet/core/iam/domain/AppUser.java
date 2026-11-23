package com.athleet.core.iam.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    private String email;

    @Column(name = "country_code")
    private String countryCode;

    private String mobile;

    /** Null until the user sets a password from their invite link. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "platform_admin", nullable = false)
    private boolean platformAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    private String qualifications;
    private String profile;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    protected AppUser() {
    }

    public AppUser(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String fullName() {
        return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
    }

    public void changePassword(String hash) {
        this.passwordHash = hash;
        this.passwordChangedAt = Instant.now();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public boolean isPlatformAdmin() { return platformAdmin; }
    public void setPlatformAdmin(boolean platformAdmin) { this.platformAdmin = platformAdmin; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public LocalDate getDateOfJoining() { return dateOfJoining; }
    public void setDateOfJoining(LocalDate dateOfJoining) { this.dateOfJoining = dateOfJoining; }
    public String getQualifications() { return qualifications; }
    public void setQualifications(String qualifications) { this.qualifications = qualifications; }
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
}
