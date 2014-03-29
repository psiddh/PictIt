package com.example.pictit;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class DataBaseManager extends SQLiteOpenHelper implements LogUtils {

  private static String TAG = "DataBaseManager";
  public static final String TABLE_GALLERY = "gallery";
  public static final String COLUMN_ID = "_id";
  public static final String PICTURE_ID = "pict_id";
  public static final String PICTURE_PLACE = "place";
  public static final String PICTURE_LAT = "lat";
  public static final String PICTURE_LONG = "longi";

  private static final String DATABASE_NAME = "galleryHelper.db";
  private static final int DATABASE_VERSION = 1;
  private Context mContext;
  private Cursor mCursor;

  SQLiteDatabase mDataBase;

  static Map<Integer, String> mMapCache = new HashMap<Integer, String>();

  private int mId = -1;
  private int mBucketColumn = 0;
  private int mDateColumn = 0;
  private int mTitleColumn = 0;
  private int mDataColumn = 0;

  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table if not exists "
      + TABLE_GALLERY + "("
      + COLUMN_ID + " integer primary key autoincrement, "
      + PICTURE_ID  + " integer,"
      + PICTURE_PLACE + " text,"
      + PICTURE_LAT   + " text,"
      + PICTURE_LONG  + " text);";

  private static final String ROW_PICT_ID_EXISTS = "Select * from " + TABLE_GALLERY + " where " + PICTURE_ID + "=";

  public DataBaseManager(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    mContext = context;
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    // This first statement exists only for testing purpose
    //database.execSQL("DROP TABLE IF EXISTS " + TABLE_GALLERY);
    database.execSQL(DATABASE_CREATE);
    mDataBase = database;

  }


@Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(DataBaseManager.class.getName(),
        "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_GALLERY);
    onCreate(db);
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

    public void performSync(Cursor cur) {
        do {
           opendb();
            if (cur.isClosed()) break;
            int id = cur.getInt(mId);
            String path = cur.getString(mDataColumn);

            String placeFound = getPictureFromDB(id);
            if (placeFound != null) {
               // Place found in DB...
               // fill up your pockets now... err cache
               //if (mMapCache.get(id) != null)
                 mMapCache.put(id, placeFound);
               continue;
            }

            List<Address> address;
            String place = null;
            GeoDecoder geoDecoder = null;
            try {
                    geoDecoder = new GeoDecoder(new ExifInterface(path));
                    if (!geoDecoder.isValid()) {
                        // This image doesn't not have valid lat / long associated to it
                        continue;
                    }

                    address = geoDecoder.getAddress(mContext);
                    if ((address!= null && address.size() > 0) && (address.get(0) != null))
                      place = address.get(0).getLocality();
                    if (!checkIfPictureExists(id, place) )
                      insertRow(-1, id, place, null, null);
                    //if (mMapCache.get(id) != null)
                       mMapCache.put(id, place);
                    //address.get(0).getLocality();
            } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
        }while (!cur.isClosed() && cur.moveToNext());
        closedb();
    }

    private void opendb() {
       if (mDataBase.isOpen())
          return;
       mDataBase = this.getWritableDatabase();
    }

    private void closedb() {
       if (mDataBase.isOpen())
           mDataBase.close();
    }
      private void insertRow(int id, int pict_id, String place, String lat, String longi) {
          ContentValues values = new ContentValues();
          values.put(PICTURE_ID,pict_id);
          values.put(PICTURE_PLACE, place);
          values.put(PICTURE_LAT, lat);
          values.put(PICTURE_LONG, longi);
          long val = mDataBase.insert(TABLE_GALLERY, null, values);
          if (-1 == val)
              Log.d(TAG, "Failed to Insert Row");
          else
              Log.d(TAG, "XXX Succesfully  Inserted Row : " + place);
      }

      private boolean checkIfPictureExists(int pict_id, String place) {
         String data = null;
          Cursor cursor = mDataBase.rawQuery(ROW_PICT_ID_EXISTS + pict_id, null);
          if(!(cursor.getCount()<=0)){
             cursor.moveToFirst();
             int index = cursor.getColumnIndex(PICTURE_PLACE);
             if (index != -1)
               data = cursor.getString(index);
             return ((data != null) && data.equalsIgnoreCase(place));
          }
          return false;
      }

      private String getPictureFromDB(int pict_id) {
          String data = null;
           Cursor cursor = mDataBase.rawQuery(ROW_PICT_ID_EXISTS + pict_id, null);
           if(!(cursor.getCount()<=0)){
              cursor.moveToFirst();
              int index = cursor.getColumnIndex(PICTURE_PLACE);
              if (index != -1)
                data = cursor.getString(index);
           }
           return data;
       }

      public static boolean isRowFound(int id, String place) {
          String placeFound = mMapCache.get(id);
          Log.d(TAG, "id " + id + "Place " + place + "placeFound " + placeFound);
          return placeFound.equalsIgnoreCase(place);
      }

      public static String getPlace(int id) {
          String placeFound = mMapCache.get(id);
          Log.d(TAG, "id " + id + "placeFound " + placeFound);
          return placeFound;
      }


}