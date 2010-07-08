package dmitrygusev.ping.services;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheEntry;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheListener;
import net.sf.jsr107cache.CacheStatistics;

import org.datanucleus.util.SoftValueMap;

public class LocalMemorySoftCache implements Cache {

    private final Cache cache;
    
    private final Map<Object, Object> map;
    
    @SuppressWarnings("unchecked")
    public LocalMemorySoftCache(Cache cache) {
        this.cache = cache;
        this.map = new SoftValueMap(100);
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
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache.containsValue(value);
    }

    @SuppressWarnings("unchecked")
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
            map.put(key, value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map getAll(Collection keys) throws CacheException {
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

    @SuppressWarnings("unchecked")
    @Override
    public Set keySet() {
        return cache.keySet();
    }

    @Override
    public void load(Object key) throws CacheException {
        cache.load(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadAll(Collection keys) throws CacheException {
        cache.loadAll(keys);
    }

    @Override
    public Object peek(Object key) {
        return cache.peek(key);
    }

    @Override
    public Object put(Object key, Object value) {
        map.put(key, value);
        return cache.put(key, value);
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    @Override
    public Collection values() {
        return cache.values();
    }
    
}
