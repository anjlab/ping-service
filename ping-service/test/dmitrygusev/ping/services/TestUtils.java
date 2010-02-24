package dmitrygusev.ping.services;

import junit.framework.Assert;

import org.junit.Test;


public class TestUtils {

	@Test
	public void removeJSessionId() {
		String url = "http://ping-service.appspot.com/job/edit/1026/2;jsessionid=GxOvYdb6PZ7lext0dRH0OA";
		
		Assert.assertEquals("http://ping-service.appspot.com/job/edit/1026/2", Utils.removeJSessionId(url));
	}
	
}
