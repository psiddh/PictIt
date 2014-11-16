package com.app.spicit;

import java.io.IOException;
import java.util.ArrayList;

import com.app.spicit.AlertDialogFrag.AlertDialogFragment;
import com.app.spicit.DataBaseManager.SyncState;

import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

public class MainActivity extends Activity implements LogUtils {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
    private static final int SHOW_GRID_AFTER_DELAY = 1002;
    private static final int GRID_DISPLAY_DELAY = 3000;

    private int mMainMenuStatusDisplay = 0xFF;

    private ProgressBar mProgress;
    private EditText mEditText;
    ImageButton mImgButton = null;
    
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    
    private String TAG = "SpickIt> MainView";

    private Handler mTextSwictherHandler = new Handler() {
        public void handleMessage (Message msg) {
            switch (msg.what) {
                case SHOW_ANIM_TEXT:
                    //success handling
                    updateTextSwitcherText();
                    mTimeOutTextVals = 2000;
                    Message mesg = new Message();
                    mesg.what = SHOW_ANIM_TEXT;
                    mTextSwictherHandler.removeMessages(SHOW_ANIM_TEXT);
                    mTextSwictherHandler.sendMessageDelayed(mesg, mTimeOutTextVals);
                    break;
                default:
                    //failure handling
                    break;
            }
        }
    };

    private TextSwitcher mSwitcher;
    // Array of String to Show In TextSwitcher
    String mTextToShow[]={"Click mic icon and SAY something like... OR simply TYPE, \"In August\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"March 1st to Oct 30th\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"June Pictures in Hawaii\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"May Sep\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"July\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"July 2014\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"December\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"In October\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"At Timbuktu\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"India Pictures\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Madagascar\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"2013 to 2014\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"2014 Pictures\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"San Francisco in August\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"2012 in London\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"California\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Between January and March\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Feb 1st Apr 1st\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"July 4th\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"4th of July\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"November 1st to December 25th 2013\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Pictures from Norway\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Dec 25th at New York\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Today's pictures\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"January 25 2013 to July 4th 2014 at Florida\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Yesterday's pictures\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Yesterday\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Christmas\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Christmas Eve\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Boxing Day\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"last week\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"this week\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"New Year's day\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Christmas Eve\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"last weekend\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"last couple of weeks\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"last couple of months\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"last couple of days\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"couple of days back\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"couple of days ago\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"couple of months back\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"couple of months ago\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"Since last month\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"this month\"",
                          "Click mic icon and SAY something like... OR simply TYPE, \"couple of weeks ago"};
    int mTimeOutTextVals = 3000;
    private static final int SHOW_ANIM_TEXT = 1003;

