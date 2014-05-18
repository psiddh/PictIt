package com.example.pictit;

// TBD: Use well-known NLP cloud based APIs for named entity recognition
// For now lets use this dumb version

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

public class UserFilterAnalyzer implements LogUtils{

    private String TAG = "pickit/UserFilterAnalyzer";

    private int   SINGLE_DAY_OFFSET_IN_MS = 86400000;

    private String mUserFilter = null;

    private String[] mWords ;

    private Context mContext;

    DataBaseManager mDbHelper;

    DateRangeManager mRangeMgr = new DateRangeManager();

    Map<String, Integer[]> mDateRangeKeyWord = new HashMap<String, Integer[]>();

    private static final int KEYWORD_MONTH_NAME = 1;
    private static final int KEYWORD_WEEKEND = 2;
    private static final int KEYWORD_MONTH = 3;
    private static final int KEYWORD_MONTH_DAYS = 4;
    private static final int KEYWORD_YEAR = 5;
    private static final int KEYWORD_TODAY = 6;
    private static final int KEYWORD_SPECIAL = 7;
    private static final int KEYWORD_PHRASES_OTHER = 8;
    private static final int KEYWORD_UNKNOWN = 0xFF;

    private int mDateStartIndex = -1;
    private int mDateEndIndex = -1;

    private int mPhraseIndex = -1;
    private int mPlaceIndex = -1;

    private int mMatchState = -1;
    private boolean mMatchStateUpdateNeededForPlace = true;
    private boolean mIsDateAPharse = false;

    private int    MATCH_SUCCESS  = 0;
    private int    MATCH_FAILURE  = 1;

    public static final int MATCH_STATE_NONE = 1000;
    public static final int MATCH_STATE_ONLY_DATES = 1001;
    public static final int MATCH_STATE_ONLY_PLACE = 1002;
    public static final int MATCH_STATE_DATES_AND_PLACE = 1003;
    public static final int MATCH_STATE_DATES_AND_PLACE_EXACT = 1004;


