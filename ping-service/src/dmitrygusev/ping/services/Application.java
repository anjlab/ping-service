package dmitrygusev.ping.services;

import static com.google.appengine.api.datastore.KeyFactory.keyToString;
import static com.google.appengine.api.labs.taskqueue.QueueFactory.getQueue;
import static dmitrygusev.ping.services.GAEHelper.addTaskNonTransactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Filter;

import javax.persistence.RollbackException;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.Link;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.entities.Ref;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.filters.AbstractFilter;
import dmitrygusev.ping.filters.BackupJobResultsFilter;
import dmitrygusev.ping.filters.RunJobFilter;
import dmitrygusev.ping.pages.job.Analytics;
import dmitrygusev.ping.pages.task.LongRunningQueryTask;
import dmitrygusev.ping.services.dao.AccountDAO;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.RefDAO;
import dmitrygusev.ping.services.dao.ScheduleDAO;
import dmitrygusev.ping.services.location.Location;
import dmitrygusev.ping.services.location.LocationResolver;
import dmitrygusev.ping.services.location.TimeZoneResolver;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final Account systemAccount = getSystemAccount();
    
    private AccountDAO accountDAO;
    private JobDAO jobDAO;
    private ScheduleDAO scheduleDAO;
    private RefDAO refDAO;
    private GAEHelper gaeHelper;
    private JobExecutor jobExecutor;
    private Mailer mailer;
    private PageRenderLinkSource linkSource;
    private RequestGlobals globals;
    private TimeZoneResolver timeZoneResolver;
    private LocationResolver locationResolver;

    public static final int DEFAULT_NUMBER_OF_JOB_RESULTS = 1000;

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateFormat DATETIME_FORMAT = new SimpleDateFormat(DATETIME_PATTERN);
    public static final DateFormat DATETIME_FORMAT_FOR_FILE_NAME = new SimpleDateFormat("yyyyMMddHHmmss");

    public static final int GOOGLE_IO_FAIL_LIMIT = 3;

    public static final String MAIL_QUEUE = "mail";
    public static final String DEFAULT_QUEUE = "default";

    public static final String ANONYMOUS_TIME_ZONE_ID_SESSION_ATTRIBUTE_NAME = "anonymous.timeZoneId";
    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    public Application(
            AccountDAO accountDAO, 
            JobDAO jobDAO,
            ScheduleDAO scheduleDAO, 
            RefDAO refDAO,
            GAEHelper gaeHelper,
            JobExecutor jobExecutor,
            Mailer mailer,
            PageRenderLinkSource linkSource,
            RequestGlobals globals,
            TimeZoneResolver timeZoneResolver,
            LocationResolver locationResolver) {
        super();
        this.accountDAO = accountDAO;
        this.jobDAO = jobDAO;
        this.scheduleDAO = scheduleDAO;
        this.refDAO = refDAO;
        this.gaeHelper = gaeHelper;
        this.jobExecutor = jobExecutor;
        this.mailer = mailer;
        this.linkSource = linkSource;
        this.globals = globals;
        this.timeZoneResolver = timeZoneResolver;
        this.locationResolver = locationResolver;
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

    public boolean updateJob(Job job, boolean checkPermission, boolean commitAfter) {
        if (checkPermission) {
            assertCanModifyJob(job);
        }
        
        boolean succeeded = internalUpdateJob(job, commitAfter);
        
        if (succeeded) {
            try {
                scheduleResultsBackupIfNeeded(job);
            } catch (URISyntaxException e) {
                logger.error("Error scheduling job backup", e);
            }
        }
        
        return succeeded;
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
        
        //    Grant administrators read-only access to any job
        //    Grant access to ping service job to everyone 
        if (!isPingServiceJob(scheduleId, jobId) 
            && !UserServiceFactory.getUserService().isUserAdmin()) {
            assertCanAccessJob(job);
        }
        
        return job;
    }

    /**
     * 
     * @param scheduleId
     * @param jobId
     * @return
     *  Returns true if the job is a job for ping service web site in production.
     */
    private boolean isPingServiceJob(Long scheduleId, Long jobId) {
        return Long.valueOf("1026").equals(scheduleId)
            && Long.valueOf("5002").equals(jobId);
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

    public String formatDateForFileName(Date date) {
        return formatDate(DATETIME_FORMAT_FOR_FILE_NAME, getTimeZone(), date);
    }
    
    public static String formatDateForFileName(Date date, TimeZone timeZone) {
        return formatDate(DATETIME_FORMAT_FOR_FILE_NAME, timeZone, date);
    }
    
    public String formatDate(Date date) {
        return formatDate(DATETIME_FORMAT, getTimeZone(), date);
    }

    public static String formatDate(DateFormat format, TimeZone timeZone, Date date) {
        format.setTimeZone(timeZone);
        
        return format.format(date);
    }

    public TimeZone getTimeZone() {
        UserService userService = UserServiceFactory.getUserService();
        
        return userService.isUserLoggedIn()
                ? getTimeZone(getUserAccount().getTimeZoneCity())
                : getTimeZoneByClientIP();
    }
    
    private TimeZone getTimeZone(String timeZoneCity) {
        return Utils.isNullOrEmpty(timeZoneCity) 
                                ? getTimeZoneByClientIP() 
                                : TimeZone.getTimeZone(Utils.getTimeZoneId(timeZoneCity));
    }
    
    private TimeZone getTimeZoneByClientIP() {
        TimeZone timeZone = UTC_TIME_ZONE;
        
        try {
            String clientIP = globals.getHTTPServletRequest().getRemoteAddr();
            
            if (!Utils.isNullOrEmpty(clientIP)) {
                Location location = locationResolver.resolveLocation(clientIP);
                
                if (!location.isEmpty()) {
                    timeZone = timeZoneResolver.resolveTimeZone(location.getLatitude(), location.getLongitude());
                }
                
                if (timeZone == null) {
                    timeZone = UTC_TIME_ZONE;
                }
            }
            
            logger.debug("Resolved timeZoneId is {}", timeZone.getID());
        } catch (Exception e) {
            logger.error("Error resolving client timezone by ip " 
                    + globals.getHTTPServletRequest().getRemoteAddr(), e);
        }
        
        return timeZone;
    }

    private static Account getSystemAccount() {
        Account account = new Account();
        account.setEmail("system");
        return account;
    }

    public Account getUserAccount() {
        Principal principal = gaeHelper.getUserPrincipal();
        Account account = principal == null 
                            ? systemAccount 
                            : accountDAO.getAccount(principal.getName());
        return account;
    }

    public void trackUserActivity() {
        Account account = getUserAccount();
        if (account == systemAccount) {
            return;
        }

        Date lastVisitDate = account.getLastVisitDate();
        if (lastVisitDate == null || visitedLongTimeAgo(lastVisitDate)) {
            account.setLastVisitDate(new Date());

//            String actionKey = "trackUserActivity-" + account.getEmail();

            try {
                //  Note: No need in barrier anymore since we're tracking activity only for HTML requests
                //  which are not simultaneous for the same user
                
//                Long barrier = memcache.increment(actionKey, 1L, 1L);
//                
//                if (barrier == null || barrier > 1L) {
//                    return;
//                }
                
                accountDAO.update(account);
                
            } finally {
//                memcache.increment(actionKey, -1L, 0L);
            }
        }
    }

    private boolean visitedLongTimeAgo(Date lastVisitDate) {
        return System.currentTimeMillis() - lastVisitDate.getTime() > TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);
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

    /**
     * Client should update the job itself after invoking this method.
     * 
     * @param job
     * @see #updateJob(Job, boolean, boolean)
     */
    public void runJob(Job job) {
        try {
            boolean prevPingFailed = job.isLastPingFailed();
            boolean prevIsGoogleIOException = job.isGoogleIOException();
            
            JobResult jobResult = jobExecutor.execute(job);

            if (isJobStatusChanged(job, prevPingFailed)) {
                job.resetStatusCounter();
                
                if (!job.isLastPingFailed() 
                        && (!prevIsGoogleIOException 
                                /* No need to notify earlier since user haven't received fail report yet */
                                || job.getPreviousStatusCounter() >= GOOGLE_IO_FAIL_LIMIT)) {
                    //    The job is up again
                    sendReport(job);
                } else if (job.isLastPingFailed() && !job.isGoogleIOException()) {
                    //    Non-Google IO failure
                    sendReport(job);
                }
            } else {
                job.incrementStatusCounter();
            }
            
            //  Register job failure on third fail (see GOOGLE_IO_FAIL_LIMIT)
            if (job.getStatusCounter() == GOOGLE_IO_FAIL_LIMIT && job.isGoogleIOException()) {
                sendReport(job);
            }
            
            job.addJobResult(jobResult);

            //  Client should update the job itself
//            internalUpdateJob(job);
            
        } catch (Exception e) {
            logger.error("Error executing job " + job.getKey(), e);
        }
    }

    private boolean isJobStatusChanged(Job job, boolean prevPingFailed) {
        return prevPingFailed ^ job.isLastPingFailed();
    }

    private void scheduleResultsBackupIfNeeded(Job job) throws URISyntaxException {
        int numberOfResults = job.getRecentJobResults(0).size();
        
        if ((numberOfResults >= DEFAULT_NUMBER_OF_JOB_RESULTS * 2) && (numberOfResults % 10 == 0)) {
            runBackupJobResultsTask(job.getKey());
        }
    }

    private final static java.util.logging.Logger transactionLogger = 
        java.util.logging.Logger.getLogger("DataNucleus.Transaction");

    private final static Filter transactionFilter = new DataNucleusTransactionLoggingFilter();
    /**
     * 
     * @param job
     * @param commitAfter Commit transaction manually (may be required when running out of Tapestry context)
     * @return
     */
    private boolean internalUpdateJob(Job job, boolean commitAfter) {
        try {
            //  Configure logger to change level of "Operation commit failed on resource..." error to WARNING
            transactionLogger.setFilter(transactionFilter);

            jobDAO.update(job, commitAfter);
            return true;
        } catch (RollbackException e) {
            //  This may happen if another job from the same schedule 
            //  updating at the same time simultaneously
            
            logger.debug("Retrying update for job: {}", job.getKey());
            
            //  Give another job a chance to commit, and commit current job after some delay
            return internalUpdateJobAfterDelay(job, commitAfter);
        }
    }

    private boolean internalUpdateJobAfterDelay(Job job, boolean commitAfter) {
        final int maxAttempts = 3;
        int attempt = 1;
        
        while (true) {
            try {
                logger.debug("Waiting for another job to commit #{} of {}", attempt, maxAttempts);
                
                Thread.sleep(1000);
                
                try {
                    //  Transaction will be reopened inside DAO if required
                    jobDAO.update(job, commitAfter);
                    logger.debug("Update after delay succeeded");
                    
                    return true;
                } catch (RollbackException e) {
                    if (attempt >= maxAttempts) {
                        logger.error("Update after delay failed", e);
                        break;
                    }
                    attempt++;
                    logger.warn("Update after delay failed, will try again...", e);
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted", e);
            }
        }
        
        return false;
    }

    public void sendReport(Job job) throws URISyntaxException {
        if (!job.isReceiveNotifications() || Utils.isNullOrEmpty(job.getReportEmail())) {
            logger.debug("Job is not configured to recieve reports or no report recepient specified");
            return;
        }
        
        String from = Mailer.PING_SERVICE_NOTIFY_GMAIL_COM;
        String to = job.getReportEmail();
        
        String subject = job.isLastPingFailed() ? job.getTitleFriendly() + " is down" : job.getTitleFriendly() + " is up again";
    
        StringBuffer body = new StringBuffer();
        
        body.append("Job results for URL: ");
        body.append(job.getPingURL());
        body.append("\n\nYou can analyze URL performance at: ");
        
        body.append(getJobUrl(job, Analytics.class));
    
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
        
    public void runBackupJobResultsTask(Key jobKey) throws URISyntaxException {
        addTaskNonTransactional(
            getQueue(MAIL_QUEUE),
            buildTaskUrl(BackupJobResultsFilter.class)
                .param(LongRunningQueryTask.JOB_KEY_PARAMETER_NAME, keyToString(jobKey))
                .param(LongRunningQueryTask.STARTTIME_PARAMETER_NAME, String.valueOf(System.currentTimeMillis())));
    }

    public void enqueueJobs(String cronString) throws URISyntaxException {
        logger.debug("Enqueueing jobs for cron string '{}'", cronString);
        
        List<Key> unmodifiableKeys = jobDAO.getJobsByCronString(cronString);
        
        List<Key> jobKeys = new ArrayList<Key>(unmodifiableKeys.size());
        for (Key key : unmodifiableKeys) {
            jobKeys.add(key);
        }
        
//        Iterable<Key> iterable = new SpareIterator<Key, Long>(jobKeys, new Colorer<Key, Long>() {
//            @Override
//            public Long getColor(Key item) {
//                return item.getParent().getId();
//            }
//        });
        
        Collections.shuffle(jobKeys);
        
        logger.debug("Found {} job(s) to enqueue", jobKeys.size());

        Queue queue = getQueue(cronString.replace(" ", "").replace(":", ""));

        List<TaskOptions> tasks = new ArrayList<TaskOptions>(jobKeys.size());
        
        for (Key key : jobKeys) {
            tasks.add(buildTaskUrl(RunJobFilter.class)
                        .param(RunJobFilter.JOB_KEY_PARAMETER_NAME, keyToString(key)));
        }

        addTaskNonTransactional(queue, tasks);

        logger.debug("Finished enqueueing jobs");
    }

    public String getPath(Class<?> pageClass, Object... context) throws URISyntaxException {
        Link link;
        
        if (context != null & context.length > 0) {
            link = linkSource.createPageRenderLinkWithContext(pageClass, context);
        } else {
            link = linkSource.createPageRenderLink(pageClass);
        }
             
        URI uri = new URI(link.toAbsoluteURI());
    
        return uri.getPath().toLowerCase();
    }

    public TaskOptions buildTaskUrl(Class<?> pageClass) throws URISyntaxException {
        String path;
        
        if (AbstractFilter.class.isAssignableFrom(pageClass)) {
            String filterName = pageClass.getSimpleName().replace("Filter", "");
            path = "/filters/" + Character.toLowerCase(filterName.charAt(0)) + filterName.substring(1);
        } else {
            path = getPath(pageClass);
        }
        
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

    public Mailer getMailer() {
        return mailer;
    }

    /**
     * Move job to new schedule.
     * 
     * @param job
     * @param target
     */
    public void moveJob(Job job, Schedule target) {
        jobDAO.delete(target.getId(), job.getKey().getId());
        Job copy = job.copy();
        target.addJob(copy);
        scheduleDAO.update(target);
    }

}
