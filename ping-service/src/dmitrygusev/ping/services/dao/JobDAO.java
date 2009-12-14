package dmitrygusev.ping.services.dao;

import java.util.List;

import dmitrygusev.ping.entities.Job;

public interface JobDAO {

	public abstract List<Job> getJobsByCronString(String cronString);

	public abstract void delete(Long scheduleId, Long jobId);

	public abstract void update(Job job);

	public abstract Job find(Long scheduleId, Long jobId);

}