package dmitrygusev.ping.services;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class Mailer {

	private static final Logger logger = Logger.getLogger(Mailer.class);
	
	public void sendMail(String from, String to, String subject, String message) {
		Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        Message msg = new MimeMessage(session);
        
        try {
	        msg.setFrom(new InternetAddress(from, from.equals("ping.service.notify@gmail.com") ? "Ping Service Notifier" : null));
	        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
	        msg.setSubject(subject);
	        msg.setText(message);
	        Transport.send(msg);
        } catch (Exception e) {
        	logger.error(
        			"Error sending mail:\n"+
        			"From: " + from + 
        			"; To: " + to + 
        			"; Subject: " + subject + 
        			"; Message: " + message + "\n\n" + 
        			e);
        }
	}
}
