package dmitrygusev.ping.entities;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.JobResultCSVImporter;
import dmitrygusev.ping.services.JobResultsAnalyzer;

public class TestJob {

    @Test
    public void testJobResult() {
        Job job = new Job();
        
        job.setLastPingTimestamp(new Date(0));
        job.setLastPingResult(Job.PING_RESULT_NOT_AVAILABLE);
        
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Moscow");
        
        StringBuilder sb = new StringBuilder();
        String formattedDate = Application.formatDate(Application.DATETIME_FORMAT, timeZone, job.getLastPingTimestamp());
        Job.buildPingResultSummary(job.getLastPingResult(), sb);
        
        Assert.assertEquals("1970-01-01 03:00:00", formattedDate);
        Assert.assertEquals("N/A", sb.toString());

        job.setLastPingResult(Job.PING_RESULT_OK);

        sb = new StringBuilder();
        formattedDate = Application.formatDate(Application.DATETIME_FORMAT, timeZone, job.getLastPingTimestamp());
        Job.buildPingResultSummary(job.getLastPingResult(), sb);
        
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
    
    @Test
    public void testJobResultCSVImporter() throws Exception {
        JobResultCSVImporter importer = new JobResultCSVImporter(null);
        
        List<JobResult> results = importer.fromStream(
                new FileInputStream("test/job-1026-6-results-20100429211642-20100615120457.txt"));
        
        assertEquals(1000, results.size());
        
        JobResult result = results.get(0);
        assertEquals(Job.PING_RESULT_OK, result.getPingResult().intValue());
        assertEquals("2010-04-29 21:16:42", Application.DATETIME_FORMAT.format(result.getTimestamp()));
        assertEquals(684, result.getResponseTime());
    }
    
    @Test
    public void testJobResultsAnalyzerPlainTextReport() throws Exception {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        
        JobResultCSVImporter importer = new JobResultCSVImporter(timeZone);
        
        List<JobResult> results = importer.fromStream(
                new FileInputStream("test/job-1026-6-results-20100429211642-20100615120457.txt"));

        JobResultsAnalyzer analyzer = new JobResultsAnalyzer(results, true);

        StringBuilder sb = analyzer.buildPlainTextReport(timeZone);
        
        System.out.println(sb);
    }
    
    @Test
    public void testJobResultsAnalyzerHtmlReport() throws Exception {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        
        JobResultCSVImporter importer = new JobResultCSVImporter(timeZone);
        
        List<JobResult> results = importer.fromStream(
                new FileInputStream("test/job-1026-6-results-20100429211642-20100615120457.txt"));

        JobResultsAnalyzer analyzer = new JobResultsAnalyzer(results, true);

        StringBuilder sb = analyzer.buildHtmlReport(timeZone);
        
        System.out.println(sb);
    }
    
    @Test
    public void testJobResultsAnalyzerAvailabilitySummary() throws Exception {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        
        JobResultCSVImporter importer = new JobResultCSVImporter(timeZone);
        
        List<JobResult> results = importer.fromStream(
                new FileInputStream("test/job-1026-6-results-20100429211642-20100615120457.txt"));

        JobResultsAnalyzer analyzer = new JobResultsAnalyzer(results, true);

        String summary = analyzer.getAvailabilitySummary();
        
        System.out.println(summary);
    }}
