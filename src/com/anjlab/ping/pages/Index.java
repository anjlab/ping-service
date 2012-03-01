package com.anjlab.ping.pages;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import javax.cache.Cache;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.BeforeRenderTemplate;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;
import org.apache.tapestry5.services.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.gae.QuotaDetails;
import com.anjlab.gae.QuotaDetails.Quota;
import com.anjlab.ping.entities.Account;
import com.anjlab.ping.entities.Job;
import com.anjlab.ping.entities.Ref;
import com.anjlab.ping.pages.task.UpdateQuotasTask;
import com.anjlab.ping.services.Application;
import com.anjlab.ping.services.GAEHelper;
import com.anjlab.ping.services.Utils;
import com.anjlab.tapestry5.AbstractReadonlyPropertyConduit;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;


public class Index {

    private static final Logger logger = LoggerFactory.getLogger(Index.class);
    
    @Property
    @Persist
    @SuppressWarnings("unused")
    private String message;
    
    @Property
    @Persist
    @SuppressWarnings("unused")
    private String messageColor;

    private Boolean quotaLimited;
    private Long timeToQuotaRefreshMillis;
    
    @AfterRender
    public void cleanup() {
        message = null;
        userAccount = null;
        account = null;
        grantedEmail = null;
        job = null;
        messageColor = null;
        quotaLimited = null;
        timeToQuotaRefreshMillis = null;
    }
    
    @Property
    private Job job;
    
    public String getCronString() {
        return (!job.isSuspended() ? job.getCronString() : "suspended");
    }
    
    public String getLastPingTimestamp() {
        Date timestamp = job.getLastPingTimestamp();
        
        return timestamp != null ? application.formatDate(timestamp) : "N/A";
    }
    
    public String getSummaryStatusCssClass()
    {
        switch (job.getHealthStatus())
        {
            case Error:
                return "status-error";
            case Unknown:
                return "status-na";
            case OK:
                return "status-okay";
            default:
                return "status-warning";
        }
    }
    
    public Long[] getJobContext() {
        return Utils.createJobContext(job);
    }

    public void onActionFromDeleteJob(Long jobId) {
        try {
            application.deleteJob(jobId, true);
        } catch (Exception e) {
            operationFailed("Error deleting job", e);
        }
    }
    
    @Property
    private Account account;

    public boolean isDeleteAccountLinkEnabled() {
        return ! account.getId().equals(getUserAccount().getId());
    }

    private Account userAccount;
    
    public Account getUserAccount() {
        if (userAccount == null) {
            String impersonatedUser = request.getParameter("impersonatedUser");
            userAccount = impersonatedUser == null
                        ? application.getUserAccount()
                        : application.getUserAccount(impersonatedUser);
        }
        return userAccount;
    }
    
    @Property
    private String grantedEmail;
    
    @Property
    private boolean readOnly;
    
    public void onSuccessFromGrantAccessTo() {
        try {
            application.grantAccess(grantedEmail, getUserAccount().getEmail(), readOnly ? Ref.ACCESS_TYPE_READONLY : Ref.ACCESS_TYPE_FULL);
        } catch (Exception e) {
            operationFailed("Grant failed", e);
        }
    }

    @Inject
    private Request request;
    
    public List<Job> getJobs() {
        return application.getAvailableJobs(getUserAccount());
    }
    
    @Inject
    private Application application;
    
    public List<Account> getAccounts() {
        return application.getAccounts(getUserAccount().getEmail());
    }
    
    public void onActionFromRemoveAccount(Long accountId) {
        try {
            application.removeAccount(accountId, getUserAccount().getEmail());
        } catch (Exception e) {
            operationFailed("Remove account failed", e);
        }
    }

    public boolean isAdmin() {
        UserService userService = UserServiceFactory.getUserService();
        
        return userService.isUserLoggedIn() 
            && userService.isUserAdmin();
    }

    @Inject
    private Cache cache;
    @Inject
    private MemcacheService memcache;
    
    public void onActionFromClearCache() {
        memcache.clearAll();
        cache.clear();
        operationSucceeded("Cache cleared");
    }
    
    public void onActionFromQuotaLimited() {
        quotaDetails.setQuotaLimited(Quota.DatastoreWrite, true);
    }
    
    @Inject
    private QuotaDetails quotaDetails;
    
