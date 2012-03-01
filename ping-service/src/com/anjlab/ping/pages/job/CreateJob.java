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
import com.anjlab.ping.services.GAEHelper;
import com.anjlab.ping.services.Utils;
import com.google.appengine.api.users.UserServiceFactory;


@SuppressWarnings("unused")
public class CreateJob {

    private static final Logger logger = LoggerFactory.getLogger(CreateJob.class);
    private Job job;
    
    public Job getJob() {
        if (job == null) {
            job = getDefaultJob();
        }
        return job;
    }
    
    public void setJob(Job job) {
        this.job = job;
    }

    @Inject
    private GAEHelper gaeHelper;
    
    private Job getDefaultJob() {
        Job result = new Job();
        
        result.setPingURL("http://");
        result.setReportEmail(gaeHelper.getUserPrincipal().getName());
        result.setUsesValidatingHttpCode(true);
        result.setValidatingHttpCode(-200);
        result.setCronString("every 1 hour");
        result.setResponseEncoding("UTF-8");
        result.setCreatedAt(new Date());
        
        return result;
    }
    
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
    }

    @Inject
    private Application application;
    
    public Index onSuccess() {
        try {
            
//            if (UserServiceFactory.getUserService().isUserAdmin()) {
                application.createJob(job);
//            } else {
//                index.operationFailed("Creation of new jobs temporarily unavailable.");
//            }

            return index;
        } catch (Exception e) {
            message = e.getMessage();
            logger.error("Error creating job", e);
        }
        return null;
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
