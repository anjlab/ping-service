package dmitrygusev.ping.pages.cron;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;

import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Utils;

public class CronIndex {

	@Inject
	private Application application;

	@Inject
	private Request request;
	
	public void onActivate() {
		String cronString = request.getParameter("schedule");
		
		if (Utils.isCronStringSupported(cronString)) {
			application.enqueueJobs(cronString);
		}
	}
	
}
