package dmitrygusev.ping.pages.task;

import static dmitrygusev.ping.services.Utils.formatTimeMillis;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Mailer;

public class CyclicBackupTask extends LongRunningQueryTask {

	@Override
	public String getQueueName() {
		return Application.BACKUP_QUEUE;
	}
	
	@Override
	protected int getMaxResultsToFetchAtATime() {
		return 1;
	}

	@Inject
	private EntityManager em;
	
	@Override
	protected Query getQuery() {
		return em.createQuery("SELECT j FROM Job j ORDER BY j.lastBackupTimestamp ASC");
	}
	
	@Inject
	private Application application;

	@Override
	protected boolean processResults(List<?> results) throws Exception {
	    if (results.size() > 0) {
			Job job = (Job)results.get(0);
			application.runBackupAndDeleteTask(job.getKey());
		}
		
		return true;
	}

	@Inject
	private Mailer mailer;
	
	@Override
	protected void completeTask() throws Exception {
		mailer.sendSystemMessageToDeveloper(
				"Backup Cycle Completed", 
				"Total time: " + formatTimeMillis(System.currentTimeMillis() - getStartTime()));
		
		application.runCyclicBackupTask();
	}
}
