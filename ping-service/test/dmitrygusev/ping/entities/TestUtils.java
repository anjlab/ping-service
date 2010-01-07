package dmitrygusev.ping.entities;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import dmitrygusev.ping.services.Utils;

public class TestUtils {

	@Test
	public void testFormatTime() {
		assertEquals("0 minutes", Utils.formatTime(0));
		assertEquals("1 minute", Utils.formatTime(1));
		assertEquals("10 minutes", Utils.formatTime(10));
		assertEquals("1 hour 1 minute", Utils.formatTime(61));
		assertEquals("10 hours 2 minutes", Utils.formatTime(10*60 + 2));
		assertEquals("11 hours", Utils.formatTime(11*60));
		assertEquals("1 day 1 minute", Utils.formatTime(24*60 + 1));
		assertEquals("1 day 1 hour", Utils.formatTime(25*60));
		assertEquals("1 day 1 hour", Utils.formatTime(25*60));
		assertEquals("2 days", Utils.formatTime(2*24*60));
		assertEquals("1 month", Utils.formatTime(30*24*60));
		assertEquals("2 months", Utils.formatTime(2*30*24*60));
		assertEquals("1 year", Utils.formatTime(365*24*60));
		assertEquals("2 years", Utils.formatTime(2*365*24*60));
		assertEquals("2 years 5 minutes", Utils.formatTime(2*365*24*60 + 5));
	}
	
}
