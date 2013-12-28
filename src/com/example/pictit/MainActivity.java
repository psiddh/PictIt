package com.example.pictit;

import java.io.IOException;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class MainActivity extends Activity implements LoaderCallbacks<Cursor>{

    private static final int PROGRESS = 0x1;

    private ProgressBar mProgress;
    private ProgressDialog progDialog;
    private int mProgressStatus = 0;
    private ImageView mImgView;
    private int LIST_ID = 1001;

    private Handler mHandler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //testPath();
        createProgressBar();
        //enumeratePics();
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
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
        CursorLoader cur = new CursorLoader(this, images,
                projection, // Which columns to return
                "",         // Which rows to return (all rows)
                null,       // Selection arguments (none)
                ""          // Ordering
                );
        return cur;
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    	String imagePath = "";
    	if (data != null) {
            //int columnIndex = data.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            //data.moveToFirst();
            //imagePath = data.getString(columnIndex);
            enumeratePics(data);
        } else {
            //imagePath = imageUri.getPath();
        }

        //setupImageView();
    	
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    void createProgressBar() {
    	mProgress = (ProgressBar) findViewById(R.id.progressBar1);
    	mImgView = (ImageView) findViewById(R.id.imageView1);
        progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        //progDialog.setMax(maxBarValue);
        progDialog.setMessage("I love Shrumpsaaa:");
        progDialog.show();

        // Start lengthy operation in a background thread
        new Thread(new Runnable() {
            public void run() {
                while (mProgressStatus < 100) {
                    mProgressStatus = doWork();

                    // Update the progress bar
                    mHandler.post(new Runnable() {
                        public void run() {
                        	progDialog.setProgress(mProgressStatus);
                        }
                    });
                }
            }
        }).start();
    }
    
    void testPath(String path) {
    	ExifInterface intf = null;
    	
    	try {
    	     //= "/storage/emulated/legacy/DCIM/Camera/IMG_20131204_121206.jpg";
			intf = new ExifInterface(path );
    	} catch(IOException e) {
    	    e.printStackTrace();
    	}

    	if(intf == null) {
    	    /* File doesn't exist or isn't an image */
    	}

    	String dateString = intf.getAttribute(ExifInterface.TAG_DATETIME);
       	Log.d("PATH : ", path);
       	Log.d("dateString : ", dateString);
       	
       	if (intf.hasThumbnail()) {
       		byte[] thumbnail = intf.getThumbnail();
       		Bitmap bmpImg = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
       		mImgView.setImageBitmap(bmpImg);
       	}
    }
    
    int doWork() {
    	try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	mProgressStatus+=10;
    	return mProgressStatus;
    }
    
    void enumeratePics(Cursor cur) {
    	

        Log.d("ListingImages"," query count= "+cur.getCount());

        if (cur.moveToFirst()) {
            String bucket;
            String date;
            String title;
            String data;
            int bucketColumn = cur.getColumnIndex(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

            int dateColumn = cur.getColumnIndex(
                MediaStore.Images.Media.DATE_TAKEN);
            
            int titleColumn = cur.getColumnIndex(
                    MediaStore.Images.Media.TITLE);
            
            int dataColumn = cur.getColumnIndex(
                    MediaStore.Images.Media.DATA);
            
            Log.d("ListingImages", cur.getPosition() + " : " + dateColumn ); 
            

            do {
                // Get the field values
                bucket = cur.getString(bucketColumn);
                date = cur.getString(dateColumn);
                title = cur.getString(titleColumn);
                data = cur.getString(dataColumn);
           
                // Do something with the values.
                Log.i("ListingImages", " bucket=" + bucket 
                       + "  date_taken=" + date + "title = " + title + "data = " + data);
                if (bucket.equals("Camera"))
                  testPath(data);
            } while (cur.moveToNext());

        }
    }
    
}
