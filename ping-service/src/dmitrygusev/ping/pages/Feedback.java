package dmitrygusev.ping.pages;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;

import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.security.GAEHelper;
import dmitrygusev.tapestry5.Utils;

public class Feedback {

	@Property
	private String message;
	
	@AfterRender
	public void cleanup() {
		message = null;
		thanks = null;
	}
	
	@Property
	@Persist
	@SuppressWarnings("unused")
	private String thanks;
	
	@Inject
	private Request request;
	
	public void onActivate() {
		String subject = request.getParameter("subject");
		this.message = Utils.isNullOrEmpty(subject) ? null : subject + "\n\n";
	}
	
	@Inject
	private GAEHelper gaeHelper;
	
	@Inject
	private Mailer mailer;
	
	public void onSuccess() {
		String subject = "Ping Service Feedback";
		
		mailer.sendMail(
				gaeHelper.getUserPrincipal() == null ? "dmitry.gusev@gmail.com"
						: gaeHelper.getUserPrincipal().getName(),
				"dmitry.gusev@gmail.com", subject, message);
		
		thanks = "Thanks for sharing your feedback!";
	}
}
