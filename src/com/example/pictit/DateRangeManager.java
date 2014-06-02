package com.example.pictit;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.util.Log;
import android.util.Pair;

public class DateRangeManager implements LogUtils{

    private String TAG = "pickit/DateRangeManage";
    Calendar Cal = null;
    private int mCurDayOfWeek = -1;
    private int mYear = -1;
    private int mMonth = -1;
    private int mDayOfMonth = -1;
    public DateRangeManager(){
        // TBD: Timezone issues ?
        //create Calendar instance
        Cal = Calendar.getInstance((Locale.getDefault()));
        mCurDayOfWeek = Cal.get(Calendar.DAY_OF_WEEK);
        mYear = Cal.get(Calendar.YEAR);
        mMonth = Cal.get(Calendar.MONTH);
        mDayOfMonth = Cal.get(Calendar.DAY_OF_MONTH);
    };

    public int getCurrentYear() {
        return mYear;
    }

    public int getCurrentMonth() {
        return mMonth;
    }

    public int getCurrentDayOfMonth() {
        return mDayOfMonth;
    }

    public Pair<Long,Long> getLastWeekEnd() {
        int offset = 0;
        // TBD: Ah ! This may vary based on different world cultures
        switch (mCurDayOfWeek) {
        case 1: // Sunday
            offset = -8;
            break;
        case 2: // Monday
        case 3: // Tuesday
        case 4: // Wednesday
        case 5: // Thursday
        case 6: // Friday
        case 7: // Saturday
            offset = mCurDayOfWeek  * -1;
            break;
        }
        int start_of_prev_weekend =  offset;

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

    public Pair<Long,Long> getToday() {
       Long val1, val2;
        Calendar today = Calendar.getInstance((Locale.getDefault()));
        Calendar today1 = Calendar.getInstance((Locale.getDefault()));
        // reset hour, minutes, seconds and millis
        today.set(Calendar.YEAR, mYear);
        today.set(Calendar.MONTH, mMonth);
        today.set(Calendar.DAY_OF_MONTH, mDayOfMonth);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        val1 = today.getTimeInMillis();
        //today.clear();
        today1.set(Calendar.YEAR, mYear);
        today1.set(Calendar.MONTH, mMonth);
        today1.set(Calendar.DAY_OF_MONTH, mDayOfMonth);
        today1.set(Calendar.HOUR_OF_DAY, 23);
        today1.set(Calendar.MINUTE, 59);
        today1.set(Calendar.SECOND, 59);
        today1.set(Calendar.MILLISECOND, 999);
        val2 = today1.getTimeInMillis();
        Pair<Long, Long> p = new Pair<Long, Long>(val1,val2);
        return p;
    }

    public Pair<Long,Long> getRange(Calendar cal1, Calendar cal2) {
       Long val1, val2;
       val1 = cal1.getTimeInMillis();
       val2 = cal2.getTimeInMillis();
       printDateAndTime(cal1);
       printDateAndTime(cal2);
       return new Pair<Long, Long>(val1,val2);
    }

    public void printDateAndTime(Calendar cal) {
        if (DEBUG) {
            //String date = "" + cal.get(Calendar.DAY_OF_MONTH) + ":" + cal.get(Calendar.MONTH) + ":" + cal.get(Calendar.YEAR);
            String date = "" + cal.get(Calendar.DAY_OF_MONTH) + ":" + cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + ":" +cal.get(Calendar.YEAR);
            String time = "" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);

            Log.d(TAG , date + " " + time);
         }
    }

    public Date convertToDate(String input) {
        Date date = null;
        if(null == input) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy");
        try {
            format.setLenient(false);
            date = (Date) format.parse(input);
        }  catch (java.text.ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return date;
    }
    @Override
    public String toString() {
     // TODO Auto-generated method stub
      return null;
    }

}