package dmitrygusev.ping.pages.cron;

import java.util.List;

import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.dao.ScheduleDAO;

public class Schedules {

    @Inject
    private ScheduleDAO scheduleDAO;
    @SuppressWarnings("unused")
    @Property
    private Schedule schedule;
    @SuppressWarnings("unused")
    @Property
    private Job job;
    public List<Schedule> getSchedules() {
        return scheduleDAO.getAll();
    }
    
}
