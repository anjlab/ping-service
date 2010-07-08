package dmitrygusev.ping.services.dao.impl;

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.dao.JobDAO;

@SuppressWarnings("unchecked")
public class JobDAOImpl implements JobDAO {

    private static final Logger logger = LoggerFactory.getLogger(JobDAOImpl.class);

	@Inject
    public EntityManager em;
	
	@Override
	public void setEntityManager(EntityManager em) {
	    this.em = em;
	}
	
	public List<Key> getJobsByCronString(String cronString) {
		Query q = em.createQuery("SELECT j.key FROM Job j WHERE j.cronString = :cronString");
		q.setParameter("cronString", cronString);
		return q.getResultList();
	}
	
	public void update(Job job, boolean commitAfter) {
	    if (!em.getTransaction().isActive()){
	        // see Application#internalUpdateJob(Job)
	        logger.debug("Transaction is not active. Begin new one...");
	        
	        // XXX Rewrite this to handle transactions more gracefully
	        em.getTransaction().begin();
	    }
		em.merge(job);
		
		if (commitAfter) {
		    em.getTransaction().commit();
		}
	}
	
	public void delete(Long scheduleId, Long id) {
	    Job job = internalGetJob(scheduleId, id);
	
		if (job != null) {
			em.remove(job);
		}
	}

    private Job internalGetJob(Long scheduleId, Long id) {
        Query q = em.createQuery("SELECT j FROM Job j WHERE j.key = :key").
            setParameter("key", createKey(Schedule.class.getSimpleName(), scheduleId).
                    getChild(Job.class.getSimpleName(), id));
    
        List<Job> result = q.getResultList();
        
        return result.isEmpty() ? null : result.get(0);
    }
	
	@Override
	public Job find(Long scheduleId, Long id) {
		return internalGetJob(scheduleId, id);
	}

	@Override
	public Job find(Key jobKey) {
		return em.find(Job.class, jobKey);
	}

    @Override
    public List<Job> getAll() {
        Query q = em.createQuery("SELECT FROM Job");
        return q.getResultList();
    }
}
