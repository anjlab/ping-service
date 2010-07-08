package dmitrygusev.ping.services.dao.impl.cache;

import static dmitrygusev.ping.services.dao.impl.cache.CacheHelper.getEntityCacheKey;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Ref;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.dao.impl.RefDAOImpl;

public class RefDAOImplCache extends RefDAOImpl {

    @Inject private Cache cache;

    @Override
    public Ref addRef(Account account, Schedule schedule, int accessType) {
        Ref result = super.addRef(account, schedule, accessType);
        
        Object entityCacheKey = getEntityCacheKey(Ref.class, getRefWideUniqueData(account.getId(), schedule.getId()));
        cache.put(entityCacheKey, result);
        
        return result;
    }
    
    private Object getRefWideUniqueData(Long accountId, Long scheduleId) {
        return accountId + "+" + scheduleId;
    }
    
    @Override
    public Ref find(Account account, Schedule schedule) {
        Object entityCacheKey = getEntityCacheKey(Ref.class, getRefWideUniqueData(account.getId(), schedule.getId()));
        Ref result = (Ref) cache.get(entityCacheKey); 
        if (result != null) {
            return result;
        }
        result = super.find(account, schedule);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }

    @Override
    public Ref find(Long id) {
        Object entityCacheKey = getEntityCacheKey(Ref.class, id);
        Ref result = (Ref) cache.get(entityCacheKey); 
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
    public List<Ref> getRefs(Account account) {
        Object entityCacheKey = getEntityCacheKey(Ref.class, getRefAccountEntityCacheKey(account.getId()));
        List<Ref> result = (List<Ref>) cache.get(entityCacheKey); 
        if (result != null) {
            return result;
        }
        result = super.getRefs(account);
        if (result != null) {
            ArrayList<Ref> serializableList = 
                new ArrayList<Ref>(result.subList(0, result.size()));
            cache.put(entityCacheKey, serializableList);
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Ref> getRefs(Schedule schedule) {
        Object entityCacheKey = getEntityCacheKey(Ref.class, getRefScheduleEntityCacheKey(schedule.getId()));
        List<Ref> result = (List<Ref>) cache.get(entityCacheKey); 
        if (result != null) {
            return result;
        }
        result = super.getRefs(schedule);
        if (result != null) {
            ArrayList<Ref> serializableList = 
                new ArrayList<Ref>(result.subList(0, result.size()));
            cache.put(entityCacheKey, serializableList);
        }
        return result;
    }
    
    @Override
    public void removeRef(Long id) {
        Ref ref = find(id);
        
        super.removeRef(id);

        Object entityCacheKey = getEntityCacheKey(Ref.class, id);
        cache.remove(entityCacheKey);
        
        abandonCache(ref);
    }

    private void abandonCache(Ref ref) {
        Object entityCacheKey = getEntityCacheKey(Ref.class, 
                getRefWideUniqueData(ref.getAccountKey().getId(), ref.getScheduleKey().getId()));
        cache.remove(entityCacheKey);
        cache.remove(getRefAccountEntityCacheKey(ref.getAccountKey().getId()));
        cache.remove(getRefScheduleEntityCacheKey(ref.getScheduleKey().getId()));
    }

    private Object getRefScheduleEntityCacheKey(Long scheduleId) {
        return getEntityCacheKey(Ref.class, "schedule/" + scheduleId);
    }

    private Object getRefAccountEntityCacheKey(Long accountId) {
        return getEntityCacheKey(Ref.class, "account/" + accountId);
    }
}
