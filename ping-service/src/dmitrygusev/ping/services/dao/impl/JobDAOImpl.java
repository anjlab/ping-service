package dmitrygusev.ping.services.dao.impl;

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.dao.JobDAO;

@SuppressWarnings("unchecked")
public class JobDAOImpl implements JobDAO {

	@Inject
    public EntityManager em;
	
	public List<Job> getJobsByCronString(String cronString) {
		Query q = em.createQuery("SELECT j FROM Job j WHERE j.cronString = :cronString");
		q.setParameter("cronString", cronString);
		return q.getResultList();
	}
	
	public void update(Job job) {
		em.merge(job);
	}
	
	public void delete(Long scheduleId, Long id) {
		Job job = find(scheduleId, id);
	
		if (job != null) {
			em.remove(job);
		}
	}
	
	@Override
	public Job find(Long scheduleId, Long id) {
		Query q = em.createQuery("SELECT j FROM Job j WHERE j.key = :key").
			setParameter("key", createKey(Schedule.class.getSimpleName(), scheduleId).
					getChild(Job.class.getSimpleName(), id));
		
		List<Job> result = q.getResultList();
		
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public Job find(Key jobKey) {
		return em.find(Job.class, jobKey);
	}
}
