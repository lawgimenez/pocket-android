package com.pocket.sdk.api.value;

import org.apache.commons.lang3.ObjectUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A String representing a date in the format of `yyyy-MM-dd HH:mm:ss`
 */
public class DateString implements Comparable<DateString> {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	
	public final String date;
	private String display;
	
	public static DateString fromMillis(long millis) {
		return new DateString(DATE_FORMAT.format(new Date(millis)));
	}
	
	public DateString(String date) {
		this.date = date;
	}
	
	/**
	 * @return The date formatted by the default DateFormat, or the raw value if there was an error parsing it or it is null.
	 */
	public String display() {
		if (display == null && date != null) {
			try {
				display = DateFormat.getDateInstance().format(parsed());
			} catch (ParseException e) {
				display = date;
			}
		}
		return display;
	}
	
	private Date parsed() throws ParseException {
		return DATE_FORMAT.parse(date); // Maybe we want to lazy init and keep this?
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		DateString that = (DateString) o;
		
		if (date != null ? !date.equals(that.date) : that.date != null) return false;
		
		return true;
	}
	
	@Override public int hashCode() {
		return date != null ? date.hashCode() : 0;
	}
	
	@Override
	public String toString() {
		return date;
	}
	
	@Override
	public int compareTo(DateString o) {
		return ObjectUtils.compare(date, o.date); // The date format is such that a string comparison is equally correct.
	}
	
	/**
	 * @return This date represented as a timestamp in milliseconds, or 0 if it is null or can't be parsed.
	 */
	public long millis() {
		try {
			return parsed().getTime();
		} catch (ParseException e) {
			return 0;
		}
	}
}
