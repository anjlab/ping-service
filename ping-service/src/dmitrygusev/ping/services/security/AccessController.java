package dmitrygusev.ping.services.security;

import java.io.IOException;
import java.security.Principal;

import org.apache.tapestry5.services.Dispatcher;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;

public class AccessController implements Dispatcher {

	private GAEHelper helper;
	
	public AccessController(GAEHelper helper) {
		this.helper = helper; 
	}
	
	public boolean dispatch(Request request, Response response) throws IOException {
		Principal principal = helper.getUserPrincipal();

		String thisURL = request.getPath();

		if (principal != null 
				|| thisURL.startsWith("/assets")
				|| thisURL.startsWith("/cron/")
				|| thisURL.startsWith("/job/run/")
				|| thisURL.startsWith("/task/")
				|| thisURL.startsWith("/welcome")
				|| thisURL.startsWith("/help")
				|| thisURL.startsWith("/feedback")) {
			return false;
		} else {
			if (thisURL.equals("/")) {
				response.sendRedirect("/welcome");				
			} else {
				response.sendRedirect(helper.createLoginURL());
			}
			return true;
		}
    }
}
