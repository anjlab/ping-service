package dmitrygusev.ping.services.dao;

import java.util.List;

import org.tynamo.jpa.annotations.CommitAfter;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;

public interface JobDAO {
    @CommitAfter
	public abstract List<Job> getJobsByCronString(String cronString);
	@CommitAfter
	public abstract void delete(Long scheduleId, Long jobId);
	@CommitAfter
	public abstract void update(Job job);
    @CommitAfter
	public abstract Job find(Long scheduleId, Long jobId);
    @CommitAfter
	public abstract Job find(Key stringToKey);
    @CommitAfter
    public abstract List<Job> getAllJobs();

}