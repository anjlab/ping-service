package dmitrygusev.ping.pages.task;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.datanucleus.store.appengine.query.JPACursorHelper;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;

import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.Utils;

public class CounterTask {

	private static final int RESULTS_TO_FETCH = 100;

	private static final Logger logger = Logger.getLogger(CounterTask.class);
	
	@Inject
	private Request request;
	
	@Inject
	private EntityManager em;
	
	@Inject
	private Mailer mailer;
	
	@Property
	@Persist("flash")
	private boolean scheduled;
	
	public void onActivate() {
		String encodedJobKey = request.getParameter("key");
		String encodedCursor = request.getParameter("cursor");
		String taskResult = request.getParameter("taskResult"); 

		scheduled = !Utils.isNullOrEmpty(encodedJobKey);
		
		if (!scheduled) {
			//	No JobKey were provided means this task can't be executed - return silently
			logger.debug("No job key specified");
			return;
		}
		
		int count;
		
		if (Utils.isNullOrEmpty(taskResult)) {
			count = 0;
		} else {
			count = Integer.parseInt(taskResult);
		}

		logger.debug("Executing task: job = " + KeyFactory.stringToKey(encodedJobKey) + "; cursor = " + encodedCursor + "; taskResult = " + taskResult);
		
		Query q = em.createQuery(
				"SELECT r FROM JobResult r " 
				+ "WHERE r.jobKey = :jobKey").
				
			setParameter("jobKey", KeyFactory.stringToKey(encodedJobKey)).
			setMaxResults(RESULTS_TO_FETCH);

		if (! Utils.isNullOrEmpty(encodedCursor)) {
			q.setHint(JPACursorHelper.CURSOR_HINT, Cursor.fromWebSafeString(encodedCursor));
		}

		List<?> results = q.getResultList();
		
		count += results.size();

		logger.debug("Results fetched: " + count);
		
		if (results.size() == 0 || results.size() < RESULTS_TO_FETCH) {
			logger.debug("Task completed");
			
			mailer.sendMail("dmitry.gusev@gmail.com", "dmitry.gusev@gmail.com", 
					"Task completed", 
					KeyFactory.stringToKey(encodedJobKey).toString() + "\n\n JobResult count: " + count);
			return;
		}
		
		logger.debug("Task will be continued");
		
		//	Continue fetching
		Cursor cursor = JPACursorHelper.getCursor(results);
		
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(null, url("/task/counter/")
					.param("key", encodedJobKey)
					.param("cursor", cursor.toWebSafeString())
					.param("taskResult", count + "")
					.method(Method.GET));
	}
	
}
