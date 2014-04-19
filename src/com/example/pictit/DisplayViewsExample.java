package com.example.pictit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
public class DisplayViewsExample extends Activity implements LoaderCallbacks<Cursor>, LogUtils
{
    private String TAG = "Pickit/DisplayView";
    // CPU & connectivity data intensive operation guarded by this flag
    private boolean mSupportGeoCoder = true;

    int id = -1;
    int bucketColumn = 0;
    int dateColumn = 0;
    int titleColumn = 0;
    int dataColumn = 0;
    int mCurrIndex = 0;
    int mGridCount = 0;

    Calendar mCalendar = Calendar.getInstance((Locale.getDefault()));
    private ShareActionProvider mShareActionProvider;

    // ************************************************************************
    // Set of Filters to compare against
    String[] mMonthNamesFilter = {"January", "february","March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    // Last WeekEnd
    String mLastWeekEndFilter = "Last Weekend";
    // Today
    String mTodayFilter = "Today";

    // Place
    String mPlaceFilter = "Place";

    //        Vs

    String mUserFilter;
    // ************************************************************************

    ArrayList<Uri> mImageUris = new ArrayList<Uri>();
    //Map<String, String> mMap = new HashMap<String, String>();
    ArrayList<String> mList = new ArrayList<String>();

    /**
     * Grid view holding the images.
     */
    private GridView displayImages;
    /**
     * Image adapter for the grid view.
     */
    private GridImageAdapter imageAdapter;

    boolean mClickStateOnGridItemAShortPress = true;

    private UserFilterAnalyzer mAnalyzer;

    private AsyncTask<Object, Bitmap, Object> mLoadImagesInBackground = null;

    ConnectivityManager mConnectivityManager;

    private DataBaseManager mDbHelper;

    MenuItem mItem;

    Display mDisplay;
    DisplayMetrics mOutMetrics;
    float mDensity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.displaygridview);
        getLoaderManager().initLoader(0, null, this);

        Intent intent = getIntent();
        String filter = intent.getExtras().getString("filter");
        mUserFilter = filter;
        mAnalyzer = new UserFilterAnalyzer(filter);
        setProgressBarIndeterminateVisibility(true);

        mConnectivityManager =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mDbHelper = DataBaseManager.getInstance(this);
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display mDisplay = wm.getDefaultDisplay();
        mOutMetrics= new DisplayMetrics ();
        mDisplay.getMetrics(mOutMetrics);
        mDensity = this.getResources().getDisplayMetrics().density;

        /*mgridView = (GridView) findViewById(R.id.gridview);
        mgridView.setBackgroundColor(color.darker_gray);
        mgridView.setVerticalSpacing(1);
        mgridView.setHorizontalSpacing(1);
        mgridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView parent,
            View v, int position, long id)
            {
                Toast.makeText(getBaseContext(),
                        "pic" + (position + 1) + " selected",
                        Toast.LENGTH_SHORT).show();
            }
        });*/

