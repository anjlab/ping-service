package dmitrygusev.ping.services.dao.impl.cache;

import static dmitrygusev.ping.services.dao.impl.cache.CacheHelper.getEntityCacheKey;

import javax.persistence.EntityManager;

import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.dao.impl.ScheduleDAOImpl;

public class ScheduleDAOImplCache extends ScheduleDAOImpl {

    @Inject private Cache cache;

    @Override
    public Schedule createSchedule(String name) {
        Schedule result = super.createSchedule(name);
        
        Object entityCacheKey = getEntityCacheKey(Schedule.class, result.getId());
        cache.put(entityCacheKey, result);

        return result;
    }
    
    @Override
    public void delete(Long id) {
        super.delete(id);
        
        Object entityCacheKey = getEntityCacheKey(Schedule.class, id);
        cache.remove(entityCacheKey);
    }
    
    @Override
    public Schedule find(Key scheduleKey) {
        Object entityCacheKey = getEntityCacheKey(Schedule.class, scheduleKey.getId());
        Schedule result = (Schedule) cache.get(entityCacheKey);
        if (result != null) {
            return result;
        }
        result = super.find(scheduleKey);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }
    
    @Inject private EntityManager em;
    
    @Override
    public void update(Schedule schedule) {
        for (Job job : schedule.getJobs()) {
            if (job.getKey() == null) {
                //  New job
                abandonJobsByCronStringCache(job.getCronString());
                //  Done. Because in current implementation only one job may be added at a time.
                break;
            }
        }
        super.update(schedule);
        //  Cache requires new transaction to put object
        em.getTransaction().commit();
        em.getTransaction().begin();
        Object entityCacheKey = getEntityCacheKey(Schedule.class, schedule.getId());
        cache.put(entityCacheKey, schedule);
    }
    
    private void abandonJobsByCronStringCache(String cronString) {
        Object entityCacheKey = getEntityCacheKey(Job.class, cronString);
        cache.remove(entityCacheKey);
    }
}