    private void initKeyWords() {
        // Month key words
        mDateRangeKeyWord.put("january", new Integer[] {KEYWORD_MONTH_NAME, 0});
        mDateRangeKeyWord.put("jan", new Integer[] {KEYWORD_MONTH_NAME, 0});
        mDateRangeKeyWord.put("february", new Integer[] {KEYWORD_MONTH_NAME, 1});
        mDateRangeKeyWord.put("feb", new Integer[] {KEYWORD_MONTH_NAME, 1});
        mDateRangeKeyWord.put("march", new Integer[] {KEYWORD_MONTH_NAME, 2});
        mDateRangeKeyWord.put("april", new Integer[] {KEYWORD_MONTH_NAME, 3});
        mDateRangeKeyWord.put("may", new Integer[] {KEYWORD_MONTH_NAME, 4});
        mDateRangeKeyWord.put("june", new Integer[] {KEYWORD_MONTH_NAME, 5});
        mDateRangeKeyWord.put("july", new Integer[] {KEYWORD_MONTH_NAME, 6});
        mDateRangeKeyWord.put("august", new Integer[] {KEYWORD_MONTH_NAME, 7});
        mDateRangeKeyWord.put("september", new Integer[] {KEYWORD_MONTH_NAME, 8});
        mDateRangeKeyWord.put("october", new Integer[] {KEYWORD_MONTH_NAME, 9});
        mDateRangeKeyWord.put("november", new Integer[] {KEYWORD_MONTH_NAME, 10});
        mDateRangeKeyWord.put("december", new Integer[] {KEYWORD_MONTH_NAME, 11});

        // Date related phrases
        mDateRangeKeyWord.put("last weekend", new Integer[] {KEYWORD_WEEKEND, -1});
        mDateRangeKeyWord.put("this weekend", new Integer[] {KEYWORD_WEEKEND, -1});
        mDateRangeKeyWord.put("around weekend", new Integer[] {KEYWORD_WEEKEND, -1});
        mDateRangeKeyWord.put("last two weekends", new Integer[] {KEYWORD_WEEKEND, -1});
        mDateRangeKeyWord.put("weekend", new Integer[] {KEYWORD_WEEKEND, -1});

        // Month related phrases
        mDateRangeKeyWord.put("last month", new Integer[] {KEYWORD_MONTH, -1});
        mDateRangeKeyWord.put("this month", new Integer[] {KEYWORD_MONTH, -1});
        mDateRangeKeyWord.put("current month", new Integer[] {KEYWORD_MONTH, -1});

        // Days of Month related keywords
        mDateRangeKeyWord.put("st", new Integer[] {KEYWORD_MONTH_DAYS, -1}); // treat this as day for now!
        mDateRangeKeyWord.put("1", new Integer[] {KEYWORD_MONTH_DAYS, 1});
        mDateRangeKeyWord.put("1 st", new Integer[] {KEYWORD_MONTH_DAYS, 1});
        mDateRangeKeyWord.put("1st", new Integer[] {KEYWORD_MONTH_DAYS, 1});
        mDateRangeKeyWord.put("2", new Integer[] {KEYWORD_MONTH_DAYS, 2});
        mDateRangeKeyWord.put("2nd", new Integer[] {KEYWORD_MONTH_DAYS, 2});
        mDateRangeKeyWord.put("3", new Integer[] {KEYWORD_MONTH_DAYS, 3});
        mDateRangeKeyWord.put("3rd", new Integer[] {KEYWORD_MONTH_DAYS, 3});
        mDateRangeKeyWord.put("4", new Integer[] {KEYWORD_MONTH_DAYS, 4});
        mDateRangeKeyWord.put("4th", new Integer[] {KEYWORD_MONTH_DAYS, 4});
        mDateRangeKeyWord.put("5", new Integer[] {KEYWORD_MONTH_DAYS, 5});
        mDateRangeKeyWord.put("5th", new Integer[] {KEYWORD_MONTH_DAYS, 5});
        mDateRangeKeyWord.put("6", new Integer[] {KEYWORD_MONTH_DAYS, 6});
        mDateRangeKeyWord.put("6th", new Integer[] {KEYWORD_MONTH_DAYS, 6});
        mDateRangeKeyWord.put("7", new Integer[] {KEYWORD_MONTH_DAYS, 7});
        mDateRangeKeyWord.put("7th", new Integer[] {KEYWORD_MONTH_DAYS, 7});
        mDateRangeKeyWord.put("8", new Integer[] {KEYWORD_MONTH_DAYS, 8});
        mDateRangeKeyWord.put("8th", new Integer[] {KEYWORD_MONTH_DAYS, 8});
        mDateRangeKeyWord.put("9", new Integer[] {KEYWORD_MONTH_DAYS, 9});
        mDateRangeKeyWord.put("9th", new Integer[] {KEYWORD_MONTH_DAYS, 9});
        mDateRangeKeyWord.put("10", new Integer[] {KEYWORD_MONTH_DAYS, 10});
        mDateRangeKeyWord.put("10th", new Integer[] {KEYWORD_MONTH_DAYS, 10});
        mDateRangeKeyWord.put("11", new Integer[] {KEYWORD_MONTH_DAYS, 11});
        mDateRangeKeyWord.put("11th", new Integer[] {KEYWORD_MONTH_DAYS, 11});
        mDateRangeKeyWord.put("12", new Integer[] {KEYWORD_MONTH_DAYS, 12});
        mDateRangeKeyWord.put("12th", new Integer[] {KEYWORD_MONTH_DAYS, 12});
        mDateRangeKeyWord.put("13", new Integer[] {KEYWORD_MONTH_DAYS, 13});
        mDateRangeKeyWord.put("13th", new Integer[] {KEYWORD_MONTH_DAYS, 13});
        mDateRangeKeyWord.put("14", new Integer[] {KEYWORD_MONTH_DAYS, 14});
        mDateRangeKeyWord.put("14th", new Integer[] {KEYWORD_MONTH_DAYS, 14});
        mDateRangeKeyWord.put("15", new Integer[] {KEYWORD_MONTH_DAYS, 15});
        mDateRangeKeyWord.put("15th", new Integer[] {KEYWORD_MONTH_DAYS, 15});
        mDateRangeKeyWord.put("16", new Integer[] {KEYWORD_MONTH_DAYS, 16});
        mDateRangeKeyWord.put("16th", new Integer[] {KEYWORD_MONTH_DAYS, 16});
        mDateRangeKeyWord.put("17", new Integer[] {KEYWORD_MONTH_DAYS, 17});
        mDateRangeKeyWord.put("17th", new Integer[] {KEYWORD_MONTH_DAYS, 17});
        mDateRangeKeyWord.put("18", new Integer[] {KEYWORD_MONTH_DAYS, 18});
        mDateRangeKeyWord.put("18th", new Integer[] {KEYWORD_MONTH_DAYS, 18});
        mDateRangeKeyWord.put("19", new Integer[] {KEYWORD_MONTH_DAYS, 19});
        mDateRangeKeyWord.put("19th", new Integer[] {KEYWORD_MONTH_DAYS, 19});
        mDateRangeKeyWord.put("20", new Integer[] {KEYWORD_MONTH_DAYS, 20});
        mDateRangeKeyWord.put("20th", new Integer[] {KEYWORD_MONTH_DAYS, 20});
        mDateRangeKeyWord.put("21", new Integer[] {KEYWORD_MONTH_DAYS, 21});
        mDateRangeKeyWord.put("21 st", new Integer[] {KEYWORD_MONTH_DAYS, 21});
        mDateRangeKeyWord.put("22", new Integer[] {KEYWORD_MONTH_DAYS, 22});
        mDateRangeKeyWord.put("22nd", new Integer[] {KEYWORD_MONTH_DAYS, 22});
        mDateRangeKeyWord.put("23", new Integer[] {KEYWORD_MONTH_DAYS, 23});
        mDateRangeKeyWord.put("23rd", new Integer[] {KEYWORD_MONTH_DAYS, 23});
        mDateRangeKeyWord.put("24", new Integer[] {KEYWORD_MONTH_DAYS, 24});
        mDateRangeKeyWord.put("24th", new Integer[] {KEYWORD_MONTH_DAYS, 24});
        mDateRangeKeyWord.put("25", new Integer[] {KEYWORD_MONTH_DAYS, 25});
        mDateRangeKeyWord.put("25th", new Integer[] {KEYWORD_MONTH_DAYS, 25});
        mDateRangeKeyWord.put("26", new Integer[] {KEYWORD_MONTH_DAYS, 26});
        mDateRangeKeyWord.put("26th", new Integer[] {KEYWORD_MONTH_DAYS, 26});
        mDateRangeKeyWord.put("27", new Integer[] {KEYWORD_MONTH_DAYS, 27});
        mDateRangeKeyWord.put("27th", new Integer[] {KEYWORD_MONTH_DAYS, 27});
        mDateRangeKeyWord.put("28", new Integer[] {KEYWORD_MONTH_DAYS, 28});
        mDateRangeKeyWord.put("28th", new Integer[] {KEYWORD_MONTH_DAYS, 28});
        mDateRangeKeyWord.put("29", new Integer[] {KEYWORD_MONTH_DAYS, 29});
        mDateRangeKeyWord.put("29th", new Integer[] {KEYWORD_MONTH_DAYS, 29});
        mDateRangeKeyWord.put("30", new Integer[] {KEYWORD_MONTH_DAYS, 30});
        mDateRangeKeyWord.put("30th", new Integer[] {KEYWORD_MONTH_DAYS, 30});
        mDateRangeKeyWord.put("31", new Integer[] {KEYWORD_MONTH_DAYS, 31});
        mDateRangeKeyWord.put("31 st", new Integer[] {KEYWORD_MONTH_DAYS, 31});

        // Year keywords
        mDateRangeKeyWord.put("2000",new Integer[] {KEYWORD_YEAR, 2000});
        mDateRangeKeyWord.put("2001",new Integer[] {KEYWORD_YEAR, 2001});
        mDateRangeKeyWord.put("2002",new Integer[] {KEYWORD_YEAR, 2002});
        mDateRangeKeyWord.put("2003",new Integer[] {KEYWORD_YEAR, 2003});
        mDateRangeKeyWord.put("2004",new Integer[] {KEYWORD_YEAR, 2004});
        mDateRangeKeyWord.put("2005",new Integer[] {KEYWORD_YEAR, 2005});
        mDateRangeKeyWord.put("2006",new Integer[] {KEYWORD_YEAR, 2006});
        mDateRangeKeyWord.put("2007",new Integer[] {KEYWORD_YEAR, 2007});
        mDateRangeKeyWord.put("2008",new Integer[] {KEYWORD_YEAR, 2008});
        mDateRangeKeyWord.put("2009",new Integer[] {KEYWORD_YEAR, 2009});
        mDateRangeKeyWord.put("2010",new Integer[] {KEYWORD_YEAR, 2010});
        mDateRangeKeyWord.put("2011",new Integer[] {KEYWORD_YEAR, 2011});
        mDateRangeKeyWord.put("2012",new Integer[] {KEYWORD_YEAR, 2012});
        mDateRangeKeyWord.put("2013",new Integer[] {KEYWORD_YEAR, 2013});
        mDateRangeKeyWord.put("2014",new Integer[] {KEYWORD_YEAR, 2014});
        mDateRangeKeyWord.put("2015",new Integer[] {KEYWORD_YEAR, 2015});
        mDateRangeKeyWord.put("2016",new Integer[] {KEYWORD_YEAR, 2016});
        mDateRangeKeyWord.put("2017",new Integer[] {KEYWORD_YEAR, 2017});
        mDateRangeKeyWord.put("2018",new Integer[] {KEYWORD_YEAR, 2018});
        mDateRangeKeyWord.put("2019",new Integer[] {KEYWORD_YEAR, 2009});
        mDateRangeKeyWord.put("2020",new Integer[] {KEYWORD_YEAR, 2020});
        mDateRangeKeyWord.put("Year",new Integer[] {KEYWORD_YEAR, -1});

        // Today keywords
        mDateRangeKeyWord.put("today",new Integer[] {KEYWORD_TODAY, -1});
        mDateRangeKeyWord.put("today's",new Integer[] {KEYWORD_TODAY, -1});

        // Special words
        mDateRangeKeyWord.put("till",new Integer[] {KEYWORD_SPECIAL, -1});
        mDateRangeKeyWord.put("since",new Integer[] {KEYWORD_SPECIAL, -1});
        mDateRangeKeyWord.put("to",new Integer[] {KEYWORD_SPECIAL, -1});
        mDateRangeKeyWord.put("and",new Integer[] {KEYWORD_SPECIAL, -1});
        mDateRangeKeyWord.put("st",new Integer[] {KEYWORD_SPECIAL, -1});

        // Other phrases
        mDateRangeKeyWord.put("couple of weeks",new Integer[] {KEYWORD_PHRASES_OTHER, -1});
        mDateRangeKeyWord.put("beginning of the world",new Integer[] {KEYWORD_PHRASES_OTHER, -1});
    }

