package dmitrygusev.ping.services.dao.impl.cache;

import static dmitrygusev.ping.services.dao.impl.cache.CacheHelper.getEntityCacheKey;
import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.services.dao.impl.AccountDAOImpl;

public class AccountDAOImplCache extends AccountDAOImpl {

    @Inject private Cache cache;
    
    @Override
    public Account find(Long id) {
        Object entityCacheKey = getEntityCacheKey(Account.class, id);
        if (cache.containsKey(entityCacheKey)) {
            return (Account) cache.get(entityCacheKey);
        }
        Account result = super.find(id);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }

    @Override
    public Account getAccount(String email) {
        Object entityCacheKey = getEntityCacheKey(Account.class, email);
        if (cache.containsKey(entityCacheKey)) {
            return (Account) cache.get(entityCacheKey);
        }
        Account result = super.getAccount(email);
        if (result != null) {
            cache.put(entityCacheKey, result);
        }
        return result;
    }
    
    @Override
    public void delete(Long id) {
        super.delete(id);
        Object entityCacheKey = getEntityCacheKey(Account.class, id);
        if (cache.containsKey(entityCacheKey)) {
            cache.remove(entityCacheKey);
        }
    }
    
    @Override
    public void update(Account account) {
        super.update(account);
        Object entityCacheKey = getEntityCacheKey(Account.class, account.getId());
        if (cache.containsKey(entityCacheKey)) {
            cache.remove(entityCacheKey);
            cache.put(entityCacheKey, account);
        }
    }
}
