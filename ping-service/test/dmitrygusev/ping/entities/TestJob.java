package dmitrygusev.ping.entities;

import java.util.Date;

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
		String formattedDate = Application.formatDate(job.getLastPingTimestamp(), "Moscow");
		Application.buildLastPingSummary(job, sb, formattedDate);
		
		Assert.assertEquals("1970-01-01 03:00:00 / N/A", sb.toString());

		job.setLastPingResult(Job.PING_RESULT_OK);

		sb = new StringBuilder();
		formattedDate = Application.formatDate(job.getLastPingTimestamp(), "Moscow");
		Application.buildLastPingSummary(job, sb, formattedDate);
		
		Assert.assertEquals("1970-01-01 03:00:00 / Okay", sb.toString());
	}
	
}
