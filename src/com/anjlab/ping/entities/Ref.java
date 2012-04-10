package com.anjlab.ping.entities;

import java.io.Serializable;

import org.datanucleus.jpa.annotations.Extension;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.google.appengine.api.datastore.Key;

@Entity
public class Ref implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5725956849471055726L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    private Key accountKey;
    @Basic
    private Key scheduleKey;
    
    private String scheduleName;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    private int accessType;
    
    public static final int ACCESS_TYPE_READONLY = 0;
    public static final int ACCESS_TYPE_FULL = 1;

    public String getAccessTypeFriendly() {
        switch (accessType) {
        case ACCESS_TYPE_FULL:
            return "Full";
        case ACCESS_TYPE_READONLY:
            return "Read Only";
        default:
            return "?";
        }
    }
    public Key getAccountKey() {
        return accountKey;
    }
    public void setAccountKey(Key accountKey) {
        this.accountKey = accountKey;
    }
    public Key getScheduleKey() {
        return scheduleKey;
    }
    public void setScheduleKey(Key scheduleKey) {
        this.scheduleKey = scheduleKey;
    }
    public int getAccessType() {
        return accessType;
    }
    public void setAccessType(int accessType) {
        this.accessType = accessType;
    }
    public Long getId() {
        return id;
    }
    public String getScheduleName() {
        return scheduleName;
    }
    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }
}
