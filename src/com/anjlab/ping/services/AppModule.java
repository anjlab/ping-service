package com.anjlab.ping.services;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.http.HttpServletResponse;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.Translator;
import org.apache.tapestry5.internal.jpa.EntityManagerSourceImpl;
import org.apache.tapestry5.internal.services.ResourceStreamer;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.annotations.Primary;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.ThreadCleanupListener;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.apache.tapestry5.jpa.EntityManagerSource;
import org.apache.tapestry5.jpa.JpaSymbols;
import org.apache.tapestry5.jpa.JpaTransactionAdvisor;
import org.apache.tapestry5.jpa.PersistenceUnitConfigurer;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.AssetPathConverter;
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
import org.apache.tapestry5.services.ResponseCompressionAnalyzer;
import org.apache.tapestry5.services.linktransform.PageRenderLinkTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.gae.LocalMemorySoftCache;
import com.anjlab.gae.QuotaDetails;
import com.anjlab.ping.services.dao.AccountDAO;
import com.anjlab.ping.services.dao.JobDAO;
import com.anjlab.ping.services.dao.RefDAO;
import com.anjlab.ping.services.dao.impl.cache.AccountDAOImplCache;
import com.anjlab.ping.services.dao.impl.cache.JobDAOImplCache;
import com.anjlab.ping.services.dao.impl.cache.RefDAOImplCache;
import com.anjlab.ping.services.location.IPResolver;
import com.anjlab.ping.services.location.LocationResolver;
import com.anjlab.ping.services.location.TimeZoneResolver;
import com.anjlab.ping.services.location.gae.GeonamesTimeZoneResolver;
import com.anjlab.ping.services.location.gae.IPWhoisNetIPResolver;
import com.anjlab.ping.services.location.gae.IPWhoisNetLocationResolver;
import com.anjlab.ping.services.security.AccessController;
import com.anjlab.tapestry5.StaticAssetResourceStreamer;
import com.anjlab.tapestry5.StaticAssetPathConverter;
import com.anjlab.tapestry5.TimeTranslator;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.utils.SystemProperty.Environment;
import com.google.apphosting.api.ApiProxy.OverQuotaException;
import com.google.apphosting.api.DeadlineExceededException;


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
    }
    
    public static AssetPathConverter decorateAssetPathConverter(
            final ResponseCompressionAnalyzer analyzer, final RequestGlobals requestGlobals) {
        return new StaticAssetPathConverter(requestGlobals);
    }
    
    @Contribute(PageRenderLinkTransformer.class)
    @Primary
    public static void provideURLRewriting(
            OrderedConfiguration<PageRenderLinkTransformer> configuration,
            final TypeCoercer typeCoercer) {

       configuration.add(
          "SupportOldUrls", new PageRenderLinkTransformer() {
            @Override
            public PageRenderRequestParameters decodePageRenderRequest(Request request) {
                String path = request.getPath();
                if (path.startsWith("/job/analytics/1026/5002")) {
                    return new PageRenderRequestParameters(
                            "job/analytics",
                            new EventContext() {
                                @Override
                                public String[] toStrings() {
                                    return new String[]{ "2865005" };
                                }
                                
                                @Override
                                public int getCount() {
                                    return 1;
                                }
                                
                                @SuppressWarnings("unchecked")
                                @Override
                                public <T> T get(Class<T> desiredType, int index) {
                                    if (desiredType == Long.class) {
                                        return (T) new Long("2865005");
                                    }
                                    return null;
                                }
                            },
                            false);
                }
                return null;
            }
            @Override
            public Link transformPageRenderLink(Link defaultLink,
                    PageRenderRequestParameters parameters) {
                return defaultLink;
            }
          });
    }
    
    public static EntityManagerSource decorateEntityManagerSource(final Logger logger,
            @Symbol(JpaSymbols.PERSISTENCE_DESCRIPTOR) final Resource persistenceDescriptor,
            PersistenceUnitConfigurer packageNamePersistenceUnitConfigurer)
    {
        //  XXX Waiting for https://issues.apache.org/jira/browse/TAP5-1848
        return new EntityManagerSourceImpl(logger, persistenceDescriptor, packageNamePersistenceUnitConfigurer, 
                                           new HashMap<String, PersistenceUnitConfigurer>())
        {
            private Map<String, EntityManagerFactory> entityManagerFactories = getEntityManagerFactories();
            
            @SuppressWarnings("unchecked")
            private Map<String, EntityManagerFactory> getEntityManagerFactories()
            {
                Field field = null;
                try
                {
                    field = this.getClass().getSuperclass().getDeclaredField("entityManagerFactories");
                    field.setAccessible(true);
                    return (Map<String, EntityManagerFactory>) field.get(this);
                    
                } catch (Exception e)
                {
                    throw new RuntimeException("Error accessing private field", e);
                } finally
                {
                    if (field != null) field.setAccessible(false);
                }
            }
            
            @Override
            public EntityManagerFactory getEntityManagerFactory(String persistenceUnitName)
            {
                EntityManagerFactory emf = entityManagerFactories.get(persistenceUnitName);
                
                if (emf == null)
                {
                    emf = Persistence.createEntityManagerFactory(persistenceUnitName);
                    
                    entityManagerFactories.put(persistenceUnitName, emf);
                }
                
                return emf;
            }
        };
    }
    
    public static void contributeIgnoredPathsFilter(Configuration<String> configuration) {
        //  GAE filters, except warmup requests
        configuration.add("/_ah/[^warmup].*");
        //  GAE Appstats
        configuration.add("/appstats/.*");
    }
    
    public static Application buildApplication(AccountDAO accountDAO, 
            JobDAO jobDAO, RefDAO refDAO, GAEHelper gaeHelper, JobExecutor jobExecutor,
            Mailer mailer, ApplicationStateManager stateManager, 
            PageRenderLinkSource linkSource, RequestGlobals globals,
            MemcacheService memcache, TimeZoneResolver timeZoneResolver,
            LocationResolver locationResolver)
    {
        return new Application(accountDAO, jobDAO, 
                refDAO, gaeHelper, jobExecutor, mailer, linkSource,
                globals, timeZoneResolver, locationResolver);
    }
    
    public static LocationResolver buildLocationResolver() {
        return new IPWhoisNetLocationResolver(URLFetchServiceFactory.getURLFetchService());
    }

    public static IPResolver buildIPResolver() {
        return new IPWhoisNetIPResolver(URLFetchServiceFactory.getURLFetchService());
    }

    public static TimeZoneResolver buildTimeZoneResolver() {
        return new GeonamesTimeZoneResolver(URLFetchServiceFactory.getURLFetchService(), "ping_service");
    }
    
    public static Cache buildGAECache(Logger logger, PerthreadManager perthreadManager) {
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            Cache cache = cacheFactory.createCache(Collections.emptyMap());
            
            final LocalMemorySoftCache cache2 = new LocalMemorySoftCache(cache);

            //  perthreadManager may be null if we creating cache from AbstractFilter
            if (perthreadManager != null) {
                perthreadManager.addThreadCleanupListener(new ThreadCleanupListener() {
                    @Override
                    public void threadDidCleanup() {
                        cache2.reset();
                    }
                });
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
    
    public static void contributeApplicationDefaults(Logger logger,
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
        boolean production = Environment.environment.value() == Environment.Value.Production;

        configuration.add(SymbolConstants.PRODUCTION_MODE, Boolean.toString(production));
        configuration.add(SymbolConstants.COMPRESS_WHITESPACE, Boolean.toString(production));

        //    DataNucleus' GAE implementation doesn't provide EMF.getMetamodel() which is required for
        //    providing entity value encoders
        configuration.add(JpaSymbols.PROVIDE_ENTITY_VALUE_ENCODERS, "false");
        configuration.add(JpaSymbols.EARLY_START_UP, "false");
        
        //    Version should be changed when any resource (that is referenced from CSS) changes
        String version = "stage-20120310";
        
        configuration.add(SymbolConstants.APPLICATION_VERSION, version);
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
    public RequestFilter buildTimingFilter(final Logger log, final Application application, final QuotaDetails quotaDetails)
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
                        try {
                            application.trackUserActivity();
                        } catch (Exception e) {
                            log.error("Error tracking user activity", e);
                            quotaDetails.checkOverQuotaException(e);
                        }
                    }
                    
                    // The responsibility of a filter is to invoke the corresponding method
                    // in the handler. When you chain multiple filters together, each filter
                    // received a handler that is a bridge to the next filter.
                    
                    return handler.service(request, response);
                }
                finally
                {
                    long elapsed = System.currentTimeMillis() - startTime;

                    log.info(String.format("Request time [%s]: %d ms", request.getPath(), elapsed));
                }
            }
        };
    }
    
    public void contributeMasterDispatcher(OrderedConfiguration<Dispatcher> configuration,
            @InjectService("AccessController") Dispatcher accessController) {
        
        //  TODO Investigate performance issue here
    
        configuration.add("AccessController", accessController, "before:PageRender");
    }

    public static ResourceStreamer decorateResourceStreamer(final ResourceStreamer streamer,
            final RequestGlobals requestGlobals, @Symbol(SymbolConstants.PRODUCTION_MODE) boolean productionMode)
    {
        return productionMode
             ? streamer
             : new StaticAssetResourceStreamer(requestGlobals, streamer);
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
    
    public static void contributeComponentMessagesSource(OrderedConfiguration<Resource> additionalBundles, TypeCoercer typeCoercer) { 
        String path = TimeTranslator.class.getName().replace(".", "/") + ".properties";
        final Resource resource = typeCoercer.coerce(path, Resource.class); 
        if (resource.exists()) { 
            additionalBundles.add("time-translator", resource); 
        } else { 
            // log or throw exceptions if you like 
        } 
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
    
    @Match("*DAO")
    public static void adviseTransactions(JpaTransactionAdvisor advisor, MethodAdviceReceiver receiver)
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
        configuration.add("cubics", "com/anjlab/cubics");
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
                logger.error("Unexpected runtime exception", exception);
                
                if (Utils.isCause(exception, OverQuotaException.class))
                {
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, null);
                }
                if (Utils.isCause(exception, DeadlineExceededException.class))
                {
                    response.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, null);
                }
                else
                {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
                }
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Match("IPResolver")
    public static void adviseCacheIPResolverMethods(final MethodAdviceReceiver receiver, Logger logger, PerthreadManager perthreadManager) {
        try {
            Map props = new HashMap();

            //  IP address of URL may change, keep it in cache for one day
            props.put(GCacheFactory.EXPIRATION_DELTA, 60 * 60 * 24);
            
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            Cache cache = cacheFactory.createCache(props);
            
            final LocalMemorySoftCache cache2 = new LocalMemorySoftCache(cache);
            
            //  We don't want local memory cache live longer than Memcache
            //  Since we don't have any mechanism to set local cache expiration
            //  we will just reset this cache after each request
            perthreadManager.addThreadCleanupListener(new ThreadCleanupListener() {
                @Override
                public void threadDidCleanup() {
                    cache2.reset();
                }
            });
            
            receiver.adviseAllMethods(new CacheMethodResultAdvice(IPResolver.class, cache2));
        } catch (CacheException e) {
            logger.error("Error instantiating cache", e);
        }
    }

    @Match("LocationResolver")
    public static void adviseCacheLocationResolverMethods(final MethodAdviceReceiver receiver, Cache cache) {
        //  Assume that location of IP address will never change, 
        //  so we don't have to set any custom cache expiration parameters
        receiver.adviseAllMethods(new CacheMethodResultAdvice(LocationResolver.class, cache));
    }
    
    @Match("TimeZoneResolver")
    public static void adviseCacheTimeZoneResolverMethods(final MethodAdviceReceiver receiver, Cache cache) {
        //  Assume that time zone of location will never change, 
        //  so we don't have to set any custom cache expiration parameters
        receiver.adviseAllMethods(new CacheMethodResultAdvice(LocationResolver.class, cache));
    }

    public static QuotaDetails buildQuotaDetails(Cache cache, MemcacheService memcache) {
        return new QuotaDetails(cache, memcache);
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