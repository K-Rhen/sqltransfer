package com.topdesk.sqltransfer;

import java.util.Calendar;
import java.util.Date;

import org.tiling.scheduling.ScheduleIterator;

public class DailyIterator implements ScheduleIterator {
	private final Calendar calendar = Calendar.getInstance();

	public DailyIterator(Date date) {
		calendar.setTime(date);
		calendar.add(Calendar.DATE, -1);
	}

	public Date next() {
		calendar.add(Calendar.DATE, 1);
		return calendar.getTime();
	}

}