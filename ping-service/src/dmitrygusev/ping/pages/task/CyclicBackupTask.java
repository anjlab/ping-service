package dmitrygusev.ping.pages.task;

import static com.google.appengine.api.memcache.Expiration.byDeltaSeconds;
import static dmitrygusev.ping.services.Utils.formatTimeMillis;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.memcache.MemcacheService;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.cron.BackupHealthInsuranceCron;
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
		return em.createQuery("SELECT j FROM Job j");
	}
	
	@Inject
	private Application application;

	@Inject
	private MemcacheService memcacheService;
	
	@Override
	protected boolean processResults(List<?> results) throws Exception {
        putInsuranceTicket();

	    if (results.size() > 0) {
			Job job = (Job)results.get(0);
			application.runBackupAndDeleteTask(job.getKey());
		}
		
		return true;
	}
	
    private void putInsuranceTicket() {
        //  This code will run at least every 24/8=3 hours a day (see 'backup' queue in queue.xml), 
        //  but lets put insurance ticket that will live a bit longer
        int seconds = (int) TimeUnit.SECONDS.convert(4L, TimeUnit.HOURS);
        
        memcacheService.put(BackupHealthInsuranceCron.INSURANCE_TICKET, "running", byDeltaSeconds(seconds));
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
