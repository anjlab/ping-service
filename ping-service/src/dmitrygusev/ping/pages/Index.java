package dmitrygusev.ping.pages;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import net.sf.jsr107cache.Cache;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Ref;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.tapestry5.AbstractReadonlyPropertyConduit;

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

    @AfterRender
    public void cleanup() {
        message = null;
        defaultSchedule = null;
        userAccount = null;
        account = null;
        grantedEmail = null;
        job = null;
        messageColor = null;
    }
    
    @Property
    private Job job;
    
    public String getLastPingTimestamp() {
        Date timestamp = job.getLastPingTimestamp();
        
        return timestamp != null ? application.formatDate(timestamp) : "N/A";
    }
    
    public String getSummaryStatusCssClass() {
        if (job.isGoogleIOException()) {
            return "status-warning";
        }
        if (job.isLastPingFailed()) {
            return "status-error";
        }
        if (job.getTotalStatusCounter() == 0) {
            return "status-na";
        }
        return "status-okay";
    }
    
    public Long[] getJobContext() {
        return Utils.createJobContext(job);
    }

    public void onActionFromDeleteJob(Long scheduleId, Long jobId) {
        try {
            application.deleteJob(scheduleId, jobId, true);
        } catch (Exception e) {
            logger.error("Error deleting job", e);
            message = "Error deleting job";
            messageColor = "red";
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
            userAccount = application.getUserAccount();
        }
        return userAccount;
    }
    
    @Property
    private String grantedEmail;
    
    @Property
    private boolean readOnly;
    
    public void onSuccessFromGrantAccessTo() {
        try {
            application.grantAccess(grantedEmail, getDefaultSchedule(),
                    readOnly ? Ref.ACCESS_TYPE_READONLY : Ref.ACCESS_TYPE_FULL);
        } catch (Exception e) {
            message = e.getMessage();
            messageColor = "red";
        }
    }
    
    private Schedule defaultSchedule;
    
    private Schedule getDefaultSchedule() {
        if (defaultSchedule == null) {
            defaultSchedule = application.getDefaultSchedule(); 
        }
        return defaultSchedule;
    }

    public List<Job> getJobs() {
        return application.getAvailableJobs();
    }
    
    @Inject
    private Application application;
    
    public List<Account> getAccounts() {
        return application.getAccounts(getDefaultSchedule());
    }
    
    public void onActionFromRemoveAccount(Long accountId) {
        try {
            application.removeAccount(accountId, getDefaultSchedule());
        } catch (Exception e) {
            message = e.getMessage();
            messageColor = "red";
        }
    }

    public void setExceptionMessage(String message) {
        this.message = message;
        messageColor = "red";
    }
    
    public void onActionFromDeleteSchedule() {
//        try {
//            application.delete(getDefaultSchedule());
//        } catch (Exception e) {
//            logger.error("Error deleting schedule", e);
//            message = "Error deleting schedule";
//            messageColor = "red";
//        }
    }

    public boolean isAdmin() {
        UserService userService = UserServiceFactory.getUserService();
        
        return userService.isUserLoggedIn() 
            && userService.isUserAdmin();
    }

    @Inject
    private Cache cache;
    
    public void onActionFromClearCache() throws URISyntaxException {
        cache.clear();
        message = "Cache cleared";
        messageColor = "green";
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
                "resultsCount");

        beanModel.reorder(
                        "SN", 
                        "titleFriendly", 
                        "scheduledBy", 
                        "cronString", 
                        "lastPingSummary", 
                        "upDownTimeInMinutes", 
                        "recentAvailabilityPercent", 
                        "totalAvailabilityPercent", 
                        "actions");
        
        return beanModel;
    }
}
