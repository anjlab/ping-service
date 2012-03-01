package com.anjlab.ping.entities;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TestTimezone {

    @Test
    public void getTime() throws Exception {
        TimeZone moscowTimezone = TimeZone.getTimeZone("GMT+03:00");
        Date date = new Date();    //    UTC time
        
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        format.setTimeZone(moscowTimezone);
        
        System.out.println(format.format(date));
        
        Calendar c = Calendar.getInstance(moscowTimezone);
        c.setTime(date);
        
        System.out.println(c.get(Calendar.HOUR_OF_DAY));
    }
    
    @Test
    public void testTimezoneModel() {
        for (String id : TimeZone.getAvailableIDs()) {
            TimeZone timeZone = TimeZone.getTimeZone(id);
            long minutes = TimeUnit.MINUTES.convert(timeZone.getRawOffset(), TimeUnit.MILLISECONDS);
            String gmtOffset = String.format("GMT%+d:%02d", minutes / 60, minutes % 60);
            String displayName = String.format("(%s) %s, %s", gmtOffset, timeZone.getDisplayName(), id);
            System.out.println(displayName);
        }
    }
}
