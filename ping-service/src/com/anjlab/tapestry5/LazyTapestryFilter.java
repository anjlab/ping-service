package com.anjlab.tapestry5;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.TapestryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.gae.ProfilingDelegate;


public class LazyTapestryFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LazyTapestryFilter.class); 
    
    private Filter tapestryFilter;
    
    private FilterConfig config;
    
    public static FilterConfig FILTER_CONFIG;
    
	@Override
    public void init(FilterConfig config) throws ServletException
    {
        FILTER_CONFIG = config;
        this.config = config;
        ProfilingDelegate.register();
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
         throws IOException, ServletException
    {
        String requestURI = ((HttpServletRequest) request).getRequestURI();
        
        if (requestURI.startsWith("/filters/") || requestURI.equalsIgnoreCase("/favicon.ico"))
        {
            return;
        }
        
        if (tapestryFilter == null)
        {
            long startTime = System.currentTimeMillis();
            
            logger.info("Creating Tapestry Filter...");
            
            tapestryFilter = new TapestryFilter();
            tapestryFilter.init(config);
            
            logger.info("Tapestry Filter created and initialized ({} ms)", System.currentTimeMillis() - startTime);
        }
        
        tapestryFilter.doFilter(request, response, chain);
    }

    @Override
    public void destroy()
    {
        tapestryFilter.destroy();
    }

}
