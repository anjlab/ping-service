package dmitrygusev.ping.services;

import static org.junit.Assert.*;

import org.junit.Test;

import dmitrygusev.ping.services.location.Location;


public class TestIPUtils {

    @Test
    public void testParseIPLocation() {
        Location location = Location.parseLocation(
                "United States, California, Mountain View (37.4192; -122.0574)");
        
        assertEquals("United States, California, Mountain View", location.getAddress());
        assertEquals("37.4192", "" + location.getLatitude());
        assertEquals("-122.0574", "" + location.getLongitude());
    }
    @Test
    public void testParseIPLocation2() {
        Location location = Location.parseLocation(
                "(37.4192; -122.0574)");
        
        assertEquals("", location.getAddress());
        assertEquals("37.4192", "" + location.getLatitude());
        assertEquals("-122.0574", "" + location.getLongitude());
    }
    
    @Test
    public void testParseIPLocation3() {
        Location location = Location.parseLocation(
                Location.empty().toString());
        
        assertEquals("null", location.getAddress());
        assertEquals("0.0", "" + location.getLatitude());
        assertEquals("0.0", "" + location.getLongitude());
    }
    
    @Test
    public void testIsEmpty() {
        assertTrue(Location.empty().isEmpty());
    }
}
