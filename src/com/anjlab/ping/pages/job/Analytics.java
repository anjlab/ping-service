package com.anjlab.ping.pages.job;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.cubics.BeanValueProvider;
import com.anjlab.cubics.Cube;
import com.anjlab.cubics.FactModel;
import com.anjlab.cubics.aggregate.histogram.Histogram.HistogramMergeStrategy;
import com.anjlab.cubics.aggregate.histogram.HistogramAggregateFactory;
import com.anjlab.cubics.aggregate.pie.PieAggregateFactory;
import com.anjlab.cubics.coerce.IntegerCoercer;
import com.anjlab.cubics.renders.html.HtmlRender;
import com.anjlab.ping.entities.Job;
import com.anjlab.ping.entities.JobResult;
import com.anjlab.ping.filters.BackupJobResultsFilter;
import com.anjlab.ping.pages.Index;
import com.anjlab.ping.services.Application;
import com.anjlab.ping.services.JobResultCSVExporter;
import com.anjlab.ping.services.JobResultsAnalyzer;
import com.anjlab.ping.services.Mailer;
import com.anjlab.ping.services.Utils;
import com.anjlab.ping.services.location.IPResolver;
import com.anjlab.ping.services.location.Location;
import com.anjlab.ping.services.location.LocationResolver;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;


public class Analytics {

    private static final Logger logger = LoggerFactory.getLogger(Analytics.class);

    private static final String CSV = "csv";

    @Property
    private Job job;
    
    private Date dateFrom;
    
    private Date dateTo;

    public String getTimeDiff() {
        return Utils.formatMillisecondsToWordsUpToMinutes(dateTo.getTime() - dateFrom.getTime());
    }
    
    public String getDateFrom() {
        return application.formatDate(dateFrom);
    }
    
    public String getDateTo() {
        return application.formatDate(dateTo);
    }
    
    public String getClientTime() {
        return application.formatDate(new Date());
    }
    
    @InjectPage
    private Index index;
    
    @SuppressWarnings("unused")
    @Property
    @Persist
    private String message;

    @AfterRender
    public void cleanup() {
        message = null;
        job = null;
        view = null;
        results = null;
    }
    
    @Inject
    private Application application;
    
    @Inject
    private Request request;
    
    public boolean isAdmin() {
        UserService userService = UserServiceFactory.getUserService();
        
        return userService.isUserLoggedIn() 
            && userService.isUserAdmin();
    }
    
    public Index onActivate(Long jobId) {
        try {
            int defaultEnd = 10000;
            try {
                this.end = request.getParameterNames().contains("end") 
                    ? Long.parseLong(request.getParameter("end")) 
                    : defaultEnd; 
            } catch(Exception e) {
                this.end = defaultEnd;
            }
            
            job = application.findJob(jobId);
            
            if (job == null) {
                index.operationFailed("Job not found");
                return index;
            }
            
            //    Set default view
            if (view == null) {
                view = DEFAULT_VIEW;
            }
            
            initResults();
        } catch (Exception e) {
            index.operationFailed(e.getMessage(), e);
            return index;
        }
        
        return null;
    }

    public Long[] onPassivate() {
        if (job != null) {
            return Utils.createJobContext(job);
        }
        return null;
    }
    
    public StreamResponse onActionFromExportCSV() {
        return getExport(CSV);
    }

    private static final String DEFAULT_VIEW = "month > day";
    
    @SuppressWarnings("unused")
    @Property
    private final String viewModel = DEFAULT_VIEW + ",dayTime > hour,dayOfWeek > month";
    
    @Property
    @Persist
    private String view;
    
    private double end;

    private List<JobResult> results;

