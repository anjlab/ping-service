package dmitrygusev.ping.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;

public class Utils {

    public static String getHttpCodesModel() {
        Map<Integer, String> codes = new TreeMap<Integer, String>();
        
        codes.put(-100, "Informational");
        codes.put(100, "Continue");
        codes.put(101, "Switching Protocols");
        codes.put(-200, "Successful");
        codes.put(200, "OK");
        codes.put(201, "Created");
        codes.put(202, "Accepted");
        codes.put(203, "Non-Authoritative Information");
        codes.put(204, "No Content");
        codes.put(205, "Reset Content");
        codes.put(206, "Partial Content");
        codes.put(-300, "Redirection");
        codes.put(300, "Multiple Choices");
        codes.put(301, "Moved Permanently");
        codes.put(302, "Found");
        codes.put(303, "See Other");
        codes.put(304, "Not Modified");
        codes.put(305, "Use Proxy");
        codes.put(306, "(Unused)");
        codes.put(307, "Temporary Redirect");
        codes.put(-400, "Client Error");
        codes.put(400, "Bad Request");
        codes.put(401, "Unauthorized");
        codes.put(402, "Payment Required");
        codes.put(403, "Forbidden");
        codes.put(404, "Not Found");
        codes.put(405, "Method Not Allowed");
        codes.put(406, "Not Acceptable");
        codes.put(407, "Proxy Authentication Required");
        codes.put(408, "Request Timeout");
        codes.put(409, "Conflict");
        codes.put(410, "Gone");
        codes.put(411, "Length Required");
        codes.put(412, "Precondition Failed");
        codes.put(413, "Request Entity Too Large");
        codes.put(414, "Request-URI Too Long");
        codes.put(415, "Unsupported Media Type");
        codes.put(416, "Requested Range Not Satisfiable");
        codes.put(417, "Expectation Failed");
        codes.put(-500, "Server Error");
        codes.put(500, "Internal Server Error");
        codes.put(501, "Not Implemented");
        codes.put(502, "Bad Gateway");
        codes.put(503, "Service Unavailable");
        codes.put(504, "Gateway Timeout");
        codes.put(505, "HTTP Version Not Supported");
        
        StringBuffer sb = new StringBuffer(); 
        
        for (Map.Entry<Integer, String> entry : codes.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey());
            sb.append("=");
            if (entry.getKey() > 0) {
                sb.append(entry.getKey());
            } else {
                sb.append(Math.abs(entry.getKey() / 100));
                sb.append("xx");
            }
            sb.append(" ");
            sb.append(entry.getValue());
        }
        
