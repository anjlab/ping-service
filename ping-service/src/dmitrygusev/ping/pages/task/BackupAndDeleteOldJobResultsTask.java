package dmitrygusev.ping.pages.task;

import static com.google.appengine.api.datastore.KeyFactory.keyToString;
import static com.google.appengine.api.datastore.KeyFactory.stringToKey;
import static com.google.appengine.api.labs.taskqueue.QueueFactory.getQueue;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static dmitrygusev.tapestry5.GAEUtils.buildTaskUrl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableMultimap;
import com.google.appengine.repackaged.com.google.common.collect.Multimap;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.JobResultDAO;

public class BackupAndDeleteOldJobResultsTask extends LongRunningQueryTask {


	private static final Logger logger = LoggerFactory.getLogger(BackupAndDeleteOldJobResultsTask.class);
	
	private static final int CHUNK_SIZE = 50;
	private static final String COUNT_PARAMETER_NAME = "count";
	public static final String TASK_ID_PARAMETER_NAME = "taskId";
	public static final long CACHED_RESULTS_FIRST_CHUNK_ID = 1;
	
	@Inject private Request request;
	@Inject private EntityManager em;

	private Job job;
	private int totalCount;
	private String taskId;
	
	@Inject
	private JobDAO jobDAO;

	@Override
	protected boolean initTask() throws Exception {
		String encodedJobKey = request.getParameter(JOB_PARAMETER_NAME);
		totalCount = readIntegerParameter(COUNT_PARAMETER_NAME, 0);
		taskId = request.getParameter(TASK_ID_PARAMETER_NAME);

		if (!Utils.isNullOrEmpty(encodedJobKey)) {
			job = jobDAO.find(stringToKey(encodedJobKey));
		}
		
		return super.initTask()
			&& !Utils.isNullOrEmpty(taskId) 
			&& job != null;
	}
	
	@Override
	protected int getMaxResultsToFetchAtATime() {
		return CHUNK_SIZE;
	}

	@Override
	protected Multimap<String, String> getTaskParameters() {
		return ImmutableMultimap.<String, String>of(
				JOB_PARAMETER_NAME, keyToString(job.getKey()), 
				COUNT_PARAMETER_NAME, String.valueOf(totalCount),
				TASK_ID_PARAMETER_NAME, taskId);
	}

	@Override
	protected Query getQuery() {
		return em.createQuery("SELECT r FROM JobResult r WHERE r.jobKey = :jobKey ORDER BY r.timestamp DESC")
					.setParameter("jobKey", job.getKey());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean processResults(List<?> results) throws Exception {
		totalCount += results.size();
		
		if (isNotFirstChunk()) {
			backupAndDelete((List<JobResult>)results);
		} else {
			logger.debug("Nothing to process here");
		}

		return true;
	}

	@Override
	protected void completeTask() throws Exception {
		if (cacheHasResults()) {
			
			logger.info("Enqueueing MailJobResultsTask for taskId {}", taskId);
			
			getQueue("mail")
				.add(null, buildTaskUrl(linkSource, MailJobResultsTask.class)
					.param(BackupAndDeleteOldJobResultsTask.JOB_PARAMETER_NAME, keyToString(job.getKey()))
					.param(BackupAndDeleteOldJobResultsTask.TASK_ID_PARAMETER_NAME, taskId)
					.param(LongRunningQueryTask.STARTTIME_PARAMETER_NAME, String.valueOf(getStartTime())));
		}
	}

	private boolean cacheHasResults() {
		return getMemcacheService().contains(taskId) 
			&& Long.parseLong(getMemcacheService().get(taskId).toString()) >= CACHED_RESULTS_FIRST_CHUNK_ID;
	}
	
	@Inject
	private JobResultDAO jobResultDAO;
	
	private void backupAndDelete(List<JobResult> results) throws MessagingException, IOException {
		if (results.size() > 0) {
			if (job.isReceiveBackups()) {
				backupResults(results);
			}

			deleteResults(results);
		}
	}

	private void deleteResults(List<JobResult> results) {
		for (JobResult result : (List<JobResult>) results) {
			jobResultDAO.delete(result.getId());
		}
	}

	private boolean isNotFirstChunk() {
		return totalCount > CHUNK_SIZE;
	}

	@Inject
	private PageRenderLinkSource linkSource;
	
	@Inject
	private Cache cache;
	
	@InjectPage
	private MailJobResultsTask mailJobResultsTask;
	
	@Inject
	private MemcacheService memcacheService;
	
	private void backupResults(List<JobResult> results) throws MessagingException, IOException {
		if (cache == null || memcacheService == null) {
			//	If the cache/memcacheService is null, the user wont be able to receive emails w/ backup
			//	unless we send him up to several hundreds of emails.
			//	In this situation lets send these emails to PING_SERVICE_NOTIFY_GMAIL_COM instead. 
			mailJobResultsTask.sendResultsByMail(results, Mailer.PING_SERVICE_NOTIFY_GMAIL_COM);
		} else {
			long id = memcacheService.increment(taskId, 1, CACHED_RESULTS_FIRST_CHUNK_ID - 1);
			
			String chunkKey = getChunkKeyInCache(taskId, id);
			ArrayList<JobResult> values = new ArrayList<JobResult>(results);
			
			logger.debug("Put {} values in cache[{}]", values.size(), chunkKey);
			cache.put(chunkKey, values);
		}
	}

	public static String getChunkKeyInCache(String taskId, long chunkId) {
		return taskId + "-" + chunkId;
	}
}
