package com.anjlab.ping.services.dao.impl.cache;

import static com.anjlab.ping.services.dao.impl.cache.CacheHelper.getEntityCacheKey;
import static com.anjlab.ping.services.dao.impl.cache.CacheHelper.getQueryCacheKey;

import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.anjlab.ping.entities.Account;
import com.anjlab.ping.entities.Ref;
import com.anjlab.ping.services.dao.impl.RefDAOImpl;


public class RefDAOImplCache extends RefDAOImpl {

    @Inject
    private Cache cache;
    
    @SuppressWarnings("unchecked")
    @Override
    public Ref addRef(Account account, String scheduleName, int accessType) {
        Ref result = super.addRef(account, scheduleName, accessType);
        
        abandonCache(result);
        
        Object entityCacheKey = getEntityCacheKey(Ref.class, getRefWideUniqueData(account.getId(), scheduleName));
        cache.put(entityCacheKey, result);
        
        return result;
    }
    
    private Object getRefWideUniqueData(Long accountId, String scheduleName) {
        return accountId + "+" + scheduleName;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Ref find(Account account, String scheduleName) {
        Object entityCacheKey = getEntityCacheKey(Ref.class, getRefWideUniqueData(account.getId(), scheduleName));
        Ref result = (Ref) cache.get(entityCacheKey); 
        if (result != null) {
            return result;
        }
        result = super.find(account, scheduleName);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
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
    public List<Ref> getRefs(String scheduleName) {
        Object entityCacheKey = getQueryCacheKey(Ref.class, scheduleName);
        List<Ref> result = (List<Ref>) cache.get(entityCacheKey); 
        if (result != null) {
            return result;
        }
        result = super.getRefs(scheduleName);
        if (result != null) {
            ArrayList<Ref> serializableList = 
                new ArrayList<Ref>(result.subList(0, result.size()));
            cache.put(entityCacheKey, serializableList);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Ref> getRefs(Account account) {
        Object entityCacheKey = getRefAccountEntityCacheKey(account.getId());
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
                getRefWideUniqueData(ref.getAccountKey().getId(), ref.getScheduleName()));
        cache.remove(entityCacheKey);
        cache.remove(getRefAccountEntityCacheKey(ref.getAccountKey().getId()));
        cache.remove(getQueryCacheKey(Ref.class, ref.getScheduleName()));
    }

    private Object getRefAccountEntityCacheKey(Long accountId) {
        return getEntityCacheKey(Ref.class, "account/" + accountId);
    }
}
