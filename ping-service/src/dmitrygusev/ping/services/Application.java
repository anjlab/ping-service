package dmitrygusev.ping.services;

import static com.google.appengine.api.datastore.KeyFactory.keyToString;
import static com.google.appengine.api.labs.taskqueue.QueueFactory.getDefaultQueue;
import static com.google.appengine.api.labs.taskqueue.QueueFactory.getQueue;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.Link;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.TaskOptions;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.entities.Ref;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.pages.task.BackupAndDeleteOldJobResultsTask;
import dmitrygusev.ping.pages.task.CountJobResultsTask;
import dmitrygusev.ping.pages.task.CyclicBackupTask;
import dmitrygusev.ping.pages.task.LongRunningQueryTask;
import dmitrygusev.ping.pages.task.MailJobResultsTask;
import dmitrygusev.ping.services.dao.AccountDAO;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.JobResultDAO;
import dmitrygusev.ping.services.dao.RefDAO;
import dmitrygusev.ping.services.dao.ScheduleDAO;

public class Application {

	public static final String BACKUP_QUEUE = "backup";
	public static final String MAIL_QUEUE = "mail";

	private static final Logger logger = LoggerFactory.getLogger(Application.class);
	
	private AccountDAO accountDAO;
	private JobDAO jobDAO;
	private ScheduleDAO scheduleDAO;
	private RefDAO refDAO;
	private JobResultDAO jobResultDAO;
	private GAEHelper gaeHelper;
	private JobExecutor jobExecutor;
	private ReportSender reportSender;
	private Mailer mailer;
	private AppSessionCache sessionCache;
	private PageRenderLinkSource linkSource;
	private RequestGlobals globals;
	
	public Application(
			AccountDAO accountDAO, 
			JobDAO jobDAO,
			ScheduleDAO scheduleDAO, 
			RefDAO refDAO,
			JobResultDAO jobResultDAO,
			GAEHelper gaeHelper,
			JobExecutor jobExecutor,
			ReportSender reportSender,
			Mailer mailer,
			ApplicationStateManager stateManager,
			PageRenderLinkSource linkSource,
			RequestGlobals globals) {
		super();
		this.accountDAO = accountDAO;
		this.jobDAO = jobDAO;
		this.scheduleDAO = scheduleDAO;
		this.refDAO = refDAO;
		this.jobResultDAO = jobResultDAO;
		this.gaeHelper = gaeHelper;
		this.jobExecutor = jobExecutor;
		this.reportSender = reportSender;
		this.mailer = mailer;
		this.sessionCache = stateManager.get(AppSessionCache.class);
		this.linkSource = linkSource;
		this.globals = globals;
	}

	public List<Account> getAccounts(Schedule schedule) {
		List<Ref> refs = refDAO.getRefs(schedule);
		
		List<Account> result = new ArrayList<Account>();
		
		for (Ref ref : refs) {
			Account account = accountDAO.find(ref.getAccountKey().getId());
			
			account.setRef(ref);
			
			result.add(account);
		}
		
		return result;
	}

	public void delete(Schedule schedule) {
		List<Ref> refs = refDAO.getRefs(schedule);
		
		for (Ref ref : refs) {
			refDAO.removeRef(ref.getId());
		}
		
		scheduleDAO.delete(schedule.getId());
	}

	public void createJob(Job job) {
		Account account = getUserAccount();
		Schedule schedule = getDefaultSchedule(account);
		schedule.addJob(job);
		scheduleDAO.update(schedule);
		refDAO.addRef(account, schedule, Ref.ACCESS_TYPE_FULL);
	}

	public Schedule getDefaultSchedule() {
		return getDefaultSchedule(getUserAccount());
	}
	
	public Schedule getDefaultSchedule(Account account) {
		List<Ref> refs = refDAO.getRefs(account);

		Schedule schedule = null;
		
		for (Ref ref : refs) {
			schedule = scheduleDAO.find(ref.getScheduleKey());
			
			if (schedule.getName().equals(account.getEmail())) {
				break;
			}
			
			schedule = null;
		}
		
		if (schedule == null) {
			schedule = scheduleDAO.createSchedule(account.getEmail());
			
			refDAO.addRef(account, schedule, Ref.ACCESS_TYPE_FULL);
		}
		
		return schedule;
	}

	public void updateJob(Job job) {
		assertCanUpdateJob(job);
		
		jobDAO.update(job);
	}

