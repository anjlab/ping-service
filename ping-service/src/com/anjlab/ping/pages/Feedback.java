package com.anjlab.ping.pages;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;

import com.anjlab.ping.services.GAEHelper;
import com.anjlab.ping.services.Mailer;
import com.anjlab.tapestry5.Utils;


public class Feedback {

    @Property
    private String message;
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
        
        if (!Utils.isNullOrEmpty(subject)) {
            this.note = "Please, provide more information so that we can help you";
        }
        if (gaeHelper.getUserPrincipal() == null) {
            String replyToReminder = "specify your email address in the message text in case you want us to contact you.";
            
            if (this.note != null) {
                this.note += ", and don't forget to " + replyToReminder;
            } else {
                this.note = "Please, " + replyToReminder;
            }
        } else {
            this.note += ".";
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
