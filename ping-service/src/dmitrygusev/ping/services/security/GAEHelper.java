package dmitrygusev.ping.services.security;

import java.security.Principal;

import org.apache.tapestry5.services.RequestGlobals;

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

	
}
