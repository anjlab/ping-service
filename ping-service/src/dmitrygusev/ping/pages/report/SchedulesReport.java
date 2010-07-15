package dmitrygusev.ping.pages.report;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.tapestry5.annotations.BeforeRenderTemplate;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.ping.services.dao.ScheduleDAO;
import dmitrygusev.tapestry5.AbstractReadonlyPropertyConduit;

public class SchedulesReport {

    @Inject
    private ScheduleDAO scheduleDAO;
    @SuppressWarnings("unused")
    @Property
    private Schedule schedule;
    
    private List<Schedule> schedules;
    
    public List<Schedule> getSchedules() {
        if (schedules == null) {
            schedules = scheduleDAO.getAll();
        }
        return schedules;
    }

    @Component(id="grid")
    private Grid grid;
    
    @BeforeRenderTemplate
    void beforeRender() {
        if (grid.getSortModel().getSortConstraints().isEmpty()) {
            //  ascending
            grid.getSortModel().updateSort("name");
        }
    }
    

    private int counter = 0;

    @Inject private BeanModelSource beanModelSource;
    @Inject private Messages messages;

    public BeanModel<?> getModel() {
        BeanModel<?> beanModel = beanModelSource.createDisplayModel(Schedule.class, messages);

        beanModel.add("jobsCount", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ((Schedule) instance).getJobs().size();
            }
        });
        beanModel.add("SN", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ++counter;
            }
        }).sortable(false);
        
        beanModel.add("merge", null);

        beanModel.exclude();

        beanModel.reorder(
                        "SN",
                        "name",
                        "id",
                        "jobsCount",
                        "meta");
        
        return beanModel;
    }
    
    public void onActionFromClearMarked() {
        List<Schedule> schedules = getSchedules();
        
        for (Schedule schedule : schedules) {
            if (schedule.getMeta() != null) {
                schedule.setMeta(null);
                scheduleDAO.update(schedule);
            }
        }
    }
    
    public void onActionFromMarkDuplicates() {
        List<Schedule> schedules = getSchedules();
        
        Collections.sort(schedules, new Comparator<Schedule>() {
            public int compare(Schedule o1, Schedule o2) {
                String criteria1 = buildCriteria(o1);
                String criteria2 = buildCriteria(o2);
                
                return criteria2.compareTo(criteria1);
            }

            private String buildCriteria(Schedule schedule) {
                return String.format("%s-%s-%s", 
                        schedule.getName(), 
                        schedule.getJobs().size(), 
                        Utils.isNullOrEmpty(schedule.getMeta()) ? 0 : 1);
            };
        });
        
        Schedule firstInGroup = null;
        boolean metaChanged = false;
        for (int i = 1; i < schedules.size(); i++) {
            Schedule previous = schedules.get(i-1);
            Schedule current = schedules.get(i);
            if (current.getName().equals(previous.getName())) {
                if (firstInGroup == null) {
                    firstInGroup = previous;
                }
                
                String meta = firstInGroup.getMeta();
                if (meta == null) {
                    meta = "";
                }
                String currentId = current.getId().toString();
                if (!meta.contains(":" + currentId + ":")) {
                    firstInGroup.setMeta(meta.concat(":").concat(currentId).concat(":"));
                    metaChanged = true;
                }
            } else {
                if (firstInGroup != null && metaChanged) {
                    scheduleDAO.update(firstInGroup);
                }
                firstInGroup = null;
                metaChanged = false;
            }
        }
    }
    
    private Schedule lookupById(Long scheduleId) {
        for (Schedule schedule : getSchedules()) {
            if (schedule.getId().equals(scheduleId)) {
                return schedule;
            }
        }
        return null;
    }
    
    @Inject
    private Application application;
    
    public void onActionFromMerge(Long scheduleId) {
        Schedule target = lookupById(scheduleId);
        
        String meta = target.getMeta();
        
        for (String sourceId : meta.split(":")) {
            if (Utils.isNullOrEmpty(sourceId)) {
                continue;
            }
            
            Schedule source = lookupById(Long.parseLong(sourceId));
            if (source.getJobs().size() > 0) {
                for (Job job : source.getJobs()) {
                    application.moveJob(job, target);
                }
            }
            application.delete(source);
        }
        
        target.setMeta(null);
        scheduleDAO.update(target);
    }
}