        return sb.toString();
    }

    private static String timeZoneModel;
    private static Map<String, String> timeZoneByCity = new HashMap<String, String>();
    
    public static String getTimeZoneModel() {
        if (isNullOrEmpty(timeZoneModel)) {
            buildTimeZoneModel();
        }
        return timeZoneModel;
    }

    private static void appendTimeZone(StringBuilder builder, String timeZoneId, String city) {
        if (builder.length() > 0) {
            builder.append(",");
        }
        builder.append(city);
        builder.append("=(");
        builder.append(timeZoneId);
        builder.append(") ");
        builder.append(city);
        
        timeZoneByCity.put(city, timeZoneId);
    }
    
    private static void buildTimeZoneModel() {
        StringBuilder builder = new StringBuilder();
        
        appendTimeZone(builder, "GMT-11:00", "International Date Line West");
        appendTimeZone(builder, "GMT-11:00", "Midway Island");
        appendTimeZone(builder, "GMT-11:00", "Samoa");
        appendTimeZone(builder, "GMT-10:00", "Hawaii");

        appendTimeZone(builder, "GMT-09:00", "Alaska");
        appendTimeZone(builder, "GMT-08:00", "Pacific Time (US & Canada)");
        appendTimeZone(builder, "GMT-08:00", "Tijuana");
        appendTimeZone(builder, "GMT-07:00", "Arizona");
        appendTimeZone(builder, "GMT-07:00", "Chihuahua");
        appendTimeZone(builder, "GMT-07:00", "Mazatlan");
        appendTimeZone(builder, "GMT-07:00", "Mountain Time (US & Canada)");

        appendTimeZone(builder, "GMT-06:00", "Central America");
        appendTimeZone(builder, "GMT-06:00", "Central Time (US & Canada)");
        appendTimeZone(builder, "GMT-06:00", "Guadalajara");
        appendTimeZone(builder, "GMT-06:00", "Mexico City");
        appendTimeZone(builder, "GMT-06:00", "Monterrey");
        appendTimeZone(builder, "GMT-06:00", "Saskatchewan");
        appendTimeZone(builder, "GMT-05:00", "Bogota");
        appendTimeZone(builder, "GMT-05:00", "Eastern Time (US & Canada)");

        appendTimeZone(builder, "GMT-05:00", "Indiana (East)");
        appendTimeZone(builder, "GMT-05:00", "Lima");
        appendTimeZone(builder, "GMT-05:00", "Quito");
        appendTimeZone(builder, "GMT-04:00", "Atlantic Time (Canada)");
        appendTimeZone(builder, "GMT-04:00", "Caracas");
        appendTimeZone(builder, "GMT-04:00", "La Paz");
        appendTimeZone(builder, "GMT-04:00", "Santiago");
        appendTimeZone(builder, "GMT-03:30", "Newfoundland");
        appendTimeZone(builder, "GMT-03:00", "Brasilia");

        appendTimeZone(builder, "GMT-03:00", "Buenos Aires");
        appendTimeZone(builder, "GMT-03:00", "Georgetown");
        appendTimeZone(builder, "GMT-03:00", "Greenland");
        appendTimeZone(builder, "GMT-02:00", "Mid-Atlantic");
        appendTimeZone(builder, "GMT-01:00", "Azores");
        appendTimeZone(builder, "GMT-01:00", "Cape Verde Is.");
        appendTimeZone(builder, "GMT+00:00", "Casablanca");
        appendTimeZone(builder, "GMT+00:00", "Dublin");
        appendTimeZone(builder, "GMT+00:00", "Edinburgh");

        appendTimeZone(builder, "GMT+00:00", "Lisbon");
        appendTimeZone(builder, "GMT+00:00", "London");
        appendTimeZone(builder, "GMT+00:00", "Monrovia");
        appendTimeZone(builder, "GMT+00:00", "UTC");
        appendTimeZone(builder, "GMT+01:00", "Amsterdam");
        appendTimeZone(builder, "GMT+01:00", "Belgrade");
        appendTimeZone(builder, "GMT+01:00", "Berlin");
        appendTimeZone(builder, "GMT+01:00", "Bern");
        appendTimeZone(builder, "GMT+01:00", "Bratislava");

        appendTimeZone(builder, "GMT+01:00", "Brussels");
        appendTimeZone(builder, "GMT+01:00", "Budapest");
        appendTimeZone(builder, "GMT+01:00", "Copenhagen");
        appendTimeZone(builder, "GMT+01:00", "Ljubljana");
        appendTimeZone(builder, "GMT+01:00", "Madrid");
        appendTimeZone(builder, "GMT+01:00", "Paris");
        appendTimeZone(builder, "GMT+01:00", "Prague");
        appendTimeZone(builder, "GMT+01:00", "Rome");
        appendTimeZone(builder, "GMT+01:00", "Sarajevo");

        appendTimeZone(builder, "GMT+01:00", "Skopje");
        appendTimeZone(builder, "GMT+01:00", "Stockholm");
        appendTimeZone(builder, "GMT+01:00", "Vienna");
        appendTimeZone(builder, "GMT+01:00", "Warsaw");
        appendTimeZone(builder, "GMT+01:00", "West Central Africa");
        appendTimeZone(builder, "GMT+01:00", "Zagreb");
        appendTimeZone(builder, "GMT+02:00", "Athens");
        appendTimeZone(builder, "GMT+02:00", "Bucharest");
        appendTimeZone(builder, "GMT+02:00", "Cairo");

        appendTimeZone(builder, "GMT+02:00", "Harare");
        appendTimeZone(builder, "GMT+02:00", "Helsinki");
        appendTimeZone(builder, "GMT+02:00", "Istanbul");
        appendTimeZone(builder, "GMT+02:00", "Jerusalem");
        appendTimeZone(builder, "GMT+02:00", "Kyev");
        appendTimeZone(builder, "GMT+02:00", "Minsk");
        appendTimeZone(builder, "GMT+02:00", "Pretoria");
        appendTimeZone(builder, "GMT+02:00", "Riga");
        appendTimeZone(builder, "GMT+02:00", "Sofia");

        appendTimeZone(builder, "GMT+02:00", "Tallinn");
        appendTimeZone(builder, "GMT+02:00", "Vilnius");
        appendTimeZone(builder, "GMT+03:00", "Baghdad");
        appendTimeZone(builder, "GMT+03:00", "Kuwait");
        appendTimeZone(builder, "GMT+03:00", "Moscow");
        appendTimeZone(builder, "GMT+03:00", "Nairobi");
        appendTimeZone(builder, "GMT+03:00", "Riyadh");
        appendTimeZone(builder, "GMT+03:00", "St. Petersburg");
        appendTimeZone(builder, "GMT+03:00", "Volgograd");

        appendTimeZone(builder, "GMT+03:30", "Tehran");
        appendTimeZone(builder, "GMT+04:00", "Abu Dhabi");
        appendTimeZone(builder, "GMT+04:00", "Baku");
        appendTimeZone(builder, "GMT+04:00", "Muscat");
        appendTimeZone(builder, "GMT+04:00", "Tbilisi");
        appendTimeZone(builder, "GMT+04:00", "Yerevan");
        appendTimeZone(builder, "GMT+04:30", "Kabul");
        appendTimeZone(builder, "GMT+05:00", "Ekaterinburg");
        appendTimeZone(builder, "GMT+05:00", "Islamabad");

        appendTimeZone(builder, "GMT+05:00", "Karachi");
        appendTimeZone(builder, "GMT+05:00", "Tashkent");
        appendTimeZone(builder, "GMT+05:30", "Chennai");
        appendTimeZone(builder, "GMT+05:30", "Kolkata");
        appendTimeZone(builder, "GMT+05:30", "Mumbai");
        appendTimeZone(builder, "GMT+05:30", "New Delhi");
        appendTimeZone(builder, "GMT+05:45", "Kathmandu");
        appendTimeZone(builder, "GMT+06:00", "Almaty");
        appendTimeZone(builder, "GMT+06:00", "Astana");

        appendTimeZone(builder, "GMT+06:00", "Dhaka");
        appendTimeZone(builder, "GMT+06:00", "Novosibirsk");
        appendTimeZone(builder, "GMT+06:00", "Sri Jayawardenepura");
        appendTimeZone(builder, "GMT+06:30", "Rangoon");
        appendTimeZone(builder, "GMT+07:00", "Bangkok");
        appendTimeZone(builder, "GMT+07:00", "Hanoi");
        appendTimeZone(builder, "GMT+07:00", "Jakarta");
        appendTimeZone(builder, "GMT+07:00", "Krasnoyarsk");
        appendTimeZone(builder, "GMT+08:00", "Beijing");

        appendTimeZone(builder, "GMT+08:00", "Chongqing");
        appendTimeZone(builder, "GMT+08:00", "Hong Kong");
        appendTimeZone(builder, "GMT+08:00", "Irkutsk");
        appendTimeZone(builder, "GMT+08:00", "Kuala Lumpur");
        appendTimeZone(builder, "GMT+08:00", "Perth");
        appendTimeZone(builder, "GMT+08:00", "Singapore");
        appendTimeZone(builder, "GMT+08:00", "Taipei");
        appendTimeZone(builder, "GMT+08:00", "Ulaan Bataar");
        appendTimeZone(builder, "GMT+08:00", "Urumqi");

        appendTimeZone(builder, "GMT+09:00", "Osaka");
        appendTimeZone(builder, "GMT+09:00", "Sapporo");
        appendTimeZone(builder, "GMT+09:00", "Seoul");
        appendTimeZone(builder, "GMT+09:00", "Tokyo");
        appendTimeZone(builder, "GMT+09:00", "Yakutsk");
        appendTimeZone(builder, "GMT+09:30", "Adelaide");
        appendTimeZone(builder, "GMT+09:30", "Darwin");
        appendTimeZone(builder, "GMT+10:00", "Brisbane");
        appendTimeZone(builder, "GMT+10:00", "Canberra");

        appendTimeZone(builder, "GMT+10:00", "Guam");
        appendTimeZone(builder, "GMT+10:00", "Hobart");
        appendTimeZone(builder, "GMT+10:00", "Melbourne");
        appendTimeZone(builder, "GMT+10:00", "Port Moresby");
        appendTimeZone(builder, "GMT+10:00", "Sydney");
        appendTimeZone(builder, "GMT+10:00", "Vladivostok");
        appendTimeZone(builder, "GMT+11:00", "Magadan");
        appendTimeZone(builder, "GMT+11:00", "New Caledonia");
        appendTimeZone(builder, "GMT+11:00", "Solomon Is.");

        appendTimeZone(builder, "GMT+12:00", "Auckland");
        appendTimeZone(builder, "GMT+12:00", "Fiji");
        appendTimeZone(builder, "GMT+12:00", "Kamchatka");
        appendTimeZone(builder, "GMT+12:00", "Marshall Is.");
        appendTimeZone(builder, "GMT+12:00", "Wellington");
        appendTimeZone(builder, "GMT+13:00", "Nuku'alofa");

        timeZoneModel = builder.toString();
    }
    
    public static String getTimeZoneId(String city) {
        if (timeZoneByCity.size() == 0) {
            getTimeZoneModel();
        }
        return timeZoneByCity.get(city);
    }

    private static final Map<String, Integer> cronModel = buildCronModel();
    private static final String cronStringModel = join(",", cronModel.keySet()); 
    
    public static String getCronStringModel() {
        return cronStringModel;
    }
    
    private static String join(String delimeter, Iterable<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(delimeter);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    /**
     * model Key is a cron string, Value is number of minutes in cron interval 
     * @return
     */
    private static Map<String, Integer> buildCronModel() {
        Map<String, Integer> result = new TreeMap<String, Integer>();
        result.put("every 1 hours", 60);
        result.put("every 30 minutes", 30);
        result.put("every 15 minutes", 15);
        result.put("every 5 minutes", 5);
        return result;
    }

    public static Integer getCronMinutes(String cronString) {
        return cronModel.containsKey(cronString) ? cronModel.get(cronString) : 0;
    }

    private static class Pair {
        public final long number;
        public final String name;
        public Pair(long number, String name) {
            this.number = number;
            this.name = name;
        }
    }
    
    public enum TimeGranularity {
        MINUTE,
        DAY
    }
    
    public static String formatMinutesToWordsUpToMinutes(long numberOfMinutes) {
        return formatMinutesToWords(numberOfMinutes, TimeGranularity.MINUTE);
    }
    
    private static String formatMinutesToWords(long numberOfMinutes, TimeGranularity timeGranularity) {
        long numberOfMinutesInYear = 365 * 24 * 60;
        long numberOfMinutesInMonth = 30 * 24 * 60;
        long numberOfMinutesInDay = 24 * 60;
        long numberOfMinutesInHour = 60;
        
        long years = numberOfMinutes / numberOfMinutesInYear;
        numberOfMinutes -= years * numberOfMinutesInYear;
        
        long months = numberOfMinutes / numberOfMinutesInMonth;
        numberOfMinutes -= months * numberOfMinutesInMonth;
        
        long days = numberOfMinutes / numberOfMinutesInDay;
        numberOfMinutes -= days * numberOfMinutesInDay;
        
        long hours = numberOfMinutes / numberOfMinutesInHour;
        numberOfMinutes -= hours * numberOfMinutesInHour;
        
        long minutes = numberOfMinutes > 0 ? numberOfMinutes : 0;

        List<Pair> pairs = new ArrayList<Pair>();
        pairs.add(new Pair(years, " year"));
        pairs.add(new Pair(months, " month"));
        pairs.add(new Pair(days, " day"));
        
        if (!(timeGranularity == TimeGranularity.DAY)) {
            pairs.add(new Pair(hours, " hour"));
            pairs.add(new Pair(minutes, " minute"));
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (Pair pair : pairs) {
            if (pair.number > 0 || (pair.name.endsWith("minute") && sb.length() == 0)) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(pair.number);
                sb.append(pair.name);
                appendIfPlural(sb, "s", pair.number);
            }
        }
        
        return sb.toString();
    }
    
    private static void appendIfPlural(StringBuilder builder, String appendWhat, long number) {
        if (number == 0 || number > 1) {
            builder.append(appendWhat);
        }
    }

    public static String getHttpCodeDescription(int validatingHttpCode) {
        String[] descriptions = getHttpCodesModel().split(",");
        
        for (String description : descriptions) {
            String[] parts = description.split("=");
            
            if (parts[0].equals(validatingHttpCode + "")) {
                return parts[1];
            }
        }
        
        return null;
    }

    public static Long[] createJobContext(Job job) {
        return new Long[]
                        {
                            job.getKey().getParent().getId(),
                            job.getKey().getId()
                        };
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static String getCSVExportFilename(Job job) {
        return getExportFilenameWithoutExtension(job) + ".csv";
    }

    public static String getZipExportFilename(Job job) {
        return getExportFilenameWithoutExtension(job) + ".zip";
    }

    private static Object getExportFilenameWithoutExtension(Job job) {
        return String.format("ping-service-{0}-{1}", 
                        String.valueOf(job.getKey().getParent().getId()), 
                        String.valueOf(job.getKey().getId()));
    }

    public static boolean isCronStringSupported(String cronString) {
        if (isNullOrEmpty(cronString)) {
            return false;
        }
        
        return cronModel.containsKey(cronString);
    }

    public static int getTimeInMinutes(int counter, String cronString) {
        return counter * getCronMinutes(cronString);
    }

    public static double calculateAvailabilityPercent(List<JobResult> results) {
        int recentSuccessCount = 0;
        for (JobResult result : results) {
            if (!result.isFailed()) {
                recentSuccessCount++;
            }
        }
        return Utils.calculatePercent(results.size(), recentSuccessCount);
    }

    public static double calculatePercent(int totalCount, int countOfInterest) {
        if (totalCount == 0) {
            return 0;
        }
        
        double percent = 100d * countOfInterest / totalCount;
        
        return percent;
    }

    public static String formatPercent(double value) {
        return String.format(Locale.ENGLISH, "%.5f", value) + "%";
    }

    public static String getTimeAgoUpToMinutes(Date timestamp) {
        if (timestamp == null) {
            return null;
        }
        
        long milliseconds = System.currentTimeMillis() - timestamp.getTime();
        
        String timeAgo = formatMillisecondsToWordsUpToMinutes(milliseconds) + " ago";
        
        return timeAgo;
    }

    public static String getTimeAgoUpToDays(Date timestamp) {
        if (timestamp == null) {
            return null;
        }
        
        long milliseconds = System.currentTimeMillis() - timestamp.getTime();
        
        String timeAgo = formatMillisecondsToWordsUpToDays(milliseconds) + " ago";
        
        return timeAgo;
    }

    public static String formatMillisecondsToWordsUpToMinutes(long totalTimeMillis) {
        return formatMillisecondsToWords(totalTimeMillis, TimeGranularity.MINUTE);
    }

    public static String formatMillisecondsToWordsUpToDays(long totalTimeMillis) {
        return formatMillisecondsToWords(totalTimeMillis, TimeGranularity.DAY);
    }

    private static String formatMillisecondsToWords(long totalTimeMillis, TimeGranularity timeGranularity) {
        String totalTimeFormatted;
        if (totalTimeMillis < getNumberOfMilliseconds(timeGranularity)) {
            totalTimeFormatted = "Less than a " + timeGranularity.name().toLowerCase();
        } else {
            totalTimeFormatted = formatMinutesToWords((long)(totalTimeMillis / 1000 / 60), timeGranularity);
        }
        return totalTimeFormatted;
    }

    private static long getNumberOfMilliseconds(TimeGranularity timeGranularity) {
        switch (timeGranularity) {
        case MINUTE:
            return 1000 * 60L;
        case DAY:
            return 1000 * 60 * 60 * 24L;

        default:
            return 0;
        }
    }
}
