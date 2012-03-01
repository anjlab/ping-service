package com.anjlab.gae;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

public class ProfilingDelegate implements Delegate<Environment> {

    private static final Logger logger = LoggerFactory.getLogger(ProfilingDelegate.class);
    
    private final Delegate<Environment> parent;
    private final String[] excludePackages;
    private final boolean logTraces;
    
    public ProfilingDelegate(Delegate<Environment> parent, boolean logTraces, String... excludePackages) {
      this.parent = parent;
      this.excludePackages = excludePackages;
      this.logTraces = logTraces;
    }
    
    @Override
    public void log(Environment env, LogRecord logRec) {
        parent.log(env, logRec);
    }
    
    @Override
    public byte[] makeSyncCall(Environment env, String pkg, String method, byte[] request) throws ApiProxyException {
        long start = System.currentTimeMillis();
        byte[] result = parent.makeSyncCall(env, pkg, method, request);
        StringBuilder builder = logTraces ? buildStackTrace(excludePackages) : null;
        logger.info("GAE/S {}.{}: ->{} ms<-\n{}", new Object[] { pkg, method, System.currentTimeMillis() - start, builder });
        return result;
    }

    /**
     * 
     * @param appPackage
     *        Only classes from this package would be included in trace.
     * @return
     */
    private static StringBuilder buildStackTrace(String... excludePackages) {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        StringBuilder builder = new StringBuilder();
        int length = traces.length;
        StackTraceElement traceElement;
        String className;
//        for (int i = 3; i < length; i++) {
//            traceElement = traces[i];
//            className = traceElement.getClassName();
//            if (className.startsWith(appPackage)) {
//                if (builder.length() > 0) {
//                    builder.append('\n');
//                }
//                builder.append("..");
//                builder.append(className.substring(className.lastIndexOf('.')));
//                builder.append('.');
//                builder.append(traceElement.getMethodName());
//                builder.append(':');
//                builder.append(traceElement.getLineNumber());
//            }
//        }
        if (builder.length() == 0) {
            nextTrace:
            for (int i = 3; i < length; i++) {
                traceElement = traces[i];
                className = traceElement.getClassName();
                for (String pkg : excludePackages) {
                    if (Pattern.matches(pkg, className)) {
                        continue nextTrace;
                    }
                }
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(className);
                builder.append('.');
                builder.append(traceElement.getMethodName());
                builder.append(':');
                builder.append(traceElement.getLineNumber());
            }
        }
        return builder;
    }
    
    @Override
    public Future<byte[]> makeAsyncCall(Environment env, String pkg, String method, byte[] request, ApiConfig config) {
        long start = System.currentTimeMillis();
        Future<byte[]> result = parent.makeAsyncCall(env, pkg, method, request, config);
        StringBuilder builder = logTraces ? buildStackTrace(excludePackages) : null;
        logger.info("GAE/A {}.{}: ->{} ms<-\n{}", new Object[] { pkg, method, System.currentTimeMillis() - start, builder });
        return result;
    }

    @Override
    public void flushLogs(Environment env) {
        parent.flushLogs(env);
    }

    @Override
    public List<Thread> getRequestThreads(Environment env) {
        return parent.getRequestThreads(env);
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        //  Note: Comment this off to profile Google API requests
        ApiProxy.setDelegate(new ProfilingDelegate(ApiProxy.getDelegate(),
                false,
                "org.mortbay.*",
                "com.google.apphosting.utils.*",
                "com.google.tracing.*",
                "org.apache.tapestry5.*",
                "^\\$[^\\.]*$"));
    }
}