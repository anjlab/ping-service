package dmitrygusev.ping.pages.task;

import static com.google.appengine.api.datastore.KeyFactory.stringToKey;
import static dmitrygusev.ping.pages.task.BackupAndDeleteOldJobResultsTask.getChunkKeyInCache;
import static dmitrygusev.ping.services.Utils.formatTimeMillis;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.pages.job.EditJob;
import dmitrygusev.ping.services.AppModule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.JobResultCSVExporter;
import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.ping.services.dao.JobDAO;

@Meta(AppModule.NO_MARKUP)
public class MailJobResultsTask {

	private static final int RESULTS_IN_ONE_EMAIL = 10000;

	private static final Logger logger = LoggerFactory.getLogger(MailJobResultsTask.class);
	
	@Inject private Mailer mailer;
	@Inject private Request request;

	public static final String TOTAL_RECORDS_PARAMETER_NAME = "totalRecords";
	public static final String FILE_NUMBER_PARAMETER_NAME = "fileNumber";
	public static final String CHUNK_ID_PARAMETER_NAME = "chunkId";

	private Job job;
	private String taskId;
	private long startTime;
	private long chunkId;
	private long fileNumber = 0;
	private long totalRecords = 0;
	
	@Inject
	private JobDAO jobDAO;
	
	private boolean initTask() {
		String encodedJobKey = request.getParameter(LongRunningQueryTask.JOB_KEY_PARAMETER_NAME);
		
		taskId = request.getParameter(BackupAndDeleteOldJobResultsTask.TASK_ID_PARAMETER_NAME);

		startTime = readLongParameter(LongRunningQueryTask.STARTTIME_PARAMETER_NAME, 0);
		chunkId = readLongParameter(CHUNK_ID_PARAMETER_NAME, BackupAndDeleteOldJobResultsTask.CACHED_RESULTS_FIRST_CHUNK_ID);
		fileNumber = readLongParameter(FILE_NUMBER_PARAMETER_NAME, 0);
		totalRecords = readLongParameter(TOTAL_RECORDS_PARAMETER_NAME, 0);
		
		if (!Utils.isNullOrEmpty(encodedJobKey)) {
			job = jobDAO.find(stringToKey(encodedJobKey));
		}
		
		return !Utils.isNullOrEmpty(taskId) 
			&& job != null;
	}

	private long readLongParameter(String parameterName, long defaultValue) {
		String stringValue = request.getParameter(parameterName);
		return Utils.isNullOrEmpty(stringValue) ? defaultValue : Long.parseLong(stringValue);
	}
	
	@Inject
	private Cache cache;
	
	private boolean taskCompleted;
	
	@SuppressWarnings("unchecked")
	public void onActivate() {
		if (!initTask()) {
			logger.warn("Task initialization failed.");
			return;
		}
		
		if (cache == null) {
			logger.warn("Cache is null.");
			return;
		}
		
		List<JobResult> resultsBuffer = new ArrayList<JobResult>(RESULTS_IN_ONE_EMAIL);
		
		String chunkKey = getChunkKeyInCache(taskId, chunkId);

		boolean reportSent = false;
		
		while (cache.containsKey(chunkKey) && !reportSent) {
			resultsBuffer.addAll((List<JobResult>) cache.get(chunkKey));
			
			if (resultsBuffer.size() >= RESULTS_IN_ONE_EMAIL) {
				
				taskCompleted = !cache.containsKey(getChunkKeyInCache(taskId, chunkId + 1));
				
				sendResults(resultsBuffer);
				resultsBuffer.clear();
				
				reportSent = true;
			}
			
			chunkId++;
			chunkKey = getChunkKeyInCache(taskId, chunkId);
		}
		
		if (reportSent && !taskCompleted) {
			continueTask();
		} else {
			if (resultsBuffer.size() > 0) {
				
				taskCompleted = true;
				
				sendResults(resultsBuffer);
				resultsBuffer.clear();
			}
		}
		
		if (taskCompleted) {
			mailer.sendSystemMessageToDeveloper(
					"Debug: Backup Completed for Job", 
					"Job: " + job.getTitleFriendly() + " / " + job.getKey() +
					"\nTotal files: " + fileNumber +
					"\nTotal records: " + totalRecords +
					"\nTotal time: " + formatTimeMillis(System.currentTimeMillis() - startTime));
		}
	}

	@Inject
	private Application application;
	
	private void continueTask() {
		try {
			logger.debug("Continue taskId {}", taskId);
			
			application.continueMailJobResultsTask(job.getKey(), taskId, startTime, chunkId, totalRecords, fileNumber);
			
		} catch (Exception e) {
			logger.error("Error enqueueing task", e);
		}
	}

	private void sendResults(List<JobResult> resultsBuffer) {
		try {
			sendResultsByMail(resultsBuffer, getReportRecipient());
		} catch (Exception e) {
			logger.error("Error sending backup by email", e);
		}
	}
	
	public void sendResultsByMail(List<JobResult> results, String reportRecipient) throws MessagingException, IOException, URISyntaxException {
		totalRecords += results.size();
		
		JobResult firstResult = (JobResult) results.get(results.size() - 1);
		JobResult lastResult = (JobResult) results.get(0);
		
		String subject = getReportSubject();

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
		builder.append("\nFile #: ");
		builder.append(++fileNumber);
		builder.append("\n# of records: ");
		builder.append(results.size());
		builder.append("\nTimeZone: ");
		builder.append(timeZone.getDisplayName());
		builder.append(" (");
		builder.append(timeZone.getID());
		builder.append(")");
		
		if (taskCompleted) {
			String totalTimeFormatted = dmitrygusev.ping.services.Utils.formatTimeMillis(System.currentTimeMillis() - startTime);
			
			builder.append("\n\nThis is the last email in current backup.");
			builder.append("\n\tTotal # of files: ");
			builder.append(fileNumber);
			builder.append("\n\tTotal # of records: ");
			builder.append(totalRecords);
			builder.append("\n\tTotal time requried: ");
			builder.append(totalTimeFormatted);
		}
		
		builder.append("\n\n----"); 
		builder.append("\nYou can disable receiving statistics backups for the job here: ");
		String editJobLink = application.getJobUrl(job, EditJob.class);
		builder.append(editJobLink);
		builder.append("\n\nNote:");
		builder.append("\nAutomatic Backups is a beta function, please use our feedback form (http://ping-service.appspot.com/feedback) to provide a feedback on it.");
		builder.append("\nFor the first time you may get a few emails at once, this is because we'll try to backup all your statistics."); 
		builder.append("\nDuring next backups you will get approximately one email per week per job (unless, of course, you disable 'Recieve Backups' setting).");
		builder.append("\nAll these emails should be in the same thread in your inbox so you can simply mute them if you don't want to bother about them.");
		builder.append("\nOnce you received an email with the statistics, this data will be deleted from Ping Service database.");
		builder.append("\nPing Service will only store 1000 latest ping results per job.");
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
        
        byte[] export = new JobResultCSVExporter().export(timeZone, (List<JobResult>)results);
		attachment.setContent(new String(export), "text/plain");
		
		mailer.sendMail(Mailer.PING_SERVICE_NOTIFY_GMAIL_COM, reportRecipient, subject, message, attachment);
	}

	private String getReportSubject() {
		return "Statistics Backup for " + job.getTitleFriendly();
	}

	private String getReportRecipient() {
		return job.getReportEmail();
	}

}
