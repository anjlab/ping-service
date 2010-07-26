package dmitrygusev.ping.pages.job;


import java.util.Date;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.Index;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.GAEHelper;
import dmitrygusev.ping.services.Utils;

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
            application.createJob(job);

            return index;
        } catch (Exception e) {
            message = e.getMessage();
            logger.error("Error creating job", e);
        }
        return null;
    }
}
