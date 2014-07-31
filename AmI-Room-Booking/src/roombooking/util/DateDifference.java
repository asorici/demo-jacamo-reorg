package roombooking.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateDifference {
	public static final double SECONDS_FACTOR = 1000;
	public static final double MINUTE_FACTOR = 60 * SECONDS_FACTOR;
	public static final double HOUR_FACTOR = 60 * MINUTE_FACTOR;
	public static final double DAY_FACTOR = 24 * HOUR_FACTOR;
	
	public static double getAsMillis(Calendar start, Calendar end) {
		return end.getTimeInMillis() - start.getTimeInMillis();
	}
	
	public static double getAsSeconds(Calendar start, Calendar end) {
		double difference = end.getTimeInMillis() - start.getTimeInMillis();
		return (difference / SECONDS_FACTOR);
	}
	
	public static double getAsMinutes(Calendar start, Calendar end) {
		double difference = end.getTimeInMillis() - start.getTimeInMillis();
		return (difference / MINUTE_FACTOR);
	}
	
	public static double getAsHours(Calendar start, Calendar end) {
		double difference = end.getTimeInMillis() - start.getTimeInMillis();
		return (difference / HOUR_FACTOR);
	}
	
	public static double getAsDays(Calendar start, Calendar end) {
		double difference = end.getTimeInMillis() - start.getTimeInMillis();
		return (difference / DAY_FACTOR);
	}
	
	public static String calendar2string(Calendar date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		return sdf.format(date.getTime());
	}
	
	public static Calendar string2calendar(String dateString) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		
		Calendar cal = Calendar.getInstance();
	    try {
			cal.setTime(sdf.parse(dateString));
	    } catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
		
	    return cal;
	}
	
	public static Calendar calendarFromTimeUnits(int day, int hour, int minute) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_YEAR, day);
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		return c;
	}
}
