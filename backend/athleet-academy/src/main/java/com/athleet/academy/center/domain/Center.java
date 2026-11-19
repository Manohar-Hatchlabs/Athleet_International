package com.athleet.academy.center.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** A training location (shown as "Class" in AJSDP via the organization's center label). */
@Entity
@Table(name = "center")
public class Center extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    private String code;
    @Column(name = "contact_name") private String contactName;
    @Column(name = "contact_country_code") private String contactCountryCode;
    @Column(name = "contact_phone") private String contactPhone;
    @Column(name = "contact_email") private String contactEmail;
    private String sport;
    @Column(name = "facility_type") private String facilityType;
    @Column(name = "facility_count") private Integer facilityCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    private String country;
    private String state;
    private String city;
    private String address1;
    private String address2;
    private String zipcode;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "geofence_radius_m", nullable = false)
    private int geofenceRadiusM = 200;

    protected Center() {
    }

    public Center(UUID organizationId, String name) {
        this.organizationId = organizationId;
        this.name = name;
    }

    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactCountryCode() { return contactCountryCode; }
    public void setContactCountryCode(String v) { this.contactCountryCode = v; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }
    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }
    public Integer getFacilityCount() { return facilityCount; }
    public void setFacilityCount(Integer facilityCount) { this.facilityCount = facilityCount; }
    public ActiveStatus getStatus() { return status; }
    public void setStatus(ActiveStatus status) { this.status = status; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }
    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }
    public String getZipcode() { return zipcode; }
    public void setZipcode(String zipcode) { this.zipcode = zipcode; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public int getGeofenceRadiusM() { return geofenceRadiusM; }
    public void setGeofenceRadiusM(int geofenceRadiusM) { this.geofenceRadiusM = geofenceRadiusM; }
}
