package dmitrygusev.ping.entities;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import dmitrygusev.ping.services.Application;


public class TestJob {

    @Test
    public void testJobResult() {
        Job job = new Job();
        
        job.setLastPingTimestamp(new Date(0));
        job.setLastPingResult(Job.PING_RESULT_NOT_AVAILABLE);
        
        StringBuilder sb = new StringBuilder();
        String formattedDate = Application.formatDate(job.getLastPingTimestamp(), "Moscow", Application.DATETIME_FORMAT);
        Job.buildLastPingSummary(job, sb);
        
        Assert.assertEquals("1970-01-01 03:00:00", formattedDate);
        Assert.assertEquals("N/A", sb.toString());

        job.setLastPingResult(Job.PING_RESULT_OK);

        sb = new StringBuilder();
        formattedDate = Application.formatDate(job.getLastPingTimestamp(), "Moscow", Application.DATETIME_FORMAT);
        Job.buildLastPingSummary(job, sb);
        
        Assert.assertEquals("1970-01-01 03:00:00", formattedDate);
        Assert.assertEquals("Okay", sb.toString());
    }

    @Test
    public void testSerializeJobResults() throws Exception {
        Job job = new Job();
        
        job.beginUpdateJobResults();
        
        for (int i = 0; i < 10000; i += 1) {
            if (i % 1000 == 0) {
                long startTime = System.currentTimeMillis();

                job.packJobResults();
                
                long endTime = System.currentTimeMillis();
                
                System.out.println(job.getRecentJobResults(100000).size() + " items = " + 
                        job.getPackedJobResultsLength() + " bytes " + (endTime - startTime));
            }
            job.addJobResult(new JobResult());
        }
    }

    @Test
    public void testReadJobResults() throws IOException {
        Job job = new Job();
        List<JobResult> results = job.getRecentJobResults(10);
        assertEquals(0, results.size());
        
        JobResult result = new JobResult();
        result.setResponseTime(1);
        job.addJobResult(result);
        
        result = new JobResult();
        result.setResponseTime(2);
        job.addJobResult(result);
        
        result = new JobResult();
        result.setResponseTime(3);
        job.addJobResult(result);
        
        results = job.getRecentJobResults(2);
        assertEquals(2, results.size());
        assertEquals(2, results.get(0).getResponseTime());
        assertEquals(3, results.get(1).getResponseTime());

        results = job.getRecentJobResults(1);
        assertEquals(1, results.size());
        assertEquals(3, results.get(0).getResponseTime());

        results = job.getRecentJobResults(3);
        assertEquals(3, results.size());

        results = job.getRecentJobResults(10);
        assertEquals(3, results.size());
        
        job.packJobResults();
    }
}
