package dmitrygusev.ping.pages;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;

import dmitrygusev.ping.services.GAEHelper;
import dmitrygusev.ping.services.Mailer;
import dmitrygusev.tapestry5.Utils;

public class Feedback {

    @Property
    private String message;
    @SuppressWarnings("unused")
    @Property
    private String note;
    
    @AfterRender
    public void cleanup() {
        message = null;
        thanks = null;
        note = null;
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
        
        if (gaeHelper.getUserPrincipal() == null) {
            this.note = "Please, provide your email address in the message so we could contact you.";
        }
    }
    
    @Inject
    private GAEHelper gaeHelper;
    
    @Inject
    private Mailer mailer;
    
    public void onSuccess() {
        String subject = "Ping Service Feedback";
        
        mailer.sendMail(
                gaeHelper.getUserPrincipal() == null ? Mailer.PING_SERVICE_NOTIFY_GMAIL_COM
                        : gaeHelper.getUserPrincipal().getName(),
                Mailer.DMITRY_GUSEV_GMAIL_COM, subject, message);
        
        thanks = "Thanks for sharing your feedback!";
    }
}
