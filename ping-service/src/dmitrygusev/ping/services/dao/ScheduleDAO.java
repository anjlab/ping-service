package dmitrygusev.ping.services.dao;

import java.util.List;

import org.tynamo.jpa.annotations.CommitAfter;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Schedule;

public interface ScheduleDAO {
	@CommitAfter
	public abstract void update(Schedule schedule);
	@CommitAfter
	public abstract void delete(Long id);
	@CommitAfter
	public abstract Schedule createSchedule(String name);
    @CommitAfter
	public abstract Schedule find(Key scheduleKey);
    @CommitAfter
    public abstract List<Schedule> getAll();

}