    public UserFilterAnalyzer(Context context, String filter) {
    mUserFilter = filter;

        mWords = mUserFilter.split("\\s+");
        if (DEBUG) {
            for (int i = 0; i < mWords.length; i++) {
              Log.d(TAG, "Word "+ i+1 + " :" + mWords[i]);
            }
        }
        initKeyWords();

    mContext = context;
    mDbHelper = DataBaseManager.getInstance(mContext);
    }

    public boolean isPrepositionKeywordFoundBeforeFilter(String compareString, boolean place) {
    int index = -1;
    int retry = 0;
    int currentIndex = 0;
    boolean foundPreposition = false;

    if (compareString == null) return false;
    String concat = "";
    for (int i = 0; i < mWords.length; i++) {
      concat += mWords[i] + " ";
      for (int j = i+1; j < mWords.length; j++) {
        concat += mWords[j] + " ";
        if(concat.toLowerCase().contains(compareString.toLowerCase())) {
          index = j;
          break;
        }
        if (j+1 == mWords.length) {
            concat = "";
        }
      }
      if(index != -1) {
          break;
      }
    }

    if (-1 == index) return foundPreposition;
    currentIndex = index;

    if(place)
      mPlaceIndex = index;

    // Now we have index
    do {
        if ((currentIndex - 1) >= 0) {
            if (isFillerWord(mWords[currentIndex - 1])) {
                currentIndex--;
                retry++;
                continue;
            }
            if (isWordAPreposition(mWords[currentIndex - 1])) {
                foundPreposition = true;
                break;
            }
            currentIndex--;
            retry++;
            continue;
        }
    } while (currentIndex > 0 && retry < 6 && currentIndex < mWords.length);  // value 3 for fault tolerance.. yeah I know

        if (DEBUG && (currentIndex - 1) >= 0 )
            Log.d(TAG, "Found preposition at index " + (currentIndex - 1) + " Word : "  + mWords[currentIndex - 1]);
        return foundPreposition;
    }

