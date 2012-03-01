package com.anjlab.ping.services.dao;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.tapestry5.jpa.annotations.CommitAfter;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.filters.RunJobFilter;
import com.google.appengine.api.datastore.Key;


public interface JobDAO {
    @CommitAfter
    public abstract List<Key> getJobsByCronString(String cronString);
    @CommitAfter
    public abstract Job delete(Long jobId);
    @CommitAfter
    public abstract void update(Job job, boolean commitAfter);
    @CommitAfter
    public abstract Job find(Long jobId);
    @CommitAfter
    public abstract Job find(Key jobKey);
    @CommitAfter
    public abstract List<Job> getAll();
    /**
     * Used in non-tapestry code
     * 
     * @param em
     * 
     * @see RunJobFilter
     */
    public abstract void setEntityManager(EntityManager em);
    @CommitAfter
    public abstract Job createJob(Job job);
    @CommitAfter
    public abstract List<Job> findByScheduleName(String scheduleName);
    @CommitAfter
    public abstract List<Job> findByPingURL(String string);

    // XXX
    public void onAfterCommitNewJob(Job job);
}