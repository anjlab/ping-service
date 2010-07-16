package dmitrygusev.ping.entities;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import dmitrygusev.ping.services.Utils;

public class TestUtils {

    @Test
    public void testFormatTime() {
        assertEquals("0 minutes", Utils.formatMinutesToWordsUpToMinutes(0));
        assertEquals("1 minute", Utils.formatMinutesToWordsUpToMinutes(1));
        assertEquals("10 minutes", Utils.formatMinutesToWordsUpToMinutes(10));
        assertEquals("1 hour 1 minute", Utils.formatMinutesToWordsUpToMinutes(61));
        assertEquals("10 hours 2 minutes", Utils.formatMinutesToWordsUpToMinutes(10*60 + 2));
        assertEquals("11 hours", Utils.formatMinutesToWordsUpToMinutes(11*60));
        assertEquals("1 day 1 minute", Utils.formatMinutesToWordsUpToMinutes(24*60 + 1));
        assertEquals("1 day 1 hour", Utils.formatMinutesToWordsUpToMinutes(25*60));
        assertEquals("1 day 1 hour", Utils.formatMinutesToWordsUpToMinutes(25*60));
        assertEquals("2 days", Utils.formatMinutesToWordsUpToMinutes(2*24*60));
        assertEquals("1 month", Utils.formatMinutesToWordsUpToMinutes(30*24*60));
        assertEquals("2 months", Utils.formatMinutesToWordsUpToMinutes(2*30*24*60));
        assertEquals("1 year", Utils.formatMinutesToWordsUpToMinutes(365*24*60));
        assertEquals("2 years", Utils.formatMinutesToWordsUpToMinutes(2*365*24*60));
        assertEquals("2 years 5 minutes", Utils.formatMinutesToWordsUpToMinutes(2*365*24*60 + 5));
    }

    @Test
    public void testFormatTime2() {
        assertEquals("Less than a day", Utils.formatMillisecondsToWordsUpToDays(0));
        assertEquals("Less than a day", Utils.formatMillisecondsToWordsUpToDays(1 * 1000));
        assertEquals("Less than a day", Utils.formatMillisecondsToWordsUpToDays(10 * 1000));
        assertEquals("Less than a day", Utils.formatMillisecondsToWordsUpToDays(61 * 1000));
        assertEquals("Less than a day", Utils.formatMillisecondsToWordsUpToDays(10*60 * 1000 + 2));
        assertEquals("Less than a day", Utils.formatMillisecondsToWordsUpToDays(11*60 * 1000));
        assertEquals("1 day", Utils.formatMillisecondsToWordsUpToDays(24*60*60*1000 + 1));
        assertEquals("1 day", Utils.formatMillisecondsToWordsUpToDays(25*60*60*1000));
        assertEquals("1 day", Utils.formatMillisecondsToWordsUpToDays(25*60*60*1000));
        assertEquals("2 days", Utils.formatMillisecondsToWordsUpToDays(2*24*60*60*1000));
        assertEquals("1 month", Utils.formatMillisecondsToWordsUpToDays(30*24*60*60*1000L));
        assertEquals("2 months", Utils.formatMillisecondsToWordsUpToDays(2*30*24*60*60*1000L));
        assertEquals("1 year", Utils.formatMillisecondsToWordsUpToDays(365*24*60*60*1000L));
        assertEquals("2 years", Utils.formatMillisecondsToWordsUpToDays(2*365*24*60*60*1000L));
        assertEquals("2 years", Utils.formatMillisecondsToWordsUpToDays(2*365*24*60*60*1000L + 5));
    }
    
}
