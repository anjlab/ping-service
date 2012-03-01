package com.anjlab.ping.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.anjlab.ping.entities.JobResult;


public class JobResultCSVExporter {

    public static final char SEPARATOR_CHAR = ';';

    public static final String[] COLUMN_HEADERS = {
        "timestamp",
        "duration",
        "resultCode",
        "httpResponseCode"
    };

    public static final String[] COLUMN_HEADER_PATTERNS = {
        Application.DATETIME_PATTERN,
        "99999",
        "00",
        "000"
    };

    private static final String HEADER_TEXT = getHeaderText();
    
    private static String getHeaderText() {
        StringBuffer buffer = new StringBuffer();
        for (String column : COLUMN_HEADERS) {
            if (buffer.length() > 0) {
                buffer.append(SEPARATOR_CHAR);
            }
            buffer.append(column);
        }
        buffer.append('\n');
        return buffer.toString();
    }

    private static final String HEADER_PATTERN = getHeaderPattern();
    
    private static String getHeaderPattern() {
        StringBuffer buffer = new StringBuffer();
        for (String pattern : COLUMN_HEADER_PATTERNS) {
            if (buffer.length() > 0) {
                buffer.append(SEPARATOR_CHAR);
            }
            buffer.append(pattern);
        }
        buffer.append('\n');
        return buffer.toString();
    }
    
    public static byte[] export(TimeZone timeZone, List<JobResult> results) throws IOException {
        DateFormat dateFormat = Application.DATETIME_FORMAT;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                HEADER_TEXT.length() + results.size() * HEADER_PATTERN.length());
        
        write(baos, HEADER_TEXT);
        
        Calendar c = Calendar.getInstance();
        
        if (timeZone != null) {
            dateFormat.setTimeZone(timeZone);
        }
        
        for (JobResult result : results) {
            c.setTime(result.getTimestamp());
            
            write(baos, dateFormat.format(c.getTime()));
            write(baos, SEPARATOR_CHAR);
            write(baos, result.getResponseTime());
            write(baos, SEPARATOR_CHAR);
            write(baos, result.getPingResult());
            write(baos, SEPARATOR_CHAR);
            write(baos, result.getHTTPResponseCode());
            write(baos, '\n');
        }
        
        return baos.toByteArray();
    }

    private static void write(OutputStream output, String s) throws IOException {
        output.write(s.getBytes());
    }
    
    private static void write(OutputStream output, char c) throws IOException {
        output.write(String.valueOf(c).getBytes());
    }
    
    private static void write(OutputStream output, int i) throws IOException {
        output.write(String.valueOf(i).getBytes());
    }
    
}
