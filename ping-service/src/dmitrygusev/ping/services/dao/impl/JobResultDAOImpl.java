package dmitrygusev.ping.services.dao.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.services.dao.JobResultDAO;

public class JobResultDAOImpl implements JobResultDAO {

    private static final Logger logger = LoggerFactory.getLogger(JobResultDAOImpl.class);
    
	@Inject
    private EntityManager em;
	
	@Override
	public void persistResult(JobResult result) {
		em.persist(result);
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
	public void delete(Long id, Integer timeout) {
		Query q = em.createQuery("DELETE FROM JobResult r WHERE r.id = :id").setParameter("id", id);
		if (timeout > 0) {
		    q.setHint("datanucleus.datastoreWriteTimeout", timeout);
		    q.setHint("javax.persistence.query.timeout", timeout);
		}
		try {
		    q.executeUpdate();
		} catch (Exception e) {
		    logger.error("Error deleting job result", e);
		}
	}
}