        setupViews();

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
            new IntentFilter("custom-event-name"));

    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mLoadImagesInBackground.isCancelled()) {
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mLoadImagesInBackground.isCancelled()) {
            mLoadImagesInBackground.cancel(true);
        }
    }

    @Override
    public void onDestroy() {
       LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
       if (!mLoadImagesInBackground.isCancelled()) {
           mLoadImagesInBackground.cancel(true);
       }
       super.onDestroy();
    }

     // Our handler for received Intents. This will be called whenever an Intent
     // with an action named "custom-event-name" is broadcasted.
     private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
         // Get extra data included in the Intent
         String message = intent.getStringExtra("message");
         Log.d("receiver", "Got message: " + message);
       }
     };
    /**
     * Setup the grid view.
     */
    private void setupViews() {
        displayImages = (GridView) findViewById(R.id.gridview);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        //displayImages.setNumColumns(metrics.widthPixels/200);
        //displayImages.setNumColumns(3);
        //displayImages.setClipToPadding(false);
        displayImages.setBackgroundColor(Color.DKGRAY);
        displayImages.setChoiceMode(displayImages.CHOICE_MODE_MULTIPLE_MODAL);
        displayImages.setMultiChoiceModeListener(new MultiChoiceModeListener());
        // Make GridView use your custom selector drawable
        //displayImages.setSelector(getResources().getDrawable(R.drawable.selector_grid));
        displayImages.setDrawSelectorOnTop(true);


        //displayImages.setVerticalSpacing(1);
        //displayImages.setHorizontalSpacing(1);
        //displayImages.setOnItemClickListener(DisplayViewsExample.this);
        displayImages.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView parent,
            View v, int position, long id)
            {
                mClickStateOnGridItemAShortPress = true;
                Toast.makeText(getBaseContext(),
                        "pic" + (position + 1) + " selected",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri imageUri = Uri.parse("file://" + mList.get(position));
                intent.setDataAndType(imageUri, "image/*");
                startActivity(intent);
            }
        });

        displayImages.setOnItemLongClickListener(new OnItemLongClickListener() {
            //this listener should show the context menu for a long click on the gridview.
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                   //return parent.showContextMenuForChild(v);
                Toast.makeText(getBaseContext(),
                        "pic" + (position + 1) + " selected - LONG KEY PRESS",
                        Toast.LENGTH_SHORT).show();
                mClickStateOnGridItemAShortPress = false;
                //v.setBackgroundColor(Color.RED);
                v.animate();
                v.setPressed(true);
                //parent.showContextMenuForChild(v);
                return true;

            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        if(mClickStateOnGridItemAShortPress) {
            //getMenuInflater().inflate(R.menu.context_standard, menu);
            //menu.setHeaderTitle("Standard Menu for "+arr[info.position]);
            //mClickStateOnGridItemAShortPress = false;
        }
        else {
            mClickStateOnGridItemAShortPress = false;
            //getMenuInflater().inflate(R.menu.context_options, menu);
            //menu.setHeaderTitle("Options Menu for "+arr[info.position]);
        }
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

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            mGridCount = data.getCount();
            setupCursor(data);

            new LoadImagesInBackGround(data, this).execute();
            imageAdapter = new GridImageAdapter(getApplicationContext());
            displayImages.setAdapter(imageAdapter);
        } else {
            //imagePath = imageUri.getPath();
        }
        //setupImageView();
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    void setupCursor(Cursor cur) {
        if (cur.moveToFirst()) {

            id = cur.getColumnIndex(
                    MediaStore.Images.Media._ID);

            bucketColumn = cur.getColumnIndex(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

            dateColumn = cur.getColumnIndex(
                MediaStore.Images.Media.DATE_TAKEN);

            titleColumn = cur.getColumnIndex(
                    MediaStore.Images.Media.TITLE);

            dataColumn = cur.getColumnIndex(
                    MediaStore.Images.Media.DATA);

            Log.d(TAG, cur.getPosition() + " : " + dateColumn );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        // Locate MenuItem with ShareActionProvider
        mItem = menu.findItem(R.id.menu_item_share);
        mItem.setEnabled(false);
        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) mItem.getActionProvider();
        //setShareIntent(createShareIntent());
        // Return true to display menu
        return true;
    }

    /*
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                Intent intent = new Intent(getBaseContext(), WiFiDirectActivity.class);
                intent.putStringArrayListExtra("image_paths", mList);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        displayImages.setNumColumns(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2);
        super.onConfigurationChanged(newConfig);
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
         if (mShareActionProvider != null) {
              mShareActionProvider.setShareIntent(shareIntent);
         }
    }

    private Intent createShareIntent() {
         //Intent shareIntent = new Intent(Intent.ACTION_SEND);
        Intent shareIntent = new Intent();
         shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);


         for (int i = 0; i < mList.size(); i++) {
             Uri imageUri = Uri.parse("file://" + mList.get(i));
             mImageUris.add(imageUri);
         }

         shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mImageUris);
         shareIntent.setType("image/jpeg");
         //shareIntent.setType("text/plain");
         //shareIntent.putExtra(Intent.EXTRA_TEXT,
                   //"http://androidtrainningcenter.blogspot.in");
         return shareIntent;
    }

    public class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
            //setBackgroundColor(Color.RED);

            //setBackgroundDrawable(checked ? getResources().getDrawable(
                    //R.drawable.blue) : null);
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void toggle() {
            setChecked(!mChecked);
        }

    }

    public class MultiChoiceModeListener implements
            GridView.MultiChoiceModeListener {
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("Select Pictures");
            mode.setSubtitle("1 picture selected");
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position,
                long id, boolean checked) {
            int selectCount = displayImages.getCheckedItemCount();
            switch (selectCount) {
            case 1:
                mode.setSubtitle("1 picture selected");
                break;
            default:
                mode.setSubtitle("" + selectCount + " pictures selected");
                break;
            }
            View v = displayImages.getChildAt(position);
            displayImages.setSelection(position);

            //v.setBackgroundResource(R.drawable.list_pressed);
            //v.setEnabled(true);
            /*View v = displayImages.getChildAt(position);
            if (checked || (selectCount == 1))
              v.setBackgroundColor(Color.RED);
            else
                v.setBackgroundColor(Color.TRANSPARENT);*/
        }
    }

    class ViewHolder {
        ImageView imageview;
        CheckBox checkbox;
        int id;
    }
    /**
     * Adapter for our image files.
     *
     */
    class GridImageAdapter extends BaseAdapter {
        private Context mContext;
        private LayoutInflater mInflater;
        private ArrayList<Bitmap> photos = new ArrayList<Bitmap>();

        public GridImageAdapter(Context context) {
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContext = context;
        }

        public void addPhoto(Bitmap photo) {
            photos.add(photo);
        }

        public int getCount() {
            // TBD: Need to check this ?
            return photos.size();
        }

        public Object getItem(int position) {
            return photos.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final ImageView imageView;
            //CheckableLayout l;
            if (convertView == null) {
                //l = new CheckableLayout(mContext);
                imageView = new ImageView(mContext);
                //l.addView(imageView);
            } else {
                imageView = (ImageView) convertView;
                //l = (CheckableLayout) convertView;
                //imageView = (ImageView) l.getChildAt(0);
            }
            int width1 = (int)mContext.getResources().getDimension(R.dimen.width);
            int height1 = (int)mContext.getResources().getDimension(R.dimen.height);
            if (position %2 == 0) {
                //width = 200;
                //height = 200;
            }
            //imageView.setLayoutParams(new GridView.LayoutParams(width,height));
            //imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //imageView.setPadding(4, 4, 4, 4);

            imageView.setImageBitmap(photos.get(position));

            /*if (position == displayImages.getSelectedItemPosition())

                imageView.setBackgroundColor(Color.RED);
                else
                    imageView.setBackgroundColor(Color.TRANSPARENT);*/
            //imageView.setBackgroundColor(Color.TRANSPARENT);
            //imageView.setPadding(4, 4, 4, 4);
            /*WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

            Display display = wm.getDefaultDisplay();
            DisplayMetrics outMetrics = new DisplayMetrics ();
            display.getMetrics(outMetrics);
            float density  = mContext.getResources().getDisplayMetrics().density;
            float dpHeight = outMetrics.heightPixels / density;
            float dpWidth  = outMetrics.widthPixels / density;

            int width=(int) (dpWidth);
            int height=(int) (dpHeight);

            imageView.setLayoutParams(new GridView.LayoutParams(width,height));
            imageView.setBackgroundColor(000000);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageBitmap(photos.get(position));*/
            return imageView;
        }
        /*public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = new ViewHolder();
            if (convertView == null) {
                convertView = mInflater.inflate(
                        R.layout.galleryitem, null);
                holder.imageview = (ImageView) convertView.findViewById(R.id.thumbImage);
                holder.checkbox = (CheckBox) convertView.findViewById(R.id.itemCheckBox);

                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.checkbox.setId(position);
            holder.imageview.setId(position);
            holder.checkbox.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    //ViewHolder holder = (ViewHolder) v.getTag();
                    SparseBooleanArray checked = displayImages.getCheckedItemPositions();
                    CheckBox cb = (CheckBox) v;
                    int id = cb.getId();
                    if (checked.get(id)){
                        cb.setChecked(false);
                        //thumbnailsselection[id] = false;
                    } else {
                        cb.setChecked(true);
                        //thumbnailsselection[id] = true;
                    }
                }
            });
            holder.imageview.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    int id = v.getId();
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri imageUri = Uri.parse("file://" + mList.get(0));
                    intent.setDataAndType(imageUri, "image/*");
                    startActivity(intent);
                }
            });
            //holder.imageview.setLayoutParams(new GridView.LayoutParams(260, 260));
            //holder.imageview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //holder.imageview.setPadding(4, 4, 4, 4);
            holder.imageview.setImageBitmap(photos.get(position));
            holder.imageview.setBackgroundColor(Color.TRANSPARENT);
            holder.checkbox.setChecked(true);
            holder.id = position;
            return convertView;
        }*/
    }

    /**
     * Add image(s) to the grid view adapter.
     *
     * @param value Array of Bitmap references
     */
    private void addGridImage(Bitmap... value) {
        for (Bitmap image : value) {
            imageAdapter.addPhoto(image);
            imageAdapter.notifyDataSetChanged();
        }
    }

    boolean addtoListIfNotFound(String path) {
        if (!mList.contains(path)) {
             mList.add(path);
             return true;
         }

         if (DEBUG) Log.d(TAG, "Path : " + path + " already found!");
         return false;
     }

    boolean removeFromList(String path) {
        if (mList.contains(path)) {
            int index = mList.indexOf(path);
            if (index != -1) {
              mList.remove(index);
              if (DEBUG) Log.d(TAG, "Path : " + path + " removed!");
            }
            return true;
         }
         return false;
     }

    class LoadImagesInBackGround extends AsyncTask<Object, Bitmap, Object> {

        private Cursor mCursor;
        private Context mContext;

        LoadImagesInBackGround(Cursor cur, Context context) {
            mCursor = cur;
            mContext = context;
        }
        /**
         * Load images in the background, and display each image on the screen.
         */
        protected String doInBackground(Object... params) {
            if (isCancelled()) return null;
            setProgressBarIndeterminateVisibility(true);
           // Again check for isCancelled() , there could be potential race conditions here
           // when task is cancelled. The thread that just got cancelled (as a result of configuration changes)
           // still latches onto old cursor object

            while (!mCursor.isClosed() && mCursor.moveToNext() && !isCancelled()) {
                Bitmap bmp = getImgBasedOnUserFilter(mCursor);
                if (bmp != null) {
                  publishProgress(bmp);
                }
            }
            mCursor.close();
            return null;
        }
        /**
         * Add a new Bitmap in the images grid.
         *
         * @param value The image.
         */
        public void onProgressUpdate(Bitmap... params) {
            addGridImage(params);
        }
        /**
         * Set the visibility of the progress bar to false.
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPreExecute(){
            mLoadImagesInBackground = this;
        }
        @Override
        protected void onPostExecute(Object result) {
            setProgressBarIndeterminateVisibility(false);
            setShareIntent(createShareIntent());
            mItem.setEnabled(true);
        }

        @Override
        protected void onCancelled(Object result) {

        }

        Bitmap getPicture(String path) {
            ExifInterface intf = null;
            Bitmap bitmap = null;
            Bitmap newBitmap = null;

            if (path == null) {
              return null;
            }
            try {
                intf = new ExifInterface(path);
            } catch(IOException e) {
                e.printStackTrace();
            }

            if(intf == null) {
                return null;
            }

            String dateString = intf.getAttribute(ExifInterface.TAG_DATETIME);
            if (DEBUG) Log.d(TAG, dateString);
            if (intf.hasThumbnail()) {
               byte[] thumbnail = intf.getThumbnail();
               //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
               bitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
               if (bitmap != null) {

                   float dpHeight = mOutMetrics.heightPixels / mDensity;
                   float dpWidth  = mOutMetrics.widthPixels / mDensity;

                   int width=(int) (dpWidth);
                   int height=(int) (dpHeight);
                   newBitmap = Bitmap.createScaledBitmap(bitmap, width, width, true);
                   bitmap.recycle();
                   if (newBitmap != null) {
                       return newBitmap;
                   }
               }
               //BitmapDrawable bmd = new m (getResources(),bmpImg);
               return bitmap;
            } else {
               Uri imageUri = null;
               try {
                   imageUri = Uri.parse("file://" + path);
                   bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                   if (bitmap != null) {
                       newBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
                       bitmap.recycle();
                       if (newBitmap != null) {
                           return newBitmap;
                       }
                   }
               } catch (IOException e) {
                   //Error fetching image, try to recover
               }
             }
            return null;
        }

        Bitmap getImgBasedOnUserFilter(Cursor cur) {
            boolean added = false;
            int dateRangeMatchFound = -1;
            String path   = null;
            do {
                if (cur.isClosed()) break;
                String curDate = cur.getString(dateColumn);
                if (cur.isClosed()) break;
                path = cur.getString(dataColumn);

                if (curDate == null) break;
                long dateinMilliSec = Long.parseLong(curDate);
                mCalendar.setTimeInMillis(dateinMilliSec);
                int monthOfYear = mCalendar.get(Calendar.MONTH);
                //if (monthOfYear >= 0 && monthOfYear <= 11 ) {
                // TBD: These checks have to much much smarter.. They are way too dumb for my liking
                Pair<Long, Long> pairRange = mAnalyzer.getDateRange(mUserFilter);
                if (null != pairRange) {
                    DateRangeManager range = new DateRangeManager();
                    range.printDateAndTime(mCalendar);
                    if ((dateinMilliSec >= pairRange.first) && (dateinMilliSec <= pairRange.second)) {
                        if (DEBUG) Log.d(TAG, "****** Added ********* ");
                        range.printDateAndTime(mCalendar);
                        if (DEBUG) Log.d(TAG, "****** Added ********* ");
                        dateRangeMatchFound= 0;
                      added = true;//addtoListIfNotFound(path);
                    } else {
                        dateRangeMatchFound= 1;
                        added = false;
                    }
                } else if(mUserFilter.toLowerCase().contains(mMonthNamesFilter[monthOfYear].toLowerCase())) {
                    added = true;//addtoListIfNotFound(path);
                }

                if (mUserFilter.toLowerCase().contains(mLastWeekEndFilter.toLowerCase())) {
                    DateRangeManager range = new DateRangeManager();
                    Pair<Long, Long> p = range.getLastWeekEnd();
                    range.printDateAndTime(mCalendar);

                    if ((dateinMilliSec >= p.first) && (dateinMilliSec <= p.second)) {
                      if (DEBUG) Log.d(TAG, "****** Added ********* ");
                      range.printDateAndTime(mCalendar);
                      if (DEBUG) Log.d(TAG, "****** Added ********* ");
                      added = true;// addtoListIfNotFound(path);
                      dateRangeMatchFound= 0;
                    } else {
                        dateRangeMatchFound= 1;
                        added = false; //!removeFromList(path);
                    }
                }
                if (mUserFilter.toLowerCase().contains(mTodayFilter.toLowerCase())) {
                    // TBD: Need to Re-factor this code with the above
                    DateRangeManager range = new DateRangeManager();
                    Pair<Long, Long> p = range.getToday();

                    range.printDateAndTime(mCalendar);

                    if ((dateinMilliSec >= p.first) && (dateinMilliSec <= p.second)) {
                        dateRangeMatchFound= 0;
                      added = true; // traddtoListIfNotFound(path);
                    } else {
                        dateRangeMatchFound= 1;
                        added = false; //!removeFromList(path);
                    }
                }
                // Following block to 'enable / disable search by places'
                // TBD All of the parse logic should eventually be wrapped into UserFileterAnalyzer
                if ((mSupportGeoCoder)) { // && (mUserFilter.toLowerCase().contains(mPlaceFilter.toLowerCase()))) {
                   GeoDecoder geoDecoder = null;
                   String addr = null;
                   Integer currentId = cur.getInt(id);
                   boolean alsoMatchCity = true; //mAnalyzer.isPrepositionKeywordFoundBeforePlace(mUserFilter);
                   //added = !alsoMatchCity;
                   List<Address> address;
                   ExifInterface intf = null;
                   try {
                       intf = new ExifInterface(path);
                   } catch(IOException e) {
                       e.printStackTrace();
                       break;
                   }
                   String attrLATITUDE = intf.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                   String attrLATITUDE_REF = intf.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                   String attrLONGITUDE = intf.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                   String attrLONGITUDE_REF = intf.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

                   if((attrLATITUDE !=null)
                     && (attrLATITUDE_REF !=null)
                     && (attrLONGITUDE != null)
                     && (attrLONGITUDE_REF !=null)){
                       // It has some valid values
                       // Try to read from Cache / db
                       String placeFound = mDbHelper.getPlace(currentId);
                       if (placeFound == null) {
                           // Place not found in cache or db ,but it has a valid GPS cod-ordinates
                           // Try and fallback on GeoCoder API to retrieve the place.
                       } else if(mUserFilter.toLowerCase().contains(placeFound.toLowerCase())) {
                           // Wow... we have the place found either in the cache or db..
                           added = true;
                           break;
                       } else {
                           // OK some place exists in cache / db but does not match with UserFilter
                           break;
                       }
                   } else {
                       continue;
                   }

                   // Before we try to retrieve from Internet, check to see if there is active connection
                   if ((mConnectivityManager.getActiveNetworkInfo() == null) ||
                      !(mConnectivityManager.getActiveNetworkInfo().isConnectedOrConnecting())) {
                      // mConnectivityManager.getActiveNetworkInfo() being null happens in airplane mode I guess
                      Log.d(TAG,"Ooops No Connection.. Try Later");
                      continue;
                   }
                   try {
                           geoDecoder = new GeoDecoder(new ExifInterface(path));
                           if (!geoDecoder.isValid()) {
                               // This image doesn't not have valid lat / long associated to it
                               // What if it is already added as a result of 'Date Range'.
                               //  - Since we cannot determine the 'locality' of this image,
                               //    check 'alsoMatchCity' flag.
                               if (alsoMatchCity && added) {
                                    // Match city is true (explicitly requested by user) and
                                    // previously added flag is true (possibly due to date range check),
                                    // but there is no associated lat/long... Sorry User!

                                    // TBD: From User point of view, maybe more is better ?
                                    added = false;
                               }
                               // Either Match city is false or was not previously added. In both cases
                               // retain the added flag as-is

                               break;
                           }
                   } catch (IOException e) {
                           // TODO Auto-generated catch block
                           e.printStackTrace();
                   }
                   address = geoDecoder.getAddress(mContext);
                   String locality = null;
                   if (address!= null && address.size() > 0)
                     locality = address.get(0).getLocality();
                   if (locality != null) {
                       // Try and insert to the d/b and cache
                       mDbHelper.updateRow(currentId, locality);
                   }
                   if ((locality != null) && (0 == mAnalyzer.compareUserFilterForCity(locality))) {
                     // At this point, 'locality' / 'city' is matched.
                     // check 'dateRangeMatchFound' has been set or not

                     // TBD: Insert this (id, place) row in database, however it may not be a good idea as potentially there is the
                     // SyncIntentService worker thread updating the d/b. It is not a good idea for another thread (main thread) to
                     // update the d/b without ensuring that db interface is thread safe !!!
                     if (dateRangeMatchFound != -1) {
                         // DateRange has been set
                         if ((dateRangeMatchFound == 0) && (alsoMatchCity)) {
                             // Date Range Match found and Match city
                             added = true;
                         } else if ((dateRangeMatchFound == 1) && (alsoMatchCity)) {
                             // date range is false , and match city
                             added = false;
                         } else {
                             // Match city flag is false, but city matches anyways
                             added = true;
                         }
                     } else {
                       // Date Range not set but Locality match succeed.
                       added = true;
                     }
                   } else {
                     // City doesn't exist
                        added = false ;
                   }
                } else {
                    if (WARN) Log.i(TAG, "Ooops! No results");
                }
            } while (false);

            if (added) {
                addtoListIfNotFound(path);
                return getPicture(path);
            }
            return null;
        } // End of function
    } // End of LoadImagesInBackGround
} // End of Main class
