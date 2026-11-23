package com.athleet.core.organization.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A tenant. A primary organization has no parent; an affiliated academy points to its primary organization.
 * Every primary organization can also operate as a self-contained academy.
 */
@Entity
@Table(name = "organization")
public class Organization extends BaseEntity {

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 150)
    private String name;

    private String city;
    private String state;
    private String country;
    private String address;
    private String email;
    private String phone;
    private String sport;

    @Column(name = "logo_key")
    private String logoKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgStatus status = OrgStatus.ACTIVE;

    /** What a center is called in the UI, e.g. "Center" or "Class" (Branding → Center Name Title). */
    @Column(name = "center_label", nullable = false)
    private String centerLabel = "Center";

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "date_format", nullable = false)
    private String dateFormat = "d/m/Y";

    protected Organization() {
    }

    public Organization(UUID parentId, String name) {
        this.parentId = parentId;
        this.name = name;
    }

    public boolean isPrimary() {
        return parentId == null;
    }

    public UUID getParentId() { return parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }
    public String getLogoKey() { return logoKey; }
    public OrgStatus getStatus() { return status; }
    public void setStatus(OrgStatus status) { this.status = status; }
    public String getCenterLabel() { return centerLabel; }
    public void setCenterLabel(String centerLabel) { this.centerLabel = centerLabel; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
}