    private boolean isFillerWord(String word){
    String fillers[] = {"A", "AN", "THE"};
    for (String s : fillers) {
        if (s.equalsIgnoreCase(word))
        return true;
         }
    return false;
    }

    private boolean isWordAPreposition(String word){
    String fillers[] = {"At", "IN", "FROM", "AROUND", "NEAR", "NEAR TO"};
    for (String s : fillers) {
        if (s.equalsIgnoreCase(word))
        return true;
         }
    return false;
    }

    public int compareUserFilterForCity(String compareString) {
        //if (DEBUG) Log.d(TAG, "compareUserFilterForCity String : " + compareString);
        String concat = "";
        for (int i = 0; i < mWords.length; i++) {
          concat += mWords[i] + " ";
          //if (DEBUG) Log.d(TAG, "First Index : " + i + " - " + concat);
            for (int j = i+1; j < mWords.length; j++) {
              concat += mWords[j] + " ";
              //if (DEBUG) Log.d(TAG, "Second Index : " + j + " - " + concat);
              if(concat.toLowerCase().contains(compareString.toLowerCase())) {
              //if (DEBUG) Log.d(TAG, "***** Place Matched - " + compareString);
                  return MATCH_SUCCESS;
              }
              if (j+1 == mWords.length) {
                 // if (DEBUG) Log.d(TAG, "**** Reset ****");
                  concat = "";
              }
            }
          if(concat.toLowerCase().contains(compareString.toLowerCase())) {
        //if (DEBUG) Log.d(TAG, "***** Place Matched - " + compareString);
            return MATCH_SUCCESS;
          }
        }
        return MATCH_FAILURE;
    }

