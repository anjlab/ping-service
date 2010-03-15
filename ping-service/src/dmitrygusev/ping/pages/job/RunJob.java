package dmitrygusev.ping.pages.job;

import static com.google.appengine.api.datastore.KeyFactory.stringToKey;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.dao.JobDAO;

public class RunJob {

	private static final Logger logger = LoggerFactory.getLogger(RunJob.class);
	
	@Inject
	private Request request;
	
	@Inject
	private Application application;
	
	@Inject
	private JobDAO jobDAO;
	
	public void onActivate() {
		String jobKey = request.getParameter("key");
		
		logger.debug("Running job: " + stringToKey(jobKey).toString());
		
		try {
			Job job = jobDAO.find(stringToKey(jobKey));
		
			if (job != null) {
				application.runJob(job);
			}
		} catch (Exception e) {
			//	Prevent to run job once again on failure
			logger.error("Error running job: " + e);
		}
	}
	
}
