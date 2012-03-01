package com.anjlab.ping.services.dao.impl.cache;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.entities.Account;
import com.anjlab.ping.entities.Job;
import com.anjlab.ping.entities.Ref;


public class CacheHelper {

    private static final Logger logger = LoggerFactory.getLogger(AccountDAOImplCache.class);

    private static final Map<Class<?>, String> entityPrefixes = getPrefixMap();

    public static Object getQueryCacheKey(Class<?> entityClass, Object queryUniqueData) {
        return getEntityCacheKey(entityClass, "q" + (queryUniqueData == null ? "null" : queryUniqueData.hashCode()));
    }

    public static Object getEntityCacheKey(Class<?> entityClass, Object entityWideUniqueData) {
        //  TODO Use hashCodes and multi-value map
        if (entityPrefixes.containsKey(entityClass)) {
            return entityPrefixes.get(entityClass) + entityWideUniqueData;
        }
        logger.error("Entity prefix not found for class \"{}\". Such entities will consume more cache space.");
        return entityClass.getName() + entityWideUniqueData;
    }

    private static Map<Class<?>, String> getPrefixMap() {
        Map<Class<?>, String> result = new HashMap<Class<?>, String>();
        
        result.put(Job.class,        "J");
        result.put(Account.class,    "A");
        result.put(Ref.class,        "R");
        
        return result;
    }

}