	private void assertCanUpdateJob(Job job) {
		assertCanModifyJob(job);
	}

	private void assertCanModifyJob(Job job) {
		Ref ref = assertCanAccessJob(job);
		
		if (ref == null) {
			return;
		}
		
		if (ref.getAccessType() != Ref.ACCESS_TYPE_FULL) {
			throw new NotAuthorizedException("Not authorized");
		}
	}

	public Job findJob(Long scheduleId, Long jobId) {
		Job job = jobDAO.find(scheduleId, jobId); 
		
		assertCanAccessJob(job);
		
		return job;
	}

	private Ref assertCanAccessJob(Job job) {
		if (job == null) {
			return null;
		}
		
		Account account = getUserAccount();

		Schedule schedule = getSchedule(job);
		
		Ref ref = refDAO.find(account, schedule);
		
		if (ref == null) {
			throw new NotAuthorizedException("Not authorized");
		}
		
		return ref;
	}

	public Schedule getSchedule(Job job) {
		Key scheduleKey = job.getKey().getParent();
		Schedule schedule = scheduleDAO.find(scheduleKey);
		return schedule;
	}

	public void removeAccount(Long accountId, Schedule schedule) {
		Account accountToRemove = accountDAO.find(accountId);
		
		if (accountToRemove == null) {
			return;
		}
		
		Account userAccount = getUserAccount();
		
		assertCanAccessSchedule(userAccount, schedule);
		assertScheduleOwner(userAccount, schedule);
		assertCantDeleteHimself(userAccount, accountToRemove);
		
		Ref ref = refDAO.find(accountToRemove, schedule);
		refDAO.removeRef(ref.getId());
	}

	private void assertCantDeleteHimself(
			Account userAccount, Account accountToRemove) {
		if (userAccount.getId().equals(accountToRemove.getId())) {
			throw new RuntimeException("You can't remove yourself");
		}
	}

	private void assertScheduleOwner(Account userAccount, Schedule schedule) {
		if (! schedule.getName().equals(userAccount.getEmail())) {
			throw new NotAuthorizedException("You're not schedule's owner");
		}
	}

	private void assertCanAccessSchedule(Account account, Schedule schedule) {
		Ref ref2 = refDAO.find(account, schedule);
		
		if (ref2 == null) {
			throw new NotAuthorizedException("Not authorized");
		}
	}

	public void grantAccess(String grantedEmail, Schedule schedule, int accessType) {
		Account grantedAccount = accountDAO.getAccount(grantedEmail);

		Account userAccount = getUserAccount();

		assertCanAccessSchedule(userAccount, schedule);
		assertScheduleOwner(userAccount, schedule);
		
		refDAO.addRef(grantedAccount, schedule, accessType);
		
		mailer.sendMail(
				"ping.service.notify@gmail.com", 
				grantedEmail, 
				"You have new shares", 
				"Hello, " + grantedEmail + "!\n\n" +
				"User " + getUserAccount().getEmail() + " is sharing his schedule with you.\n\n" +
				"You can view shared jobs in your schedule on http://ping-service.appspot.com\n\n" +
				"--\n" +
				"If you think this message was sent to you by mistake, just ignore it.");
	}

	public void deleteJob(Long scheduleId, Long jobId) {
		Job job = jobDAO.find(scheduleId, jobId);
		
		if (job == null) {
			throw new RuntimeException("Job not found");
		}
		
		assertCanDeleteJob(job);
		
		jobDAO.delete(scheduleId, jobId);
	}

	private void assertCanDeleteJob(Job job) {
		assertCanModifyJob(job);
	}

	public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	public static final DateFormat DATETIME_FORMAT = new SimpleDateFormat(DATETIME_PATTERN);
	public static final DateFormat DATETIME_FORMAT_FOR_FILE_NAME = new SimpleDateFormat("yyyyMMddHHmmss");

	public String formatDateForFileName(Date date) {
		String timeZoneCity = getUserAccount().getTimeZoneCity();

		return formatDate(date, timeZoneCity, DATETIME_FORMAT_FOR_FILE_NAME);
	}
	
	public static String formatDateForFileName(Date date, String timeZoneCity) {
		return formatDate(date, timeZoneCity, DATETIME_FORMAT_FOR_FILE_NAME);
	}
	
	public String formatDate(Date date) {
		String timeZoneCity = getUserAccount().getTimeZoneCity();

		return formatDate(date, timeZoneCity, DATETIME_FORMAT);
	}

