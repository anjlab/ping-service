package dmitrygusev.ping.services.dao.impl;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.dao.ScheduleDAO;

public class ScheduleDAOImpl implements ScheduleDAO {

    @Inject
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
        em.createQuery("DELETE FROM Schedule s WHERE s.id = :id")
               .setParameter("id", id)
               .executeUpdate();
    }

    @Override
    public Schedule find(Key scheduleKey) {
        return em.find(Schedule.class, scheduleKey);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Schedule> getAll() {
        return em.createQuery("SELECT FROM Schedule").getResultList();
    }
}
