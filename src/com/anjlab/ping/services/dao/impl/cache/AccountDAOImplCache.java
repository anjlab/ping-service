package com.anjlab.ping.services.dao.impl.cache;

import static com.anjlab.ping.services.dao.impl.cache.CacheHelper.getEntityCacheKey;

import javax.cache.Cache;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.anjlab.ping.entities.Account;
import com.anjlab.ping.services.dao.impl.AccountDAOImpl;


public class AccountDAOImplCache extends AccountDAOImpl {

    @Inject private Cache cache;
    
    @SuppressWarnings("unchecked")
    @Override
    public Account find(Long id) {
        Object entityCacheKey = getEntityCacheKey(Account.class, id);
        Account result = (Account) cache.get(entityCacheKey);
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
    public Account getAccount(String email) {
        Object entityCacheKey = getEntityCacheKey(Account.class, email);
        Account result = (Account) cache.get(entityCacheKey);
        if (result != null) {
            return result;
        }
        result = super.getAccount(email);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }
    
    @Override
    public void delete(Long id) {
        super.delete(id);
        Object entityCacheKey = getEntityCacheKey(Account.class, id);
        cache.remove(entityCacheKey);
        
        //  TODO Account with email as a key will remain in cache, remove it also 
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void update(Account account) {
        super.update(account);
        Object entityCacheKey = getEntityCacheKey(Account.class, account.getId());
        cache.put(entityCacheKey, account);

        entityCacheKey = getEntityCacheKey(Account.class, account.getEmail());
        cache.put(entityCacheKey, account);
    }
}
