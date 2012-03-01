package com.anjlab.ping.pages;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.anjlab.ping.entities.Account;
import com.anjlab.ping.services.Application;
import com.anjlab.ping.services.Utils;
import com.anjlab.ping.services.dao.AccountDAO;


public class Settings {

    @Property
    @Persist(PersistenceConstants.FLASH)
    @SuppressWarnings("unused")
    private String message;

    @Property
    @Persist(PersistenceConstants.FLASH)
    @SuppressWarnings("unused")
    private String messageColor;

    @Property
    @SuppressWarnings("unused")
    private final String timeZoneModel = Utils.getTimeZoneModel();
    
    @AfterRender
    public void cleanup() {
        userAccount = null;
        quotas = null;
        usedQuotas = null;
    }
    
    @Property
    private String quota;
    
    private Map<String, String> usedQuotas;
    
    public String getTotalQuota() {
        return String.valueOf(getUserAccount().getMaxNumberOfJobs(quota));
    }
    
    public String getUsedQuota() {
        if (usedQuotas == null) {
            usedQuotas = application.getUsedQuotas();
        }
        return usedQuotas.get(quota);
    }
    
    private List<String> quotas;
    
    public List<String> getQuotas() {
        if (quotas == null) {
            String[] strings = Utils.getCronStringModel().split(",");
            quotas = Arrays.asList(strings);
        }
        return quotas;
    }
    
    @Inject
    private Application application;
    
    private Account userAccount;
    
    public Account getUserAccount() {
        if (userAccount == null) {
            userAccount = application.getUserAccount();
        }
        return userAccount;
    }
    
    @Inject
    private AccountDAO accountDAO;
    
    public void onSuccess() {
        try {
            accountDAO.update(getUserAccount());

            this.message = "Saved";
            this.messageColor = "green";
        } catch (Exception e) {
            this.message = e.getMessage();
            this.messageColor = "red";
        }
    }
    
}
