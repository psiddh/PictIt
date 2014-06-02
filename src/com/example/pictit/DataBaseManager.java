package com.example.pictit;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class DataBaseManager extends SQLiteOpenHelper implements LogUtils {

  private static String TAG = "DataBaseManager";
  public static final String TABLE_GALLERY = "gallery";
  public static final String COLUMN_ID = "_id";
  public static final String PICTURE_ID = "pict_id";
  public static final String PICTURE_PLACE = "place";
  public static final String PICTURE_COUNTRY = "country";
  public static final String PICTURE_ADMIN = "admin";
  public static final String PICTURE_LAT = "lat";
  public static final String PICTURE_LONG = "longi";

  private static final String DATABASE_NAME = "galleryHelper.db";
  private static final int DATABASE_VERSION = 1;
  private Context mContext;
  private SQLiteDatabase mDataBase;

  private static DataBaseManager mInstance = null;
  static ConcurrentHashMap<Integer, ArrayList<String>> mMapCache = new ConcurrentHashMap<Integer, ArrayList<String>>();

  private int mId = -1;
  private int mBucketColumn = 0;
  private int mDateColumn = 0;
  private int mTitleColumn = 0;
  private int mDataColumn = 0;

  private int INDEX_PLACE = 0;
  private int INDEX_COUNTRY = 1;
  private int INDEX_ADMIN = 2;

  ConnectivityManager mConnectivityManager;

  public enum SyncState {
       SYNC_STATE_UNKNOWN,
       SYNC_STATE_INITIATED,
       SYNC_STATE_INPROGRESS,
       SYNC_STATE_COMPLETED,
       SYNC_STATE_UPDATE,
       SYNC_STATE_ABORTED,
       SYNC_STATE_INCOMPLETE,
  }

  public static SyncState state;

  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table if not exists "
      + TABLE_GALLERY + "("
      + COLUMN_ID + " integer primary key autoincrement, "
      + PICTURE_ID  + " integer,"
      + PICTURE_PLACE + " text,"
      + PICTURE_COUNTRY + " text,"
      + PICTURE_ADMIN + " text,"
      + PICTURE_LAT   + " text,"
      + PICTURE_LONG  + " text);";

    private static final String ROW_PICT_ID_EXISTS = "Select * from " + TABLE_GALLERY + " where " + PICTURE_ID + "=";

    private static final String COUNT_ROWS = "select * from " + TABLE_GALLERY;

    private static final String GET_PLACES_UNIQUE = "select DISTINCT" + PICTURE_PLACE + " from " + TABLE_GALLERY;

    /**
     * constructor should be private to prevent direct instantiation.
     * make call to static factory method "getInstance()" instead.
     */
    private DataBaseManager(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
      mContext = context;
      state = SyncState.SYNC_STATE_UNKNOWN;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
      // This first statement exists only for testing purpose
      if (TEST_DB_INITIAL_CREATION_IN_NO_INTERNET_STATE)
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_GALLERY);
      database.execSQL(DATABASE_CREATE);
      mDataBase = database;
      state = SyncState.SYNC_STATE_INITIATED;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(DataBaseManager.class.getName(),
          "Upgrading database from version " + oldVersion + " to "
              + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_GALLERY);
      onCreate(db);
    }

    private void opendb() {
       if (mDataBase != null && mDataBase.isOpen())
          return;
       if (DEBUG) Log.d(TAG,"yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy - DB Opened");
       mDataBase = this.getWritableDatabase();
    }

    private void closedb() {
       if (mDataBase != null && mDataBase.isOpen() && state != SyncState.SYNC_STATE_INPROGRESS) {
           mDataBase.close();
           if (DEBUG) Log.d(TAG,"yyyyyyyyyyyyyyyyyy - DB Closed");
       }
    }
      private void insertRow(int id, int pict_id, String place, String country, String admin, String lat, String longi) {
          ContentValues values = new ContentValues();
          values.put(PICTURE_ID,pict_id);
          values.put(PICTURE_PLACE, place);
          values.put(PICTURE_COUNTRY, country);
          values.put(PICTURE_ADMIN, admin);
          values.put(PICTURE_LAT, lat);
          values.put(PICTURE_LONG, longi);
          long val = mDataBase.insert(TABLE_GALLERY, null, values);
          if (-1 == val)
              Log.d(TAG, "Failed to Insert Row");
          else
              Log.d(TAG, "XXX Succesfully  Inserted Row : " + place + " " + country + " " + admin );
      }

      private boolean checkIfPictureExists(int pict_id, String place) {
         String data = null;
         boolean ret = false;
          Cursor cursor = mDataBase.rawQuery(ROW_PICT_ID_EXISTS + pict_id, null);
          if(!(cursor.getCount()<=0)){
             cursor.moveToFirst();
             int index = cursor.getColumnIndex(PICTURE_PLACE);
             if (index != -1)
               data = cursor.getString(index);
             ret = ((data != null) && data.equalsIgnoreCase(place));
          }
          cursor.close();
          return ret;
      }

      private ArrayList<String> getPictureInfoFromDB(int pict_id) {
           String place = "";
           String country = "";
           String admin = "";
           ArrayList<String> placeList = new ArrayList<String>();
           Cursor cursor = mDataBase.rawQuery(ROW_PICT_ID_EXISTS + pict_id, null);
           if(!(cursor.getCount()<=0)){
              //if (DEBUG) Log.d(TAG, "xxxxxxxxxxxxx Count = " + cursor.getCount());
              cursor.moveToFirst();
              int index = cursor.getColumnIndex(PICTURE_PLACE);
              if (index != -1) {
                  place = cursor.getString(index);
              }
              placeList.add(place);

              index = cursor.getColumnIndex(PICTURE_COUNTRY);
              if (index != -1) {
              country = cursor.getString(index);
              }
              placeList.add(country);

              index = cursor.getColumnIndex(PICTURE_ADMIN);
              if (index != -1) {
              admin = cursor.getString(index);
              }
              placeList.add(admin);
           }
           cursor.close();
           return placeList;
      }

      private int countRowsinDB() {
           Cursor cursor = mDataBase.rawQuery(COUNT_ROWS,null);
           int cnt = cursor.getCount();
           cursor.close();
           return cnt;
      }

      private void performSync(Cursor cur) {
      opendb();
      if (null == mConnectivityManager)
          mConnectivityManager =  (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
          do {
              state = SyncState.SYNC_STATE_INPROGRESS;
              if (cur.isClosed()) break;
              int id = cur.getInt(mId);
              String path = cur.getString(mDataColumn);

              // See if the picture exists in DB
              ArrayList<String> placeFound = getPictureInfoFromDB(id);
              if (DEBUG) Log.d(TAG,"Cache Count - " + mMapCache.size());
              if (placeFound != null && placeFound.size() > 0) {
                   // Place found in DB...fill up your pockets now... err cache
                   mMapCache.put(id, placeFound);
                   // We have found in DB and updated cache as well.. we are done with this picture
                   continue;
              }

              // Try and fetch it from Internet (GeoDecoder)
              if (DEBUG) Log.d(TAG, "Read from Database ... id : " + id + "  DB Count : " + countRowsinDB());
              // Before we try to retrieve from Internet, check to see if there is active connection
              if ((mConnectivityManager.getActiveNetworkInfo() == null) ||
                     !(mConnectivityManager.getActiveNetworkInfo().isConnectedOrConnecting())) {
                     // mConnectivityManager.getActiveNetworkInfo() being null happens in airplane mode I guess
                  Log.d(TAG,"Ooops No Connection.. Try Later");
                 continue;
              }
              List<Address> address;
              String place = null;
              String country = null;
              String adminArea = null;
              GeoDecoder geoDecoder = null;
              try {
                      geoDecoder = new GeoDecoder(new ExifInterface(path));
                      if (!geoDecoder.isValid()) {
                          // This image doesn't not have valid lat / long associated to it
                          continue;
                      }
                      address = geoDecoder.getAddress(mContext);
                      if ((address!= null && address.size() > 0) && (address.get(0) != null)) {
                    placeFound = new  ArrayList<String> ();
                        // Fetched successfully from Internet
                        place = address.get(0).getLocality();
                        if (place != null)
                        placeFound.add(place);
                        country = address.get(0).getCountryName();
                        if (country != null)
                        placeFound.add(country);
                        adminArea = address.get(0).getAdminArea();
                        if (adminArea != null)
                        placeFound.add(adminArea);
                      } else
                          continue;
                      // Update the DB with pictureID and the place..but again check if it is really
                      // present in db again. This check 'getPictureInfoFromDB' earlier should have sufficed.
                      // It doesn't hurt to absolutely check against 'pict_id' & 'place' in the db. If not
                      // found, insert the row in the database and of-course update the cache.
                      if (!checkIfPictureExists(id, place) )
                        insertRow(-1, id, place, country, adminArea, null, null);
                      mMapCache.put(id, placeFound);
              } catch (IOException e) {
                      // TODO Auto-generated catch block
                      e.printStackTrace();
              }
          } while (!cur.isClosed() && cur.moveToNext());
          state = SyncState.SYNC_STATE_COMPLETED;
          cur.close();
          closedb();
          /*if (TEST_DB_INITIAL_CREATION_AND_CACHE_UPDATE_FOR_PLACE) {
          int count =0;
          for(String value: mMapCache.values()) {
              if ((value != null) && value.equals("San Francisco")) {
                count++;
              }
            }
            Log.d(TAG,"******************** COUNT VALUES ... " + count);
          }*/
      }

      private void deletedb() {
          // This is only for testing purpose
          opendb();
          mDataBase.execSQL("DROP TABLE IF EXISTS " + TABLE_GALLERY);
          closedb();
      }

      // **********  PUBLIC FUNCTIONS **********************

      public static DataBaseManager getInstance(Context ctx) {
          // use the application context so that there is no
          // accidental leak
          if (mInstance == null) {
              mInstance = new DataBaseManager(ctx.getApplicationContext());
          }
          return mInstance;
      }

      public void startSync() {
          // which image properties are we querying
          String[] projection = new String[]{
                  MediaStore.Images.Media._ID,
                  MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                  MediaStore.Images.Media.DATE_TAKEN,
                  MediaStore.Images.Media.TITLE,
                  MediaStore.Images.Media.DATA
          };

          // Get the base URI for the People table in the Contacts content provider.
          Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

          // Make the query.
          // Make the query.
          Cursor cur = mContext.getContentResolver().query(images,
                          projection, // Which columns to return
                          "",         // Which rows to return (all rows)
                          null,       // Selection arguments (none)
                          ""          // Ordering
                          );

          Log.i(TAG," query count="+cur.getCount());

          if (cur.moveToFirst()) {
              mId = cur.getColumnIndex(
                      MediaStore.Images.Media._ID);
              mBucketColumn = cur.getColumnIndex(
                  MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

              mDateColumn = cur.getColumnIndex(
                  MediaStore.Images.Media.DATE_TAKEN);

              mTitleColumn = cur.getColumnIndex(
                      MediaStore.Images.Media.TITLE);

              mDataColumn = cur.getColumnIndex(
                      MediaStore.Images.Media.DATA);

              Log.d(TAG, cur.getPosition() + " : " + mDataColumn );
              performSync(cur);
          }
      }

      public boolean isRowFound(int id, String place) {
      ArrayList<String> placesFound = mMapCache.get(id);
      if (!isAtleastSingleValuePresentInList(placesFound))
      return false;
      return isValueFoundInList(placesFound, place);
      }

      public void updateRow(int id, String place, String country, String admin) {
      ArrayList<String> placesFound = mMapCache.get(id);
          if (null != placesFound ) {
              return;
          }
          if (isAtleastSingleValuePresentInList(placesFound))
              return;
          mMapCache.put(id, placesFound);
          opendb();
          if (!checkIfPictureExists(id, place))
              insertRow(-1, id, place, country, admin, null, null);
          closedb();
      }

      public ArrayList<String> getPlace(int id) {
      ArrayList<String> placesFound = mMapCache.get(id);
          //if (DEBUG) Log.d(TAG, "id " + id + "placeFound " + placeFound);
          if (placesFound == null || placesFound.size() == 0) {
              // remember, this is a public function
              opendb();
              placesFound = getPictureInfoFromDB(id);
              closedb();
          }
          return placesFound;
      }

      public SyncState getState() {
          return state;
      }

      public void setState(SyncState state) {
          this.state = state;
      }

      public String retreivePlaceFromStringIfExists(String userFilter) {
          String placeFound = null;
          for (ArrayList<String> place : mMapCache.values()) {
          if ((place == null) || (place.size() == 0))
          break;
              if(userFilter.toLowerCase().contains(place.get(INDEX_PLACE).toLowerCase())) {
                  placeFound = place.get(INDEX_PLACE);
                  if (DEBUG) Log.d(TAG,  " xxxx placeFound in the UserString -  " + placeFound);
                      break;
              }
          }
          return placeFound;
      }

      public ArrayList<String> retreiveAllPlacesFromStringIfExists(String userFilter) {
      ArrayList<String> placeFound = new ArrayList<String>();
      String place = "";
      String country = "";
      String admin = "";
      boolean found = false;

          for (ArrayList<String> places : mMapCache.values()) {
          if ((places == null) || (places.size() == 0))
          continue;
          placeFound.clear();
          found = false;
          if (places.size() > 0) {
              place = places.get(INDEX_PLACE);
          }
          if (places.size() > 1) {
              country = places.get(INDEX_COUNTRY);
          }
          if (places.size() > 2) {
              admin = places.get(INDEX_ADMIN);
          }
              if((place != null) && userFilter.toLowerCase().contains(place.toLowerCase())) {
                  found = true;
                  placeFound.add(place);
              }
              if((country != null) && userFilter.toLowerCase().contains(country.toLowerCase())) {
                  found = true;
                  placeFound.add(country);
              }

              if((admin != null) && userFilter.toLowerCase().contains(admin.toLowerCase())) {
                  found = true;
                  placeFound.add(admin);
              }

              if (found) {
              break;
              }
          }
          if (!found) {
              placeFound.add("");
              placeFound.add("");
              placeFound.add("");
          }
          return placeFound;
      }

      public boolean isAtleastSingleValuePresentInList (ArrayList<String> values) {
          if (values !=null && (values.size()) > 0) {
              if (values.get(INDEX_PLACE) == "" &&
                  values.get(INDEX_COUNTRY) == "" &&
                  values.get(INDEX_ADMIN) == "" ) {
                  return  false;
              }
              return true;
          }
          return false;
      }

      private boolean isValueFoundInList (ArrayList<String> values, String found) {
          if (values !=null && (values.size()) > 0) {
          if (values.get(INDEX_PLACE) == "" &&
          values.get(INDEX_COUNTRY) == "" &&
          values.get(INDEX_ADMIN) == "" ) {
          return  false;
          }
          String place = values.get(INDEX_PLACE);
          String country = values.get(INDEX_COUNTRY);
          String admin = values.get(INDEX_ADMIN);
          if (((place != null) && place.equalsIgnoreCase(found)) ||
          ((country != null) && place.equalsIgnoreCase(country)) ||
          ((admin != null) && place.equalsIgnoreCase(admin))) {
          return true;
          }
          }
          return false;
      }
}