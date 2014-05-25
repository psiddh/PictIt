package com.example.pictit;

import java.io.IOException;
import java.util.ArrayList;

import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.speech.RecognizerIntent;
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

public class MainActivity extends Activity {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
    private static final int SHOW_GRID_AFTER_DELAY = 1002;
    private static final int GRID_DISPLAY_DELAY = 3000;

    private ProgressBar mProgress;
    private EditText mEditText;
    ImageButton mImgButton = null;
    private UserFilterAnalyzer mUserFilterAnalyzer;
    //private DrawerLayout mDrawerLayout;
    //private ListView mDrawerList;
    //private ActionBarDrawerToggle mDrawerToggle;

    //private CharSequence mDrawerTitle;
    //private String[] mPlanetTitles;

    private Handler mTextSwictherHandler = new Handler() {
        public void handleMessage (Message msg) {
            switch (msg.what) {
                case SHOW_ANIM_TEXT:
                    //success handling
                    updateTextSwitcherText();
                    mTimeOutTextVals += 1000;
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
    String mTextToShow[]={"Say, \"Show pictures taken in August\"",
                          "Say, \"Show pictures taken at Timbuktu\"",
                          "Say, \"Pictures from San Francisco taken in August\"",
                          "Say, \"Show Pictures between January and March\"",
                          "Say, \"Pictures taken on July 4th\"",
                          "Say, \"Pictures between November 1st and November 25th\"",
                          "Say, \"Pictures from Norway\"",
                          "Say, \"Pictures taken on December 25th at New York\"",
                          "Say, \"Show Pictures taken today\"",
                          "Say, \"Pictures since last couple of weeks\""};
    int mTimeOutTextVals = 3000;
    private static final int SHOW_ANIM_TEXT = 1003;

    //private ShareActionProvider mShareActionProvider;
    /* The click listener for ListView in the navigation drawer */
    /*private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //selectItem(position);
        }
    }*/

    private Handler mHandler = new Handler() {
        public void handleMessage (Message msg) {
            switch (msg.what) {
                case SHOW_GRID_AFTER_DELAY:
                    //success handling
                    Bundle b = msg.getData();
                    String filter = b.getString("filter");
                    Intent intent = new Intent(getBaseContext(), DisplayViewsExample.class);
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

   /* public void setupDrawers() {
        mDrawerTitle = "Manage Events";//getTitle();
        mPlanetTitles = getResources().getStringArray(R.array.events_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        //mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setBackgroundColor(0xF5F5DC);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  // host Activity
                mDrawerLayout,         // DrawerLayout object
                R.drawable.ic_navigation_drawer,  // nav drawer image to replace 'Up' caret
                R.string.drawer_open,  // "open drawer" description for accessibility
                R.string.drawer_close  //"close drawer" description for accessibility
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(getTitle());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
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
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);
        // Nah! don't do this as well
        //setupDrawers();
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
                mImgButton.setVisibility(View.VISIBLE);
                return false;
           }
        });
        mImgButton = (ImageButton) findViewById(R.id.imageButton);
        OnClickListener mClkListener =new OnClickListener() {
            public void onClick(View button) {
               if (button.isSelected()){
                   button.setSelected(false);
               } else {
                   mImgButton.setSelected(false);
                   //put all the other buttons you might want to disable here...
                   button.setSelected(true);
                   mHandler.removeMessages(SHOW_GRID_AFTER_DELAY);
                   Message msg = new Message();
                   Bundle b = new Bundle();
                   b.putString("filter",mEditText.getText().toString());
                   msg.what = SHOW_GRID_AFTER_DELAY;
                   msg.setData(b);
                   mHandler.sendMessage(msg);
               }
            }
        };
        mImgButton.setOnClickListener(mClkListener);
        //getLoaderManager().initLoader(0, null, this);
    }

    /*@Override
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
        if (data != null) {
            // Nah! don't do this till the idea is mature
            //setupImageView(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }*/

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