    private Handler mHandler = new Handler() {
        public void handleMessage (Message msg) {
            switch (msg.what) {
                case SHOW_GRID_AFTER_DELAY:
                    //success handling
                    Bundle b = msg.getData();
                    String filter = b.getString("filter");
                    Intent intent = new Intent(getBaseContext(), ResultsView.class);
                    intent.putExtra("filter", filter);
                    startActivity(intent);
                    break;
                case 0:
                    //failure handling
                    break;
            }
            mProgress.setVisibility(View.GONE);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        if (true) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            MenuItem item = menu.findItem(R.id.menu_item_map);
            item.setVisible(true);
        }
        
        if (mMainMenuStatusDisplay != 0xFF) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            MenuItem item = menu.findItem(R.id.menu_item_info);
            item.setVisible(true);
        }
        //mMainMenuStatusDisplay = 0xFF;
        return true;
    }

    // method to Update the TextSwitcher Text
    private void updateTextSwitcherText() {
        int Count = mTextToShow.length;
        int random = (int)(Math.random() * (Count));
        if(random <= Count)
          mSwitcher.setText(mTextToShow[random]);
    }

    public void setupTextSwitcher() {
         mSwitcher = (TextSwitcher) findViewById(R.id.textSwitcher);
         // Set the ViewFactory of the TextSwitcher that will create TextView object when asked
         mSwitcher.setFactory(new ViewFactory() {
             public View makeView() {
                 // TODO Auto-generated method stub
                 // create new textView and set the properties like color, size etc
                 TextView myText = new TextView(MainActivity.this);
                 myText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                 myText.setTextSize(24);
                 myText.setTextColor(Color.WHITE);
                 return myText;
             }
         });
         // Declare the in and out animations and initialize them
         Animation in = AnimationUtils.loadAnimation(this,android.R.anim.fade_in);
         Animation out = AnimationUtils.loadAnimation(this,android.R.anim.fade_out);

         // set the animation type of textSwitcher
         mSwitcher.setInAnimation(in);
         mSwitcher.setOutAnimation(out);

         updateTextSwitcherText();

         Message msg = new Message();
         msg.what = SHOW_ANIM_TEXT;
         mTextSwictherHandler.removeMessages(SHOW_ANIM_TEXT);
         mTextSwictherHandler.sendMessageDelayed(msg, mTimeOutTextVals);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeMessages(SHOW_GRID_AFTER_DELAY);
        mTextSwictherHandler.removeMessages(SHOW_ANIM_TEXT);
        // Just in case
        mProgress.setVisibility(View.GONE);

    }

    @Override
    public void onResume() {
        super.onResume();
        Message msg = new Message();
        msg.what = SHOW_ANIM_TEXT;
        mTextSwictherHandler.sendMessageDelayed(msg, mTimeOutTextVals);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(SHOW_GRID_AFTER_DELAY);
        mTextSwictherHandler.removeMessages(SHOW_ANIM_TEXT);

    }

    /*public void setupImageView(Cursor cur) {
        ExifInterface intf = null;
        int count = cur.getCount();
        if (count == 0 ) return;
        int Min = 1;
        int random = Min + (int)(Math.random() * ((count - Min) + 1));

        if (cur.moveToPosition(random)) {
            String path = null;
            int dataColumn = cur.getColumnIndex(
                    MediaStore.Images.Media.DATA);
            if (!cur.isClosed()) {
                path = cur.getString(dataColumn);
                try {
                    intf = new ExifInterface(path);
                } catch(IOException e) {
                    e.printStackTrace();
                }

                if(intf == null) {
                    return;
                }
                if(path != null) {
                    Bitmap myBitmap = null ;
                    ImageView myImage = (ImageView) findViewById(R.id.img_one);
                    if (intf.hasThumbnail()) {
                        byte[] thumbnail = intf.getThumbnail();
                        Bitmap rawMyBitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
                        myBitmap = Bitmap.createScaledBitmap(rawMyBitmap, 200, 200, true);
                        rawMyBitmap.recycle();
                    } else {
                       File imgFile = new  File(path);
                       if(imgFile.exists()){
                           Bitmap rawMyBitmap  = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                           myBitmap = Bitmap.createScaledBitmap(rawMyBitmap, 200, 200, true);
                            rawMyBitmap.recycle();
                       }
                    }
                    Matrix matrix = new Matrix();
                    matrix.postRotate(30);
                    Bitmap rotated = Bitmap.createBitmap(myBitmap, 0, 0, 200, 200,
                            matrix, true);
                    //myImage.setImageBitmap(myBitmap);
                    myImage.setImageBitmap(rotated);
                }
            }
        }
    }*/

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        //mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_item_map:
            	// Launch Map mode
                Intent intent = new Intent(getBaseContext(), MapFragmentActivity.class);
                intent.putExtra("filter", "test");
                startActivity(intent);
            	break;
            case R.id.menu_item_info:
                String msg = "Voice filter by 'places' on your Gallery --> Camera pictures may not work due to an unexpected error! \n\n" +
                             "However search by 'dates' or 'date ranges' shall continue to work.";
                switch(mMainMenuStatusDisplay) {
                    case -2:
                        break;
                    case -1:
                        msg = "You may see inconsistent results! \n\n" +
                              "Probale Reason(s): Either there is / was no active data connection detected at the time of sync (or) Some Pictures' Geo-Cooridnates have not been decoded properly by GeoCoder service. As a result, filtering by 'place' may yield incomplete results";
                        break;
                    case 0:
                        msg = "Background Sync Status Completed! \n\nReady to search by 'dates' AND / OR 'places' on your Gallery --> Camera  pictures. \n\n" +
                              "TIP: Please ensure that your camera pictures were GeoTagged properly in order to successfully search by 'places' ";
                        break;
                    case 1:
                        msg = "Please Wait! Background Sync In Progress. \n\nSearch by 'places' may yield incomplete / inconsistent results. \n\n" +
                              "However search by 'dates' or 'date ranges' shall continue to work. ";
                        break;
                    case 2:
                        msg = "Please Wait! Background Sync Update In Progress. \n\nSearch by 'places' may yield incomplete / inconsistent results. \n\n" +
                              "However search by 'dates' or 'date ranges' shall continue to work. ";
                        break;
                    default:
                        break;
                }
                DialogFragment newFragment = AlertDialogFragment.newInstance(
                        "INFO", msg);
                newFragment.show(getFragmentManager(), "dialog");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);        
        setupTextSwitcher();
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mProgress = (ProgressBar) findViewById(R.id.progressBar1);
        Intent msgIntent = new Intent(this, SyncIntentService.class);
        startService(msgIntent);
        final ImageButton button = (ImageButton) findViewById(R.id.button1);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                speak();
                //showGridView("Sonoma");
            }
        });
        mEditText = (EditText) findViewById(R.id.editText1);
        mEditText.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mHandler.removeMessages(SHOW_GRID_AFTER_DELAY);
                mProgress.setVisibility(View.GONE);
                return false;
           }
        });
        mEditText.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){
                if(s.length() > 0) {
                    mImgButton.setVisibility(View.VISIBLE);
                } else {
                    mImgButton.setVisibility(View.GONE);
                }
            }
        });
        mImgButton = (ImageButton) findViewById(R.id.imageButton);
        OnClickListener mClkListener =new OnClickListener() {
            public void onClick(View button) {
                mHandler.removeMessages(SHOW_GRID_AFTER_DELAY);
                Message msg = new Message();
                Bundle b = new Bundle();
                b.putString("filter",mEditText.getText().toString());
                msg.what = SHOW_GRID_AFTER_DELAY;
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        };
        mImgButton.setOnClickListener(mClkListener);
        LocalBroadcastManager.getInstance(this).registerReceiver(message,
                new IntentFilter("sync-state"));
    }

    private BroadcastReceiver message = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            SyncState state = (SyncState) intent.getSerializableExtra("sync-status");
            if (state == SyncState.SYNC_STATE_COMPLETED) {
                mMainMenuStatusDisplay = 0;
            } else if (state == SyncState.SYNC_STATE_INCOMPLETE) {
                mMainMenuStatusDisplay = -1;
            } else if (state == SyncState.SYNC_STATE_INPROGRESS) {
                mMainMenuStatusDisplay = 1;
            } else if (state == SyncState.SYNC_STATE_ABORTED) {
                mMainMenuStatusDisplay = -2;
            } else if (state == SyncState.SYNC_STATE_UPDATE) {
                mMainMenuStatusDisplay = 2;
            }

            invalidateOptionsMenu();

        }
    };
    void showGridView(String filter) {
        // Just in case, remove any previously queued messages
        // TBD: Revisit this later?
        mHandler.removeMessages(SHOW_GRID_AFTER_DELAY);

        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("filter",filter);
        msg.what = SHOW_GRID_AFTER_DELAY;
        msg.setData(b);
        mEditText.setText(filter);
        mEditText.setSelection(mEditText.getText().length());
        mProgress.setVisibility(View.VISIBLE);
        mHandler.sendMessageDelayed(msg, GRID_DISPLAY_DELAY);
    }

    void testPath(String path) {
        ExifInterface intf = null;
        try {
            intf = new ExifInterface(path );
        } catch(IOException e) {
            e.printStackTrace();
        }
        if(intf == null) {
            /* File doesn't exist or isn't an image */
        }

        String dateString = intf.getAttribute(ExifInterface.TAG_DATETIME);
        if (DEBUG) Log.d(TAG, path);
        if (DEBUG) Log.d(TAG, dateString);
           if (intf.hasThumbnail()) {
               byte[] thumbnail = intf.getThumbnail();
               LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
               Bitmap bmpImg = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
               BitmapDrawable bmd = new BitmapDrawable(getResources(),bmpImg);
               ImageView imageView = new ImageView(this);
               imageView.setPadding(2, 0, 5, 5);
               imageView.setScaleType(ImageView.ScaleType.FIT_XY);
               imageView.setLayoutParams(layoutParams);
               imageView.setImageDrawable(bmd);
           }
    }

    public void speak() {
          Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
          // Specify the calling package to identify your application
          intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
            .getPackage().getName());
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
       //int SearchState = data.getIntExtra("SearchState", 0);
       if (!textMatchList.isEmpty()) {
        // If first Match contains the 'search' word
        // Then start web search.
        {
            String searchQuery = textMatchList.get(0);
            //showToastMessage("Command :  " + searchQuery);
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
    }

    /**
     * Helper method to show the toast message
     **/
     void showToastMessage(String message){
         Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
     }

}
