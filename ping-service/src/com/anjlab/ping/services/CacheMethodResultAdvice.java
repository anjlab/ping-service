/**
 * 
 */
package com.anjlab.ping.services;

import javax.cache.Cache;

import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
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
    public void advise(MethodInvocation invocation) {
        String invocationSignature = getInvocationSignature(invocation);
        
        String entityCacheKey = String.valueOf(invocationSignature.hashCode());

        Object result;
        
        if (cache.containsKey(entityCacheKey))
        {
            result = cache.get(entityCacheKey);

            logger.debug("Using invocation result ({}) from cache '{}'", invocationSignature, result);

            invocation.setReturnValue(result);
        }
        else 
        {
            invocation.proceed();
            
            if (!invocation.didThrowCheckedException())
            {
                result = invocation.getReturnValue();
                
                cache.put(entityCacheKey, result);
            }
        }
    }

    private String getInvocationSignature(MethodInvocation invocation) {
        StringBuilder builder = new StringBuilder(150);
        builder.append(advisedClass.getName());
        builder.append('.');
        builder.append(invocation.getMethod().getName());
        builder.append('(');
        for (int i = 0; i < invocation.getMethod().getParameterTypes().length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            Class<?> type = invocation.getMethod().getParameterTypes()[i];
            builder.append(type.getName());
            builder.append(' ');

            Object param = invocation.getParameter(i);
            builder.append(param != null ? param : nullObject);
        }
        builder.append(')');
        
        return builder.toString();
    }
    
}