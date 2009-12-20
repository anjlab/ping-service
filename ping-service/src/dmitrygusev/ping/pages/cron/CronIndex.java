package dmitrygusev.ping.pages.cron;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;

import dmitrygusev.ping.services.Application;
import dmitrygusev.tapestry5.Utils;

public class CronIndex {

	@Inject
	private Application application;

	@Inject
	private Request request;
	
	@Inject
	private PageRenderLinkSource linkSource;
	
	public void onActivate() {
		String cronString = request.getParameter("schedule");
		
		if (! Utils.isNullOrEmpty(cronString)) {
			application.runJobs(cronString, linkSource);
		}
	}
	
}