    private boolean isQuotaLimited() {
        if (quotaLimited == null) {
            quotaLimited = quotaDetails.isQuotaLimited();
        }
        return quotaLimited;
    }
    
    private long getTimeToQuotaRefreshMillis() {
        if (isQuotaLimited() && timeToQuotaRefreshMillis == null) {
            timeToQuotaRefreshMillis = quotaDetails.getQuotaLimitExpirationMillis() - System.currentTimeMillis();
        }
        return timeToQuotaRefreshMillis;
    }
    
    public String getSystemMessageClass() {
        return isQuotaLimited() ? "system-message" : "";
    }
    
    public String getSystemMessage() {
        if (!isQuotaLimited()) {
            return "";
        }
        
        String timeToQuotaRefresh = Utils.formatMillisecondsToWordsUpToMajorUnits(getTimeToQuotaRefreshMillis()).toLowerCase();
        String timeToQuotaRefreshMinutes = Utils.formatMillisecondsToWordsUpToMinutes(getTimeToQuotaRefreshMillis()).toLowerCase();
        
        boolean showHint = !timeToQuotaRefresh.equals(timeToQuotaRefreshMinutes);
        
        String hintStartTag = showHint ? "<span title='" + timeToQuotaRefreshMinutes + "' class='system-message-underline'>" : "";
        String hintEndTag = showHint ? "</span>" : "";
        
        return "We're sorry, but Ping Service is out of free quota limits right now. " +
               "Next quota refresh will be in " + hintStartTag + timeToQuotaRefresh + hintEndTag + ".";
    }
    
    private void operationSucceeded(String message) {
        this.message = message;
        this.messageColor = "green";
    }
    
    public void operationFailed(String message, Exception... e) {
        this.message = message;
        this.messageColor = "red";
        if (e != null) {
            logger.error(message, e);
        }
    }
    
    private int counter = 0;
    
    @Component(id="grid")
    private Grid grid;
    
    @BeforeRenderTemplate
    void beforeRender() {
        if (grid.getSortModel().getSortConstraints().isEmpty()) {
            //  ascending
            grid.getSortModel().updateSort("titleFriendly");
        }
    }
    
    @Inject private BeanModelSource beanModelSource;
    @Inject private Messages messages;
    
    public BeanModel<?> getModel() {
        BeanModel<?> beanModel = beanModelSource.createDisplayModel(Job.class, messages);

        beanModel.add("SN", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ++counter;
            }
        }).sortable(false);

        beanModel.add("actions", null);
        
        beanModel.exclude(
                "estimatedSerializedSize",
                "receiveNotifications",
                "pingURL", 
                "createdAt", 
                "lastBackupTimestamp", 
                "statusCounter", 
                "statusCounterFriendlyShort", 
                "recentAvailabilityPercentFriendly", 
                "totalAvailabilityPercentFriendly", 
                "googleIOException", 
                "totalStatusCounter", 
                "totalSuccessStatusCounter", 
                "previousStatusCounter", 
                "totalStatusCounterFriendly", 
                "totalSuccessStatusCounterFriendly", 
                "previousStatusCounterFriendly", 
                "title", 
                "shortenURL", 
                "lastPingFailed", 
                "responseEncoding", 
                "validatingRegexp", 
                "validatingHttpCode", 
                "lastPingDetails", 
                "reportEmail", 
                "lastPingResult", 
                "usesValidatingRegexp", 
                "usesValidatingHttpCode", 
                "lastPingTimestamp", 
                "statusCounterFriendly", 
                "receiveBackups", 
                "validationSummary",
                "resultsCount",
                "modifiedAt",
                "modifiedBy",
                "healthStatus",
                "suspended",
                "suspendReason",
                "suspendedAt",
                "suspendedBy",
                "lastPingWasTooLongAgo",
                "scheduledBy", 
                "scheduleName");

        beanModel.reorder(
                        "SN", 
                        "titleFriendly", 
                        "cronString", 
                        "lastPingSummary", 
                        "upDownTimeInMinutes", 
                        "recentAvailabilityPercent", 
                        "totalAvailabilityPercent", 
                        "actions");
        
        return beanModel;
    }
    
    public void onActionFromUdpateQuotas() throws URISyntaxException {
        GAEHelper.addTaskNonTransactional(QueueFactory.getQueue(Application.DEFAULT_QUEUE),
                application.buildTaskUrl(UpdateQuotasTask.class));
    }
}
