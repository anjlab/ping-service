package com.anjlab.ping.pages.job;

import java.util.Date;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.Select;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.pages.Index;
import com.anjlab.ping.services.Application;
import com.anjlab.ping.services.NotAuthorizedException;
import com.anjlab.ping.services.Utils;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.DeadlineExceededException;


@SuppressWarnings("unused")
public class EditJob {

    private static final Logger logger = LoggerFactory.getLogger(EditJob.class);
    
    @Property
    private Job job;

    @Property
    private final String httpCodesModel = Utils.getHttpCodesModel();
    
    @Property
    private final String cronStringModel = Utils.getCronStringModel();
    
    @InjectPage
    private Index index;
    
    @Property
    @Persist
    private String message;
    
    @AfterRender
    public void cleanup() {
        message = null;
        job = null;
    }
    
    @Inject
    private Application application;
    
    public Object onSuccess() {
        Object resultPage = index;
        
        try {
//            if (UserServiceFactory.getUserService().isUserAdmin()) {
                job.fireModified(application.getUserAccount().getEmail());
                application.updateJob(job, true, false);
//            } else {
//                index.operationFailed("Job editing temporarily unavailable.");
//            }

        } catch (NotAuthorizedException e) {
            index.operationFailed("Not authorized", e);
        } catch (Exception e) {
            message = e.getMessage();
            if (message == null) {
                message = e.getClass().getSimpleName();
            }
            message = "Error updating job";
            resultPage = null;

            logger.error(message, e);
        }

        return resultPage;
    }
    
    public Index onActivate(Long jobId) {
        try {
            job = application.findJob(jobId);
            
            if (job == null) {
                index.operationFailed("Job not found");
                return index;
            }
        } catch (Exception e) {
            index.operationFailed(e.getMessage(), e);
            return index;
        }
        
        return null;
    }
    
    public Long[] onPassivate() {
        if (job != null) {
            return Utils.createJobContext(job);
        }
        return null;
    }
    
    public String getLastPingSummary() {
        return job.getLastPingSummary();
    }
    
    public String getLastPingTimestamp() {
        Date timestamp = job.getLastPingTimestamp();
        
        return timestamp != null 
             ? application.formatDate(timestamp) 
             : "N/A";
    }
    
    public String getCreatedAtFormatted() {
        return application.formatDate(job.getCreatedAt());
    }
    
    public String getScheduleName() {
        return Utils.isNullOrEmpty(job.getScheduleName()) ? "<Unknown>" : job.getScheduleName();
    }
    
    public Long[] getJobContext() {
        return Utils.createJobContext(job);
    }

    @Inject
    private Messages messages;
    
    @Component(id="jobForm")
    private Form jobForm;
    
    @Component(id="cronString")
    private Select cronStringField;
    
    public void onValidateFromJobForm() {
        if (application.isQuotaLimitsForCreateOrUpdateExceeded(job)) {
            jobForm.recordError(cronStringField, messages.get("account-cron-string-quota-limit"));
        }
    }

}
