package com.example.pictit;

import java.io.IOException;
import java.util.ArrayList;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.SearchManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
//import android.widget.ShareActionProvider;
import android.widget.Toast;

public class MainActivity extends Activity implements LoaderCallbacks<Cursor>{

    private static final int PROGRESS = 0x1;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
    private ProgressBar mProgress;
    private ProgressDialog progDialog;
    private int mProgressStatus = 0;
    private ImageView mImgView;
    private LinearLayout mLinearLayout;
    private int LIST_ID = 1001;

    //private ShareActionProvider mShareActionProvider;

    private Handler mHandler = new Handler();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        // Locate MenuItem with ShareActionProvider
        /*MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        setShareIntent(createShareIntent());
        // Return true to display menu  */
        return true;
    }

    // Call to update the share intent
    /*private void setShareIntent(Intent shareIntent) {
         if (mShareActionProvider != null) {
              mShareActionProvider.setShareIntent(shareIntent);
         }
    }
    private Intent createShareIntent() {
         Intent shareIntent = new Intent(Intent.ACTION_SEND);
         shareIntent.setType("text/plain");
         shareIntent.putExtra(Intent.EXTRA_TEXT,
                   "http://androidtrainningcenter.blogspot.in");
         return shareIntent;
    }  */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //testPath();
    	//mLinearLayout = (LinearLayout) findViewById(R.id.linear1);
        //createProgressBar();
        //enumeratePics();
        final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	speak();
            	//showGridView("January");
            }
        });
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
            //enumeratePics(data);
        } else {
            //imagePath = imageUri.getPath();
        }

        //setupImageView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    void showGridView(String filter) {
    	Intent intent = new Intent(getBaseContext(), DisplayViewsExample.class);
    	intent.putExtra("filter", filter);
        startActivity(intent);
    }
    void createProgressBar() {
    	/*mProgress = (ProgressBar) findViewById(R.id.progressBar1);
    	//mImgView = (ImageView) findViewById(R.id.imageView1);
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
        }).start();*/
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
       		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
       		Bitmap bmpImg = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
       		BitmapDrawable bmd = new BitmapDrawable(getResources(),bmpImg);
       	    ImageView imageView = new ImageView(this);
            imageView.setPadding(2, 0, 5, 5);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            //imageView.setLayoutParams(new Gallery.LayoutParams(150, 120));
            imageView.setLayoutParams(layoutParams);
            imageView.setImageDrawable(bmd);
       		//mImgView.setPadding(2, 0, 9, 5);
       		//mImgView.setImageDrawable(bmd);
       		//mImgView.setImageBitmap(bmpImg);

          //mLinearLayout.addView(imageView);
       	}
    }

    public void speak() {
		  Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

		  // Specify the calling package to identify your application
		  intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
		    .getPackage().getName());

		  // Display an hint to the user about what he should say.
		  //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, metTextHint.getText()
		//.toString());

		  // Given an hint to the recognizer about what the user is going to say
		  //There are two form of language model available
		  //1.LANGUAGE_MODEL_WEB_SEARCH : For short phrases
		  //2.LANGUAGE_MODEL_FREE_FORM  : If not sure about the words or phrases and its domain.
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
		    RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

  	  //Start the Voice recognizer activity for the result.
  	  startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
   	 ArrayList<String> textMatchList = null;
     if (requestCode == VOICE_RECOGNITION_REQUEST_CODE)

      //If Voice recognition is successful then it returns RESULT_OK
      if(resultCode == RESULT_OK) {

       textMatchList = data
       .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

       //Bundle extras = data.getExtras();
       int SearchState = data.getIntExtra("SearchState", 0);
       if (!textMatchList.isEmpty()) {
        // If first Match contains the 'search' word
        // Then start web search.
        if (textMatchList.get(0).contains("search")) {

           String searchQuery = textMatchList.get(0);
                                              searchQuery = searchQuery.replace("search","");
           Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
           search.putExtra(SearchManager.QUERY, searchQuery);
           startActivity(search);
        } else {
       	 String searchQuery = textMatchList.get(0);
       	 showToastMessage("Command :  " + searchQuery);
       	showGridView(searchQuery);
        }
       }
      //Result code for various error.
      }else if(resultCode == RecognizerIntent.RESULT_AUDIO_ERROR){
       showToastMessage("Audio Error");
      }else if(resultCode == RecognizerIntent.RESULT_CLIENT_ERROR){
       showToastMessage("Client Error");
      }else if(resultCode == RecognizerIntent.RESULT_NETWORK_ERROR){
       showToastMessage("Network Error");
      }else if(resultCode == RecognizerIntent.RESULT_NO_MATCH){
       showToastMessage("No Match");
      }else if(resultCode == RecognizerIntent.RESULT_SERVER_ERROR){
       showToastMessage("Server Error");
      }
     super.onActivityResult(requestCode, resultCode, data);


     Intent resultIntent = new Intent();
     resultIntent.putExtra(RecognizerIntent.EXTRA_RESULTS, textMatchList);
     //TODO Add extras or a data URI to this intent as appropriate.
     setResult(Activity.RESULT_OK, resultIntent);
     //CameraMainActivity.this.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Helper method to show the toast message
     **/
     void showToastMessage(String message){
      Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
                //if (!bucket.equals("Camera"))
                  testPath(data);
            } while (cur.moveToNext());

        }
    }

}