    public String getCubeHTML() {
        TimeZone timeZone = application.getTimeZone();
        
        for (JobResult result : results) {
            result.setTimeZone(timeZone);
        }
        
        FactModel<JobResult> model = new FactModel<JobResult>(new BeanValueProvider<JobResult>(JobResult.class));
        
        model.setDimensions(view.split(" > "));
        model.setMeasures("responseTime", "pingResult");
        model.declareCustomAggregate(new PieAggregateFactory<JobResult>(new IntegerCoercer()), "pingResult");
        model.declareCustomAggregate(
                new HistogramAggregateFactory<JobResult>(HistogramMergeStrategy.NumericRanges, 0, end / 10, end), 
                "responseTime");
        
        Cube<JobResult> cube = Cube.createCube(model, results);
        
        HtmlRender<JobResult> render = new HtmlRender<JobResult>(cube);
        
        render.getAggregatesOptions("pingResult").
            reorder("pie-" + Job.PING_RESULT_OK + "-%", "count", "pie").
            exclude("min", "max", "sum", "avg").
            setFormat("pie-" + Job.PING_RESULT_OK + "-%", "%.5f").
            setLabel("pie-" + Job.PING_RESULT_OK + "-%", "%").
            setLabel("count", "# of pings").
            setLabel("pie", "chart");
        
        render.getAggregatesOptions("responseTime").
            reorder("avg").
            exclude("count").
            setLabel("histogram", "chart");
        
        render.getMeasuresOptions().
            setLabel("responseTime", "Response Time, ms").
            setLabel("pingResult", "Availability");
        
        render.getDimensionsOptions().
            setLabel("all", "All").
            setLabel("month", "Month").
            setLabel("weekOfMonth", "Week Of Month").
            setLabel("day", "Day").
            setLabel("hour", "Hour").
            setLabel("dayTime", "Day Time").
            setLabel("dayOfWeek", "Day Of Week");
        
        return render.render().toString();
    }

    private void initResults() {
        results = job.getRecentJobResults(Application.DEFAULT_NUMBER_OF_JOB_RESULTS);
        
        if (results.size() > 0) {
            dateFrom = results.get(0).getTimestamp();
            dateTo = results.get(results.size() - 1).getTimestamp();
        } else {
            dateTo = dateFrom = new Date();
        }
    }
    
    private StreamResponse getExport(final String format) {
        return new StreamResponse() {
            @Override
            public void prepareResponse(Response response) {
                response.setHeader(
                        "Content-Disposition", 
                        "attachment; filename=" + getFilename());
            }
            
            private String getFilename() {
                return Utils.getCSVExportFilename(job);
            }

            @Override
            public InputStream getStream() throws IOException {
                InputStream csvExport = new ByteArrayInputStream(
                        JobResultCSVExporter.export(application.getTimeZone(), results));
                
                return csvExport;
            }
            
            @Override
            public String getContentType() {
                return "text/csv";
            }
        };
    }
    
    public void onActionFromRunJob() {
        application.runJob(job);
        application.updateJob(job, false, false);
        
        message = job.getLastPingSummary();
    }
    
    public void onActionFromSendResultsByMail() throws Exception {
        BackupJobResultsFilter filter = new BackupJobResultsFilter();
        filter.setApplication(application);
        filter.sendResultsByMail(
                job, job.getRecentJobResults(Integer.MAX_VALUE), Mailer.DMITRY_GUSEV_GMAIL_COM);
    }
    
    public String getLocationMetrics() {
        Location pingServiceLocation = getPingServiceLocation();
        Location pingURLLocation = getJobLocation();
        
        long distance = pingServiceLocation.distanceInMeters(pingURLLocation);
        
        return "It is " + formatDistanceInKilometers(distance) 
        + " kilometers from <span class='hoverable' title='" 
        + pingServiceLocation + "'>Ping Service</span> to <span class='hoverable' title='" 
        + pingURLLocation + "'>" 
        + job.getTitleFriendly() + "</span>.";
    }

    private String formatDistanceInKilometers(long distanceInMeters) {
        return String.format("%,d", distanceInMeters / 1000);
    }

    private Location pingServiceLocation;
    
    @Inject
    private LocationResolver locationResolver;
    @Inject
    private IPResolver ipResolver;
    
    public Location getPingServiceLocation() {
        if (pingServiceLocation == null) {
            pingServiceLocation = locationResolver.resolveLocation(
                    ipResolver.resolveIp("http://ping-service.appspot.com"));
        }
        return pingServiceLocation;
    }

    private Location jobLocation;
    
    public Location getJobLocation() {
        if (jobLocation == null) {
            jobLocation = locationResolver.resolveLocation(ipResolver.resolveIp(job.getPingURL()));
        }
        return jobLocation;
    }

    public String getDetailedReport() {
        JobResultsAnalyzer analyzer = new JobResultsAnalyzer(results, true);
        
        TimeZone timeZone = application.getTimeZone();
        
        StringBuilder report = analyzer.buildHtmlReport(timeZone);
        
        report.insert(0, "<p>Time Zone: " + timeZone.getDisplayName() + " (" + timeZone.getID() + ")</p>");
        
        return report.toString();
    }
    
    public Long[] getJobContext() {
        return Utils.createJobContext(job);
    }
    
    public void onActionFromDeleteJob() {
        try {
            //  This is admin action
            if (isAdmin()) {
                application.deleteJob(job.getKey().getId(), false);
            }
        } catch (Exception e) {
            logger.error("Error deleting job", e);
            message = "Error deleting job";
        }
    }
}
