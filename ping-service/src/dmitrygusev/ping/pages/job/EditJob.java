package dmitrygusev.ping.pages.job;

import java.util.Date;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.apphosting.api.DeadlineExceededException;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.Index;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.NotAuthorizedException;
import dmitrygusev.ping.services.Utils;

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
			application.updateJob(job, true, false);
		} catch (NotAuthorizedException e) {
			index.setExceptionMessage(e.getMessage());
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
	
	public Index onActivate(Long scheduleId, Long jobId) {
		try {
			job = application.findJob(scheduleId, jobId);
			
			if (job == null) {
				index.setExceptionMessage("Job not found");
				return index;
			}
		} catch (Exception e) {
			index.setExceptionMessage(e.getMessage());
            logger.error("Error activating page", e);
			return index;
		}
		
		return null;
	}
	
	public Long[] onPassivate() {
		if (job != null) {
			return new Long[]
			                {
								job.getKey().getParent().getId(), 
								job.getKey().getId()
							};
		}
		return null;
	}
	
	public String getLastPingSummary() {
		return job.getLastPingSummary();
	}
	
	public String getLastPingTimestamp() {
	    Date timestamp = job.getLastPingTimestamp();
        
        return timestamp != null 
             ? Application.formatDate(timestamp, Application.DATETIME_FORMAT, application.getTimeZone()) 
             : "N/A";
	}
	
	public String getCreatedAtFormatted() {
	    return application.formatDate(job.getCreatedAt());
	}
}
