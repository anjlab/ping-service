package dmitrygusev.ping.filters;

import static com.google.appengine.api.datastore.KeyFactory.stringToKey;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.pages.job.EditJob;
import dmitrygusev.ping.pages.task.MailJobResultsTask;
import dmitrygusev.ping.pages.task.RunJobTask;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.JobResultCSVExporter;
import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.Utils;

public class MailJobResultsFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(MailJobResultsTask.class);
    
    @Override
    protected void processRequest() 
            throws Exception
    {
        String encodedJobKey = globals.getHTTPServletRequest().getParameter(RunJobTask.JOB_KEY_PARAMETER_NAME);

        if (Utils.isNullOrEmpty(encodedJobKey)) {
            return;
        }
        
        Key key = stringToKey(encodedJobKey);
        
        logger.debug("Running mail job: {}", key);

        Job job = jobDAO.find(key);
    
        if (job == null) {
            return;
        }

        List<JobResult> resultsBuffer = new ArrayList<JobResult>(Application.DEFAULT_NUMBER_OF_JOB_RESULTS);
        
        resultsBuffer.addAll(job.removeJobResultsExceptRecent(Application.DEFAULT_NUMBER_OF_JOB_RESULTS));

        if (resultsBuffer.size() > 0) {
            if (application.updateJob(job, false, true)) {

                sendResultsByMail(job, resultsBuffer, job.getReportEmail());
    
                application.getMailer().sendSystemMessageToDeveloper(
                            "Debug: Backup Completed for Job", 
                            "Job: " + job.getTitleFriendly() + " / " + job.getPingURL() + " / " + job.getKey() +
                            "\nTotal files: 1" +
                            "\nTotal records: " + resultsBuffer.size());
            } else {
                logger.error("Error saving job. Backup will not be sent to user this time.");
            }
        }
    }
    
    public void sendResultsByMail(Job job, List<JobResult> results, String reportRecipient) throws MessagingException, IOException, URISyntaxException {
        JobResult firstResult = (JobResult) results.get(0);
        JobResult lastResult = (JobResult) results.get(results.size() - 1);
        
        String subject = "Statistics Backup for " + job.getTitleFriendly();

        String timeZoneCity = null;
        TimeZone timeZone = Application.getTimeZone(timeZoneCity);

        StringBuilder builder = new StringBuilder();
        builder.append("Job results for period: ");
        builder.append(Application.formatDate(firstResult.getTimestamp(), timeZoneCity, Application.DATETIME_FORMAT));
        builder.append(" - ");
        builder.append(Application.formatDate(lastResult.getTimestamp(), timeZoneCity, Application.DATETIME_FORMAT));
        builder.append(" (");
        builder.append(Utils.formatTimeMillis(lastResult.getTimestamp().getTime() - firstResult.getTimestamp().getTime()));
        builder.append(")");
        builder.append("\n# of records: ");
        builder.append(results.size());
        builder.append("\nTimeZone: ");
        builder.append(timeZone.getDisplayName());
        builder.append(" (");
        builder.append(timeZone.getID());
        builder.append(")");
        
        builder.append("\n\tAvailability percent for the period: ");
        builder.append(Utils.formatPercent(Utils.calculateAvailabilityPercent(results)));
        
        builder.append("\n\n----"); 
        builder.append("\nYou can disable receiving statistics backups for the job here: ");
        String editJobLink = application.getJobUrl(job, EditJob.class);
        builder.append(editJobLink);
        builder.append("\n\nNote:");
        builder.append("\nAutomatic Backups is a beta function, please use our feedback form (http://ping-service.appspot.com/feedback) to provide a feedback on it.");
        builder.append("\nYou will get approximately one email per week per job depending on job's cron string.");
        builder.append("\nOnce you received an email with the statistics, this data will be deleted from Ping Service database.");
        builder.append("\nPing Service will only store ");
        builder.append(Application.DEFAULT_NUMBER_OF_JOB_RESULTS);
        builder.append(" latest ping results per job.");
        builder.append("\nWe're doing this to keep Ping Service free, since we're running out of free quota limit of Google App Engine infrastructure.");
        builder.append("\nWe're sorry for any inconvenience you might get from this email.");
        builder.append("\nThank you for understanding.");
        
        String message = builder.toString();
        
        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setFileName(
                "job-" 
                + job.getKey().getParent().getId() + "-" +
                + job.getKey().getId() + "-results-" 
                + Application.formatDateForFileName(firstResult.getTimestamp(), timeZoneCity) + "-" 
                + Application.formatDateForFileName(lastResult.getTimestamp(), timeZoneCity) + ".txt");
        
        byte[] export = JobResultCSVExporter.export(timeZone, (List<JobResult>)results);
        
        attachment.setContent(new String(export), "text/plain");
        
        application.getMailer().sendMail(Mailer.PING_SERVICE_NOTIFY_GMAIL_COM, reportRecipient, subject, message, attachment);
    }
    
}
