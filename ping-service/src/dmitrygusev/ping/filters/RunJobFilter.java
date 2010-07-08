package dmitrygusev.ping.filters;

import static com.google.appengine.api.datastore.KeyFactory.stringToKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.task.RunJobTask;
import dmitrygusev.ping.services.Utils;

public class RunJobFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(RunJobFilter.class);
    
    @Override
    protected void processRequest() throws Exception {
        String encodedJobKey = globals.getHTTPServletRequest().getParameter(RunJobTask.JOB_KEY_PARAMETER_NAME);

        if (Utils.isNullOrEmpty(encodedJobKey)) {
            return;
        }
        
        Key key = stringToKey(encodedJobKey);
        
        logger.debug("Running job: {}", key);

        Job job = jobDAO.find(key);
        
        if (job == null) {
            return;
        }
        
        application.runJob(job);
        
        application.updateJob(job, false, true);
    }

}
