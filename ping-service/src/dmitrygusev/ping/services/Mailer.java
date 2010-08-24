package dmitrygusev.ping.services;

import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mailer {

    public static final String PING_SERVICE_NOTIFY_GMAIL_COM = "ping.service.notify@gmail.com";
    public static final String DMITRY_GUSEV_GMAIL_COM = "dmitry.gusev@gmail.com";
    
    private static final Logger logger = LoggerFactory.getLogger(Mailer.class);
    
    public void sendSystemMessageToDeveloper(String subject, String message, MimeBodyPart... attachments) {
        sendMail(PING_SERVICE_NOTIFY_GMAIL_COM, DMITRY_GUSEV_GMAIL_COM, subject, message, attachments);
    }
    
    public void sendMail(String from, String to, String subject, String message, MimeBodyPart... attachments) {
        sendMail2("text/plain", from, to, subject, message, attachments);
    }

    public void sendMail2(String mimeType, String from, String to, String subject, String text, MimeBodyPart... attachments) {
        if (Utils.isNullOrEmpty(to)) {
            logger.warn("mail can't be delivered to (recipient == null):\n{}", text);
            return;
        }
        
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        Message message = new MimeMessage(session);
        
        try {
            Multipart mp = new MimeMultipart("mixed");

            BodyPart content = new MimeBodyPart();
            content.setContent(text, mimeType);
            mp.addBodyPart(content);

            if (attachments != null && attachments.length > 0) {
                for (MimeBodyPart attachment : attachments) {
                    mp.addBodyPart(attachment);
                }
            }

            message.setFrom(new InternetAddress(from, from.equals(PING_SERVICE_NOTIFY_GMAIL_COM) ? "Ping Service Notifier" : null));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setContent(mp);
            message.saveChanges();
            
            Transport.send(message);
        } catch (Exception e) {
            logger.error(
                    "Error sending mail:"+
                    "\n\tFrom: " + from + 
                    "\n\tTo: " + to + 
                    "\n\tSubject: " + subject + 
                    "\n\tMessage:\n\n" + (Utils.isNullOrEmpty(text) ? "-" : text.substring(0, 100) + "..."), 
                    e);
        }
    }
}
