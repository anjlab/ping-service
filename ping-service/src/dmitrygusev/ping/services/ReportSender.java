package dmitrygusev.ping.services;

import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.job.EditJob;

public class ReportSender {

	private RequestGlobals requestGlobals;
	private Mailer mailer;
	
	public ReportSender(RequestGlobals requestGlobals, Mailer mailer) {
		this.requestGlobals = requestGlobals;
		this.mailer = mailer;
	}

	public void sendReport(Job job, PageRenderLinkSource linkSource) {
		String from = "ping.service.notify@gmail.com";
		String to = job.getReportEmail();
        String subject = job.isLastPingFailed() ? job.getTitleFriendly() + " is down" : job.getTitleFriendly() + " is up again";

		StringBuffer body = new StringBuffer();
        
        body.append("Job results for URL: ");
        body.append(job.getPingURL());
        body.append("\n\nYou can view/edit job settings at: ");
        body.append(buildJobURL(job, linkSource));

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
		
		mailer.sendMail(from, to, subject, message);
	}

	private String buildJobURL(Job job, PageRenderLinkSource linkSource) {
		String url = getBaseAddress() +
						linkSource.createPageRenderLinkWithContext(
								EditJob.class, job.getKey().getParent().getId(), job.getKey().getId());
		
		if (url.contains(";jsessionid=")) {
			url = url.substring(0, url.indexOf(";jsessionid="));
		}
		
		return url;
	}

	private String getBaseAddress() {
		HttpServletRequest request = requestGlobals.getHTTPServletRequest();
		
		String baseAddr = request.getScheme() + "://" + request.getServerName() 
			 + (request.getLocalPort() == 0 ? "" : ":" + request.getLocalPort());
		
		return baseAddr;
	}

}
