package com.anjlab.ping.pages.task;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.services.AppModule;
import com.anjlab.ping.services.dao.JobDAO;

@Meta(AppModule.NO_MARKUP)
public class UpdateJobsTask {

    private static final Logger logger = LoggerFactory.getLogger(UpdateJobsTask.class);
    
    @Inject
    private JobDAO jobDAO;
    
    public void onActivate() throws URISyntaxException {
        List<Job> jobs = jobDAO.getAll();
        logger.warn("Found {} jobs", jobs.size());
        int count = 0;
        for (Job job : jobs) {
            if (job.getModifiedAt() == null) {
                logger.warn("Updating {} job", ++count);
                job.fireModified("system");
            }
            jobDAO.update(job, false);
        }
    }
}