	public static String formatDate(Date date, String timeZoneCity, DateFormat format) {
		TimeZone timezone = getTimeZone(timeZoneCity);

		format.setTimeZone(timezone);
		
		return format.format(date);
	}

	public TimeZone getTimeZone() {
		return getTimeZone(getUserAccount().getTimeZoneCity());
	}
	
	public static TimeZone getTimeZone(String timeZoneCity) {
		return Utils.isNullOrEmpty(timeZoneCity) 
								? TimeZone.getDefault() 
								: TimeZone.getTimeZone(Utils.getTimeZoneId(timeZoneCity));
	}
	
	public String getLastPingSummary(Job job) {
		StringBuilder sb = new StringBuilder();
		
		if (job.getLastPingTimestamp() != null) {
			String formattedDate = formatDate(job.getLastPingTimestamp());

			buildLastPingSummary(job, sb, formattedDate);
		} else {
			sb.append("N/A");
		}
		
		
		return sb.toString();
	}

	public static void buildLastPingSummary(Job job, StringBuilder sb, String formattedDate) {
		checkResult(job, sb, Job.PING_RESULT_NOT_AVAILABLE, "N/A");
		checkResult(job, sb, Job.PING_RESULT_OK, "Okay");
		checkResult(job, sb, Job.PING_RESULT_HTTP_ERROR, "HTTP failed");
		checkResult(job, sb, Job.PING_RESULT_CONNECTIVITY_PROBLEM, "Failed connecting");
		checkResult(job, sb, Job.PING_RESULT_REGEXP_VALIDATION_FAILED, "Regexp failed");

		sb.insert(0, " / ");
		sb.insert(0, formattedDate);
	}

