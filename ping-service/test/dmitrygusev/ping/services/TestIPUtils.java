package dmitrygusev.ping.services;

import static org.junit.Assert.*;

import org.junit.Test;

import dmitrygusev.ping.services.IPUtils.IPLocation;

public class TestIPUtils {

    @Test
    public void testParseIPLocation() {
        IPLocation location = IPLocation.parseLocation(
                "United States, California, Mountain View (37.4192; -122.0574; 64.233.169.141)");
        
        assertEquals("United States, California, Mountain View", location.address);
        assertEquals("37.4192", "" + location.latitude);
        assertEquals("-122.0574", "" + location.longitude);
        assertEquals("64.233.169.141", location.ip);
    }
    @Test
    public void testParseIPLocation2() {
        IPLocation location = IPLocation.parseLocation(
                "(37.4192; -122.0574; 64.233.169.141)");
        
        assertEquals("", location.address);
        assertEquals("37.4192", "" + location.latitude);
        assertEquals("-122.0574", "" + location.longitude);
        assertEquals("64.233.169.141", location.ip);
    }
    
    @Test
    public void testParseIPLocation3() {
        IPLocation location = IPLocation.parseLocation(
                IPLocation.empty().toString());
        
        assertEquals("null", location.address);
        assertEquals("0.0", "" + location.latitude);
        assertEquals("0.0", "" + location.longitude);
        assertEquals("null", location.ip);
    }
    
    @Test
    public void testIsEmpty() {
        assertTrue(IPLocation.empty().isEmpty());
    }
}
