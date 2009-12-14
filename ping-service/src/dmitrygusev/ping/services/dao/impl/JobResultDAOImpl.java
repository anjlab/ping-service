package dmitrygusev.ping.services.dao.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.services.dao.JobResultDAO;

public class JobResultDAOImpl implements JobResultDAO {

	@PersistenceContext
    private EntityManager em;
	
	@Override
	public void persistResult(JobResult result) {
		em.persist(result);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<JobResult> getResults(Job job) {
		List<JobResult> result = new ArrayList<JobResult>();
		
		Calendar c = Calendar.getInstance();
		c.set(2009, Calendar.SEPTEMBER, 1);
		
		Date timestamp = c.getTime(); 
		
		int prevSize;
		
		do {
			prevSize = result.size();
			
			Query q = em.createQuery(
					"SELECT r FROM JobResult r " 
					+ "WHERE r.jobKey = :jobKey AND r.timestamp > :timestamp "
					+ "ORDER BY r.timestamp ASC").
					
				setParameter("jobKey", job.getKey()).
				setParameter("timestamp", timestamp);
		
			result.addAll(q.getResultList());
			
			if (result.size() > 0) {
				c.setTime(result.get(result.size() - 1).getTimestamp());
				c.add(Calendar.SECOND, 1);
				timestamp = c.getTime();
			}
			
		} while (prevSize != result.size() && false /* don't do more than one request */);
		
		return result;
	}

}
