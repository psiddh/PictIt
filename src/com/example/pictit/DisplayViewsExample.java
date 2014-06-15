package com.example.pictit;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.pictit.AlertDialogFrag.AlertDialogFragment;
import com.example.pictit.DataBaseManager.SyncState;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ViewSwitcher;
import android.app.ActivityManager;
public class DisplayViewsExample extends Activity implements LoaderCallbacks<Cursor>, LogUtils {
    private String TAG = "SpikIt> DisplayView";
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

    String mUserFilter;

    DateRangeManager mRangeMgr = new DateRangeManager();

    Pair<Long, Long> mPairRange = null;

    boolean mPhraseAsTitle = false;

    ArrayList<String> mUserFilterContainsAPLACES = null;

    int mMatchState = -1;

    Calendar mTitleCalendar = Calendar.getInstance((Locale.getDefault()));

    ActionBar mActBar = null;

    int mUpdateSubTitleRequired = 0;
    ArrayList<String> mPlaceList = new ArrayList<String>();
    boolean mIsTitleDate = false;

    public static final int IsPlace = 1 << 0;
    public static final int IsCountry = 1 << 1;
    public static final int IsAdminArea = 1 << 2;
    public static final int IsOther = 1 << 3;
    // ************************************************************************

    ArrayList<Uri> mImageUris = new ArrayList<Uri>();
    ArrayList<String> mList = new ArrayList<String>();

    private ArrayList<Bitmap> photos = new ArrayList<Bitmap>();

    int memClass = 0;//((ActivityManager) this.getSystemService( Context.ACTIVITY_SERVICE )).getMemoryClass();
    int cacheSize = 0; //1024 * 1024 * memClass / 8;

    private LruCache<String, BitmapDrawable> mMemoryCache = null ; //new LruCache<String, Bitmap>(cacheSize) {

    /**
     * Grid view holding the images.
     */
    private GridView mDisplayImages;
    /**
     * Image adapter for the grid view.
     */
    private GridImageAdapter mImageAdapter;

    private enum SelectState {
           ALL,
           ALL_INPROGRESS,
           ALL_DONE,
           ALL_PICK_INDIVIDUAL,
           CHERRY_PICK,
           NONE
    }

    SelectState mState = SelectState.NONE;
    private static final int SELECT_ALL_ITEMS = 1001;
    boolean mClickStateOnGridItemAShortPress = true;

    private UserFilterAnalyzer mAnalyzer;

    private AsyncTask<Object, Bitmap, Object> mLoadImagesInBackground = null;

    ConnectivityManager mConnectivityManager;

    private DataBaseManager mDbHelper;

    MenuItem mItem;

    Display mDisplay;
    DisplayMetrics mOutMetrics;
    float mDensity;

    ViewSwitcher mViewSwitcher;
    boolean mShowGrid = false;

