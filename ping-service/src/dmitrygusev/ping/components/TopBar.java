package dmitrygusev.ping.components;

import java.security.Principal;

import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.services.GAEHelper;

public class TopBar {

    @Inject
    private GAEHelper gaeHelper;
    
    public Principal getPrincipal() {
        return gaeHelper.getUserPrincipal();
    }
    
    public String getLogoutURL() {
        return gaeHelper.createLogoutURL("/");
    }
}
