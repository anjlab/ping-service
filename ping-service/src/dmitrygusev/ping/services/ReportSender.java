package dmitrygusev.ping.services;


import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.job.Analytics;

public class ReportSender {

	private static final Logger logger = LoggerFactory.getLogger(ReportSender.class);
	
	private Mailer mailer;
	
	public ReportSender(Mailer mailer) {
		this.mailer = mailer;
	}

	public void sendReport(Job job, Application application) throws URISyntaxException {
		String from = Mailer.PING_SERVICE_NOTIFY_GMAIL_COM;
		String to = job.getReportEmail();
		
        String subject = job.isLastPingFailed() ? job.getTitleFriendly() + " is down" : job.getTitleFriendly() + " is up again";

		StringBuffer body = new StringBuffer();
        
        body.append("Job results for URL: ");
        body.append(job.getPingURL());
        body.append("\n\nYou can analyze URL performance at: ");
        
		body.append(application.getJobUrl(job, Analytics.class));

        body.append("\n\nYour ");
        body.append(job.isLastPingFailed() ? "up" : "down");
        body.append("time status counter was: ");
        body.append(job.getPreviousStatusCounterFriendly());
        
        body.append("\n\nDetailed report:\n\n");

        if (job.isGoogleIOException()) {
        	body.append("Your server didn't respond in 10 seconds." +
        			   "\nWe can't wait longer: http://code.google.com/intl/en/appengine/docs/java/urlfetch/overview.html#Requests\n\n");
        }
        
        body.append(job.getLastPingDetails());
        
		String message = body.toString();

		if (Utils.isNullOrEmpty(to)) {
			logger.warn("job's reportEmail property not specified, report can't be sent:\n{}", message);
		} else {
			mailer.sendMail(from, to, subject, message);
		}
	}

}
