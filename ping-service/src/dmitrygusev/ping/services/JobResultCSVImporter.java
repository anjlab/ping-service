package dmitrygusev.ping.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import dmitrygusev.ping.entities.JobResult;

public class JobResultCSVImporter {

    private interface ValueSetter<T, V> {
        public void setValue(T instance, V value);
    }

    private Map<String, ValueSetter<JobResult, String>> valueSetters;
    
    public JobResultCSVImporter(TimeZone timeZone) {
        initValueSetters(timeZone);
    }

    private void initValueSetters(final TimeZone timeZone) {
        valueSetters = new HashMap<String, ValueSetter<JobResult, String>>();
        
        valueSetters.put("timestamp", new ValueSetter<JobResult, String>() {
            private DateFormat format;
            {
                format = (DateFormat) Application.DATETIME_FORMAT.clone();
                if (timeZone != null) {
                    format.setTimeZone(timeZone);
                }
            }
            @Override
            public void setValue(JobResult instance, String value) {
                try {
                    instance.setTimestamp(format.parse(value));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        valueSetters.put("duration", new ValueSetter<JobResult, String>() {
            @Override
            public void setValue(JobResult instance, String value) {
                instance.setResponseTime(Integer.parseInt(value));
            }
        });
        valueSetters.put("resultCode", new ValueSetter<JobResult, String>() {
            @Override
            public void setValue(JobResult instance, String value) {
                instance.setPingResult(Integer.parseInt(value));
            }
        });
    }

    
    public List<JobResult> fromStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        String line = reader.readLine();
        
        String[] headers = line.split(JobResultCSVExporter.SEPARATOR_CHAR + "");
        
        List<JobResult> results = new ArrayList<JobResult>();
        
        while ((line = reader.readLine()) != null) {
            JobResult result = new JobResult();
            String[] values = line.split(JobResultCSVExporter.SEPARATOR_CHAR + "");
            
            for (int i = 0; i < values.length; i++) {
                valueSetters.get(headers[i]).setValue(result, values[i]);
            }
            
            results.add(result);
        }
        
        return results;
    }

}
