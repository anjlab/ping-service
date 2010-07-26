package dmitrygusev.tapestry5;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.services.Request;

public class Utils {

    public static String getFriendlyDate(Request request, Messages messages, Date date) {
        if (date == null) {
            return messages.get("date-not-set");
        }
        return DateFormat
                .getDateInstance(DateFormat.MEDIUM, request.getLocale())
                .format(date);
    }

    public static String getFriendlyTime(Messages messages, Date value) {
        if (value == null) {
            return messages.get("time-not-set");
        }
        
        Calendar calendar = Calendar.getInstance();
        
        calendar.setTime(value);
        
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        return hours + ":" + leadingZeros(String.valueOf(minutes), 2);
    }

    public static String leadingZeros(String s, int minLength) {
        int diff = s.length() - minLength;
        
        return diff < 0 ? repeatChar('0', Math.abs(diff)) + s : s ;
    }

    public static String repeatChar(char c, int times) {
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        
        return builder.toString();
    }
    
    public static String getDayContext(Date day) {
        if (day == null) {
            return null;
        }
        
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        return df.format(day);
    }

    public static Date parseFromDayContext(String dayContext) {
        if (dayContext == null) {
            return null;
        }
        
        String[] dateFragments = dayContext.split("-");
        
        if (dateFragments.length != 3) {
            return null;
        }
        
        Calendar calendar = Calendar.getInstance();
        
        calendar.set(
                Integer.parseInt(dateFragments[2]), 
                //    Месяцы нумеруются с нуля
                Integer.parseInt(dateFragments[1]) - 1, 
                Integer.parseInt(dateFragments[0]));
        
        Date date = calendar.getTime();
        
        return date;
    }

    /**
     * 
     * @param args
     * @param key
     * @param keyValueSeparator Регулярное выражение, описывающее разделитель
     * @param defaultValue
     * @return
     */
    public static String getValue(Object[] args, String key, String keyValueSeparator, String defaultValue) {
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            
            String[] keyValuePair = arg.toString().split(keyValueSeparator);
            
            if (keyValuePair == null || keyValuePair.length != 2) {
                continue;
            }
            
            if (keyValuePair[0].equals(key)) {
                return (keyValuePair[1] == null || keyValuePair[1].length() == 0) ? null : keyValuePair[1];
            }
        }
        return defaultValue;
    }

    public static String getFriendlyDateLong(Date day) {
        DateFormat df = new SimpleDateFormat("EEEE, dd MMMM yyyy");
        String date = df.format(day);
        
        date = capitalizeFirstLetter(date);
        
        return date;
    }

    public static String capitalizeFirstLetter(String date) {
        if (isNullOrEmpty(date)) {
            return null;
        }
        
        char firstLetter = Character.toUpperCase(date.charAt(0));
        
        date = firstLetter + date.substring(1);
        return date;
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }
}
