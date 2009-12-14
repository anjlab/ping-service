package dmitrygusev.ping.entities;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Key;

@Entity
public class JobResult {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	@Basic
	private Key jobKey;
	private Integer responseTime;
	private Boolean failed;
	private Date timestamp;
	//	Since November 01 2009
	private Integer pingResult;
	
	public Key getJobKey() {
		return jobKey;
	}
	public void setJobKey(Key jobKey) {
		this.jobKey = jobKey;
	}
	public int getResponseTime() {
		return responseTime;
	}
	public void setResponseTime(int responseTime) {
		this.responseTime = responseTime;
	}
	public boolean isFailed() {
		return failed;
	}
	public void setFailed(boolean failed) {
		this.failed = failed;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public Long getId() {
		return id;
	}
	
	@Transient
	private Calendar calendar;
	
	@Transient
	private TimeZone timeZone;
	
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}
	
	private Calendar getCalendar() {
		if (calendar == null) {
			calendar = timeZone == null ? Calendar.getInstance() : Calendar.getInstance(timeZone);
			calendar.setTime(timestamp);
		}
		return calendar;
	}
	
	public int getYear() {
		return getCalendar().get(Calendar.YEAR);
	}
	public String getMonth() {
		return String.format("%02d", getCalendar().get(Calendar.MONTH)) 
			 + " - " 
		     + getCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH);
	}
	public int getDay() {
		return getCalendar().get(Calendar.DATE);
	}
	public int getHour() {
		return getCalendar().get(Calendar.HOUR_OF_DAY);
	}
	public String getDayOfWeek() {
		return getCalendar().get(Calendar.DAY_OF_WEEK) 
			 + " - " 
			 + getCalendar().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
	}
	public String getDayTime() {
		int hour = getHour();
		if (hour >= 9 && hour < 17) {
			return "Day";
		}
		if (hour >= 17 && hour < 22) {
			return "Evening";
		}
		return "Night";
	}
	public int getSucceeded() {
		return failed ? 0 : 1;
	}
	public int getWeekOfMonth() {
		return getCalendar().get(Calendar.WEEK_OF_MONTH);
	}
	public void setPingResult(int pingResult) {
		this.pingResult = pingResult;
	}
	public Integer getPingResult() {
		if (pingResult == null) {
			//	For older records (earlier then November 1 2009)
			return failed 
				 ? Job.PING_RESULT_CONNECTIVITY_PROBLEM		//	This was the major fail reason 
				 : Job.PING_RESULT_OK;
		}
		return pingResult;
	}
}
