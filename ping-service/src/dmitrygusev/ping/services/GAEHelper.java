package dmitrygusev.ping.services;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.security.Principal;
import java.util.Arrays;

import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TransientFailureException;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class GAEHelper {

    private static final Logger logger = LoggerFactory.getLogger(GAEHelper.class);
    
	private RequestGlobals requestGlobals;
    private UserService userService = UserServiceFactory.getUserService();
    
    public GAEHelper(RequestGlobals requestGlobals) {
		this.requestGlobals = requestGlobals;
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
		return userService.createLogoutURL(returnURL);
	}

	public String createLoginURL(String returnURL) {
		return userService.createLoginURL(returnURL);
	}

	public static TaskOptions buildTaskUrl(String path) {
		return url(path.endsWith("/") ? path : path + "/").method(Method.GET);
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
