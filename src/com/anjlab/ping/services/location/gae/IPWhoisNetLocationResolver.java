package com.anjlab.ping.services.location.gae;

import static com.anjlab.ping.services.location.Location.parseDouble;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.services.Utils;
import com.anjlab.ping.services.location.Location;
import com.anjlab.ping.services.location.LocationResolver;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;


public class IPWhoisNetLocationResolver implements LocationResolver {

    private static final Logger logger = LoggerFactory.getLogger(IPWhoisNetLocationResolver.class);

    private final URLFetchService urlFetchService;
    
    public IPWhoisNetLocationResolver(URLFetchService urlFetchService) {
        this.urlFetchService = urlFetchService;
    }

    public Location resolveLocation(String ip) {
        try {
            HTTPRequest request = new HTTPRequest(new URL("http://ip-whois.net/ip_geo.php?ip=" + ip), HTTPMethod.GET);
            request.getFetchOptions().setDeadline(10d);
            request.getFetchOptions().doNotFollowRedirects();
            request.getFetchOptions().allowTruncate();
            
            HTTPResponse fetch = urlFetchService.fetch(request);

            String responseText = new String(fetch.getContent(), "Windows-1251");

            Pattern countryPattern = Pattern.compile("Страна: ([^<]*)<br>");
            Pattern regionPattern = Pattern.compile("Регион: ([^<]*)<br>");
            Pattern cityPattern = Pattern.compile("Город: ([^<]*)<br>");
            Pattern latitudePattern = Pattern.compile("Широта: ([^<]*)<br>");
            Pattern longitudePattern = Pattern.compile("Долгота: ([^<]*)<br>");
            
            return new Location(
                    buildLocationAddress(responseText, countryPattern, regionPattern, cityPattern),
                    parseDouble(matchGroup(latitudePattern, responseText)),
                    parseDouble(matchGroup(longitudePattern,responseText)));
            
        } catch (Exception e) {
            logger.warn("Error resolving Location from IP", e);
        }
        return Location.empty();
    }

    private static String buildLocationAddress(String responseText,
            Pattern countryPattern, Pattern regionPattern, Pattern cityPattern) {
        StringBuilder builder = new StringBuilder();
        
        appendText(builder, matchGroup(countryPattern, responseText));
        appendText(builder, matchGroup(regionPattern, responseText));
        appendText(builder, matchGroup(cityPattern, responseText));
        
        return builder.toString();
    }

    private static void appendText(StringBuilder builder, String text) {
        if (!Utils.isNullOrEmpty(text)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(text);
        }
    }

    private static String matchGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}
