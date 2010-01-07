package dmitrygusev.ping.pages.job;


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
	}
	
	@Inject
	private Application application;
	
	@Inject
	private Request request;
	
	public Index onActivate(Long scheduleId, Long jobId) {
		int defaultEnd = 2800;
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
	
	public String getCubeHTML() {
		List<JobResult> results = jobResultDAO.getResults(job, 1000);

		TimeZone timeZone = application.getTimeZone();

		for (JobResult result : results) {
			result.setTimeZone(timeZone);
		}
		
		FactModel<JobResult> model = new FactModel<JobResult>(JobResult.class);
		
		model.setDimensions(view.split(" > "));
		model.setMeasures("responseTime", "succeeded");
		model.declareCustomAggregate(new PieAggregateFactory<JobResult>(new IntegerCoercer()), "succeeded");
		model.declareCustomAggregate(
				new HistogramAggregateFactory<JobResult>(HistogramMergeStrategy.NumericRanges, 0, end / 10, end), 
				"responseTime");
		
		Cube<JobResult> cube = Cube.createCube(model, results);
		
		HtmlRender<JobResult> render = new HtmlRender<JobResult>(cube);
		
		render.getAggregatesOptions("succeeded").
			reorder("pie-1-%", "count").
			exclude("min", "max", "sum", "avg", "pie").
			setFormat("pie-1-%", "%.5f").
			setLabel("pie-1-%", "avail. %").
			setLabel("count", "# of pings");

		render.getAggregatesOptions("responseTime").
			reorder("avg").
			exclude("count").
			setLabel("histogram", "chart");
		
		render.getMeasuresOptions().
			setLabel("responseTime", "Response Time, ms").
			setLabel("succeeded", "Availability");
		
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
				InputStream csvExport = csvExporter.export(job);
				
				return csvExport;
			}
			
			@Override
			public String getContentType() {
				return "text/csv";					
			}
		};
	}
}
