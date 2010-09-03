package dmitrygusev.ping.pages;

import static dmitrygusev.ping.services.Utils.isNullOrEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.CleanupRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.RequestGlobals;

import dmitrygusev.ping.services.location.IPResolver;
import dmitrygusev.ping.services.location.Location;
import dmitrygusev.ping.services.location.LocationResolver;

public class TraceRoute {

    @Property
    @Persist(PersistenceConstants.SESSION)
    private String traceRoute;
    
    @Inject
    private LocationResolver locationResolver;
    
    @Property
    private List<String> ipList;
    private Map<String, Location> locations;
    private String ips;
    
    @Property
    private String ip;
    
    public Location getLocation() {
        return locations.get(ip);
    }
    
    @Inject
    private RequestGlobals globals;
    
    @Inject
    private IPResolver ipResolver;
    
    public void onActivate() {
        if (isNullOrEmpty(traceRoute)) {
            traceRoute = 
                "Your IP: " + globals.getHTTPServletRequest().getRemoteAddr() 
                + "\nPing Service IP: " + ipResolver.resolveIp("http://ping-service.appspot.com");
        }
    }
    
    @CleanupRender
    public void cleanup() {
        ips = null;
        ipList = null;
        locations = null;
    }
    
    public String getIps() {
        if (ips == null) {
            StringBuilder builder = new StringBuilder("{");

            ipList = extractIPs(traceRoute);
            locations = new HashMap<String, Location>();

            for (String ip : ipList) {
                if (ip.startsWith("192.168.") || ip.startsWith("127.")) {
                    continue;
                }

                Location location = locationResolver.resolveLocation(ip);
                
                if (location.isEmpty()) {
                    continue;
                }

                if (builder.length() > 1) {
                    builder.append(",");
                }
                
                builder.append('"');
                builder.append(ip);
                builder.append("\":");

                builder.append(location.toJSON());
                
                locations.put(ip, location);
            }
            builder.append("}");
            
            ips = builder.toString();
        }
        return ips;
    }

    public static List<String> extractIPs(String traceRoute) {
        List<String> ips = new ArrayList<String>();

        if (isNullOrEmpty(traceRoute)) {
            return ips;
        }

        Matcher matcher = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)\n?").matcher(traceRoute);
        
        while (matcher.find()) {
            String ip = matcher.group(1);
            ips.add(ip);
        }
           
        return ips;
    }
    
}
