package com.anjlab.ping.components;

import java.security.Principal;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.anjlab.ping.services.GAEHelper;


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