    private String mShowWarningMenuItem = null;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage (Message msg) {
                 switch (msg.what) {
                  case SELECT_ALL_ITEMS:
                    setShareIntent(createCheckedItemsIntent());
                    break;
                   default:
                     break;
                 }
            }
     };

     public static int getBitmapSize(BitmapDrawable value) {
         Bitmap bitmap = value.getBitmap();

         // From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
         // larger than bitmap byte count.
         if (Utils.hasKitKat() &&  bitmap != null && !bitmap.isRecycled()) {
             return bitmap.getAllocationByteCount();
         }

         if (Utils.hasHoneycombMR1()) {
             return bitmap.getByteCount();
         }

         // Pre HC-MR1
         return bitmap.getRowBytes() * bitmap.getHeight();
     }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.displaygridview);
        getLoaderManager().initLoader(0, null, this);
        mActBar = getActionBar();

        memClass = ((ActivityManager) this.getSystemService( Context.ACTIVITY_SERVICE )).getMemoryClass();
        cacheSize = (1024 *  1024 * memClass) / 8;

        Intent intent = getIntent();
        String filter = intent.getExtras().getString("filter");
        mUserFilter = filter;
        mAnalyzer = new UserFilterAnalyzer(this, filter);
        mPairRange = mAnalyzer.getDateRange(mUserFilter);
        String title = getTitleFromPair(mPairRange);
        mMatchState = mAnalyzer.getMatchState();
        updateTitle(title);
        setProgressBarIndeterminateVisibility(false);
        mViewSwitcher = (ViewSwitcher) findViewById(R.id.displayViewSwitcher);
        mActBar.setHomeButtonEnabled(true);
        mActBar.setDisplayHomeAsUpEnabled(true);

        mConnectivityManager =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        mDbHelper = DataBaseManager.getInstance(this);
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display mDisplay = wm.getDefaultDisplay();
        mOutMetrics= new DisplayMetrics ();
        mDisplay.getMetrics(mOutMetrics);
        mDensity = this.getResources().getDisplayMetrics().density;
        mViewSwitcher.setBackgroundColor(Color.DKGRAY);

        setupViews();

        if (mDbHelper.getState() == DataBaseManager.SyncState.SYNC_STATE_COMPLETED) {
            mUserFilterContainsAPLACES = mDbHelper.retreiveAllPlacesFromStringIfExists(mUserFilter);
        } else if (mDbHelper.getState() == DataBaseManager.SyncState.SYNC_STATE_INPROGRESS) {
            TextView txtView = (TextView) mViewSwitcher.findViewById(R.id.displayViewProgressTextView);
            txtView.setText("Please wait, Loading pictures may take a while! Background Sync is still in-progress");
        }
    }

    /*private void updayeWarningMenuItem() {
        SyncState state = mDbHelper.getState();
        if (state == SyncState.SYNC_STATE_INCOMPLETE) {
            mShowWarningMenuItem = "Warning: You may see Inconsistent / Incorrect Results. \n\n" +
                    "Probable Reason: Background Sync State is in-complete due to an unexpected error! ";
        } else if (state == SyncState.SYNC_STATE_INPROGRESS) {
            mShowWarningMenuItem = "Warning: You may see Inconsistent / Incorrect Results. \n\n" +
                    "Probable Reason: Background Sync still in-progress! \n\n" +
                    "TIP: Please retry again when the sync is completed (or) retry after some time for exact results";
        } else if (state == SyncState.SYNC_STATE_ABORTED) {
            mShowWarningMenuItem = "Warning: You may see Inconsistent / Incorrect Results. \n\n" +
                    "Probable Reason: Background Sync State is in-complete due to an unexpected error! ";
        } else if (state == SyncState.SYNC_STATE_UPDATE) {
            mShowWarningMenuItem = "Warning: You may see Inconsistent / Incorrect Results. \n\n" +
                    "Probable Reason: Background Sync UPDATE still in-progress! \n\n" +
                    "TIP: Please retry again when the sync is up-to-date (or) retry after some time for exact results";
        } else if (state == SyncState.SYNC_STATE_COMPLETED) {
            mShowWarningMenuItem = "Everything looks OK \n\n" +
                    "TIP: If you still see Inconsistent / Incorrect Results while filtering your pictures by 'places', " +
                    "Please ensure that your 'camera' pictures were GeoTagged (at the time of taking the picture(s)) in order to successfully search by 'places' ";
        }
    }*/

    private String getTitleFromPair(Pair<Long, Long> pair) {
        long ms_in_day = 86400000;
        Calendar current = Calendar.getInstance((Locale.getDefault()));
        long secondPair = 0;
        String title = "";
        if (pair == null)
          return title;

        if (pair.second - pair.first <= ms_in_day) {
            mIsTitleDate = true;
        }
        mTitleCalendar.clear();
        mTitleCalendar.setTimeInMillis(pair.first);
        title += mTitleCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);
        title += " ";

        title += mTitleCalendar.get(Calendar.DAY_OF_MONTH);
        title += ", '";

        title += mTitleCalendar.get(Calendar.YEAR) % 100;

        if (pair.second - pair.first <= ms_in_day) {
            return title;
        }
        title += " - ";

        if (pair.second >= current.getTimeInMillis()) {
            secondPair = current.getTimeInMillis();
            title += "(Today) ";
        }
        else {
            secondPair = pair.second;
        }
        mTitleCalendar.setTimeInMillis(secondPair);
        title += mTitleCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);
        title += " ";

        title += mTitleCalendar.get(Calendar.DAY_OF_MONTH);
        title += ", '";

        title += mTitleCalendar.get(Calendar.YEAR) % 100;

        mIsTitleDate = true;
        return title;
    }

    private void updateTitle(String title) {
        String phrase = mAnalyzer.getPhraseIfExistsInUserFilter();
        if (phrase != null) {
            phrase = phrase.toUpperCase() + " : " + title;
          title = phrase;
        }
        mActBar.setTitle(title);
    }

    private void updateSubTitleAndTitleIfNecessary() {
        String subTitle = "";
        for (int index = 0; index < mPlaceList.size(); index++) {
            if (index != 0) {
                subTitle += ", ";
            }
            subTitle += mPlaceList.get(index);
        }

        if (!mIsTitleDate)
            mActBar.setTitle(subTitle);
        else {
            mActBar.setSubtitle(subTitle);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLoadImagesInBackground != null && !mLoadImagesInBackground.isCancelled()) {
        }
        mHandler.removeMessages(SELECT_ALL_ITEMS);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLoadImagesInBackground != null && !mLoadImagesInBackground.isCancelled()) {
            mLoadImagesInBackground.cancel(true);
        }
    }

    @Override
    public void onDestroy() {
       if (mLoadImagesInBackground != null && !mLoadImagesInBackground.isCancelled()) {
           mLoadImagesInBackground.cancel(true);
       }
       //mImageAdapter.clearCache();
       super.onDestroy();
    }

     // Our handler for received Intents. This will be called whenever an Intent
     // with an action named "custom-event-name" is broadcasted.
     /*private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
         // Get extra data included in the Intent
         String message = intent.getStringExtra("message");
         Log.d("receiver", "Got message: " + message);
       }
     };*/
    /**
     * Setup the grid view.
     */
    private void setupViews() {
        mDisplayImages = (GridView) findViewById(R.id.gridview);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDisplayImages.setBackgroundColor(Color.DKGRAY);
        mDisplayImages.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mDisplayImages.setMultiChoiceModeListener(new MultiChoiceModeListener());
        mDisplayImages.setDrawSelectorOnTop(true);
        //setupTextSwitcher();
        //mDisplayImages.setNumColumns(metrics.widthPixels/200);
        //mDisplayImages.setNumColumns(3);
        //mDisplayImages.setClipToPadding(false);
        // Make GridView use your custom selector drawable
        //mDisplayImages.setSelector(getResources().getDrawable(R.drawable.selector_grid));
        //mDisplayImages.setVerticalSpacing(1);
        //mDisplayImages.setHorizontalSpacing(1);
        //mDisplayImages.setOnItemClickListener(DisplayViewsExample.this);
        mDisplayImages.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent,
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

        mDisplayImages.setOnItemLongClickListener(new OnItemLongClickListener() {
            //this listener should show the context menu for a long click on the gridview.
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                   //return parent.showContextMenuForChild(v);
                Toast.makeText(getBaseContext(),
                        "pic" + (position + 1) + " selected - LONG KEY PRESS",
                        Toast.LENGTH_SHORT).show();
                mState = SelectState.CHERRY_PICK;
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
        //AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

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

            new LoadImagesInBackGround(this.getApplication(), data).execute();
            mImageAdapter = new GridImageAdapter(this.getApplication());
            mDisplayImages.setAdapter(mImageAdapter);
        } else {
            //imagePath = imageUri.getPath();
        }
        //setupImageView();
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    void setupCursor(Cursor cur) {
        if (cur.moveToLast() && !cur.isClosed()) {
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
        if (mShowGrid) {
            getMenuInflater().inflate(R.menu.menu_display_view, menu);
            MenuItem item = menu.findItem(R.id.menu_item_pick_all);
            item.setVisible(true);
        }
        if (mShowWarningMenuItem != null) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            MenuItem item = menu.findItem(R.id.menu_item_info);
            item.setVisible(true);
        }
        return true;
    }

    /*
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_pick_all:
                mState = SelectState.ALL;
                selectAll();
                mState = SelectState.ALL_DONE;
                mHandler.removeMessages(SELECT_ALL_ITEMS);
                Message mesg = new Message();
                mesg.what = SELECT_ALL_ITEMS;
                mHandler.sendMessageDelayed(mesg, 100);
                return true;
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_item_info:
               DialogFragment newFragment = AlertDialogFragment.newInstance(
                       "STATUS", mShowWarningMenuItem);
               newFragment.show(getFragmentManager(), "dialog");
                    break;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mDisplayImages.setNumColumns(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2);
        super.onConfigurationChanged(newConfig);
    }

    private void selectAll() {
        for(int i=0; i < mImageAdapter.getCount(); i++) {
            mState = SelectState.ALL_INPROGRESS;
            mDisplayImages.setItemChecked(i, true);
        }
        return;
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
         if (mShareActionProvider != null) {
              mShareActionProvider.setShareIntent(shareIntent);
         }
    }

    private Intent createCheckedItemsIntent() {
        int selectCount = mDisplayImages.getCheckedItemCount();
        Log.d(TAG, "Selected Items " + selectCount);
        if (selectCount > 0) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            SparseBooleanArray checkedItems = mDisplayImages.getCheckedItemPositions();
            if (checkedItems != null) {
                mImageUris.clear();
                for (int index = 0; index < checkedItems.size(); index++) {
                    int position = checkedItems.keyAt(index);
                    if(position <= mList.size() && checkedItems.valueAt(index)) {
                        Uri imageUri = Uri.parse("file://" + mList.get(position));
                        mImageUris.add(imageUri);
                    }
                }
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mImageUris);
                shareIntent.setType("image/jpeg");
                return shareIntent;
            }
        }
        return null;
    }

    public class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked = false;

        public CheckableLayout(Context mContext) {
            super(mContext);
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
            if (checked) {
              setBackground(getResources().getDrawable(R.drawable.bggrid));
            } else {
                setBackground(null);
            }
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
            String state = null;
            if ((mState == SelectState.ALL) || (mState == SelectState.ALL_INPROGRESS))
                state = "Multi-Select ";
            else
                state = "Cherry-Pick ";
            mode.setTitle(state + "Mode");
            //mode.setSubtitle("1 Picture picked");
            //MenuInflater inflater = getMenuInflater();
            //inflater.inflate(R.menu.contextual_actions, menu);

            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_select_mode, menu);
            // Locate MenuItem with ShareActionProvider
            mItem = menu.findItem(R.id.menu_item_share);
            mItem.setEnabled(true);
            // Fetch and store ShareActionProvider
            mShareActionProvider = (ShareActionProvider) mItem.getActionProvider();
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            //mState = SelectState.CHERRY_PICK;
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        public void onDestroyActionMode(ActionMode mode) {
            setShareIntent(null);
            mItem.setEnabled(false);
            mState = SelectState.NONE;
            mImageUris.clear();
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position,
                long id, boolean checked) {
            int selectCount = mDisplayImages.getCheckedItemCount();
            String operation = (mState == SelectState.ALL) ? "selected" : "picked";
            switch (selectCount) {
                case 1:
                    mode.setSubtitle("1 " + operation);
                    break;
                default:
                    mode.setSubtitle("" + selectCount + " " + operation);
                    break;
            }

            if(mList.size() >= position && mState != SelectState.ALL_INPROGRESS) {
                Uri imageUri = Uri.parse("file://" + mList.get(position));
                if (checked && !mImageUris.contains(imageUri))
                   mImageUris.add(imageUri);
                else
                   mImageUris.remove(imageUri);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mImageUris);
                shareIntent.setType("image/jpeg");
                if (mShareActionProvider != null) {
                    mShareActionProvider.setShareIntent(shareIntent);
               }
            }
        }
    }
    /**
     * Adapter for our image files.
     *
     */
    class GridImageAdapter extends BaseAdapter {
        private Context mContext;
        private Resources mResources = null;
        public void addBitmapToCache(String data, BitmapDrawable value) {
            //BEGIN_INCLUDE(add_bitmap_to_cache)
            if (data == null || value == null) {
                return;
            }

            // Add to memory cache
            if (mMemoryCache != null) {
                if (RecyclingBitmapDrawable.class.isInstance(value)) {
                    // The removed entry is a recycling drawable, so notify it
                    // that it has been added into the memory cache
                    ((RecyclingBitmapDrawable) value).setIsCached(true, 0);
                }
                if (getBitmapFromMemCache(data) == null)
                    mMemoryCache.put(data, value);
            }
        }

        public BitmapDrawable getBitmapFromMemCache(String data) {
            BitmapDrawable memValue = null;
            if (mMemoryCache != null) {
                memValue = mMemoryCache.get(data);
            }

            return memValue;
        }

        public void clearCache() {
            if (mMemoryCache != null) {
                mMemoryCache.evictAll();
                Log.d(TAG, "Memory cache cleared");
            }
        }

        public GridImageAdapter(Application application) {
            super();
            mContext = application;
            mResources = mContext.getResources();
            mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
                protected int sizeOf(String key, BitmapDrawable value) {
                    final int bitmapSize = getBitmapSize(value) / 1024;
                    return bitmapSize == 0 ? 1 : bitmapSize;
                }


                protected void entryRemoved( boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue ) {
                    Log.d(TAG, "Entry Removed with key " + key + " evicted : " + evicted);
                    if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
                        // The removed entry is a recycling drawable, so notify it
                        // that it has been removed from the memory cache
                        ((RecyclingBitmapDrawable) oldValue).setIsCached(false, 1);
                    }
                  }

            };
        }

        public void addPhoto(Bitmap photo) {
            photos.add(photo);
        }

        public void addLRUPhoto(Bitmap photo) {
            int val = mMemoryCache.putCount();
            BitmapDrawable drawable = null;
            drawable = new RecyclingBitmapDrawable(mResources, photo);
            addBitmapToCache(val+"", drawable);
        }

        public int getCount() {
            //return photos.size();
            return mMemoryCache.putCount();
        }

        public Object getItem(int position) {
            //return photos.get(position);
            return null; //mMemoryCache.get(position+"");
        }

        public long getItemId(int position) {
            return 0; //position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView ;
            //CheckableLayout l;
            if (convertView == null) {
                //l = new CheckableLayout(mContext);
                imageView = new RecyclingImageView(mContext);
                //imageView.setLayoutParams(new GridView.LayoutParams(240, 240));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                //imageView = new ImageView(mContext);
                //l.addView(imageView);
            } else {
                //l = (CheckableLayout) convertView;
                //imageView = (RecyclingImageView) l.getChildAt(0);
                imageView = (ImageView) convertView;
            }

            BitmapDrawable drawable = getBitmapFromMemCache(position+"");
            if (drawable!= null && !drawable.getBitmap().isRecycled()) {
               imageView.setImageDrawable(drawable);
               imageView.setPadding(4, 4, 4, 4);
            }
            if (drawable == null) {
                    BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                    task.execute(position+"");

            }
             return imageView;
        }
    }

    /**
     * Add image(s) to the grid view adapter.
     *
     * @param value Array of Bitmap references
     */
    private void addGridImage(Bitmap... value) {
        for (Bitmap image : value) {
            //mImageAdapter.addPhoto(image);
            mImageAdapter.addLRUPhoto(image);
        }
        mImageAdapter.notifyDataSetChanged();
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

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>{
           private final WeakReference<ImageView> imageViewReference;
           public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
           }

           @Override
           protected Bitmap doInBackground(String... params) {
               int index = mList.indexOf(String.valueOf(params[0]));
               if (index != -1 ) return null;
            final Bitmap bitmap = getPicture(mList.get(index));
            mImageAdapter.addLRUPhoto(bitmap);
            return bitmap;
           }

           @Override
           protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
             final ImageView imageView = (ImageView)imageViewReference.get();
             if (imageView != null) {
              imageView.setImageBitmap(bitmap);
             // mDisplayImages.
             }
            }
           }
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
        //mDisplay.getMetrics(mOutMetrics);
        float dpHeight = mOutMetrics.heightPixels / mDensity;
        float dpWidth  = mOutMetrics.widthPixels / mDensity;
        int width=(int) (dpWidth);
        int height=(int) (dpHeight);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ) {
            width = height;
        }
        if (intf.hasThumbnail() ) {
           byte[] thumbnail = intf.getThumbnail();
           BitmapFactory.Options options = new BitmapFactory.Options();
           options.inJustDecodeBounds = true;
           BitmapFactory.decodeByteArray(thumbnail,0,thumbnail.length,options);

           // Calculate inSampleSize
           options.inSampleSize = calculateInSampleSize(options, width, width);
           // Decode bitmap with inSampleSize set
           options.inJustDecodeBounds = false;
           bitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length, options);
           if (bitmap != null) {
               newBitmap = Bitmap.createScaledBitmap(bitmap, width, width, true);
               if (newBitmap!= bitmap){
                   bitmap.recycle();
                   bitmap = null;
               }
               if (newBitmap != null) {
                   return newBitmap;
               }
           }
           return bitmap;
        } else  {
           Uri imageUri = null;
            try {
               // First decode with inJustDecodeBounds=true to check dimensions
               final BitmapFactory.Options options = new BitmapFactory.Options();
               options.inJustDecodeBounds = true;
               imageUri = Uri.parse("file://" + path);
               BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null,options);

               // Calculate inSampleSize
               options.inSampleSize = calculateInSampleSize(options, width, width);

               // Decode bitmap with inSampleSize set
               options.inJustDecodeBounds = false;
               bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null,options);
               //bitmap = decodeSampledBitmapFromFile(path,width,width);

               if (bitmap != null) {
                   newBitmap = Bitmap.createScaledBitmap(bitmap, width, width, true);
                   if (newBitmap!= bitmap){
                       bitmap.recycle();
                       bitmap = null;
                   }
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

    public Bitmap decodeSampledBitmapFromFile(String filename,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // If we're running on Honeycomb or newer, try to use inBitmap
        if (Utils.hasHoneycomb()) {
            //addInBitmapOptions(options, cache);
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }

private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
    }

    return inSampleSize;
}
    class LoadImagesInBackGround extends AsyncTask<Object, Bitmap, Object> {

        private Cursor mCursor;
        private Context mContext;

        LoadImagesInBackGround(Application application, Cursor cur) {
            mCursor = cur;
            mContext = application;
        }
        /**
         * Load images in the background, and display each image on the screen.
         */
        protected String doInBackground(Object... params) {
           // Again check for isCancelled() , there could be potential race conditions here
           // when task is cancelled. The thread that just got cancelled (as a result of configuration changes)
           // still latches onto old cursor object
            do {
                if (mCursor.isClosed() || isCancelled())
                    return null;
                Bitmap bmp = getImgBasedOnUserFilter(mCursor);
                if (bmp != null) {
                    //for (int i = 0; i < 50; i++)
                        publishProgress(bmp);
                }
            } while (!mCursor.isClosed() && mCursor.moveToPrevious() && !isCancelled());
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
            //mLoadImagesInBackground = this;
        }
        @Override
        protected void onPostExecute(Object result) {
            if (mList.size() == 0) {
                // No results found! Show the indication to user
                View v = mViewSwitcher.findViewById(R.id.displayViewProgressBar);
                v.setVisibility(View.GONE);
                TextView txtView = (TextView) mViewSwitcher.findViewById(R.id.displayViewProgressTextView);
                txtView.setText("Sorry! No results found. Try again ...");

                mShowWarningMenuItem = "Zilch! Please consider the following tips in order to see the desired results \n\n" +
                        "TIP 1: If you are trying to filter your pictures by 'places' / 'place' where the pictures have been taken, " +
                        "Please ensure that your 'camera' pictures were GeoTagged (at the time of taking the picture(s)) in order to successfully search by 'places' \n\n " +
                         "TIP 2: Please ensure that 'voice command to text' translation of dates and places has happened properly \n\n"+
                         "TIP 3: When the voice filters are applied in the form of 'dates' / 'date ranges' / 'place' , Please ensure that pictures have the respective meta-data (timestamp , place) associated with it.\n\n" +
                         "TIP 4: Note that filters are ONLY applied to 'camera' pictures in the photo Albums.";
            }
            setProgressBarIndeterminateVisibility(false);
            if (mShowWarningMenuItem != null) {
                invalidateOptionsMenu();
            }
        }

        @Override
        protected void onCancelled(Object result) {

        }



       /* private Bitmap decodeSampledBitmapFromResource(int resId,
                int reqWidth, int reqHeight) {

            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decode(res, resId, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeResource(res, resId, options);
        }*/

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
                if (null != mPairRange) {
                   mRangeMgr.printDateAndTime(mCalendar);
                   if ((dateinMilliSec >= mPairRange.first) && (dateinMilliSec <= mPairRange.second)) {
                       //if (DEBUG) Log.d(TAG, "****** Added ********* ");
                       mRangeMgr.printDateAndTime(mCalendar);
                       //if (DEBUG) Log.d(TAG, "****** Added ********* ");
                       dateRangeMatchFound= 0;
                       added = true;//addtoListIfNotFound(path);
                   } else {
                       dateRangeMatchFound= 1;
                       added = false;
                   }
                }
                // Following block to 'enable / disable search by places'
                // TBD All of the parse logic should eventually be wrapped into UserFileterAnalyzer
                if (mSupportGeoCoder) { // && (mUserFilter.toLowerCase().contains(mPlaceFilter.toLowerCase()))) {
                   GeoDecoder geoDecoder = null;
                   SyncState dbState = mDbHelper.getState();
                   int matchState = mMatchState;
                   if (cur.isClosed()) break;
                   Integer currentId = cur.getInt(id);
                   if (dbState != DataBaseManager.SyncState.SYNC_STATE_COMPLETED) {
                       matchState = mAnalyzer.getMatchState();
                       mUserFilterContainsAPLACES = mDbHelper.retreiveAllPlacesFromStringIfExists(mUserFilter);
                   }

                   boolean alsoMatchCity = false;
                   if (mUserFilterContainsAPLACES != null && mUserFilterContainsAPLACES.size() > 0 &&
                       mDbHelper.isAtleastSingleValuePresentInList(mUserFilterContainsAPLACES)) {
                       alsoMatchCity = (UserFilterAnalyzer.MATCH_STATE_DATES_AND_PLACE_EXACT == matchState) || (UserFilterAnalyzer.MATCH_STATE_PHRASE_AND_PLACE_EXACT == matchState);
                       if (added && alsoMatchCity) {
                           added = false;
                       }
                   }
                   List<Address> address = null;
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
                     && (attrLONGITUDE_REF !=null)) {
                       String placeFound = "";
                       String countryFound = "";
                       String adminAreaFound = "";
                       ArrayList<String> placeList;
                       // It has some valid values
                       // Try to read from Cache / db

                       do {
                               placeList = mDbHelper.getPlace(currentId);
                               if (placeList == null || placeList.size() == 0) {
                                   break;
                               }
                               placeFound  = placeList.get(0);
                               if (placeList != null && placeList.size() < 2) {
                                  break;
                               }
                               countryFound  = placeList.get(1);
                               if (placeList != null && placeList.size() < 3) {
                                  break;
                               }
                               adminAreaFound  = placeList.get(2);
                       } while (false);

                       if (mDbHelper.isAtleastSingleValuePresentInList(placeList) == false) {
                           // Place not found in cache or db ,but it has a valid GPS cod-ordinates
                           // Try and fallback on GeoCoder API to retrieve the place.
                       } else if((mUserFilter.toLowerCase().contains(placeFound.toLowerCase())    ||
                                 (mUserFilter.toLowerCase().contains(countryFound.toLowerCase())) ||
                                 (mUserFilter.toLowerCase().contains(adminAreaFound.toLowerCase())))) {
                           // Wow... we have the place found either in the cache or db..
                           int index = mPlaceList.indexOf(placeFound.toUpperCase());
                           if (index == -1 && mUserFilter.toLowerCase().contains(placeFound.toLowerCase())) {
                               mPlaceList.add(placeFound.toUpperCase());
                               mUpdateSubTitleRequired |= IsPlace;
                           }
                           index = mPlaceList.indexOf(countryFound.toUpperCase());
                           if (index == -1 && mUserFilter.toLowerCase().contains(countryFound.toLowerCase())) {
                               mPlaceList.add(countryFound.toUpperCase());
                               mUpdateSubTitleRequired |= IsCountry;
                           }
                           index = mPlaceList.indexOf(adminAreaFound.toUpperCase());
                           if (index == -1 && mUserFilter.toLowerCase().contains(adminAreaFound.toLowerCase())) {
                               mPlaceList.add(adminAreaFound.toUpperCase());
                               mUpdateSubTitleRequired |= IsAdminArea;
                           }
                           alsoMatchCity = (UserFilterAnalyzer.MATCH_STATE_DATES_AND_PLACE_EXACT == matchState) || (UserFilterAnalyzer.MATCH_STATE_PHRASE_AND_PLACE_EXACT == matchState);
                           if (dateRangeMatchFound != -1) {
                               // DateRange has been set
                               if ((dateRangeMatchFound == 0) && (alsoMatchCity)) {
                                   // Date Range Match found and Match city
                                   added = true;
                               } else if ((dateRangeMatchFound == 1) && (alsoMatchCity)) {
                                   // date range Match is false , and match city
                                   added = false;
                               } else {
                                   // Match city flag is false, but city matches anyways
                                   added = true;
                               }
                           } else {
                             // Date Range not set but Locality match succeed.
                               added = true;
                           }
                           if (mUpdateSubTitleRequired != 0) {
                               runOnUiThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       updateSubTitleAndTitleIfNecessary();
                                       mUpdateSubTitleRequired = 0;
                                   }
                               });
                           }
                           break;
                       } else {
                           // OK some place exists in cache / db but does not match with UserFilter

                           // At this point show a UI indication that something could have gone wrong

                           // 'Jan pictures in Timbuktu' . Here Timbuktu is as good a string. We do not
                           // have intelligence to treat this as a place. So all pictures in Jan will
                           // show up anyways ??? Not really I guess
                           if ((matchState == UserFilterAnalyzer.MATCH_STATE_DATES_AND_UNKNOWN_PLACE_EXACT) && added)
                               added = false;  // Some unknown place was asked to match.. Sorry User!
                           break;
                       }
                   } else {
                       if ((matchState == UserFilterAnalyzer.MATCH_STATE_DATES_AND_UNKNOWN_PLACE_EXACT) && added)
                           added = false;  // Some unknown place was asked to match.. Sorry User!
                       continue;
                   }

                   // Before we try to retrieve from Internet, check to see if there is active connection
                   if ((mConnectivityManager.getActiveNetworkInfo() == null) ||
                      !(mConnectivityManager.getActiveNetworkInfo().isConnectedOrConnecting())) {
                      // mConnectivityManager.getActiveNetworkInfo() being null happens in airplane mode I guess
                      Log.d(TAG,"Ooops No Connection.. Try Later");
                      mShowWarningMenuItem = "Warning: You may see Inconsistent / Incorrect Results. \n\n" +
                              "Probable Reason: Check your internet connection! It looks like there is no active data connection. ";
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
                   try {
                       address = geoDecoder.getAddress(mContext);
                   } catch (IOException e) {
                       // TODO Auto-generated catch block
                       //e.printStackTrace();
                       mShowWarningMenuItem = "Warning: You may see Inconsistent / Incorrect Results. \n\n" +
                                 "Probable Reason:  Geocoding / Reverse Geocoding Service was not available partially for a few / all pictures. As a result, such pictures cannot be displayed in the grid view. Please retry again later if you see incomplete results. \n\n" +
                                 "TIP: If the issue persists, (Though not ideal!) please consider rebooting the device. This issue is outside the scope of the application";
                   }
                   String locality = null;
                   String country = null;
                   String adminArea = null;
                   if (address!= null && address.size() > 0) {
                     locality = address.get(0).getLocality();
                     country = address.get(0).getCountryName();
                     adminArea = address.get(0).getAdminArea();
                   }
                   if (locality != null) {
                       // Try and insert to the d/b and cache
                       mDbHelper.updateRow(currentId, locality, country, adminArea);
                   }
                   if ((locality != null) && (0 == mAnalyzer.compareUserFilterForCity(locality))) {
                       alsoMatchCity = (UserFilterAnalyzer.MATCH_STATE_DATES_AND_PLACE_EXACT == matchState) || (UserFilterAnalyzer.MATCH_STATE_PHRASE_AND_PLACE_EXACT == matchState);
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
                       // This check is important, because if pic is already added as a result of previous 'filter match'
                       // If already added and place is not found in the User-filter, then retain it as added!
                       if (!added) {
                         // City doesn't exist
                         added = false ;
                       }
                   }
                } else {
                    if (WARN) Log.i(TAG, "Ooops! No results");
                }
            } while (false);

            if (added) {
                addtoListIfNotFound(path);
                if (mShowGrid == false && mList.size() == 1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Show the grid view and turn on indeterminate dialog in title region
                            mViewSwitcher.showNext();
                            mShowGrid = true;
                            setProgressBarIndeterminateVisibility(true);
                            invalidateOptionsMenu();
                        }
                    });
                }
                try {
                    return getPicture(path);
                } catch (OutOfMemoryError e) {
                    //Log.e("Map", "DisplayView - Out Of Memory Error " + e.getLocalizedMessage());
                    /*try {
                           android.os.Debug.dumpHprofData("/sdcard/dump.hprof");
                         }
                    catch (IOException e1) {
                           e1.printStackTrace();
                    }*/
                }

            }
            return null;
        } // End of function
    } // End of LoadImagesInBackGround
} // End of Main class
