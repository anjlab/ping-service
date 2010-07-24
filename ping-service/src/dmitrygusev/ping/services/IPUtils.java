package dmitrygusev.ping.services;

import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

public class IPUtils {

    private static final Logger logger = LoggerFactory.getLogger(IPUtils.class);

    private static final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
    
    public static String resolveIp(String url) {
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

    public static class IPLocation {
        public final String ip;
        public final String address;
        public final double latitude;
        public final double longitude;
        
        public IPLocation(String ip, String address, double latitude, double longitude) {
            this.ip = ip;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        public static IPLocation parseLocation(String location) {
            Matcher matcher = Pattern.compile("([^\\(]*)\\(([^;]+); ([^;]+); ([^\\)]+)\\)").matcher(location);
            return matcher.find() 
                 ? new IPLocation(
                         matcher.group(4),
                         matcher.group(1).trim(), 
                         parseDouble(matcher.group(2)), 
                         parseDouble(matcher.group(3)))
                 : empty();
        }
        
        @Override
        public String toString() {
            return (address + " (" + latitude + "; " + longitude + "; " + ip + ")").trim();
        }
        
        public static IPLocation empty() {
            return new IPLocation(null, null, 0, 0);
        }
        
        public boolean isEmpty() {
            return ip == null && address == null && latitude == 0.0 && longitude == 0.0;
        }
        
        public long distanceInMeters(IPLocation toLocation) {
            double earthRadius = 3958.75;
            double dLat = Math.toRadians(toLocation.latitude-latitude);
            double dLng = Math.toRadians(toLocation.longitude-longitude);
            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                       Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(toLocation.latitude)) *
                       Math.sin(dLng/2) * Math.sin(dLng/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            double dist = earthRadius * c;

            int meterConversion = 1609;

            return Math.round(dist * meterConversion);
        }
    }
    
    public static IPLocation resolveLocation(String ip) {
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
            
            return new IPLocation(
                    ip,
                    buildLocationAddress(responseText, countryPattern, regionPattern, cityPattern),
                    parseDouble(matchGroup(latitudePattern, responseText)),
                    parseDouble(matchGroup(longitudePattern,responseText)));
            
        } catch (Exception e) {
            logger.warn("Error resolving Location from IP", e);
        }
        return IPLocation.empty();
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

    private static double parseDouble(String value) {
        return Utils.isNullOrEmpty(value) ? 0 : Double.parseDouble(value);
    }
    
    private static String matchGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}
