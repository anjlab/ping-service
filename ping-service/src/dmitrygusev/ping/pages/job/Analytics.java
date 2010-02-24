package dmitrygusev.ping.pages.job;


import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
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
import org.datanucleus.store.appengine.query.JPACursorHelper;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;
import com.google.appengine.api.users.UserServiceFactory;

import anjlab.cubics.Cube;
import anjlab.cubics.FactModel;
import anjlab.cubics.aggregate.histogram.HistogramAggregateFactory;
import anjlab.cubics.aggregate.histogram.Histogram.HistogramMergeStrategy;
import anjlab.cubics.aggregate.pie.PieAggregateFactory;
import anjlab.cubics.coerce.IntegerCoercer;
import anjlab.cubics.renders.HtmlRender;
import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;
import dmitrygusev.ping.pages.Index;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.JobResultCSVExporter;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.ping.services.dao.JobResultDAO;
import dmitrygusev.ping.services.security.GAEHelper;

@SuppressWarnings("unused")
public class Analytics {

	private static final String CSV = "csv";

	@Property
	private Job job;
	
	private Date dateFrom;
	
	private Date dateTo;

	public String getDateFrom() {
		return application.formatDate(dateFrom);
	}
	
	public String getDateTo() {
		return application.formatDate(dateTo);
	}
	
	@InjectPage
	private Index index;
	
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
		return UserServiceFactory.getUserService().isUserAdmin();
	}
	
	public Index onActivate(Long scheduleId, Long jobId) {
		int defaultEnd = 10000;
		try { 
			this.end = request.getParameterNames().contains("end") 
				? Long.parseLong(request.getParameter("end")) 
				: defaultEnd; 
		} catch(Exception e) {
			this.end = defaultEnd;
		}
		
		try {
			job = application.findJob(scheduleId, jobId);
			
			if (job == null) {
				index.setExceptionMessage("Job not found");
				return index;
			}
			
			//	Set default view
			if (view == null) {
				view = DEFAULT_VIEW;
			}
			
			initResults();

		} catch (Exception e) {
			index.setExceptionMessage(e.getMessage());
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
	
	@Inject
	private JobResultCSVExporter csvExporter;
	
	public StreamResponse onActionFromExportCSV() {
		return getExport(CSV);
	}

	private static final String DEFAULT_VIEW = "year > month > weekOfMonth > day";
	
	@Property
	private final String viewModel = DEFAULT_VIEW + ",dayTime > hour,dayOfWeek > month";
	
	@Property
	@Persist
	private String view;
	
	@Inject
	private JobResultDAO jobResultDAO;
	
	private double end;

	private List<JobResult> results;

	public String getCubeHTML() {
		TimeZone timeZone = application.getTimeZone();

		for (JobResult result : results) {
			result.setTimeZone(timeZone);
		}
		
		FactModel<JobResult> model = new FactModel<JobResult>(JobResult.class);
		
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
			setLabel("year", "Year").
			setLabel("month", "Month").
			setLabel("weekOfMonth", "Week Of Month").
			setLabel("day", "Day").
			setLabel("hour", "Hour").
			setLabel("dayTime", "Day Time").
			setLabel("dayOfWeek", "Day Of Week");
		
		return render.render().toString();
	}

	private void initResults() {
		results = jobResultDAO.getResults(job, 1000);

		if (results.size() > 0) {
			dateTo = results.get(0).getTimestamp();
			dateFrom = results.get(results.size() - 1).getTimestamp();
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
				InputStream csvExport = csvExporter.export(results);
				
				return csvExport;
			}
			
			@Override
			public String getContentType() {
				return "text/csv";					
			}
		};
	}
	
	public void onActionFromRunCounterTask() {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(null, url("/task/counter/")
					.param("key", KeyFactory.keyToString(job.getKey()))
					.method(Method.GET));
	}
}
