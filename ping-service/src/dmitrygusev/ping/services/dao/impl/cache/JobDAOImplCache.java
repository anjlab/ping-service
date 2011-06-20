package dmitrygusev.ping.services.dao.impl.cache;

import static dmitrygusev.ping.services.dao.impl.cache.CacheHelper.getEntityCacheKey;

import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.ping.services.dao.impl.JobDAOImpl;

public class JobDAOImplCache extends JobDAOImpl {

    @Inject private Cache cache;
    
    public JobDAOImplCache() {
    }
    
    public JobDAOImplCache(Cache cache) {
        this.cache = cache;
    }
    
    @Override
    public void delete(Long scheduleId, Long id) {
        super.delete(scheduleId, id);
        Object entityCacheKey = getEntityCacheKey(Job.class, getJobWideUniqueData(scheduleId, id));
        cache.remove(entityCacheKey);
        abandonScheduleCache(scheduleId);
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
        return getJobWideUniqueData(jobKey.getParent().getId(), jobKey.getId());
    }

    private String getJobWideUniqueData(Long scheduleId, Long id) {
        return scheduleId + "/" + id;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Job find(Long scheduleId, Long id) {
        Object entityCacheKey = getEntityCacheKey(Job.class, getJobWideUniqueData(scheduleId, id));
        Job result = (Job) cache.get(entityCacheKey);
        if (result != null) {
            return result;
        }
        result = super.find(scheduleId, id);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void update(Job job, boolean commitAfter) {
        super.update(job, commitAfter);
        Object entityCacheKey = getEntityCacheKey(Job.class, getJobWideUniqueData(job.getKey()));

        Job cachedJob = (Job)cache.get(entityCacheKey);

        if (cachedJob != null) {
            
            if (!cachedJob.getCronString().equals(job.getCronString())) {
                abandonJobsByCronStringCache(cachedJob.getCronString());
                abandonJobsByCronStringCache(job.getCronString());
            }
            
            cache.put(entityCacheKey, job);
        } else {
            abandonJobsByCronStringCache();
        }
        
        updateJobInScheduleCache(job);
    }

    @SuppressWarnings("unchecked")
    private void updateJobInScheduleCache(Job job) {
        Long scheduleId = job.getKey().getParent().getId();
        
        Object entityCacheKey = getEntityCacheKey(Schedule.class, scheduleId);
        
        Schedule schedule = (Schedule) cache.get(entityCacheKey);
        
        if (schedule == null) {
            return; //  Nothing to update
        }
        
        schedule.updateJob(job);
        
        cache.put(entityCacheKey, schedule);
    }

    private void abandonScheduleCache(Long scheduleId) {
        Object entityCacheKey = getEntityCacheKey(Schedule.class, scheduleId);
        cache.remove(entityCacheKey);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Key> getJobsByCronString(String cronString) {
        Object entityCacheKey = getEntityCacheKey(Job.class, cronString);
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
    
    private void abandonJobsByCronStringCache() {
        for (String cronString : Utils.getCronStringModel().split(",")) {
            abandonJobsByCronStringCache(cronString);
        }
    }
    
    private void abandonJobsByCronStringCache(String cronString) {
        Object entityCacheKey = getEntityCacheKey(Job.class, cronString);
        cache.remove(entityCacheKey);
    }
}