	private static void checkResult(Job job, StringBuilder sb, int resultCode, String message) {
		if (job.containsResult(resultCode)) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(message);
		}
	}
	
	public Account getUserAccount() {
		return sessionCache.getUserAccount(gaeHelper, accountDAO);
	}

	public List<Job> getAvailableJobs() {
		Account account = getUserAccount();
		
		List<Ref> refs = refDAO.getRefs(account);
		
		List<Job> result = new ArrayList<Job>();
		
		for (Ref ref : refs) {
			Schedule schedule = scheduleDAO.find(ref.getScheduleKey());
			
			for (Job job : schedule.getJobs()) {
				job.setSchedule(schedule);
				
				result.add(job);
			}
		}
		
		return result;
	}

	public static final int GOOGLE_IO_FAIL_LIMIT = 3;
	
	public void enqueueJobs(String cronString) {
		logger.debug("Enqueueing jobs for cron string: " + cronString);
		
		List<Job> jobs = jobDAO.getJobsByCronString(cronString);
		
		logger.debug("Found " + jobs.size() + " job(s) to enqueue");

		Queue queue = getQueue(cronString.replace(" ", ""));

		for (Job job : jobs) {
			queue.add(null, GAEHelper.buildTaskUrl("/job/run/").param("key", keyToString(job.getKey())));
		}
		
		logger.debug("Finished enqueueing jobs");
	}

	public void runJob(Job job) {
		try {
			boolean prevPingFailed = job.isLastPingFailed();
			boolean prevIsGoogleIOException = job.isGoogleIOException();
			
			JobResult jobResult = jobExecutor.execute(job);

			if (prevPingFailed ^ job.isLastPingFailed()) {
				job.resetStatusCounter();
				
				if (!job.isLastPingFailed() 
						&& (!prevIsGoogleIOException 
								/* No need to notify earlier since user didn't received fail report yet */
								|| job.getPreviousStatusCounter() >= GOOGLE_IO_FAIL_LIMIT)) {
					//	The job is up again 
					reportSender.sendReport(job, this);
				} else if (job.isLastPingFailed() && !job.isGoogleIOException()) {
					//	Non-Google IO failure
					reportSender.sendReport(job, this);
				}
			} else {
				job.incrementStatusCounter();
			}
			
			if (job.getStatusCounter() == GOOGLE_IO_FAIL_LIMIT && job.isGoogleIOException()) {
				//	Register job failure on third fail (see GOOGLE_IO_FAIL_LIMIT)
				reportSender.sendReport(job, this);
			}
			
			jobDAO.update(job);
			jobResultDAO.persistResult(jobResult);
		} catch (Exception e) {
			logger.error("Error executing job " + job.getKey() + ": " + e);
		}
	}

	public void sendInvite(String friendEmail) {
		String myEmail = gaeHelper.getUserPrincipal().getName();
		mailer.sendMail(
				myEmail, 
				friendEmail, 
				"Invitation to Ping Service", 
				"Hello there!\n\n" +
				"I'm using Ping Service and thought you might also be interested in it.\n\n" +
				"You can check it here: http://ping-service.appspot.com\n\n" +
				"--\n" +
				"This message was sent to you by " + myEmail + " via Ping Service friend invite.\n" +
				"If you think this message was sent to you by mistake, just ignore it.");
	}
	
	public void runCyclicBackupTask() throws URISyntaxException {
		getQueue(BACKUP_QUEUE)
			.add(null, buildTaskUrl(CyclicBackupTask.class));
	}
	
	public void runBackupAndDeleteTask(Key jobKey) throws URISyntaxException {
		long id = new Random().nextLong();
		
		getDefaultQueue()
			.add(null, buildTaskUrl(BackupAndDeleteOldJobResultsTask.class)
				.param(BackupAndDeleteOldJobResultsTask.JOB_PARAMETER_NAME, keyToString(jobKey))
				.param(BackupAndDeleteOldJobResultsTask.TASK_ID_PARAMETER_NAME, String.valueOf(id)));
	}
	
	public void runMailJobResultsTask(Key jobKey, String taskId, long backupStartTime) throws URISyntaxException {
		getQueue(MAIL_QUEUE)
			.add(null, buildTaskUrl(MailJobResultsTask.class)
				.param(BackupAndDeleteOldJobResultsTask.JOB_PARAMETER_NAME, keyToString(jobKey))
				.param(BackupAndDeleteOldJobResultsTask.TASK_ID_PARAMETER_NAME, taskId)
				.param(LongRunningQueryTask.STARTTIME_PARAMETER_NAME, String.valueOf(backupStartTime)));
	}
	
	public void continueMailJobResultsTask(Key jobKey, String taskId, long backupStartTime, long chunkId, long totalRecords, long fileNumber) throws URISyntaxException {
		getQueue(MAIL_QUEUE)
			.add(null, buildTaskUrl(MailJobResultsTask.class)
				.param(BackupAndDeleteOldJobResultsTask.JOB_PARAMETER_NAME, keyToString(jobKey))
				.param(BackupAndDeleteOldJobResultsTask.TASK_ID_PARAMETER_NAME, taskId)
				.param(LongRunningQueryTask.STARTTIME_PARAMETER_NAME, String.valueOf(backupStartTime))
				.param(MailJobResultsTask.CHUNK_ID_PARAMETER_NAME, String.valueOf(chunkId))
				.param(MailJobResultsTask.TOTAL_RECORDS_PARAMETER_NAME, String.valueOf(totalRecords))
				.param(MailJobResultsTask.FILE_NUMBER_PARAMETER_NAME, String.valueOf(fileNumber)));
	}
	
	public void runCountJobResultsTask(Key jobKey) throws URISyntaxException {
		getDefaultQueue()
			.add(null, buildTaskUrl(CountJobResultsTask.class)
				.param(CountJobResultsTask.JOB_PARAMETER_NAME, keyToString(jobKey)));
	}

	public String getPath(Class<?> pageClass, Object... context) throws URISyntaxException {
		Link link;
		
		if (context != null & context.length > 0) {
			link = linkSource.createPageRenderLinkWithContext(pageClass, context);
		} else {
			link = linkSource.createPageRenderLink(pageClass);
		}
			 
		URI uri = new URI(link.toAbsoluteURI());
	
		return uri.getPath();
	}

	public TaskOptions buildTaskUrl(Class<?> pageClass) throws URISyntaxException {
		String path = getPath(pageClass);
		
		return GAEHelper.buildTaskUrl(path);
	}

	public String getJobUrl(Job job, Class<?> pageClass) throws URISyntaxException {
		String url = getBaseAddress() + getPath(pageClass, job.getKey().getParent().getId(), job.getKey().getId());
	    
		return url;
	}

	public String getBaseAddress() {
		HttpServletRequest request = globals.getHTTPServletRequest();
		
		String baseAddr = request.getScheme() + "://" + request.getServerName() 
			 + (request.getLocalPort() == 0 ? "" : ":" + request.getLocalPort());
		
		return baseAddr;
	}
	
}
