package com.anjlab.ping.services.dao.impl.cache;

import static com.anjlab.ping.services.dao.impl.cache.CacheHelper.getEntityCacheKey;
import static com.anjlab.ping.services.dao.impl.cache.CacheHelper.getQueryCacheKey;

import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.services.Utils;
import com.anjlab.ping.services.dao.impl.JobDAOImpl;
import com.google.appengine.api.datastore.Key;

//  TODO Rewrite with AOP
public class JobDAOImplCache extends JobDAOImpl {

    private static final Logger logger = LoggerFactory.getLogger(JobDAOImplCache.class);
    
    @Inject
    private Cache cache;
    
    public JobDAOImplCache() {
    }
    
    public JobDAOImplCache(Cache cache) {
        this.cache = cache;
    }
    
    @Override
    public Job delete(Long id) {
        Job job = super.delete(id);
        Object entityCacheKey = getEntityCacheKey(Job.class, getJobWideUniqueData(id));
        cache.remove(entityCacheKey);
        
        if (job != null) {
            abandonJobCaches(job);
        }
        
        return job;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Job find(Key jobKey) {
        Object entityCacheKey = getEntityCacheKey(Job.class, getJobWideUniqueData(jobKey));
        Job result = (Job) cache.get(entityCacheKey);
        if (result != null) {
            return result;
        }
        result = super.find(jobKey);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }

    private String getJobWideUniqueData(Key jobKey) {
        Long[] parts = Utils.createJobContext(jobKey);
        return getJobWideUniqueData(parts[0]);
    }

    private String getJobWideUniqueData(Long id) {
        return "" + id;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Job find(Long id) {
        Object entityCacheKey = getEntityCacheKey(Job.class, getJobWideUniqueData(id));
        Job result = (Job) cache.get(entityCacheKey);
        if (result != null) {
            return result;
        }
        result = super.find(id);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onAfterCommitNewJob(Job job) {
      logger.info("New job created: {} ({})", job.getPingURL(), job.getKey());
      
      Object entityCacheKey = getEntityCacheKey(Job.class, getJobWideUniqueData(job.getKey()));
      cache.put(entityCacheKey, job);
      
      abandonJobsByCronStringQueryCache(job.getCronString());
      abandonJobsByScheduleNameQueryCache(job.getScheduleName());
      abandonJobsByPingUrlQueryCache(job.getPingURL());
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void update(Job job, boolean commitAfter) {
        super.update(job, commitAfter);
        Object entityCacheKey = getEntityCacheKey(Job.class, getJobWideUniqueData(job.getKey()));

        Job cachedJob = (Job)cache.get(entityCacheKey);

        if (cachedJob != null) {
            
            if (!Utils.equals(cachedJob.getCronString(), job.getCronString())) {
                abandonJobsByCronStringQueryCache(cachedJob.getCronString());
                abandonJobsByCronStringQueryCache(job.getCronString());
            }
            
            if (!Utils.equals(cachedJob.getScheduleName(), job.getScheduleName())) {
                abandonJobsByScheduleNameQueryCache(cachedJob.getScheduleName());
                abandonJobsByScheduleNameQueryCache(job.getScheduleName());
            }
            
            if (!Utils.equals(cachedJob.getPingURL(), job.getPingURL())) {
                abandonJobsByPingUrlQueryCache(cachedJob.getPingURL());
                abandonJobsByPingUrlQueryCache(job.getPingURL());
            }
            
            updateJobInAllQueries(job);
            
            cache.put(entityCacheKey, job);
        } else {
            abandonJobCaches(job);
        }
    }

    private void updateJobInAllQueries(Job job) {
        //  TODO Write update
        abandonJobCaches(job);
    }

    private void abandonJobCaches(Job job) {
        abandonJobsByCronStringQueryCache(job.getCronString());
        abandonJobsByScheduleNameQueryCache(job.getScheduleName());
        abandonJobsByPingUrlQueryCache(job.getPingURL());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Key> getJobsByCronString(String cronString) {
        Object entityCacheKey = getQueryCacheKey(Job.class, cronString);
        List<Key> result = (List<Key>) cache.get(entityCacheKey);
        if (result != null) {
            return result;
        }
        result = super.getJobsByCronString(cronString);
        if (result != null) {
            ArrayList<Key> serializableList = 
                new ArrayList<Key>(result.subList(0, result.size()));
            cache.put(entityCacheKey, serializableList);
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Job> findByPingURL(String pingURL) {
        Object entityCacheKey = getQueryCacheKey(Job.class, pingURL);
        List<Job> result = (List<Job>) cache.get(entityCacheKey);
        if (result != null) {
            return result;
        }
        result = super.findByPingURL(pingURL);
        if (result != null) {
            ArrayList<Job> serializableList = 
                new ArrayList<Job>(result.subList(0, result.size()));
            cache.put(entityCacheKey, serializableList);
        }
        return result;
    }
    
    private void abandonJobsByPingUrlQueryCache(String pingUrl) {
        Object entityCacheKey = getQueryCacheKey(Job.class, pingUrl);
        cache.remove(entityCacheKey);
    }
    
    private void abandonJobsByScheduleNameQueryCache(String scheduleName) {
        Object entityCacheKey = getQueryCacheKey(Job.class, scheduleName);
        cache.remove(entityCacheKey);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Job> findByScheduleName(String scheduleName) {
        Object entityCacheKey = getQueryCacheKey(Job.class, scheduleName);
        List<Job> result = (List<Job>) cache.get(entityCacheKey);
        if (result != null) {
            return result;
        }
        result = super.findByScheduleName(scheduleName);
        if (result != null) {
            ArrayList<Job> serializableList = 
                new ArrayList<Job>(result.subList(0, result.size()));
            cache.put(entityCacheKey, serializableList);
        }
        return result;
    }
    
    private void abandonJobsByCronStringQueryCache(String cronString) {
        Object entityCacheKey = getQueryCacheKey(Job.class, cronString);
        cache.remove(entityCacheKey);
    }
}
