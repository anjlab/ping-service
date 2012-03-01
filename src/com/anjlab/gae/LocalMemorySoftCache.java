package com.anjlab.gae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.cache.CacheListener;
import javax.cache.CacheStatistics;

import org.datanucleus.util.SoftValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.memcache.MemcacheServiceException;

public class LocalMemorySoftCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(LocalMemorySoftCache.class);

    private final Cache cache;
    
    private final Map<Object, Object> map;
    
    @SuppressWarnings("unchecked")
    public LocalMemorySoftCache(Cache cache) {
        this.map = new SoftValueMap(100);
        this.cache = cache;
    }

    @Override
    public void addListener(CacheListener listener) {
        cache.addListener(listener);
    }

    @Override
    public void clear() {
        map.clear();
        cache.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key)
            || cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value)
            || cache.containsValue(value);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set entrySet() {
        return cache.entrySet();
    }

    @Override
    public void evict() {
        map.clear();
        cache.evict();
    }

    @Override
    public Object get(Object key) {
        Object value = map.get(key);
        if (value == null) {
            value = cache.get(key);
            if (value instanceof PartialArrayList) {
                ((PartialArrayList) value).setCache(cache);
                value = ((PartialArrayList) value).get();
            }
            map.put(key, value);
        }
        return value;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map getAll(Collection keys) {
        return cache.getAll(keys);
    }

    @Override
    public CacheEntry getCacheEntry(Object key) {
        return cache.getCacheEntry(key);
    }

    @Override
    public CacheStatistics getCacheStatistics() {
        return cache.getCacheStatistics();
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set keySet() {
        return cache.keySet();
    }

    @Override
    public void load(Object key) {
        cache.load(key);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void loadAll(Collection keys) {
        cache.loadAll(keys);
    }

    @Override
    public Object peek(Object key) {
        return cache.peek(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(Object key, Object value) {
        map.put(key, value);
        try
        {
            //  TODO Partial saving of big lists
            ArrayList<?> listValue = value instanceof ArrayList<?> ? (ArrayList<?>) value : null;
            
            if ( listValue != null 
                    && listValue.size() > 1
                    && listValue.get(0) instanceof SerializableEstimations)
            {
                PartialArrayList list = new PartialArrayList(key, (ArrayList<SerializableEstimations>) value);
                list.setCache(cache);
                return list.put();
            } else {
                return cache.put(key, value);
            }
        } catch (MemcacheServiceException e) {
            logger.warn("Error putting value into cache", e);
            
            //  A value may be already in cache. We should remove it to avoid
            //  not synchronous duplicates there and here in local map.
            remove(key);
            
            return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void putAll(Map t) {
        map.putAll(t);
        cache.putAll(t);
    }

    @Override
    public Object remove(Object key) {
        map.remove(key);
        return cache.remove(key);
    }

    @Override
    public void removeListener(CacheListener listener) {
        cache.removeListener(listener);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection values() {
        return cache.values();
    }
    
    /**
     * Reset in-memory cache but leave original cache untouched.
     */
    public void reset() {
        map.clear();
    }
}
