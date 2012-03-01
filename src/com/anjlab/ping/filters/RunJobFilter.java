package com.anjlab.ping.filters;

import static com.google.appengine.api.datastore.KeyFactory.stringToKey;

import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.services.Utils;
import com.google.appengine.api.datastore.Key;


public class RunJobFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(RunJobFilter.class);
    public static final String JOB_KEY_PARAMETER_NAME = "job";
    
    @Override
    protected void processRequest(EntityTransaction tx) throws Exception {
        String encodedJobKey = globals.getHTTPServletRequest().getParameter(RunJobFilter.JOB_KEY_PARAMETER_NAME);

        if (Utils.isNullOrEmpty(encodedJobKey)) {
            return;
        }
        
        Key key = stringToKey(encodedJobKey);
        
        logger.debug("Running job: {}", key);

        Job job = jobDAO.find(key);
        
        if (job == null) {
            return;
        }
        
        if (job.isSuspended()) {
            logger.debug("{} suspended", job);
            return;
        }
        
        application.runJob(job);
        
        application.updateJob(job, false, true);
    }

}
