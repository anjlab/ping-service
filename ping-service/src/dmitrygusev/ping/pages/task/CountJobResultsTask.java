package dmitrygusev.ping.pages.task;


import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;

import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.repackaged.com.google.common.collect.ArrayListMultimap;
import com.google.appengine.repackaged.com.google.common.collect.Multimap;

import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.Utils;

public class CountJobResultsTask extends LongRunningQueryTask {

	private static final String RESULT_PARAMETER_NAME = "result";
	
	@Inject private Request request;
	@Inject private Mailer mailer;
	@Inject private EntityManager em;

	private int counter;
	private String encodedJobKey;

	@Override
	protected int getMaxResultsToFetchAtATime() {
		return 1000;
	}

	@Override
	protected Query getQuery() {
		return em.createQuery("SELECT r FROM JobResult r WHERE r.jobKey = :jobKey").
			     setParameter("jobKey", KeyFactory.stringToKey(encodedJobKey));
	}

	@Override
	protected boolean initTask() {
		counter = readIntegerParameter(RESULT_PARAMETER_NAME, 0);
		encodedJobKey = request.getParameter(JOB_PARAMETER_NAME);
		
		return !Utils.isNullOrEmpty(encodedJobKey);
	}

	@Override
	protected Multimap<String, String> getTaskParameters() {
		Multimap<String, String> parameters = ArrayListMultimap.create();

		parameters.put(JOB_PARAMETER_NAME, encodedJobKey);
		parameters.put(RESULT_PARAMETER_NAME, String.valueOf(counter));
		
		return parameters;
	}

	@Override
	protected boolean processResults(List<?> results) {
		counter += results.size();
		
		return true;
	}

	@Override
	protected void completeTask() {
		String keyString = request.getParameter(JOB_PARAMETER_NAME);
		
		mailer.sendSystemMessageToDeveloper(
				"Task completed", 
				KeyFactory.stringToKey(keyString).toString() + "\n\n JobResult count: " + counter);
	}
	
}
