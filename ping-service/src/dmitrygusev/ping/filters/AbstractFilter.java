package dmitrygusev.ping.filters;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.internal.services.LinkImpl;
import org.apache.tapestry5.internal.services.RequestGlobalsImpl;
import org.apache.tapestry5.internal.services.ResponseImpl;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.services.AppModule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.JobExecutor;
import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.impl.cache.JobDAOImplCache;

public abstract class AbstractFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFilter.class);

    protected EntityManagerFactory emf;

    protected Application application;
    
    protected JobDAO jobDAO;

    protected final RequestGlobals globals = new RequestGlobalsImpl();

    public AbstractFilter() {
        super();
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @SuppressWarnings("unchecked")
    protected void lazyInit() {
        //  One instance might already be created by tapestry-jpa
        System.setProperty("appengine.orm.disable.duplicate.emf.exception", "");
        
        emf = Persistence.createEntityManagerFactory("transactions-optional");
        
        jobDAO = new JobDAOImplCache(AppModule.buildCache(logger));
        
        application = new Application(null, 
                                      jobDAO, 
                                      null, 
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
                                              String pageAddress = pageClass.getName().substring("dmitrygusev.ping.pages".length()).replaceAll("\\.", "/");
                                              
                                              return new LinkImpl(pageAddress, 
                                                                  false, 
                                                                  false, 
                                                                  new ResponseImpl(globals.getHTTPServletResponse()), 
                                                                  null);
                                          }
                                          @Override public Link createPageRenderLinkWithContext(Class pageClass, Object... context)
                                          {
                                              StringBuilder pageAddress = new StringBuilder(pageClass.getName().substring("dmitrygusev.ping.pages".length()).replaceAll("\\.", "/"));
                                              
                                              for (Object object : context) {
                                                  pageAddress.append("/");
                                                  pageAddress.append(object);
                                              }
                                              
                                              return new LinkImpl(pageAddress.toString(), 
                                                                  false, 
                                                                  false, 
                                                                  new ResponseImpl(globals.getHTTPServletResponse()), 
                                                                  null);
                                          }
                                      }, 
                                      globals, 
                                      null);
    }

    @Override
    public void destroy() {
        
    }

}