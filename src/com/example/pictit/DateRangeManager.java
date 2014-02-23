package com.example.pictit;

import java.util.Calendar;

import android.util.Log;
import android.util.Pair;

public class DateRangeManager implements LogUtils{

    Calendar Cal = null;
	public DateRangeManager(){
		//create Calendar instance
		Cal = Calendar.getInstance();
	};

	public Pair<Long,Long> getLastWeekEnd()
	{
		int offset = 1;
		int cur_day_of_week = Cal.get(Calendar.DAY_OF_WEEK);
		switch (cur_day_of_week) {
		case 1: // Sunday
			offset = -9;
			break;
		case 2: // Monday
		case 3: // Tuesday
		case 4: // Wednesday
		case 5: // Thursday
		case 6: // Friday
		case 7: // Saturday
			offset = (cur_day_of_week + 1) * -1;
			break;
		}
		int start_of_prev_weekend = cur_day_of_week + offset;

		Calendar prev_weekend_start = Calendar.getInstance();
		// reset hour, minutes, seconds and millis
		prev_weekend_start.set(Calendar.HOUR_OF_DAY, 0);
		prev_weekend_start.set(Calendar.MINUTE, 0);
		prev_weekend_start.set(Calendar.SECOND, 0);
		prev_weekend_start.set(Calendar.MILLISECOND, 0);
		prev_weekend_start.add(Calendar.DATE, start_of_prev_weekend);

		if (DEBUG) {
		  String date1 = "" + prev_weekend_start.get(Calendar.DAY_OF_MONTH) + ":" + prev_weekend_start.get(Calendar.MONTH) + ":" + prev_weekend_start.get(Calendar.YEAR);
		  String time1 = "" + prev_weekend_start.get(Calendar.HOUR_OF_DAY) + ":" + prev_weekend_start.get(Calendar.MINUTE) + ":" + prev_weekend_start.get(Calendar.SECOND);
		  Log.d("    Start Date: " , date1 + " " + time1 + " -- ");
		}

		Long val1 = prev_weekend_start.getTimeInMillis();

		// Compute from Saturday earliest morning (12:00 AM i;e Friday Midnight) till Monday earliest morning (i;e Sunday Morning)
		prev_weekend_start.add(Calendar.DATE, 2);

		if (DEBUG) {
		  String date2 = "" + prev_weekend_start.get(Calendar.DAY_OF_MONTH) + ":" + prev_weekend_start.get(Calendar.MONTH) + ":" + prev_weekend_start.get(Calendar.YEAR);
		  String time2 = "" + prev_weekend_start.get(Calendar.HOUR_OF_DAY) + ":" + prev_weekend_start.get(Calendar.MINUTE) + ":" + prev_weekend_start.get(Calendar.SECOND);
		  Log.d("    End Date: ", date2 + " " + time2);
		}

		Long val2 = prev_weekend_start.getTimeInMillis();
		Pair <Long,Long>p =new Pair<Long, Long>(val1,val2);
		return p;
	}

	@Override
	public String toString() {
	 // TODO Auto-generated method stub
	  return null;
	}

}