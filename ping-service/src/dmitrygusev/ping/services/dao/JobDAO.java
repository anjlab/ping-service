package dmitrygusev.ping.services.dao;

import java.util.List;

import javax.persistence.EntityManager;

import org.tynamo.jpa.annotations.CommitAfter;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.filters.RunJobFilter;

public interface JobDAO {
    @CommitAfter
    public abstract List<Key> getJobsByCronString(String cronString);
    @CommitAfter
    public abstract void delete(Long scheduleId, Long jobId);
    @CommitAfter
    public abstract void update(Job job, boolean commitAfter);
    @CommitAfter
    public abstract Job find(Long scheduleId, Long jobId);
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

}