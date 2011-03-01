package dmitrygusev.ping.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.junit.Ignore;
import org.junit.Test;

import com.google.appengine.repackaged.org.json.JSONArray;
import com.google.appengine.repackaged.org.json.JSONObject;

import dmitrygusev.ping.services.location.Location;

public class TestTimezoneIds {

    @Ignore
    @Test
    public void testGetTimezoneIdByCityName() throws Exception {
        List<String> timezones = new ArrayList<String>();
        String[] availableIDs = TimeZone.getAvailableIDs();        
        String[] pairs = Utils.getTimeZoneModel().split(",");
        for (String pair : pairs) {
            String[] values = pair.split("=");
            String cityName = values[0];
            
            boolean found = false;
            for (String timezoneId : availableIDs) {
                if (timezoneId.contains(cityName)) {
                    timezones.add(timezoneId + "=" + cityName + " (" + TimeZone.getTimeZone(timezoneId).getDisplayName() + ")");
                    found = true;
                }
            }
            if (!found) {
                URL url = new URL("http://ws.geonames.org/searchJSON?q=" + cityName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

                StringBuilder builder = new StringBuilder(10000);
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                
                JSONObject json = new JSONObject(builder.toString());
                Long count = json.getLong("totalResultsCount");
                if (count > 0) {
                    JSONArray array = json.getJSONArray("geonames");
                    JSONObject geoLocation = array.getJSONObject(0);
                    Location location = new Location(geoLocation.getString("countryName") + ", " + geoLocation.getString("toponymName"), 
                            new Double(geoLocation.getString("lat")), 
                            new Double(geoLocation.getString("lng")));
                    timezones.add(location + "=" + cityName);
                } else {
                    timezones.add("?=" + cityName);
                }
                
                reader.close();
            }
        }
        Collections.sort(timezones);
        List<String> notListedTimezones = new ArrayList<String>();
        for (String id : availableIDs) {
            notListedTimezones.add(id);
        }
        for (String timezone : timezones) {
            System.out.println(timezone);
            String id = timezone.split("=")[0];
            if (!id.equals("?")) {
                notListedTimezones.remove(id);
            }
        }
        System.out.println("===========");
        for (String notListed : notListedTimezones) {
            System.out.println(notListed + "=" + (TimeZone.getTimeZone(notListed).getRawOffset() / (1000 * 60 * 60)));
        }
    }
}
