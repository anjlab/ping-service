package dmitrygusev.ping.pages.job;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.Index;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.NotAuthorizedException;
import dmitrygusev.ping.services.Utils;

@SuppressWarnings("unused")
public class EditJob {

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
			application.updateJob(job, true);
		} catch (NotAuthorizedException e) {
			index.setExceptionMessage(e.getMessage());
		} catch (Exception e) {
			message = e.getMessage();
			resultPage = null;
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
		return application.getLastPingSummary(job);
	}
}
