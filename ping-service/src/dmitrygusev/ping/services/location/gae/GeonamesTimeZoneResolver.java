package dmitrygusev.ping.services.location.gae;

import static java.lang.String.format;

import java.net.URL;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.repackaged.org.json.JSONObject;

import dmitrygusev.ping.services.location.TimeZoneResolver;

public class GeonamesTimeZoneResolver implements TimeZoneResolver {
    
    public static final Logger logger = LoggerFactory.getLogger(GeonamesTimeZoneResolver.class);

    private final URLFetchService urlFetchService;
    private final String username;
    
    public GeonamesTimeZoneResolver(URLFetchService urlFetchService) {
        this(urlFetchService, null);
    }
    
    public GeonamesTimeZoneResolver(URLFetchService urlFetchService, String username) {
        this.urlFetchService = urlFetchService;
        this.username = username;
    }
    
    @Override
    public TimeZone resolveTimeZone(double latitude, double longitude) {
        try {
            HTTPRequest request = new HTTPRequest(
                    new URL(formatURL(latitude, longitude)), 
                    HTTPMethod.GET);
            request.getFetchOptions().setDeadline(10d);
            request.getFetchOptions().doNotFollowRedirects();
            request.getFetchOptions().allowTruncate();
            
            HTTPResponse fetch = urlFetchService.fetch(request);
            
            JSONObject json = new JSONObject(new String(fetch.getContent()));
            
            String timezoneId = json.getString("timezoneId");
            
            if (timezoneId == null) {
                logger.warn("JSON response missing information about timezoneId: {}", json);
                
                return null;
            }
            
            logger.debug("Location {}/{} resolved with time zone id '{}'", 
                    new Object[] {Double.valueOf(latitude), Double.valueOf(longitude), timezoneId});
            
            return TimeZone.getTimeZone(timezoneId);
        }
        catch (Exception e) {
            logger.warn("Error resolving TimeZone from lat/lng pair", e);
        }
        return null;
    }

    public String formatURL(double latitude, double longitude) {
        return format(Locale.ENGLISH, "http://ws.geonames.org/timezoneJSON?lat=%.2f&lng=%.2f%s", 
                latitude, longitude, username == null ? "" : "&username=" + username);
    }
    
}
