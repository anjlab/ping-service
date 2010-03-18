package dmitrygusev.ping.pages.task;

import static com.google.appengine.api.labs.taskqueue.QueueFactory.getDefaultQueue;
import static com.google.appengine.api.labs.taskqueue.QueueFactory.getQueue;

import java.util.List;

import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.datanucleus.store.appengine.query.JPACursorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableMultimap;
import com.google.appengine.repackaged.com.google.common.collect.Multimap;

import dmitrygusev.ping.services.GAEHelper;
import dmitrygusev.ping.services.Utils;

public abstract class LongRunningQueryTask {

	private static final Logger logger = LoggerFactory.getLogger(LongRunningQueryTask.class);

	public static final String JOB_PARAMETER_NAME = "job";
	public static final String STARTTIME_PARAMETER_NAME = "startTime";
	public static final String CURSOR_PARAMETER_NAME = "cursor";
	
	protected abstract Query getQuery();
	
	protected abstract int getMaxResultsToFetchAtATime();

	private long startTime;
	private long requestStartTime;
	
	public long getRequestDuration() {
		return System.currentTimeMillis() - requestStartTime;
	}
	
	public String getQueueName() {
		return getDefaultQueue().getQueueName();
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * 
	 * @return If returns true the processing will begin, otherwise the task wont get started.
	 */
	protected boolean initTask() throws Exception {
		startTime = readLongParameter(STARTTIME_PARAMETER_NAME, System.currentTimeMillis());
		
		return true;
	}
	
	/**
	 * 
	 * @param results
	 * @return If returns true then processing will continue, otherwise the task would be terminated and {@link #completeTask()} will never be invoked.  
	 */
	protected abstract boolean processResults(List<?> results) throws Exception;

	protected void completeTask() throws Exception { ; }

	protected Multimap<String, String> getTaskParameters() { 
		return ImmutableMultimap.<String, String>builder().build(); 
	}

	@Inject 
	private Request request; 
	
	public void onActivate() {
		try {
			requestStartTime = System.currentTimeMillis();
			
			logTaskParameters();

			if (!initTask()) {
				logger.warn("Task initialization failed. The task will be terminated.");
				return;
			}
	
			runTask();
			
		} catch (Exception e) {
			logger.error("Error executing task", e);
		}
	}

	private void runTask() throws Exception {
		List<?> results = prepareQuery().getResultList();

		logger.debug("# of results fetched: {}", results.size());
		
		long startTime = System.currentTimeMillis(); 
		
		boolean taskTerminated = !processResults(results);
		
		long endTime = System.currentTimeMillis();
		
		logger.debug("Chunk processing tooked {} ms", endTime - startTime);
		
		if (taskTerminated) {
			logger.warn("Task has been terminated.");
			return;
		}
		
		if (results.size() == 0 || results.size() < getMaxResultsToFetchAtATime()) {
			logger.debug("Completing the task...");
			completeTask();
			logger.debug("Task completed.");
			return;
		}
		
		logger.debug("Task will be continued.");
		continueTask(results);
	}

	private Query prepareQuery() {
		String encodedCursor = request.getParameter(CURSOR_PARAMETER_NAME);
		
		int maxResults = getMaxResultsToFetchAtATime();
		
		Query q = getQuery().setMaxResults(maxResults);
	
		if (! Utils.isNullOrEmpty(encodedCursor)) {
			q.setHint(JPACursorHelper.CURSOR_HINT, Cursor.fromWebSafeString(encodedCursor));
		}
		return q;
	}

	private void continueTask(List<?> results) {
		Cursor cursor = JPACursorHelper.getCursor(results);
		
		Queue queue = getQueue(getQueueName());
		
		TaskOptions taskOptions = GAEHelper.buildTaskUrl(request.getPath())
					.param(CURSOR_PARAMETER_NAME, cursor.toWebSafeString())
					.param(STARTTIME_PARAMETER_NAME, String.valueOf(startTime));
		
		Multimap<String, String> parameters = getTaskParameters();
		
		for (String key : parameters.keys()) {
			for (String value : parameters.get(key)) {
				taskOptions.param(key, value);
			}
		}
		
		queue.add(null, taskOptions);
	}

	private void logTaskParameters() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Executing task " + request.getPath() + " with parameters:");
		
		for (String name : request.getParameterNames()) {
			builder.append("\n\t" + name + " = ");
			
			boolean firstValue = true;
			for (String value : request.getParameters(name)) {
				if (!firstValue) {
					builder.append("; ");
				}
				if (JOB_PARAMETER_NAME.equals(name)) {
					value = KeyFactory.stringToKey(value).toString();
				}
				builder.append(value);
			}
		}
	
		logger.debug(builder.toString());
	}

	protected int readIntegerParameter(String parameterName, int defaultValue) {
		String stringValue = request.getParameter(parameterName);
		
		return Utils.isNullOrEmpty(stringValue) ? defaultValue : Integer.parseInt(stringValue);
	}

	protected long readLongParameter(String parameterName, long defaultValue) {
		String stringValue = request.getParameter(parameterName);
	
		return Utils.isNullOrEmpty(stringValue) ? defaultValue : Long.parseLong(stringValue);
	}
	
}
