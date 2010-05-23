package dmitrygusev.ping.entities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;

import org.apache.tapestry5.beaneditor.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Utils;

@Entity
public class Job implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1077399963209971165L;

    private static final Logger logger = LoggerFactory.getLogger(Job.class);
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Key key;

	//	URL to ping
	@Column(nullable=false)
	@Validate("required,regexp=(http://|https://).+")
	private String pingURL;
	//	Validating regexp
	@Basic(fetch=FetchType.EAGER)
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
	
	private Boolean receiveBackups;
	private Date lastBackupTimestamp;
	
	// Since 13.05.2010
	@Basic
	private Blob packedJobResults;
	@Transient
	private List<JobResult> jobResults;
	@Transient
	private boolean updatingJobResults;
	
	public static final int PING_RESULT_NOT_AVAILABLE = 1;
	public static final int PING_RESULT_OK = 2;
	public static final int PING_RESULT_CONNECTIVITY_PROBLEM = 4;
	public static final int PING_RESULT_HTTP_ERROR = 8;
	public static final int PING_RESULT_REGEXP_VALIDATION_FAILED = 16;
	
	public Job() {
		lastPingResult = PING_RESULT_NOT_AVAILABLE;
		lastBackupTimestamp = new Date();
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
	public Date getLastBackupTimestamp() {
        return lastBackupTimestamp;
    }
	public void setLastBackupTimestamp(Date lastBackupTimestamp) {
        this.lastBackupTimestamp = lastBackupTimestamp;
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
	    if (lastPingDetails != null && lastPingDetails.length() > 1024 * 100) {
	        // Persist only 100 KB of ping details
	        lastPingDetails = lastPingDetails.substring(0, 1024 * 100);
	    }
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
		return schedule == null ? null : schedule.getName();
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
		if (Utils.isNullOrEmpty(title)) {
			return getShortenURL();
		}
		return title;
	}
	public int getStatusCounter() {
		return statusCounter == null ? 0 : statusCounter;
	}
	public String getStatusCounterFriendly() {
		return formatCounter(getStatusCounter());
	}
	public String getStatusCounterFriendlyShort() {
		return formatCounterShort(getStatusCounter());
	}
	public int getUpDownTimeInMinutes() {
		return Utils.getTimeInMinutes(getStatusCounter(), cronString);
	}
	private String formatCounter(int counter) {
		return formatCounterShort(counter) + " (" + counter + ")";
	}
	private String formatCounterShort(int counter) {
		return Utils.formatTime(Utils.getTimeInMinutes(counter, cronString));
	}
	private void setStatusCounter(int statusCounter) {
		this.statusCounter = statusCounter;
	}
	public int getPreviousStatusCounter() {
		return previousStatusCounter == null ? 0 : previousStatusCounter;
	}
	public String getPreviousStatusCounterFriendly() {
		return formatCounter(getPreviousStatusCounter());
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
	public String getTotalStatusCounterFriendly() {
		return formatCounter(getTotalStatusCounter());
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
	public String getTotalSuccessStatusCounterFriendly() {
		return formatCounter(getTotalSuccessStatusCounter());
	}
	private void incrementTotalSuccessStatusCounter() {
		int correction = 0;
		if (totalSuccessStatusCounter == null) {
			correction = -1;
		}

		this.totalSuccessStatusCounter = getTotalSuccessStatusCounter() + 1 + correction;
	}
	public double getTotalAvailabilityPercent() {
		return Utils.calculatePercent(getTotalStatusCounter(), getTotalSuccessStatusCounter());
	}
	public String getTotalAvailabilityPercentFriendly() {
		return Utils.formatPercent(getTotalAvailabilityPercent());
	}
    public double getRecentAvailabilityPercent() {
        int recentCount = Application.DEFAULT_NUMBER_OF_JOB_RESULTS;
        List<JobResult> results = getRecentJobResults(recentCount);
        return Utils.calculateAvailabilityPercent(results);
    }

    public String getRecentAvailabilityPercentFriendly() {
        return Utils.formatPercent(getRecentAvailabilityPercent());
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
	public void setReceiveBackups(boolean receiveBackups) {
		this.receiveBackups = receiveBackups;
	}
	public boolean isReceiveBackups() {
		return receiveBackups == null ||	//	Receive backups by default 
			   receiveBackups.booleanValue();
	}
	@Override
	public int hashCode() {
	    return getKey() == null 
	         ? super.hashCode()
	         : getKey().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (!(obj instanceof Job)) {
	        return false;
	    }
	    Job job = (Job) obj;
	    return getKey() == null
	         ? super.equals(obj)
	         : getKey().equals(job.getKey());
	}

	/**
	 * If client changes returned results he is responsible 
	 * for those changes to be persisted back to {@value #packedJobResults}, 
	 * e.g., by calling {@link #endUpdateJobResults()}. 
	 * 
	 * @return At most <code>numberOfResults</code> of recently added job results.
	 * If <code>numberOfResults == 0</code> returns all results.
	 * @since 13.05.2010
	 */
    public List<JobResult> getRecentJobResults(int numberOfResults) {
	    readJobResults();
	    
	    return numberOfResults == 0
	         ? jobResults
	         : (jobResults.size() == 0 
	                 ? jobResults
	                 : jobResults.subList(
	                         jobResults.size() > numberOfResults 
                                 ? jobResults.size() - numberOfResults 
                                 : 0, 
                             jobResults.size()));
	}

    @SuppressWarnings("unchecked")
    private void readJobResults() {
        if (jobResults == null) {
	        if (packedJobResults == null || packedJobResults.getBytes().length == 0) {
	            jobResults = new ArrayList<JobResult>();
	        } else {
    	        ObjectInputStream ois = null;
    	        try {
                    ois = new ObjectInputStream(new ByteArrayInputStream(packedJobResults.getBytes()));
                    jobResults = (List<JobResult>) ois.readObject();
                } catch (Exception e) {
                    logger.error("Error unpacking job results", e);
                    jobResults = new ArrayList<JobResult>();
                } finally {
                    if (ois != null) {
                        try { ois.close(); } catch (IOException e) { logger.error("Error closing ois", e); }
                    }
                }
	        }
	    }
    }

    /**
     * Call this method before batch adding job results
     * 
     * @since 13.05.2010
     */
    public void beginUpdateJobResults() {
        updatingJobResults = true;
    }
    
    /**
     * Call this method to flush batch added job results
     * 
     * @since 13.05.2010
     */
    public void endUpdateJobResults() {
        updatingJobResults = false;
        packJobResults();
    }
    
    /**
     * 
     * @param jobResult
     * 
     * @since 13.05.2010
     */
	public void addJobResult(JobResult jobResult) {
	    readJobResults();
	    jobResults.add(jobResult);
	    if (!updatingJobResults) {
	        packJobResults();
	    }
	}
	
    public void addJobResult(int index, JobResult result) {
        readJobResults();
        jobResults.add(index, result);
        if (!updatingJobResults) {
            packJobResults();
        }
    }

    public List<JobResult> removeJobResultsExceptRecent(int numberOfResultsToLeave) {
        readJobResults();
        
        List<JobResult> results = new ArrayList<JobResult>();
        
        while (jobResults.size() > numberOfResultsToLeave) {
            results.add(jobResults.get(0));
            jobResults.remove(0);
        }
        
        if (results.size() > 0) {
            packJobResults();
        }
        
        return results;
    }
    
	/**
	 * 
	 * @throws IOException
	 * @since 13.05.2010
	 */
    @PrePersist
	@PreUpdate
	void packJobResults() {
        if (jobResults != null) {
    	    ByteArrayOutputStream baos = new ByteArrayOutputStream(jobResults.size() * 14 + 365);
    	    ObjectOutputStream oos = null;
    	    try {
        	    oos = new ObjectOutputStream(baos);
        	    oos.writeObject(jobResults);
    	    } catch (IOException e) {
    	        logger.error("Error packing job results", e);
    	    } finally {
    	        if (oos != null) {
    	            try { oos.close(); } catch (IOException e) { logger.error("Error closing oos", e); }
    	        }
    	    }
    	    packedJobResults = new Blob(baos.toByteArray());
        }
	}
    int getPackedJobResultsLength() {
        return packedJobResults == null ? 0 : packedJobResults.getBytes().length;
    }

}
