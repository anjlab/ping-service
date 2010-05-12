package dmitrygusev.ping.entities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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
		Application.buildLastPingSummary(job, sb, formattedDate);
		
		Assert.assertEquals("1970-01-01 03:00:00 / N/A", sb.toString());

		job.setLastPingResult(Job.PING_RESULT_OK);

		sb = new StringBuilder();
		formattedDate = Application.formatDate(job.getLastPingTimestamp(), "Moscow", Application.DATETIME_FORMAT);
		Application.buildLastPingSummary(job, sb, formattedDate);
		
		Assert.assertEquals("1970-01-01 03:00:00 / Okay", sb.toString());
	}

	@Test
	public void testSerializeJobResults() throws Exception {
	    List<JobResult> list = new ArrayList<JobResult>();
	    
	    for (int i = 0; i < 10000; i += 1) {
	        list.add(new JobResult());
	        
	        if (i % 1000 == 0) {
	            logObjectSerialization(list);
	        }
	    }
	}

    private void logObjectSerialization(List<JobResult> list)
            throws IOException {
        long startTime = System.currentTimeMillis();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ObjectOutputStream oos = new ObjectOutputStream(baos);
	    
	    oos.writeObject(list);
	    
	    oos.close();
	    
	    byte[] data = baos.toByteArray();
	    
	    long endTime = System.currentTimeMillis();
	    
	    System.out.println(list.size() + " items = " + data.length + " bytes " + (endTime - startTime));
    }
	
}
