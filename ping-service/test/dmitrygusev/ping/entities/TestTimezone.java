package dmitrygusev.ping.entities;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
    
}
