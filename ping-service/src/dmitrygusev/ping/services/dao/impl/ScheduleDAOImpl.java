package dmitrygusev.ping.services.dao.impl;

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.dao.ScheduleDAO;

public class ScheduleDAOImpl implements ScheduleDAO {

	@PersistenceContext
	private EntityManager em;
	
	public Schedule createSchedule(String name) {
		Schedule schedule = new Schedule();
		schedule.setName(name);
		
		em.persist(schedule);
		
		return schedule;
	}

	@Override
	public void update(Schedule schedule) {
		em.merge(schedule);
	}

	@Override
	public void delete(Long id) {
		Schedule schedule = find(createKey(Schedule.class.getSimpleName(), id));
		em.remove(schedule);
	}

	@Override
	public Schedule find(Key scheduleKey) {
		return em.find(Schedule.class, scheduleKey);
	}
}
