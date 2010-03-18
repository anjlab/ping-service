package dmitrygusev.ping.services;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.security.Principal;

import org.apache.tapestry5.services.RequestGlobals;

import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class GAEHelper {

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

	
}
