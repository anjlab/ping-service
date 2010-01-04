package dmitrygusev.ping.services.dao;

import java.util.List;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;

public interface JobResultDAO {

	public void persistResult(JobResult result);

	public List<JobResult> getResults(Job job);
	
	public List<JobResult> getResults(Job job, int maxResults);
	
}
