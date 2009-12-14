package dmitrygusev.tapestry5;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.tapestry5.Field;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.Translator;
import org.apache.tapestry5.ValidationException;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.FormSupport;

public class TimeTranslator implements Translator<Date> {

	public static final String LONG_TIME_REGEXP = "^(\\d|[01]\\d|[2][0123]):[012345]\\d$";
	
	public String getMessageKey() {
		return "time-format-exception";
	}

	public String getName() {
		return "time";
	}

    public Date parseClient(Field field, String clientValue, String message)
    	 throws ValidationException
	{
		try
		{
		    return parseTime(new Date(), clientValue.trim());
		}
		catch (Exception ex)
		{
		    throw new ValidationException(message);
		}
	}

    public void render(Field field, String message, MarkupWriter writer, FormSupport formSupport)
    {
        formSupport.addValidation(field, "regexp", message, LONG_TIME_REGEXP);
    }

	public Class<Date> getType() {
		return Date.class;
	}

	@Inject
	public Messages messages;	
	
	public String toClient(Date value) {
		return Utils.getFriendlyTime(messages, value);
	}

	/**
	 * 
	 * @param date
	 * @param time Строка в формате ЧЧ:ММ
	 * @return
	 */
	public static Date parseTime(Date date, String time) {
		if (date == null) {
			throw new IllegalArgumentException("date");
		}
		if (time == null) {
			throw new IllegalArgumentException("time");
		}
		
		String regex = LONG_TIME_REGEXP;
		
		if (! Pattern.matches(regex, time)) {
			throw new IllegalArgumentException(
					MessageFormat.format(
							"Неверный формат времени: \"{0}\" не удовлетворяет регулярному выражению \"{1}\"",
							time, regex));
		}
		
		String[] timeParts = time.split(":");
	
		Calendar calendar = Calendar.getInstance();
		
		calendar.setTime(date);
		
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
		calendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	
		return calendar.getTime();
	}

}
