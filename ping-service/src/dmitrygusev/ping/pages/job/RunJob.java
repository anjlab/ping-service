package dmitrygusev.ping.pages.job;

import static com.google.appengine.api.datastore.KeyFactory.stringToKey;

import org.apache.log4j.Logger;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.dao.JobDAO;

public class RunJob {

	private static final Logger logger = Logger.getLogger(RunJob.class);
	
	@Inject
	private Request request;
	
	@Inject
	private Application application;
	
	@Inject
	private JobDAO jobDAO;
	
	@Inject
	private PageRenderLinkSource linkSource;
	
	public void onActivate() {
		String jobKey = request.getParameter("key");
		
		logger.debug("Running job: " + jobKey);
		
		try {
			Job job = jobDAO.find(stringToKey(jobKey));
		
			if (job != null) {
				application.runJob(linkSource, job);
			}
		} catch (Exception e) {
			//	Prevent to run job once again on failure
			logger.error("Error running job: " + e);
		}
	}
	
}
