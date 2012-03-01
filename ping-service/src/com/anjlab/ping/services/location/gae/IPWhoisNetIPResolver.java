package com.anjlab.ping.services.location.gae;

import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.services.location.IPResolver;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;


public class IPWhoisNetIPResolver implements IPResolver {
    
    public static final Logger logger = LoggerFactory.getLogger(IPWhoisNetIPResolver.class);

    public final URLFetchService urlFetchService;
    
    public IPWhoisNetIPResolver(URLFetchService urlFetchService) {
        this.urlFetchService = urlFetchService;
    }
    
    public String resolveIp(String url) {
        try {
            HTTPRequest request = new HTTPRequest(new URL("http://ip-whois.net/website_ip.php"), HTTPMethod.POST);
            request.getFetchOptions().setDeadline(10d);
            request.getFetchOptions().doNotFollowRedirects();
            request.getFetchOptions().allowTruncate();
            request.setPayload(("T1=" + new URI(url).getHost()).getBytes());
            
            HTTPResponse fetch = urlFetchService.fetch(request);
            
            Matcher matcher = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(new String(fetch.getContent()));
            
            if (matcher.find()) {
                return matcher.group(0);
            }
        } catch (Exception e) {
            logger.warn("Error resolving IP from URL", e);
        }
        return null;
    }
}
