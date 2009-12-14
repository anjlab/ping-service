package dmitrygusev.ping.entities;

import java.util.Date;
import java.util.Locale;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.tapestry5.beaneditor.Validate;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

@Entity
public class Job {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Key key;

	//	URL to ping
	@Column(nullable=false)
	@Validate("required")
	private String pingURL;
	//	Validating regexp
	private String validatingRegexp;
	//	Cron string to select jobs against a cron action
	@Column(nullable=false)
	@Validate("required")
	private String cronString;
	//	Email to send reports to
	@Validate("email")
	private String reportEmail;
	private Date lastPingTimestamp;
	private int lastPingResult;
	@Basic
	private Text lastPingDetails;
	private boolean usesValidatingRegexp;
	private boolean usesValidatingHttpCode;
	private Integer validatingHttpCode;
	private String responseEncoding;
	private String title;
	
	//	Number of times this job stays in failed or okay status
	private Integer statusCounter;
	private Integer previousStatusCounter;
	private Integer totalStatusCounter;
	private Integer totalSuccessStatusCounter;
	
	@Transient
	private Schedule schedule;
	
	public static final int PING_RESULT_NOT_AVAILABLE = 1;
	public static final int PING_RESULT_OK = 2;
	public static final int PING_RESULT_CONNECTIVITY_PROBLEM = 4;
	public static final int PING_RESULT_HTTP_ERROR = 8;
	public static final int PING_RESULT_REGEXP_VALIDATION_FAILED = 16;
	
	public Job() {
		lastPingResult = PING_RESULT_NOT_AVAILABLE;
	}

	public boolean isLastPingFailed() {
		return ! (containsResult(PING_RESULT_NOT_AVAILABLE) || containsResult(PING_RESULT_OK));
	}

	public boolean containsResult(int resultCode) {
		return (lastPingResult & resultCode) == resultCode;
	}
	
	public String getValidationSummary() {
		StringBuffer sb = new StringBuffer();
		
		if (usesValidatingHttpCode) {
			sb.append("HTTP Code");
		}
		if (usesValidatingRegexp) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("Regexp");
		}
		if (sb.length() == 0) {
			sb.append("None");
		}
		
		return sb.toString();
	}

	public String getPingURL() {
		return pingURL;
	}
	public void setPingURL(String pingURL) {
		this.pingURL = pingURL;
	}
	public String getValidatingRegexp() {
		return validatingRegexp;
	}
	public void setValidatingRegexp(String validatingRegexp) {
		this.validatingRegexp = validatingRegexp;
	}
	public String getCronString() {
		return cronString;
	}
	public void setCronString(String cronString) {
		this.cronString = cronString;
	}
	public String getReportEmail() {
		return reportEmail;
	}
	public void setReportEmail(String reportEmail) {
		this.reportEmail = reportEmail;
	}
	public Key getKey() {
		return key;
	}
	public Date getLastPingTimestamp() {
		return lastPingTimestamp;
	}
	public void setLastPingTimestamp(Date lastPingTimestamp) {
		this.lastPingTimestamp = lastPingTimestamp;
	}
	public int getLastPingResult() {
		return lastPingResult;
	}
	public void setLastPingResult(int lastPingResult) {
		this.lastPingResult = lastPingResult;
	}
	public String getLastPingDetails() {
		return lastPingDetails == null ? null : lastPingDetails.getValue();
	}
	public void setLastPingDetails(String lastPingDetails) {
		this.lastPingDetails = new Text(lastPingDetails);
	}
	public boolean isUsesValidatingRegexp() {
		return usesValidatingRegexp;
	}
	public void setUsesValidatingRegexp(boolean usesValidatingRegexp) {
		this.usesValidatingRegexp = usesValidatingRegexp;
	}
	public boolean isUsesValidatingHttpCode() {
		return usesValidatingHttpCode;
	}
	public void setUsesValidatingHttpCode(boolean usesValidatingHttpCode) {
		this.usesValidatingHttpCode = usesValidatingHttpCode;
	}
	public Integer getValidatingHttpCode() {
		return validatingHttpCode;
	}
	public void setValidatingHttpCode(Integer validatingHttpCode) {
		this.validatingHttpCode = validatingHttpCode;
	}
	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}
	public Schedule getSchedule() {
		return schedule;
	}
	public String getScheduledBy() {
		return schedule.getName();
	}
	public String getResponseEncoding() {
		return responseEncoding;
	}
	public void setResponseEncoding(String responseEncoding) {
		this.responseEncoding = responseEncoding;
	}
	public String getShortenURL() {
		if (pingURL == null) {
			return null;
		}
		return pingURL.length() > 47 
			 ? pingURL.substring(0, 46) + "..." 
			 : pingURL;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTitleFriendly() {
		if (title == null || title.isEmpty()) {
			return getShortenURL();
		}
		return title;
	}
	public int getStatusCounter() {
		return statusCounter == null ? 0 : statusCounter;
	}
	private void setStatusCounter(int statusCounter) {
		this.statusCounter = statusCounter;
	}
	public int getPreviousStatusCounter() {
		return previousStatusCounter == null ? 0 : previousStatusCounter;
	}
	private void setPreviousStatusCounter(int previousStatusCounter) {
		this.previousStatusCounter = previousStatusCounter;
	}
	public boolean isGoogleIOException() {
		return isLastPingFailed() 
			&& getLastPingResult() == PING_RESULT_CONNECTIVITY_PROBLEM
		    && getLastPingDetails() != null 
		    && getLastPingDetails().contains("google")
		    && (getLastPingDetails().contains("java.io.IOException: Timeout")
		    		|| getLastPingDetails().contains("java.io.IOException: Could not fetch URL")
		    		|| getLastPingDetails().contains("DeadlineExceededException"));
	}
	public int getTotalStatusCounter() {
		return totalStatusCounter == null 
		     ? getPreviousStatusCounter() + getStatusCounter() 
		     : totalStatusCounter;
	}
	private void incrementTotalStatusCounter() {
		int correction = 0;
		if (totalStatusCounter == null) {
			correction = -1;
		}
		
		this.totalStatusCounter = getTotalStatusCounter() + 1 + correction;
		
		if (! isLastPingFailed()) {
			incrementTotalSuccessStatusCounter();
		}
	}
	public int getTotalSuccessStatusCounter() {
		return totalSuccessStatusCounter == null 
			 ? (isLastPingFailed() ? getPreviousStatusCounter() : getStatusCounter())
			 : totalSuccessStatusCounter;
	}
	private void incrementTotalSuccessStatusCounter() {
		int correction = 0;
		if (totalSuccessStatusCounter == null) {
			correction = -1;
		}

		this.totalSuccessStatusCounter = getTotalSuccessStatusCounter() + 1 + correction;
	}
	public String getAvailabilityPercent() {
		int total = getTotalStatusCounter();
		
		if (total == 0) {
			return "0.00000 %";
		}
		
		int totalSuccess = getTotalSuccessStatusCounter();
		
		return String.format(Locale.ENGLISH, "%.5f", 100d * totalSuccess / total) + " %";
	}
	public void resetStatusCounter() {
		setPreviousStatusCounter(getStatusCounter());
		setStatusCounter(0);
		incrementStatusCounter();
	}
	public void incrementStatusCounter() {
		setStatusCounter(getStatusCounter() + 1);
		incrementTotalStatusCounter();
	}
	public String getLastPingSummary() {
		return lastPingTimestamp == null ? "" : lastPingTimestamp.toString(); 
	}
}
