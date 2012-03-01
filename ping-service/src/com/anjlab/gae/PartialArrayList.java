package com.anjlab.gae;

import java.io.Serializable;
import java.util.ArrayList;

import javax.cache.Cache;

public class PartialArrayList implements Serializable {

    private static final long serialVersionUID = 2430022649786329747L;
    
    private transient Cache cache;
    private transient ArrayList<SerializableEstimations> listValue;
    private Object key;
    
    private int partsCount;
    
    public PartialArrayList(Object key, ArrayList<SerializableEstimations> listValue) {
        this.key = key;
        this.listValue = listValue;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ArrayList<?> get() {
        ArrayList result = new ArrayList();
        for (int i = 0; i < partsCount; i++) {
            ArrayList<?> currentPart = (ArrayList<?>) cache.get(getPartKey(i));
            if (currentPart == null) {
                //  Some parts were removed from cache
                return null;
            }
            result.addAll(currentPart);
        }
        return result;
    }
    
    @SuppressWarnings({ "unchecked" })
    public Object put() {
        this.partsCount = 0;
        
        ArrayList<SerializableEstimations> currentPart = new ArrayList<SerializableEstimations>();
        int currentSize = 0;
        
        for (SerializableEstimations item : listValue) {
            int itemSize = item.getEstimatedSerializedSize();
            //  Memcache single entry size limit is 1MB
            if (currentSize > 0 && (currentSize + itemSize > 900000)) {
                putPart(currentPart);
                
                currentPart = new ArrayList<SerializableEstimations>();
                currentSize = 0;
            }
            currentPart.add(item);
            currentSize += itemSize;
        }
        if (currentSize > 0) {
            putPart(currentPart);
        }
        
        return cache.put(key, this);
    }

    @SuppressWarnings("unchecked")
    private void putPart(ArrayList<SerializableEstimations> currentPart) {
        cache.put(getPartKey(partsCount), currentPart);
        partsCount++;
    }

    private String getPartKey(int partIndex) {
        return key.toString() + "-" + partIndex;
    }
    
}