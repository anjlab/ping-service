package dmitrygusev.ping.services.dao;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Schedule;

public interface ScheduleDAO {

	public abstract void update(Schedule schedule);

	public abstract void delete(Long id);

	public abstract Schedule createSchedule(String name);

	public abstract Schedule find(Key scheduleKey);

}