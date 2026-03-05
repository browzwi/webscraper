package com.browzwi.webscraper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a business discovered through URL discovery from Google Maps.
 *
 * @since 1.0
 */
@Entity
@Table(name = "discovered_business")
public class DiscoveredBusiness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Column(length = 500)
    private String address;

    @Column(name = "phone_number", length = 100)
    private String phoneNumber;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Lob
    @Column(name = "social_media_links", columnDefinition = "TEXT")
    private String socialMediaLinks;

    @Column(nullable = false, length = 50)
    private String status = "DISCOVERED";

    @ManyToOne
    @JoinColumn(name = "discovery_job_id", nullable = false)
    private DiscoveryJob discoveryJob;

    public DiscoveredBusiness() {
    }

    public Long getId() {
        return id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getSocialMediaLinks() {
        return socialMediaLinks;
    }

    public void setSocialMediaLinks(String socialMediaLinks) {
        this.socialMediaLinks = socialMediaLinks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DiscoveryJob getDiscoveryJob() {
        return discoveryJob;
    }

    public void setDiscoveryJob(DiscoveryJob discoveryJob) {
        this.discoveryJob = discoveryJob;
    }
}
