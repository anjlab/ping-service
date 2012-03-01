package com.anjlab.ping.services;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TransientFailureException;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class GAEHelper {

    private static final Logger logger = LoggerFactory.getLogger(GAEHelper.class);
    
    private RequestGlobals requestGlobals;
    private UserService userService;

    private Map<String, String> cachedLoginUrls;
    private Map<String, String> cachedLogoutUrls;
    
    public GAEHelper(RequestGlobals requestGlobals) {
        this.requestGlobals = requestGlobals;
        this.userService = UserServiceFactory.getUserService();
        
        this.cachedLoginUrls = new HashMap<String, String>();
        this.cachedLogoutUrls = new HashMap<String, String>();
    }
    
    public Principal getUserPrincipal() {
        return requestGlobals.getHTTPServletRequest().getUserPrincipal();
    }

    public String createLoginURL() {
        return createLoginURL(requestGlobals.getHTTPServletRequest().getRequestURI());
    }

    public String createLogoutURL() {
        return createLogoutURL(requestGlobals.getHTTPServletRequest().getRequestURI());
    }

    public String createLogoutURL(String returnURL) {
        if (!cachedLogoutUrls.containsKey(returnURL)) {
            cachedLogoutUrls.put(returnURL, userService.createLogoutURL(returnURL));
        }
        return cachedLogoutUrls.get(returnURL);
    }

    public String createLoginURL(String returnURL) {
        if (!cachedLoginUrls.containsKey(returnURL)) {
            cachedLoginUrls.put(returnURL, userService.createLoginURL(returnURL));
        }
        return cachedLoginUrls.get(returnURL);
    }

    public static TaskOptions buildTaskUrl(String path) {
        return withUrl(path.endsWith("/") ? path : path + "/").method(Method.GET);
    }
    
    public static void addTaskNonTransactional(Queue queue, TaskOptions options) {
        addTaskNonTransactional(queue, Arrays.asList(options));
    }

    public static void addTaskNonTransactional(Queue queue, Iterable<TaskOptions> options) {
        int retryCount = 0;
        while (true) {
            try {
                queue.add(null, options);
                break;
            } catch (TransientFailureException e) {
                retryCount++;
                
                if (retryCount > 3) {
                    logger.error("Give up");
                    break;
                }
                
                logger.debug("Retry #{} after TransientFailureException: {}", retryCount, e);
            }
        }
    }
}
