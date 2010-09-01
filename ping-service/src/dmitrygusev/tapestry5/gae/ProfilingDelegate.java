package dmitrygusev.tapestry5.gae;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

public class ProfilingDelegate implements Delegate<Environment> {

    private static final Logger logger = LoggerFactory.getLogger(ProfilingDelegate.class);
    
    private final Delegate<Environment> parent;
    private final String appPackage;
    
    public ProfilingDelegate(Delegate<Environment> parent, String appPackage) {
      this.parent = parent;
      this.appPackage = appPackage;
    }
    
    public void log(Environment env, LogRecord logRec) {
        parent.log(env, logRec);
    }
    
    @Override
    public byte[] makeSyncCall(Environment env, String pkg, String method, byte[] request) throws ApiProxyException {
        long start = System.currentTimeMillis();
        byte[] result = parent.makeSyncCall(env, pkg, method, request);
        StringBuilder builder = buildStackTrace(appPackage);
        logger.info("GAE/S {}.{}: ->{} ms<-\n{}", new Object[] { pkg, method, System.currentTimeMillis() - start, builder });
        return result;
    }

    /**
     * 
     * @param appPackage
     *        Only classes from this package would be included in trace.
     * @return
     */
    public static StringBuilder buildStackTrace(String appPackage) {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        StringBuilder builder = new StringBuilder();
        int length = traces.length;
        StackTraceElement traceElement;
        String className;
        for (int i = 3; i < length; i++) {
            traceElement = traces[i];
            className = traceElement.getClassName();
            if (className.startsWith(appPackage)) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append("..");
                builder.append(className.substring(className.lastIndexOf('.')));
                builder.append('.');
                builder.append(traceElement.getMethodName());
                builder.append(':');
                builder.append(traceElement.getLineNumber());
            }
        }
        if (builder.length() == 0) {
            for (int i = 1; i < length; i++) {
                traceElement = traces[i];
                className = traceElement.getClassName();
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
        StringBuilder builder = buildStackTrace(appPackage);
        logger.info("GAE/A {}.{}: ->{} ms<-\n{}", new Object[] { pkg, method, System.currentTimeMillis() - start, builder });
        return result;
    }
}