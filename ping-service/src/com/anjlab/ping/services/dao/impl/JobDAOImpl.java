package com.anjlab.ping.services.dao.impl;

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.services.dao.JobDAO;
import com.google.appengine.api.datastore.Key;


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
    
    public Job delete(Long id) {
        Job job = internalGetJob(id);
    
        if (job != null) {
            em.remove(job);
        }
        
        return job;
    }

    private Job internalGetJob(Long id) {
        Key jobKey = createKey(Job.class.getSimpleName(), id);
        
        return em.find(Job.class, jobKey);
    }
    
    @Override
    public Job find(Long id) {
        return internalGetJob(id);
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
    
    @Override
    public List<Job> findByScheduleName(String scheduleName) {
        Query q = em.createQuery("SELECT FROM Job j WHERE j.scheduleName = :scheduleName").
                        setParameter("scheduleName", scheduleName);
        return q.getResultList();
    }
    
    @Override
    public Job createJob(Job job) {
        em.persist(job);
        return job;
    }

    @Override
    public List<Job> findByPingURL(String pingURL) {
        Query q = em.createQuery("SELECT FROM Job j WHERE j.pingURL = :pingURL").
                setParameter("pingURL", pingURL);
        return q.getResultList();
    }
    
    @Override
    public void onAfterCommitNewJob(Job job) {
    }
}
