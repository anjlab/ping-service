package dmitrygusev.ping.pages.task;

import static dmitrygusev.ping.pages.task.BackupAndDeleteOldJobResultsTask.CHUNK_SIZE;
import static dmitrygusev.ping.pages.task.BackupAndDeleteOldJobResultsTask.COUNT_PARAMETER_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableMultimap;
import com.google.appengine.repackaged.com.google.common.collect.Multimap;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.JobResultDAO;

public class PurgeDeletedJobsTask extends LongRunningQueryTask {

    @Inject private EntityManager em;
    @Inject private JobResultDAO jobResultDAO;
    @Inject private JobDAO jobDAO;
    @Inject private Mailer mailer;

    private int totalCount;
    
    private List<Key> allJobsKeys;

    @Override
    public String getQueueName() {
        return "purge-deleted";
    }
    
    @Override
    protected Query getQuery() {
        return em.createQuery("SELECT r FROM JobResult r");
    }

    @Override
    protected int getMaxResultsToFetchAtATime() {
        return CHUNK_SIZE;
    }
    
    @Override
    protected boolean initTask() throws Exception {
        totalCount = readIntegerParameter(COUNT_PARAMETER_NAME, 0);

        List<Job> allJobs = jobDAO.getAllJobs();
        allJobsKeys = new ArrayList<Key>(allJobs.size());
        
        for (Job job : allJobs) {
            allJobsKeys.add(job.getKey());
        }
        
        Collections.sort(allJobsKeys);
        
        return super.initTask();
    }

    @Override
    protected Multimap<String, String> getTaskParameters() {
        return ImmutableMultimap.<String, String>of(
                COUNT_PARAMETER_NAME, String.valueOf(totalCount));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean processResults(List<?> results) throws Exception {
        totalCount += deleteUnbindedResults((List<JobResult>)results);
        
        return true;
    }

    /**
     * 
     * @param results
     * @return Returns number of results deleted.
     */
    private int deleteUnbindedResults(List<JobResult> results) {
        int count = 0;
        for (JobResult result : (List<JobResult>) results) {
            if (getRequestDuration() > 20000) {
                //  Spent only around 2/3 of request duration limit to delete job results
                break;
            }
            if (!isJobDeleted(result.getJobKey())) {
                continue;
            }
            jobResultDAO.delete(result.getId());
            count++;
        }
        return count;
    }
    
    private boolean isJobDeleted(final Key key) {
        return Collections.binarySearch(allJobsKeys, key) < 0;
    }

    @Override
    protected void completeTask() throws Exception {
        mailer.sendSystemMessageToDeveloper(
                this.getClass().getName() + " completed", 
                "totalCount: " + totalCount + "\ntotalTime: " + 
                Utils.formatTimeMillis(System.currentTimeMillis() - getStartTime()));
    }
}
