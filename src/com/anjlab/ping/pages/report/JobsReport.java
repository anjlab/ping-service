package com.anjlab.ping.pages.report;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tapestry5.annotations.BeforeRenderTemplate;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.services.Application;
import com.anjlab.ping.services.Utils;
import com.anjlab.ping.services.dao.AccountDAO;
import com.anjlab.ping.services.dao.JobDAO;
import com.anjlab.tapestry5.AbstractReadonlyPropertyConduit;


public class JobsReport {

    @Property
    private Job job;
    @Inject
    private AccountDAO accountDAO;
    @Inject
    private JobDAO jobDAO;
    
    private List<Job> jobs;
    
    public List<Job> getJobs() {
        if (jobs == null) {
//          java.lang.UnsupportedOperationException: This operation is not supported on Query Results
//          at org.datanucleus.store.query.AbstractQueryResult.contains(AbstractQueryResult.java:250)
//          at java.util.AbstractCollection.removeAll(AbstractCollection.java:353)
            List<Job> dbJobs = jobDAO.getAll();
            jobs = new ArrayList<Job>();
            jobs.addAll(dbJobs);
        }
        return jobs;
    }
    
    private int counter = 0;
    
    @Component(id="grid")
    private Grid grid;
    
    @BeforeRenderTemplate
    void beforeRender() {
        if (grid.getSortModel().getSortConstraints().isEmpty()) {
            //  ascending
            grid.getSortModel().updateSort("createdAt");
            //  descending
            grid.getSortModel().updateSort("createdAt");
        }
    }
    
    @Inject private BeanModelSource beanModelSource;
    @Inject private Messages messages;
    
    public BeanModel<?> getModel() {
        BeanModel<?> beanModel = beanModelSource.createDisplayModel(Job.class, messages);

        beanModel.exclude("createdAt",
                          "lastBackupTimestamp",
                          "cronString",
                          "recentAvailabilityPercent", 
                          "totalAvailabilityPercent");

        beanModel.add("recentAvailabilityPercent", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ((Job) instance).getRecentAvailabilityPercentFriendly().replace("%", "");
            }
        });
        beanModel.add("totalAvailabilityPercent", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ((Job) instance).getTotalAvailabilityPercentFriendly().replace("%", "");
            }
        });
        beanModel.add("cronString", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                Job job = (Job) instance;
                return job.isSuspended()
                     ? "suspended"
                     : job.getCronString().replace("every ", "")
                                          .replace(" hours", "h")
                                          .replace(" minutes", "m");
            }
        });
        beanModel.add("lastBackupTimestamp", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                Date date = ((Job) instance).getLastBackupTimestamp();
                return date == null ? null : Application.DATETIME_FORMAT.format(date);
            }
        });
        beanModel.add("createdAt", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                Date date = ((Job) instance).getCreatedAt();
                return date == null ? null : Application.DATETIME_FORMAT.format(date);
            }
        });
        beanModel.add("userLastVisit", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                Date date = accountDAO.getAccount(((Job) instance).getScheduleName()).getLastVisitDate();
                return date == null ? null : Application.DATETIME_FORMAT.format(date);
            }
        });
        beanModel.add("SN", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ++counter;
            }
        }).sortable(false);

        beanModel.add("analytics", null);
        beanModel.add("details", null);
        
        beanModel.exclude(
                "lastPingDetails",
                "lastPingFailed",
                "lastPingResult",
                "usesValidatingRegexp",
                "usesValidatingHttpCode",
                "validatingRegexp",
                "validatingHttpCode",
                "responseEncoding",
                "shortenUrl",
                "title",
                "pingURL",
                "statusCounter",
                "statusCounterFriendlyShort",
                "upDownTimeInMinutes",
                "previousStatusCounter",
                "totalStatusCounter",
                "totalSuccessStatusCounter",
                "totalAvailabilityPercentFriendly",
                "recentAvailabilityPercentFriendly",
                "lastPingTimestamp",
                "validationSummary",
                "reportEmail",
                "statusCounterFriendly",
                "previousStatusCounterFriendly",
                "googleIOException",
                "totalStatusCounterFriendly",
                "totalSuccessStatusCounterFriendly",
                "healthStatus",
                "lastPingWasTooLongAgo",
                "suspended",
                "suspendReason",
                "suspendedBy",
                "suspendedAt",
                "modifiedAt",
                "modifiedBy",
                "scheduleName",
                "estimatedSerializedSize");

        beanModel.reorder(
                        "SN", 
                        "cronString",
                        "titleFriendly", 
                        "lastPingSummary",
                        "recentAvailabilityPercent",
                        "totalAvailabilityPercent",
                        "scheduledBy",
                        "userLastVisit",
                        "createdAt",
                        "resultsCount");
        
        return beanModel;
    }
    
    public Long[] getJobContext() {
        return Utils.createJobContext(job);
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

    public String getLastPingTimestamp() {
        return Utils.formatTime(job.getLastPingTimestamp());
    }

    public String getLastBackupTimestampFriendly() {
        return Utils.getTimeAgoUpToDays(job.getLastBackupTimestamp());
    }
    
    public String getCreatedAtFriendly() {
        return Utils.getTimeAgoUpToDays(job.getCreatedAt());
    }
    
    public String getUserLastVisitFriendly() {
        return Utils.getTimeAgoUpToDays(accountDAO.getAccount(job.getScheduleName()).getLastVisitDate());
    }

    public String getLastBackupTimestamp() {
        return Utils.formatTime(job.getLastBackupTimestamp());
    }
    
    public String getCreatedAt() {
        return Utils.formatTime(job.getCreatedAt());
    }
    
    public String getUserLastVisit() {
        return Utils.formatTime(accountDAO.getAccount(job.getScheduleName()).getLastVisitDate());
    }
    
    private Map<String, Integer> countersByAccount;
    public Map<String, Integer> getCountersByAccount() {
        if (countersByAccount == null) {
            countersByAccount = new HashMap<String, Integer>();
            for (Job job : getJobs()) {
                String account = job.getScheduledBy();
                Integer count = countersByAccount.get(account);
                if (count == null) {
                    count = 0;
                }
                count++;
                countersByAccount.put(account, count);
            }
        }
        return countersByAccount;
    }
    
    private Map<String, Integer> countersByCronString;
    public Map<String, Integer> getCountersByCronString() {
        if (countersByCronString == null) {
            countersByCronString = new HashMap<String, Integer>();
            for (Job job : getJobs()) {
                String cronString = job.getCronString();
                Integer count = countersByCronString.get(cronString);
                if (count == null) {
                    count = 0;
                }
                count++;
                countersByCronString.put(cronString, count);
            }
        }
        return countersByCronString;
    }
    
    private Map<String, Integer> countersByNumberOfJobs;
    public Map<String, Integer> getCountersByNumberOfJobs() {
        if (countersByNumberOfJobs == null) {
            countersByNumberOfJobs = new HashMap<String, Integer>();
            for (Entry<?, ?> entry : getCountersByAccount().entrySet()) {
                String key = entry.getValue().toString();
                Integer count = countersByNumberOfJobs.get(key);
                if (count == null) {
                    count = 0;
                }
                count++;
                countersByNumberOfJobs.put(key, count);
            }
        }
        return countersByNumberOfJobs;
    }

}
