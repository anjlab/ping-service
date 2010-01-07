package dmitrygusev.ping.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import dmitrygusev.ping.entities.JobResult;

public class JobResultCSVExporter {

	private static final char SEPARATOR_CHAR = ';';

	private static final String[] COLUMN_HEADERS = {
		"timestamp",
		"year",
		"month",
		"day",
		"hour",
		"minute",
		"day of week",
		"duration",
		"succeeded"
	};

	private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd hh:mm:ss";

	private static final String[] COLUMN_HEADER_PATTERNS = {
		TIMESTAMP_PATTERN,
		"2009",
		"12",
		"31",
		"24",
		"59",
		"7",
		"99999",
		"0"
	};

	private static final String HEADER_TEXT = getHeaderText();
	
	private static String getHeaderText() {
		StringBuffer buffer = new StringBuffer();
		for (String column : COLUMN_HEADERS) {
			if (buffer.length() > 0) {
				buffer.append(SEPARATOR_CHAR);
			}
			buffer.append(column);
		}
		buffer.append('\n');
		return buffer.toString();
	}

	private static final String HEADER_PATTERN = getHeaderPattern();
	
	private static String getHeaderPattern() {
		StringBuffer buffer = new StringBuffer();
		for (String pattern : COLUMN_HEADER_PATTERNS) {
			if (buffer.length() > 0) {
				buffer.append(SEPARATOR_CHAR);
			}
			buffer.append(pattern);
		}
		buffer.append('\n');
		return buffer.toString();
	}
	
	public InputStream export(List<JobResult> results) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP_PATTERN);

		ByteArrayOutputStream baos = new ByteArrayOutputStream(
				HEADER_TEXT.length() + results.size() * HEADER_PATTERN.length());
		
		write(baos, HEADER_TEXT);
		
		Calendar c = Calendar.getInstance();
		
		for (JobResult result : results) {
			c.setTime(result.getTimestamp());
			
			write(baos, dateFormat.format(c.getTime()));
			write(baos, SEPARATOR_CHAR);
			write(baos, c.get(Calendar.YEAR));
			write(baos, SEPARATOR_CHAR);
			write(baos, c.get(Calendar.MONTH) + 1);
			write(baos, SEPARATOR_CHAR);
			write(baos, c.get(Calendar.DAY_OF_MONTH));
			write(baos, SEPARATOR_CHAR);
			write(baos, c.get(Calendar.HOUR_OF_DAY));
			write(baos, SEPARATOR_CHAR);
			write(baos, c.get(Calendar.MINUTE));
			write(baos, SEPARATOR_CHAR);
			write(baos, c.get(Calendar.DAY_OF_WEEK));
			write(baos, SEPARATOR_CHAR);
			write(baos, result.getResponseTime());
			write(baos, SEPARATOR_CHAR);
			write(baos, result.isFailed() ? 0 : 1);
			write(baos, '\n');
		}
		
        return new ByteArrayInputStream(baos.toByteArray());
	}

	private static void write(OutputStream output, String s) throws IOException {
		output.write(s.getBytes());
	}
	
	private static void write(OutputStream output, char c) throws IOException {
		output.write(String.valueOf(c).getBytes());
	}
	
	private static void write(OutputStream output, int i) throws IOException {
		output.write(String.valueOf(i).getBytes());
	}
	
}
