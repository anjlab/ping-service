package dmitrygusev.ping.services;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.Translator;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.services.ApplicationStateContribution;
import org.apache.tapestry5.services.ApplicationStateCreator;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.Dispatcher;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestFilter;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.RequestHandler;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tynamo.jpa.JPASymbols;
import org.tynamo.jpa.JPATransactionAdvisor;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import dmitrygusev.ping.services.dao.AccountDAO;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.JobResultDAO;
import dmitrygusev.ping.services.dao.RefDAO;
import dmitrygusev.ping.services.dao.ScheduleDAO;
import dmitrygusev.ping.services.dao.impl.AccountDAOImpl;
import dmitrygusev.ping.services.dao.impl.JobDAOImpl;
import dmitrygusev.ping.services.dao.impl.JobResultDAOImpl;
import dmitrygusev.ping.services.dao.impl.RefDAOImpl;
import dmitrygusev.ping.services.dao.impl.ScheduleDAOImpl;
import dmitrygusev.ping.services.security.AccessController;
import dmitrygusev.tapestry5.TimeTranslator;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to
 * configure and extend Tapestry, or to place your own service definitions.
 */
public class AppModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(JobExecutor.class);
        binder.bind(Mailer.class);
        
        binder.bind(AccountDAO.class, AccountDAOImpl.class);
        binder.bind(JobDAO.class, JobDAOImpl.class);
        binder.bind(JobResultDAO.class, JobResultDAOImpl.class);
        binder.bind(RefDAO.class, RefDAOImpl.class);
        binder.bind(ScheduleDAO.class, ScheduleDAOImpl.class);
    }

    public static void contributeIgnoredPathsFilter(Configuration<String> configuration) {
    	//	GAE filters
    	configuration.add("/_ah/.*");
    }
    
    public static Application buildApplication(
    		ScheduleDAO scheduleDAO, 
    		AccountDAO accountDAO, 
    		JobDAO jobDAO, 
    		RefDAO refDAO, 
    		JobResultDAO jobResultDAO,
    		GAEHelper gaeHelper, 
    		JobExecutor jobExecutor, 
    		Mailer mailer,
    		ApplicationStateManager stateManager,
    		PageRenderLinkSource linkSource,
    		RequestGlobals globals)
    {
    	return new Application(accountDAO, jobDAO, scheduleDAO, 
    			refDAO, jobResultDAO, gaeHelper, jobExecutor, mailer,
    			stateManager, linkSource, globals);
    }

    public static Cache buildCache(Logger logger) {
        try {
        	CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
			return cacheFactory.createCache(Collections.emptyMap());
		} catch (CacheException e) {
			logger.error("Error instantiating cache", e);
			return null;
		}
    }

    public static MemcacheService buildMemcacheService(Logger logger) {
    	MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
    	
    	if (memcacheService == null) {
    		logger.error("MemcacheService is null.");
    	}
    	
    	return memcacheService;
    }
    
    public static GAEHelper buildGAEHelper(RequestGlobals requestGlobals) {
    	return new GAEHelper(requestGlobals);
    }
    
    public static AccessController buildAccessController(GAEHelper helper) {
    	return new AccessController(helper);
    }
    
    public static Logger buildLogger() {
    	return LoggerFactory.getLogger(AppModule.class);
    }
    
    public static JobResultCSVExporter buildJobResultCSVExporter() {
    	return new JobResultCSVExporter();
    }
    
    public static void contributeApplicationDefaults(
            MappedConfiguration<String, String> configuration)
    {
        // Contributions to ApplicationDefaults will override any contributions to
        // FactoryDefaults (with the same key). Here we're restricting the supported
        // locales to just "en" (English). As you add localised message catalogs and other assets,
        // you can extend this list of locales (it's a comma separated series of locale names;
        // the first locale name is the default when there's no reasonable match).
        
        configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en");

        // The factory default is true but during the early stages of an application
        // overriding to false is a good idea. In addition, this is often overridden
        // on the command line as -Dtapestry.production-mode=false
        configuration.add(SymbolConstants.PRODUCTION_MODE, "true");
        configuration.add(SymbolConstants.COMPRESS_WHITESPACE, "true");

        configuration.add(JPASymbols.PERSISTENCE_UNIT, "transactions-optional");
        //	DataNucleus' GAE implementation doesn't provide EMF.getMetamodel() which is required for
        //	providing entity value encoders
        configuration.add(JPASymbols.PROVIDE_ENTITY_VALUE_ENCODERS, "false");
    }

    /**
     * This is a service definition, the service will be named "TimingFilter". The interface,
     * RequestFilter, is used within the RequestHandler service pipeline, which is built from the
     * RequestHandler service configuration. Tapestry IoC is responsible for passing in an
     * appropriate Logger instance. Requests for static resources are handled at a higher level, so
     * this filter will only be invoked for Tapestry related requests.
     * 
     * <p>
     * Service builder methods are useful when the implementation is inline as an inner class
     * (as here) or require some other kind of special initialization. In most cases,
     * use the static bind() method instead. 
     * 
     * <p>
     * If this method was named "build", then the service id would be taken from the 
     * service interface and would be "RequestFilter".  Since Tapestry already defines
     * a service named "RequestFilter" we use an explicit service id that we can reference
     * inside the contribution method.
     */    
    public RequestFilter buildTimingFilter(final Logger log)
    {
        return new RequestFilter()
        {
            public boolean service(Request request, Response response, RequestHandler handler)
                    throws IOException
            {
                long startTime = System.currentTimeMillis();

                try
                {
                    // The responsibility of a filter is to invoke the corresponding method
                    // in the handler. When you chain multiple filters together, each filter
                    // received a handler that is a bridge to the next filter.
                    
                    return handler.service(request, response);
                }
                finally
                {
                    long elapsed = System.currentTimeMillis() - startTime;

                    log.info(String.format("Request time: %d ms", elapsed));
                }
            }
        };
    }
    
    public void contributeMasterDispatcher(OrderedConfiguration<Dispatcher> configuration,
            @InjectService("AccessController") Dispatcher accessController) {
            configuration.add("AccessController", accessController, "before:PageRender");
    }

    /**
     * This is a contribution to the RequestHandler service configuration. This is how we extend
     * Tapestry using the timing filter. A common use for this kind of filter is transaction
     * management or security. The @Local annotation selects the desired service by type, but only
     * from the same module.  Without @Local, there would be an error due to the other service(s)
     * that implement RequestFilter (defined in other modules).
     */
    public void contributeRequestHandler(OrderedConfiguration<RequestFilter> configuration,
    		@InjectService("TimingFilter") final RequestFilter timingFilter,
            @InjectService("Utf8Filter") final RequestFilter utf8Filter)
    {
        // Each contribution to an ordered configuration has a name, When necessary, you may
        // set constraints to precisely control the invocation order of the contributed filter
        // within the pipeline.
        
    	configuration.add("Utf8Filter", utf8Filter); // handle UTF-8
        configuration.add("Timing", timingFilter);
    }

    public static void contributeTranslatorSource(Configuration<Translator<Date>> configuration)
    {
        configuration.add(new TimeTranslator());
    }
    
    public void contributeValidationMessagesSource(OrderedConfiguration<String> configuration) {
		String messagesSource = TimeTranslator.class.getName().replace(".", "/");
		configuration.add("time-translator", messagesSource);
	}

    public RequestFilter buildUtf8Filter(
			@InjectService("RequestGlobals") final RequestGlobals requestGlobals) {
		return new RequestFilter() {
			public boolean service(Request request, Response response, 
					RequestHandler handler) throws IOException {
				requestGlobals.getHTTPServletRequest().setCharacterEncoding("UTF-8");
				return handler.service(request, response);
			}
		};
	}
    
    public void contributeApplicationStateManager(
    		MappedConfiguration<Class<?>, ApplicationStateContribution> configuration,
    		final GAEHelper helper, final AccountDAO accountDAO)
    {
    	ApplicationStateCreator<AppSessionCache> creator = new ApplicationStateCreator<AppSessionCache>() {
	        public AppSessionCache create() {
	        	return new AppSessionCache();
	        }
    	};
    
      configuration.add(AppSessionCache.class, new ApplicationStateContribution("session", creator));
    }
    
    public void contributeRegexAuthorizer(Configuration<String> regex)
    {
    	String pathPattern = "([^/.]+/)*[^/.]+\\.((css)|(js)|(jpg)|(jpeg)|(png)|(gif))$";
    	regex.add("^anjlab/cubics/css/" + pathPattern);
    	regex.add("^anjlab/cubics/images/" + pathPattern);
    	regex.add("^anjlab/cubics/js/" + pathPattern);
    	regex.add("^anjlab/cubics/js/jquery-1.3.2.js");
    }
    
    @Match("*DAO*")
    public static void adviseTransactions(JPATransactionAdvisor advisor, MethodAdviceReceiver receiver)   {
        advisor.addTransactionCommitAdvice(receiver);
    }

}