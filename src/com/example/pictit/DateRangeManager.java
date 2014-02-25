package com.example.pictit;

import java.util.Calendar;
import java.util.Locale;

import android.util.Log;
import android.util.Pair;

public class DateRangeManager implements LogUtils{

	private String TAG = "pickit/DateRangeManage";
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
		// TBD: Ah ! This may vary based on different world cultures
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
			offset = (mCurDayOfWeek + 2) * -1;
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

		this.printDateAndTime(prev_weekend_start);
		Long val1 = prev_weekend_start.getTimeInMillis();
		// Compute from Saturday earliest morning (12:00 AM i;e Friday Midnight) till Monday earliest morning (i;e Sunday night)
		prev_weekend_start.add(Calendar.DATE, 2);
		this.printDateAndTime(prev_weekend_start);

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

	public void printDateAndTime(Calendar cal) {
	    if (DEBUG) {
	        String date = "" + cal.get(Calendar.DAY_OF_MONTH) + ":" + cal.get(Calendar.MONTH) + ":" + cal.get(Calendar.YEAR);
	        String time = "" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);

	        Log.d(TAG , date + " " + time);
	     }
	}

	@Override
	public String toString() {
	 // TODO Auto-generated method stub
	  return null;
	}

}