    public Pair<Long,Long> getRangeForSingleDateIfValid(Calendar range1, Calendar range2) {
        long offset = 1;
        if ((range1.isSet(Calendar.MONTH)) && (range1.isSet(Calendar.DAY_OF_MONTH))) {
          range1.set(Calendar.YEAR,mRangeMgr.getCurrentYear());
        } else if ((range1.isSet(Calendar.MONTH)) && (range1.isSet(Calendar.YEAR))) {
           range1.set(Calendar.DAY_OF_MONTH,1);
           offset = range1.getActualMaximum(Calendar.DAY_OF_MONTH);
        } else if ((range1.isSet(Calendar.YEAR)) && (range1.isSet(Calendar.DAY_OF_MONTH))) {
           range1.set(Calendar.MONTH,range1.get(mRangeMgr.getCurrentMonth()));
        } else {
            // ONLY month is set. Ex:- May
            if (range1.isSet(Calendar.MONTH) && !range1.isSet(Calendar.DAY_OF_MONTH) && !range1.isSet(Calendar.YEAR)) {
                range1.set(Calendar.YEAR,mRangeMgr.getCurrentYear());
                range1.set(Calendar.DAY_OF_MONTH,1);
                offset = range1.getActualMaximum(Calendar.DAY_OF_MONTH);
            } else if (!range1.isSet(Calendar.MONTH) && !range1.isSet(Calendar.DAY_OF_MONTH) && range1.isSet(Calendar.YEAR))  {
                // Only YEAR is set Ex:- 2013
                int year = mRangeMgr.getCurrentYear();
                boolean isLeapYear = ((year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0)));
                offset = isLeapYear ? 366 : 365;
                range1.set(Calendar.MONTH,Calendar.JANUARY);
                range1.set(Calendar.DAY_OF_MONTH,1);

            }  else if (!range1.isSet(Calendar.MONTH) && range1.isSet(Calendar.DAY_OF_MONTH) && !range1.isSet(Calendar.YEAR))  {
                // Only DAY is set Ex:- 1st -- does not make sense.. but for completeness, lets assume something
                   range1.set(Calendar.YEAR,mRangeMgr.getCurrentYear());
                   range1.set(Calendar.MONTH,mRangeMgr.getCurrentMonth());
               } else {
                return null;
            }
        }
        // Looks like a valid single date at this point
        long val1 = range1.getTimeInMillis();
        long val2 = val1 + SINGLE_DAY_OFFSET_IN_MS * (offset - 1);
        Pair<Long, Long> p = new Pair<Long, Long>(val1,val2);
        return p;
    }

    public Pair<Long,Long> getDateRange(String compareString) {
        //if (DEBUG) Log.d(TAG, "getDateRange String : " + compareString);
        int index = 0;
        int[] validRange = {0,0,0};
        int unknownCnt = 0;
        Calendar range1 = getNewCalObj(true);
        Calendar range2 = getNewCalObj(false);
        for (index = 0; index < mWords.length; index++) {
            if(!mDateRangeKeyWord.containsKey(mWords[index].toLowerCase())) {
                continue;
            }
            Integer[] keyword_Val = mDateRangeKeyWord.get(mWords[index].toLowerCase());
            if (keyword_Val == null) {
              return null;
            }
            switch (keyword_Val[0]) {
              case KEYWORD_YEAR :
                  if (mDateStartIndex == -1)
                      mDateStartIndex = index;
                  mDateEndIndex = index;
                  if (!range1.isSet(Calendar.YEAR) && !isCalendarObjSet(range2) && (!(validRange[1] == 1 || validRange[2] == 1)))
                    range1.set(Calendar.YEAR,keyword_Val[1]);
                  else
                    range2.set(Calendar.YEAR,keyword_Val[1]);
                  validRange[0]++;
                  break;
              case KEYWORD_MONTH_NAME :
                  if (mDateStartIndex == -1)
                      mDateStartIndex = index;
                  mDateEndIndex = index;
                  if (!range1.isSet(Calendar.MONTH) && (!(validRange[0] == 1 || validRange[2] == 1)))
                      range1.set(Calendar.MONTH,keyword_Val[1]);
                  else
                    range2.set(Calendar.MONTH,keyword_Val[1]);
                  validRange[1]++;
                  break;
              case KEYWORD_MONTH_DAYS :
                  if (mDateStartIndex == -1)
                      mDateStartIndex = index;
                  mDateEndIndex = index;
                  if (!range1.isSet(Calendar.DAY_OF_MONTH) && (!(validRange[0] == 1 || validRange[1] == 1)))
                      range1.set(Calendar.DAY_OF_MONTH,keyword_Val[1]);
                  else
                    range2.set(Calendar.DAY_OF_MONTH,keyword_Val[1]);
                  validRange[2]++;
                  break;
              case KEYWORD_WEEKEND :
              case KEYWORD_MONTH :
              case KEYWORD_TODAY :
              case KEYWORD_SPECIAL:
              case KEYWORD_PHRASES_OTHER :
                  //if (DEBUG) Log.d(TAG, "Word : " + mWords[index] + " " +  getStringFromKeyWord(keyword_Val));
                  break;
              default:
                  if (DEBUG && (mWords[index] != null)) Log.d(TAG, mWords[index] + " - Unknown Cnt : " + unknownCnt);
                  // Uh! dumb, lets stick to this fault tolerance for now.
                  if(++unknownCnt > 3) return null;
                  break;
            }
        }

        if (!(validRange[0] > 1 || validRange[1] > 1 || validRange[2] > 1)) {
          Pair<Long, Long> p = getRangeForSingleDateIfValid(range1, range2);
          return p;
        }

        if (range1.isSet(Calendar.YEAR) && !range2.isSet(Calendar.YEAR)) {
            int thisYear = range1.get(Calendar.YEAR);
            range2.set(Calendar.YEAR,thisYear);
        }

        if (range2.isSet(Calendar.YEAR) && !range1.isSet(Calendar.YEAR)) {
            int thisYear = range2.get(Calendar.YEAR);
            range1.set(Calendar.YEAR,thisYear);
            if (range1.getTimeInMillis() > range2.getTimeInMillis()) {
                range1.add(Calendar.YEAR, -1);
            }
        }

        if (!range1.isSet(Calendar.YEAR) && !range2.isSet(Calendar.YEAR)) {
            range1.set(Calendar.YEAR,mRangeMgr.getCurrentYear());
            range2.set(Calendar.YEAR,mRangeMgr.getCurrentYear());
        }

        if (!range1.isSet(Calendar.MONTH))
            range1.set(Calendar.MONTH,0);
        if (!range2.isSet(Calendar.MONTH)) {
            // Now this can be an interesting usecase, Say "February 15th and 16th"
            if (range2.isSet(Calendar.DAY_OF_MONTH) && range1.isSet(Calendar.DAY_OF_MONTH)) {
                int thisMonth = range1.get(Calendar.MONTH);
                range2.set(Calendar.MONTH,thisMonth);
            } else {
                range2.set(Calendar.MONTH,0);
            }
        }

        if (!range1.isSet(Calendar.DAY_OF_MONTH))
            range1.set(Calendar.DAY_OF_MONTH,1);
        if (!range2.isSet(Calendar.DAY_OF_MONTH))
            range2.set(Calendar.DAY_OF_MONTH,1);

        if (0 == range1.compareTo(range2)) {
            //return null;
        }

        if (range1.getTimeInMillis() > range2.getTimeInMillis()) {
          return mRangeMgr.getRange(range2, range1);
        }

        Pair<Long, Long> p = mRangeMgr.getRange(range1, range2);
        return p;
    }

    private boolean isCalendarObjSet(Calendar cal) {
        return (cal.isSet(Calendar.YEAR) || cal.isSet(Calendar.MONTH) || cal.isSet(Calendar.DAY_OF_MONTH));
    }

    private String getStringFromKeyWord(Integer[] keyword_Val) {
        switch(keyword_Val[0]) {
            case KEYWORD_MONTH_NAME:
                return "KEYWORD_MONTH_NAME";
            case KEYWORD_WEEKEND:
                return "KEYWORD_WEEKEND";
            case KEYWORD_MONTH:
                return "KEYWORD_MONTH";
            case KEYWORD_MONTH_DAYS:
            return "KEYWORD_MONTH_DAYS";
            case KEYWORD_YEAR:
                return "KEYWORD_YEAR";
            case KEYWORD_TODAY:
                return "KEYWORD_TODAY";
            case KEYWORD_PHRASES_OTHER:
                return "KEYWORD_PHRASES_OTHER";
            case KEYWORD_SPECIAL:
                return "KEYWORD_SPECIAL";
            case KEYWORD_UNKNOWN:
                return "KEYWORD_UNKNOWN";
            default:
                return "";
        }

    }

    /*private int computeDateRanges(Map<String, Integer> pattern) {
        Calendar range1 = getNewCalObj();
        Calendar range2 = getNewCalObj();

        int count = Collections.frequency(new ArrayList<Integer>(pattern.values()), KEYWORD_YEAR);
        if (count >= 2) {

        }

        for (Entry<String, Integer> entry : pattern.entrySet()) {
              String key = entry.getKey();
              Integer value = entry.getValue();
              switch (value) {
                case KEYWORD_YEAR:
                    break;
                default:
                    break;
              }
        }
        //col.
        return 0;
    }*/

    private Calendar getNewCalObj(boolean isFirstValue) {
        Calendar cal = Calendar.getInstance((Locale.getDefault()));
        // reset hour, minutes, seconds and millis
        cal.clear();
        if (isFirstValue) {
          cal.set(Calendar.HOUR_OF_DAY, 0);
          cal.set(Calendar.MINUTE, 0);
          cal.set(Calendar.SECOND, 0);
          cal.set(Calendar.MILLISECOND, 0);
        } else {
          cal.set(Calendar.HOUR_OF_DAY, 23);
          cal.set(Calendar.MINUTE, 59);
          cal.set(Calendar.SECOND, 59);
          cal.set(Calendar.MILLISECOND, 999);
        }
        return cal;
    }
    @Override
    public String toString() {
      // TODO Auto-generated method stub
      return null;
    }

    public String getStartDate() {
        if ((mDateStartIndex == -1) || (mDateStartIndex >= mWords.length))
            return null;
        return mWords[mDateStartIndex];
    }

    public int getMatchState() {
        int mMatchState = MATCH_STATE_NONE;
        Pair<Long, Long> mPairRange = getDateRange(mUserFilter);
        String place = mDbHelper.retreivePlaceFromStringIfExists(mUserFilter);
        boolean alsoMatchCity = false;
        boolean alsoMatchDate = false;
        if (place != null) {
            alsoMatchCity = isPrepositionKeywordFoundBeforeFilter(place, true);
        }
        alsoMatchDate = isPrepositionKeywordFoundBeforeFilter(getStartDate(), false);

        if ((mPairRange != null) && (place != null)) {
        if (alsoMatchCity || alsoMatchDate) {
             mMatchState = MATCH_STATE_DATES_AND_PLACE_EXACT;
        } else {
            mMatchState = MATCH_STATE_DATES_AND_PLACE;
        }
        } else if (mPairRange != null) {
            // place is null here
            mMatchState = MATCH_STATE_ONLY_DATES;
        } else if (place != null) {
            mMatchState = MATCH_STATE_ONLY_PLACE;
        } else {
            // maintain status-quo
        }

        return mMatchState;
    }
}