/**
 * 
 */
package dmitrygusev.ping.services;

import javax.cache.Cache;

import org.apache.tapestry5.ioc.Invocation;
import org.apache.tapestry5.ioc.MethodAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheMethodResultAdvice implements MethodAdvice {

    private static final Logger logger = LoggerFactory.getLogger(CacheMethodResultAdvice.class);
    
    private final Cache cache;
    private final Class<?> advisedClass;
    private final Object nullObject = new Object();
    
    public CacheMethodResultAdvice(Class<?> advisedClass, Cache cache) {
        this.advisedClass = advisedClass;
        this.cache = cache;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void advise(Invocation invocation) {
        String invocationSignature = getInvocationSignature(invocation);
        
        String entityCacheKey = String.valueOf(invocationSignature.hashCode());

        Object result;
        
        if (cache.containsKey(entityCacheKey))
        {
            result = cache.get(entityCacheKey);

            logger.debug("Using invocation result ({}) from cache '{}'", invocationSignature, result);

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

    private String getInvocationSignature(Invocation invocation) {
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
        
        return builder.toString();
    }
    
}