package dmitrygusev.ping.services.dao.impl.cache;

import static dmitrygusev.ping.services.dao.impl.cache.CacheHelper.getEntityCacheKey;
import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.datastore.Key;

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
        if (cache.containsKey(entityCacheKey)) {
            return (Schedule) cache.get(entityCacheKey);
        }
        Schedule result = super.find(scheduleKey);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }
    
    @Override
    public void update(Schedule schedule) {
        super.update(schedule);
        Object entityCacheKey = getEntityCacheKey(Schedule.class, schedule.getId());
        if (cache.containsKey(entityCacheKey)) {
            cache.remove(entityCacheKey);
            cache.put(entityCacheKey, schedule);
        }
    }
}
