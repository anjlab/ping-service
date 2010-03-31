package dmitrygusev.ping.pages.cron;

import java.net.URISyntaxException;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.services.AppModule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Utils;

@Meta(AppModule.NO_MARKUP)
public class CronIndex {

	private static final Logger logger = LoggerFactory.getLogger(CronIndex.class);

	@Inject
	private Application application;

	@Inject
	private Request request;
	
	public void onActivate() {
		String cronString = request.getParameter("schedule");
		
		if (Utils.isCronStringSupported(cronString)) {
			try {
				application.enqueueJobs(cronString);
			} catch (URISyntaxException e) {
				logger.error("Error enqueueing jobs", e);
			}
		}
	}
	
}
