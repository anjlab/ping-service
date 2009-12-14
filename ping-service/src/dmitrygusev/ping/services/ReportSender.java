package dmitrygusev.ping.services;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.job.EditJob;

public class ReportSender {

	private static final Logger logger = Logger.getLogger(ReportSender.class);
	
	private RequestGlobals requestGlobals;
	
	public ReportSender(RequestGlobals requestGlobals) {
		this.requestGlobals = requestGlobals;
	}

	public void sendReport(Job job, PageRenderLinkSource linkSource) {
		Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("ping.service.notify@gmail.com", "Ping Service Notifier"));
            msg.addRecipient(Message.RecipientType.TO,
                             new InternetAddress(job.getReportEmail()));
            
            msg.setSubject(
            		job.isLastPingFailed() 
            			? job.getTitleFriendly() + " is down" 
            			: job.getTitleFriendly() + " is up again");

            StringBuffer body = new StringBuffer();
            
	        body.append("Job results for URL: ");
	        body.append(job.getPingURL());
	        body.append("\n\nYou can view/edit job settings at: ");
	        body.append(
	        		getBaseAddress() +
	        		linkSource.createPageRenderLinkWithContext(
	        				EditJob.class, job.getKey().getParent().getId(), job.getKey().getId()));

	        body.append("\n\nYour ");
	        body.append(job.isLastPingFailed() ? "up" : "down");
	        body.append("time status counter was: ");
	        body.append(job.getPreviousStatusCounter());
	        
	        body.append("\n\nDetailed report:\n\n");

	        if (job.isGoogleIOException()) {
	        	body.append("Your server didn't respond in 10 seconds." +
	        			   "\nWe can't wait longer: http://code.google.com/intl/en/appengine/docs/java/urlfetch/overview.html#Requests\n\n");
	        }
	        
	        body.append(job.getLastPingDetails());
	        
            msg.setText(body.toString());
            
            Transport.send(msg);
        } catch (Exception e) {
            logger.error("Error sending email on job " + job.getKey() + ":" + e);
        }
	}

	private String getBaseAddress() {
		HttpServletRequest request = requestGlobals.getHTTPServletRequest();
		
		String baseAddr = request.getScheme() + "://" + request.getServerName() 
			 + (request.getLocalPort() == 0 ? "" : ":" + request.getLocalPort());
		
		return baseAddr;
	}

}
