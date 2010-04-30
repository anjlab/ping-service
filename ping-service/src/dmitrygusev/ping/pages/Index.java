package dmitrygusev.ping.pages;

import java.net.URISyntaxException;
import java.util.List;
import java.util.TimeZone;

import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.users.UserServiceFactory;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Ref;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Utils;

public class Index {
	
	@Property
	@Persist
	@SuppressWarnings("unused")
	private String message;
	
	@Property
	@Persist
	@SuppressWarnings("unused")
	private String messageColor;

	@AfterRender
	public void cleanup() {
		message = null;
		defaultSchedule = null;
		userAccount = null;
		account = null;
		grantedEmail = null;
		job = null;
		messageColor = null;
		timeZone = null;
	}
	
	private TimeZone timeZone;
	
	@Property
	private Job job;
	
	public String getLastPingSummary() {
		return application.getLastPingSummary(job, getTimeZone());
	}

    private TimeZone getTimeZone() {
        if (timeZone == null) {
            timeZone = application.getTimeZone();
        }
        return timeZone;
    }
	
	public String getSummaryStatusCssClass() {
		if (job.isGoogleIOException()) {
			return "status-warning";
		}
		if (job.isLastPingFailed()) {
			return "status-error";
		}
		if (job.getTotalStatusCounter() == 0) {
			return "status-na";
		}
		return "status-okay";
	}
	
	public Long[] getJobContext() {
		return Utils.createJobContext(job);
	}

	public void onActionFromDeleteJob(Long scheduleId, Long jobId) {
		try {
			application.deleteJob(scheduleId, jobId);
		} catch (Exception e) {
			message = e.getMessage();
			messageColor = "red";
		}
	}
	
	@Property
	private Account account;

	public boolean isDeleteAccountLinkEnabled() {
		return ! account.getId().equals(getUserAccount().getId());
	}

	private Account userAccount;
	
	public Account getUserAccount() {
		if (userAccount == null) {
			userAccount = application.getUserAccount();
		}
		return userAccount;
	}
	
	@Property
	private String grantedEmail;
	
	@Property
	private boolean readOnly;
	
	public void onSuccessFromGrantAccessTo() {
		try {
			application.grantAccess(grantedEmail, getDefaultSchedule(),
					readOnly ? Ref.ACCESS_TYPE_READONLY : Ref.ACCESS_TYPE_FULL);
		} catch (Exception e) {
			message = e.getMessage();
			messageColor = "red";
		}
	}
	
	private Schedule defaultSchedule;
	
	private Schedule getDefaultSchedule() {
		if (defaultSchedule == null) {
			defaultSchedule = application.getDefaultSchedule(); 
		}
		return defaultSchedule;
	}

	public List<Job> getJobs() {
		return application.getAvailableJobs();
	}
	
	@Inject
	private Application application;
	
	public List<Account> getAccounts() {
		return application.getAccounts(getDefaultSchedule());
	}
	
	public void onActionFromRemoveAccount(Long accountId) {
		try {
			application.removeAccount(accountId, getDefaultSchedule());
		} catch (Exception e) {
			message = e.getMessage();
			messageColor = "red";
		}
	}

	public void setExceptionMessage(String message) {
		this.message = message;
		messageColor = "red";
	}
	
	public void onActionFromDeleteSchedule() {
		try {
			application.delete(getDefaultSchedule());
	 	} catch (Exception e) {
	 		message = e.getMessage();
			messageColor = "red";
	 	}
	}

	public boolean isAdmin() {
		return UserServiceFactory.getUserService().isUserAdmin();
	}

	public void onActionFromRunCyclicBackupTask() throws URISyntaxException {
		application.runCyclicBackupTask();
	}
}
