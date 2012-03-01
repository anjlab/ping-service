package com.anjlab.ping.filters;

import static com.anjlab.ping.services.AppModule.buildGAECache;
import static com.anjlab.ping.services.AppModule.buildMemcacheService;
import static com.anjlab.ping.services.AppModule.buildQuotaDetails;

import java.io.IOException;

import javax.cache.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.internal.services.BaseURLSourceImpl;
import org.apache.tapestry5.internal.services.DefaultSessionPersistedObjectAnalyzer;
import org.apache.tapestry5.internal.services.LinkImpl;
import org.apache.tapestry5.internal.services.LinkSecurity;
import org.apache.tapestry5.internal.services.RequestGlobalsImpl;
import org.apache.tapestry5.internal.services.RequestImpl;
import org.apache.tapestry5.internal.services.ResponseImpl;
import org.apache.tapestry5.internal.services.TapestrySessionFactoryImpl;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.gae.LocalMemorySoftCache;
import com.anjlab.gae.QuotaDetails;
import com.anjlab.ping.services.Application;
import com.anjlab.ping.services.JobExecutor;
import com.anjlab.ping.services.Mailer;
import com.anjlab.ping.services.dao.JobDAO;
import com.anjlab.ping.services.dao.impl.cache.JobDAOImplCache;


public abstract class AbstractFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFilter.class);

    protected EntityManagerFactory emf;

    protected Application application;
    
    protected JobDAO jobDAO;

    protected final RequestGlobals globals = new RequestGlobalsImpl();

    protected Cache cache;
    
    protected QuotaDetails quotaDetails;
    
    public AbstractFilter() {
        super();
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        cache = buildGAECache(logger, null);
        
        quotaDetails = buildQuotaDetails(cache, buildMemcacheService(logger));
    }

    @SuppressWarnings("rawtypes")
    protected void lazyInit() {
        //  One instance might already be created by tapestry-jpa
        System.setProperty("appengine.orm.disable.duplicate.emf.exception", "");
        
        emf = Persistence.createEntityManagerFactory("transactions-optional");
        
        jobDAO = new JobDAOImplCache(cache);
        
        application = new Application(null, 
                                      jobDAO, 
                                      null, 
                                      null, 
                                      new JobExecutor(), 
                                      new Mailer(), 
                                      new PageRenderLinkSource()
                                      {
                                          @Override public Link createPageRenderLinkWithContext(Class pageClass, EventContext eventContext) { return null; }
                                          @Override public Link createPageRenderLinkWithContext(String pageName, EventContext eventContext) { return null; }
                                          @Override public Link createPageRenderLinkWithContext(String pageName, Object... context) { return null; }
                                          @Override public Link createPageRenderLink(String pageName) { return null; }
                                          @Override public Link createPageRenderLink(Class pageClass) 
                                          {
                                              String pageAddress = pageClass.getName().substring(Application.APP_PAGES_PACKAGE.length()).replaceAll("\\.", "/");
                                              
                                              return new LinkImpl(pageAddress, 
                                                      false, 
                                                      LinkSecurity.INSECURE, 
                                                      new ResponseImpl(globals.getHTTPServletRequest(), globals.getHTTPServletResponse()), 
                                                      null,
                                                      new BaseURLSourceImpl(globals.getRequest(), "", 0, 0));
                                          }
                                          @Override public Link createPageRenderLinkWithContext(Class pageClass, Object... context)
                                          {
                                              StringBuilder pageAddress = new StringBuilder(pageClass.getName().substring(Application.APP_PAGES_PACKAGE.length()).replaceAll("\\.", "/"));
                                              
                                              for (Object object : context) {
                                                  pageAddress.append("/");
                                                  pageAddress.append(object);
                                              }
                                              
                                              return new LinkImpl(pageAddress.toString(), 
                                                                  false, 
                                                                  LinkSecurity.INSECURE, 
                                                                  new ResponseImpl(globals.getHTTPServletRequest(), globals.getHTTPServletResponse()), 
                                                                  null,
                                                                  new BaseURLSourceImpl(globals.getRequest(), "", 0, 0));
                                          }
                                      }, 
                                      globals,
                                      null,
                                      null);
    }

    public void setApplication(Application application) {
        this.application = application;
    }
    
    @Override
    public void destroy() {
        
    }

    protected abstract void processRequest(EntityTransaction tx) throws Exception;
    
    protected boolean disableFilter() {
//        logger.warn("Temporarily disabling all filters");
        return false;
    }
    
    @Override
    public synchronized void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        long startTime = System.currentTimeMillis();
        
        EntityManager em = null;
        EntityTransaction tx = null;
        
        try
        {
            if (disableFilter()) {
                logger.debug("Filter disabled for request: {}", ((HttpServletRequest) request).getRequestURI());
                return;
            }
            
            if (emf == null) {
                lazyInit();
            }
            
            em = emf.createEntityManager();
            
            tx = em.getTransaction();
            
            tx.begin();
            
            jobDAO.setEntityManager(em);
            
            HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            HttpServletResponse httpServletResponse = (HttpServletResponse)response;
            globals.storeServletRequestResponse(httpServletRequest, httpServletResponse);
            globals.storeRequestResponse(new RequestImpl(httpServletRequest, 
                                                         httpServletRequest.getCharacterEncoding(), 
                                                         new TapestrySessionFactoryImpl(
                                                                 true, 
                                                                 new DefaultSessionPersistedObjectAnalyzer(),
                                                                 httpServletRequest)), 
                                         new ResponseImpl(httpServletRequest, httpServletResponse));
            
            processRequest(tx);
            
            if (tx.isActive()) {
                tx.commit();
            }
        }
        catch (Exception e)
        {
            logger.error("Error processing request", e);
            
            quotaDetails.checkOverQuotaException(e);
        }
        finally
        {
            if (cache instanceof LocalMemorySoftCache) {
                ((LocalMemorySoftCache) cache).reset();
            }
            
            if (tx != null && tx.isActive())
            {
                tx.rollback();
            }
            
            if (em != null) {
                em.close();
            }
            
            long endTime = System.currentTimeMillis();
            
            logger.debug("Total time: " + (endTime - startTime));
        }
    }

}