package dmitrygusev.ping.services;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.Translator;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.Local;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.RegistryShutdownHub;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.ComponentEventLinkEncoder;
import org.apache.tapestry5.services.Dispatcher;
import org.apache.tapestry5.services.MarkupRenderer;
import org.apache.tapestry5.services.MarkupRendererFilter;
import org.apache.tapestry5.services.MetaDataLocator;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.PageRenderRequestParameters;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestExceptionHandler;
import org.apache.tapestry5.services.RequestFilter;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.RequestHandler;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tynamo.jpa.JPAEntityManagerSource;
import org.tynamo.jpa.JPASymbols;
import org.tynamo.jpa.JPATransactionAdvisor;
import org.tynamo.jpa.JPATransactionManager;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.apphosting.api.DeadlineExceededException;

import dmitrygusev.ping.services.dao.AccountDAO;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.RefDAO;
import dmitrygusev.ping.services.dao.ScheduleDAO;
import dmitrygusev.ping.services.dao.impl.cache.AccountDAOImplCache;
import dmitrygusev.ping.services.dao.impl.cache.JobDAOImplCache;
import dmitrygusev.ping.services.dao.impl.cache.RefDAOImplCache;
import dmitrygusev.ping.services.dao.impl.cache.ScheduleDAOImplCache;
import dmitrygusev.ping.services.security.AccessController;
import dmitrygusev.tapestry5.TimeTranslator;
import dmitrygusev.tapestry5.gae.LazyJPAEntityManagerSource;
import dmitrygusev.tapestry5.gae.LazyJPATransactionManager;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to
 * configure and extend Tapestry, or to place your own service definitions.
 */
