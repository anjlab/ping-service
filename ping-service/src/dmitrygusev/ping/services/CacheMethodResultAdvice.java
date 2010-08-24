/**
 * 
 */
package dmitrygusev.ping.services;

import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.ioc.Invocation;
import org.apache.tapestry5.ioc.MethodAdvice;

public class CacheMethodResultAdvice implements MethodAdvice {

    private Cache cache;
    private Class<?> advisedClass;
    private Object nullObject = new Object();
    
    public CacheMethodResultAdvice(Class<?> advisedClass, Cache cache) {
        this.advisedClass = advisedClass;
        this.cache = cache;
    }
    
    @Override
    public void advise(Invocation invocation) {
        String entityCacheKey = getEntityCacheKey(invocation);
        
        Object result;
        
        if (cache.containsKey(entityCacheKey))
        {
            result = cache.get(entityCacheKey);
            
            invocation.overrideResult(result);
        }
        else 
        {
            invocation.proceed();
            
            if (!invocation.isFail())
            {
                result = invocation.getResult();
                
                cache.put(entityCacheKey, result);
            }
        }
    }

    private String getEntityCacheKey(Invocation invocation) {
        StringBuilder builder = new StringBuilder(150);
        builder.append(advisedClass.getName());
        builder.append('.');
        builder.append(invocation.getMethodName());
        builder.append('(');
        for (int i = 0; i < invocation.getParameterCount(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            Class<?> type = invocation.getParameterType(i);
            builder.append(type.getName());
            builder.append(' ');

            Object param = invocation.getParameter(i);
            builder.append(param != null ? param : nullObject);
        }
        builder.append(')');
        
        String entityCacheKey = String.valueOf(builder.toString().hashCode());
        return entityCacheKey;
    }
    
}