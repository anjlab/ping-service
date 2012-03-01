package com.anjlab.ping.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.anjlab.ping.services.Utils;

@Entity
public class Account implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = -3714340195981301403L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Transient
    private Ref ref;
    
    @Column(nullable=false)
    private String email;
    private String timeZoneCity;
    
    // Since 13.05.2010
    private Date lastVisitDate;
    private Date creationDate;

    //  Since 26.02.2012
    private String quotaLimits;
    
    public Account() {
    }
    
    public int getMaxNumberOfJobs(String cronString) {
        Map<String, String> limits = parseQuotaLimits();
        if (!limits.containsKey(cronString)) {
            return 0;
        }
        return Integer.parseInt(limits.get(cronString));
    }
    public void setMaxNumberOfJobs(String cronString, int value) {
        setQuotaLimit(cronString, String.valueOf(value));
    }
    private Map<String, String> parseQuotaLimits() {
        Map<String, String> limits = new HashMap<String, String>();
        if (quotaLimits != null) {
            String[] pairs = quotaLimits.split(";");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    limits.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return limits;
    }
    private void setQuotaLimit(String quota, String limit) {
        Map<String, String> limits = parseQuotaLimits();
        limits.put(quota, limit);
        StringBuilder builder = new StringBuilder();
        for (String key : limits.keySet()) {
            builder.append(key).append("=").append(limits.get(key)).append(";");
        }
        quotaLimits = builder.toString();
    }
    public String getEmail() {
        return email == null ? null : email.toLowerCase();
    }
    public void setEmail(String email) {
        this.email = email == null ? null : email.toLowerCase();
    }
    public Long getId() {
        return id;
    }
    public void setRef(Ref ref) {
        this.ref = ref;
    }
    public Ref getRef() {
        return ref;
    }
    public String getRefAccessType() {
        return ref.getAccessTypeFriendly();
    }
    public void setTimeZoneCity(String timeZoneCity) {
        this.timeZoneCity = timeZoneCity;
    }
    public String getTimeZoneCity() {
        return timeZoneCity;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Date getLastVisitDate() {
        return lastVisitDate;
    }
    public void setLastVisitDate(Date lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }
    public boolean isSystem() {
        return "system".equals(email);
    }

    public void setDefaultQuotas() {
        setMaxNumberOfJobs(Utils.EVERY_1_HOURS, 25);
        setMaxNumberOfJobs(Utils.EVERY_DAY_00_00, 10);
    }

    public boolean hasAnyQuotaLimitsApplied() {
        return !Utils.isNullOrEmpty(quotaLimits);
    }
}
