/**
 * 
 */
package dmitrygusev.ping.services.location;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dmitrygusev.ping.services.Utils;

public class Location implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = -1581969263720274037L;
    
    private final String address;
    private final double latitude;
    private final double longitude;
    
    public Location(String address, double latitude, double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public String getAddress() {
        return address;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public static Location parseLocation(String location) {
        Matcher matcher = Pattern.compile("([^\\(]*)\\(([^;]+); ([^;]+)\\)").matcher(location);
        return matcher.find() 
             ? new Location(
                     matcher.group(1).trim(),
                     parseDouble(matcher.group(2)), 
                     parseDouble(matcher.group(3)))
             : empty();
    }
    
    @Override
    public String toString() {
        return (address + " (" + latitude + "; " + longitude + ")").trim();
    }
    
    public static Location empty() {
        return new Location(null, 0, 0);
    }
    
    public boolean isEmpty() {
        return address == null && latitude == 0.0 && longitude == 0.0;
    }
    
    public long distanceInMeters(Location toLocation) {
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

    public static double parseDouble(String value) {
        return Utils.isNullOrEmpty(value) ? 0 : Double.parseDouble(value);
    }
}