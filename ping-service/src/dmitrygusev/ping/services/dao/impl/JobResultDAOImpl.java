package dmitrygusev.ping.services.dao.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.services.dao.JobResultDAO;

public class JobResultDAOImpl implements JobResultDAO {

	@Inject
    private EntityManager em;
	
	@Override
	public void persistResult(JobResult result) {
		em.persist(result);
	}

	@Override
	public List<JobResult> getResults(Job job) {
		int maxResults = 0;
		
		return getResults(job, maxResults);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<JobResult> getResults(Job job, int maxResults) {
		Query q = em.createQuery("SELECT r FROM JobResult r WHERE r.jobKey = :jobKey ORDER BY r.timestamp DESC")
						.setParameter("jobKey", job.getKey());
		
		if (maxResults > 0) {
			q.setMaxResults(maxResults);
		}
	
		return q.getResultList();
	}
	
	@Override
	public void delete(Long id) {
		Query q = em.createQuery("DELETE FROM JobResult r WHERE r.id = :id").setParameter("id", id);
		q.executeUpdate();
	}
}
