package com.anjlab.gae;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.cache.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.services.Utils;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.apphosting.api.ApiProxy.OverQuotaException;


public class QuotaDetails {

    private static final Logger logger = LoggerFactory.getLogger(QuotaDetails.class);
    
    public static final String DATASTORE_WRITE_EXCEPTION =
            "The API call datastore_v\\d+\\.Put\\(\\) required more quota than is available.*";

    public enum Quota {
        DatastoreWrite
    }
    
    private Cache cache;
    private MemcacheService memcache;
    
    //  Cache is a wrapper for MemcacheService, but cache instance may also
    //  be LocalMemorySoftCache instance, and we need both of them here 
    //  to synchronize local cache with MemcacheService
    public QuotaDetails(Cache cache, MemcacheService memcache) {
        this.cache = cache;
        this.memcache = memcache;
    }
    
    public boolean isQuotaLimited(Quota quota) {
        Object marker = cache.get(quota);
        if (marker == null || !(marker instanceof Long)) {
            return false;
        }
        
        return true;
    }
    
    public void setQuotaLimited(Quota quota, boolean isLimited) {
        if (isLimited) {
            int minutesInMillis = 1000 * 60;
            
            //  Daily quotas are refreshed daily at midnight Pacific time.
            //  http://code.google.com/intl/ru/appengine/docs/quotas.html#Safety_Quotas_and_Billable_Quotas
            
            TimeZone US_PACIFIC_TIME = TimeZone.getTimeZone("America/Los_Angeles");
            Calendar cal = Calendar.getInstance(US_PACIFIC_TIME);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int date = cal.get(Calendar.DATE);
            cal.clear();
            cal.set(year, month, date);
            
            long previousQuotaRefreshTimeMillis = cal.getTimeInMillis();
            
            cal.add(Calendar.DATE, 1);
            cal.add(Calendar.MINUTE, 5);    //  Wait 5 minutes after quota refresh
            
            long nextQuotaRefreshTimeMillis = cal.getTimeInMillis();
            
            long currentTimeMillis = System.currentTimeMillis();
            
            Integer expirationDeltaMillis = (currentTimeMillis - previousQuotaRefreshTimeMillis) > (60 * minutesInMillis) 
                                          ? (int) (nextQuotaRefreshTimeMillis - currentTimeMillis)
                                          : (int) (5 * minutesInMillis);
            
            logger.warn("Setting 'quota limited' marker for DatastoreWrite operations with expiration delta = {}", 
                    Utils.formatMillisecondsToWordsUpToMinutes(expirationDeltaMillis));
            
            Long expirationTimeMillis = currentTimeMillis + expirationDeltaMillis;
            
            //  Remove object from local cache
            cache.remove(quota);
            
            memcache.put(quota, expirationTimeMillis, Expiration.byDeltaMillis(expirationDeltaMillis));
        } else {
            // this will remove object from local cache, and from memcache
            cache.remove(quota);
        }
    }

    public void checkOverQuotaException(Exception e) {
        if (e == null) {
            return;
        }
        
        if (Utils.isCause(e, OverQuotaException.class)) {
            if (Pattern.matches(DATASTORE_WRITE_EXCEPTION, e.getMessage())) {
                setQuotaLimited(Quota.DatastoreWrite, true);
            }
        }
    }

    public boolean isQuotaLimited() {
        return isQuotaLimited(Quota.DatastoreWrite);
    }

    public long getQuotaLimitExpirationMillis() {
        Object marker = cache.get(Quota.DatastoreWrite);
        if (marker == null || !(marker instanceof Long)) {
            return 0;
        }
        return (Long) marker;
    }
}
