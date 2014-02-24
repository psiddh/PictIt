package com.example.pictit;

import java.util.Calendar;
import java.util.Locale;

import android.util.Log;
import android.util.Pair;

public class DateRangeManager implements LogUtils{

    Calendar Cal = null;
    private int mCurDayOfWeek = 0;
	public DateRangeManager(){
		// TBD: Timezone issues ?
		//create Calendar instance
		Cal = Calendar.getInstance((Locale.getDefault()));
		mCurDayOfWeek = Cal.get(Calendar.DAY_OF_WEEK);
	};

	public Pair<Long,Long> getLastWeekEnd()
	{
		int offset = 1;
		switch (mCurDayOfWeek) {
		case 1: // Sunday
			offset = -9;
			break;
		case 2: // Monday
		case 3: // Tuesday
		case 4: // Wednesday
		case 5: // Thursday
		case 6: // Friday
		case 7: // Saturday
			offset = (mCurDayOfWeek + 1) * -1;
			break;
		}
		int start_of_prev_weekend = mCurDayOfWeek + offset;

		Calendar prev_weekend_start = Calendar.getInstance((Locale.getDefault()));
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
		return new Pair<Long, Long>(val1,val2);
	}

	public Pair<Long,Long> getToday()
	{
	   Long val1, val2;
	    Calendar today = Calendar.getInstance((Locale.getDefault()));
		// reset hour, minutes, seconds and millis
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		val1 = today.getTimeInMillis();
		today.add(Calendar.DATE, 1);
		val2 = today.getTimeInMillis();
		return new Pair<Long, Long>(val1,val2);
	}

	@Override
	public String toString() {
	 // TODO Auto-generated method stub
	  return null;
	}

}