public class AppModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(JobExecutor.class).preventReloading().preventDecoration();
        binder.bind(Mailer.class).preventReloading().preventDecoration();

        binder.bind(AccountDAO.class, AccountDAOImplCache.class).preventReloading();
        binder.bind(JobDAO.class, JobDAOImplCache.class).preventReloading();
        binder.bind(RefDAO.class, RefDAOImplCache.class).preventReloading();
        binder.bind(ScheduleDAO.class, ScheduleDAOImplCache.class).preventReloading();
    }

    public static void contributeIgnoredPathsFilter(Configuration<String> configuration) {
        //    GAE filters
        configuration.add("/_ah/.*");
        //  GAE Appstats
        configuration.add("/appstats/.*");
    }
    
    public static Application buildApplication(ScheduleDAO scheduleDAO, 
                                               AccountDAO accountDAO, 
                                               JobDAO jobDAO, 
                                               RefDAO refDAO, 
                                               GAEHelper gaeHelper, 
                                               JobExecutor jobExecutor, 
                                               Mailer mailer,
                                               ApplicationStateManager stateManager,
                                               PageRenderLinkSource linkSource,
                                               RequestGlobals globals,
                                               MemcacheService memcache)
    {
        return new Application(accountDAO, jobDAO, scheduleDAO, 
                refDAO, gaeHelper, jobExecutor, mailer, linkSource,
                globals);
    }

    public static Cache buildCache(Logger logger, PerthreadManager perthreadManager) {
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            Cache cache = cacheFactory.createCache(Collections.emptyMap());
            
            LocalMemorySoftCache cache2 = new LocalMemorySoftCache(cache);

            if (perthreadManager != null) {
                perthreadManager.addThreadCleanupListener(cache2);
            }

            return cache2;
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
    
    public static AccessController buildAccessController(GAEHelper helper, RequestGlobals globals) {
        return new AccessController(helper, globals);
    }
    
    public static Logger buildLogger() {
        return LoggerFactory.getLogger(AppModule.class);
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
        //    DataNucleus' GAE implementation doesn't provide EMF.getMetamodel() which is required for
        //    providing entity value encoders
        configuration.add(JPASymbols.PROVIDE_ENTITY_VALUE_ENCODERS, "false");
        
        configuration.add(SymbolConstants.APPLICATION_VERSION, "beta");
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
    public RequestFilter buildTimingFilter(final Logger log, final Application application)
    {
        return new RequestFilter()
        {
            public boolean service(Request request, Response response, RequestHandler handler)
                    throws IOException
            {
                long startTime = System.currentTimeMillis();

                try
                {
                    if (!request.getPath().startsWith("/assets/") 
                            && !request.getPath().startsWith("/favicon.ico")) {
                        application.trackUserActivity();
                    }
                    
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
        
            //  TODO Investigate performance issue here
        
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

    public static void contributeTranslatorSource(MappedConfiguration<Class<?>, Translator<Date>> configuration)
    {
        configuration.add(Date.class, new TimeTranslator());
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
    
    public JPAEntityManagerSource buildLazyJPAEntityManagerSource(
            @Inject @Symbol(JPASymbols.PERSISTENCE_UNIT) String persistenceUnit, 
            RegistryShutdownHub hub)
    {
        LazyJPAEntityManagerSource source = new LazyJPAEntityManagerSource(persistenceUnit);
        
        hub.addRegistryShutdownListener(source);
        
        return source;
    }
    
    public JPATransactionManager buildLazyJPATransactionManager(final JPAEntityManagerSource source)
    {
        return new LazyJPATransactionManager(source);
    }

    @SuppressWarnings("unchecked")
    public void contributeServiceOverride(
            MappedConfiguration<Class, Object> configuration,
            @Local JPAEntityManagerSource source,
            @Local JPATransactionManager manager)
    {
        configuration.add(JPAEntityManagerSource.class, source);
        configuration.add(JPATransactionManager.class, manager);
    }
    
    @Match("*DAO")
    public static void adviseTransactions(JPATransactionAdvisor advisor, MethodAdviceReceiver receiver)
    {
        advisor.addTransactionCommitAdvice(receiver);
    }

    /*
     * Support pages without markup
     */
    private static final String NO_MARKUP_SYMBOL = "NoMarkup";
    public static final String NO_MARKUP = NO_MARKUP_SYMBOL + "=true";

    public static void contributeFactoryDefaults(MappedConfiguration<String, String> configuration)
    {
        configuration.add(NO_MARKUP_SYMBOL, "");
    }
        
    public void contributeMarkupRenderer(OrderedConfiguration<MarkupRendererFilter> configuration, 
                                         final MetaDataLocator metaDataLocator, 
                                         final ComponentEventLinkEncoder linkEncoder, 
                                         final RequestGlobals globals)
    {
        configuration.add(NO_MARKUP_SYMBOL, 
            new MarkupRendererFilter()
            {
                @Override
                public void renderMarkup(MarkupWriter writer, MarkupRenderer renderer) {
                    PageRenderRequestParameters parameters = linkEncoder.decodePageRenderRequest(globals.getRequest());
    
                    boolean noMarkup = metaDataLocator.findMeta(NO_MARKUP_SYMBOL, parameters.getLogicalPageName(), 
                                                                Boolean.class);
                    
                    if (noMarkup) {
                        //  Provide default (empty) markup
                        writer.element("html");
                    } else {
                        renderer.renderMarkup(writer);
                    }
                }
            }, "before:*");
    }
    
    public static void contributeClasspathAssetAliasManager(MappedConfiguration<String, String> configuration)
    {
        configuration.add("cubics", "anjlab/cubics");
    }
    
    public RequestExceptionHandler decorateRequestExceptionHandler(
            final Logger logger,
            final Response response,
            @Symbol(SymbolConstants.PRODUCTION_MODE)
            boolean productionMode)
    {
        if (!productionMode) return null;

        return new RequestExceptionHandler()
        {
            public void handleRequestException(Throwable exception) throws IOException
            {
                logger.error("Unexpected runtime exception: " + exception.getMessage(), exception);
                
                if (exception instanceof DeadlineExceededException) {
                    response.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, null);
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
                }
            }
        };
    }
    
//    @Match("*")
//    public static void adviseProfiler(final MethodAdviceReceiver receiver)
//    {
//        final MethodAdvice advice = new ProfilingAdvice(receiver.getInterface().getName());
//
//        for (Method m : receiver.getInterface().getMethods()) {
//            receiver.adviseMethod(m, advice);
//        };
//    }
//    
//    public static void contributeComponentClassTransformWorker(
//            OrderedConfiguration<ComponentClassTransformWorker> configuration,
//            ObjectLocator locator,
//            InjectionProvider injectionProvider,
//            ComponentClassResolver resolver)
//    {
//        configuration.add("ProfilerWorker", new ComponentClassTransformWorker() {
//            
//            @Override
//            public void transform(ClassTransformation transformation, final MutableComponentModel model) {
//                final MethodAdvice profilingAdvice = new ProfilingAdvice(transformation.getClassName());
//
//                for (TransformMethod method : transformation.matchMethods(
//                        new Predicate<TransformMethod>() {
//                            @Override
//                            public boolean accept(TransformMethod method) {
//                                return !method.getMethodIdentifier().contains("getComponentResources")
//                                    && !Modifier.isStatic(method.getSignature().getModifiers())
//                                    && !Modifier.isAbstract(method.getSignature().getModifiers());
//                            }
//                        }))
//                {
//                    ComponentMethodAdvice advice = new ComponentMethodAdvice()
//                    {
//                        public void advise(ComponentMethodInvocation invocation)
//                        {
//                            profilingAdvice.advise(invocation);
//                        }
//                    }; 
//                    method.addAdvice(advice);
//                }
//            }
//        }, "before:Log");
//    }
}