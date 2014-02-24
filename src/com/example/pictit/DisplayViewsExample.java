package com.example.pictit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.R.color;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
public class DisplayViewsExample extends Activity implements LoaderCallbacks<Cursor>, LogUtils
{
    private String TAG = "Pickit/DisplayView";
    int bucketColumn = 0;

    int dateColumn = 0;

    int titleColumn = 0;

    int dataColumn = 0;
    int mCurrIndex = 0;

    private GridView mgridView;
    private int mGridCount = 0;
    Calendar mCalendar = Calendar.getInstance();

    // Set of Filters to compare against
    String[] mMonthNames = {"January", "february","March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    // Last WeekEnd
    String mLastWeekEnd = "Last Weekend";

    // Today
    String mToday = "Today";

    private ShareActionProvider mShareActionProvider;

    ArrayList<Uri> mImageUris = new ArrayList<Uri>();

    String mUserFilter;

    //Map<String, String> mMap = new HashMap<String, String>();
    ArrayList<String> mList = new ArrayList<String>();

    /**
     * Grid view holding the images.
     */
    private GridView displayImages;
    /**
     * Image adapter for the grid view.
     */
    private GridImageAdapter imageAdapter1;

    /**
     * Display used for getting the width of the screen.
     */
    private Display display;

    //private int[] mCheck = new int(32);


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.displaygridview);
        getLoaderManager().initLoader(0, null, this);

        Intent intent = getIntent();
        String filter = intent.getExtras().getString("filter");
        mUserFilter = filter;

        setProgressBarIndeterminateVisibility(true);

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

    }

    /**
     * Setup the grid view.
     */
    private void setupViews() {
        displayImages = (GridView) findViewById(R.id.gridview);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        //displayImages.setNumColumns(metrics.widthPixels/200);
        displayImages.setNumColumns(3);
        //displayImages.setClipToPadding(false);
        displayImages.setBackgroundColor(Color.DKGRAY);
        displayImages.setChoiceMode(displayImages.CHOICE_MODE_MULTIPLE);
        //displayImages.setVerticalSpacing(1);
        //displayImages.setHorizontalSpacing(1);
        //displayImages.setOnItemClickListener(DisplayViewsExample.this);
        displayImages.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView parent,
            View v, int position, long id)
            {
                v.setBackgroundColor(Color.RED);
                //v.setPressed(true);
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
        String test = images.toString();

        test = MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString();

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
            mGridCount = data.getCount();
            setupCursor(data);
            performQueryUsingUserFilter(data);
            new LoadImagesInBackGround().execute();
            //displayImages.setAdapter(new GridImageAdapter(this));

            imageAdapter1 = new GridImageAdapter(getApplicationContext());
            displayImages.setAdapter(imageAdapter1);
        } else {
            //imagePath = imageUri.getPath();
        }
        //setupImageView();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return null;

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    void setupCursor(Cursor cur) {
        if (cur.moveToFirst()) {
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

    boolean addtoListIfNotFound(String path) {
       if (!mList.contains(path)) {
            mList.add(path);
            return true;
        }

        if (DEBUG) Log.d(TAG, "Path : " + path + " already found!");
        return false;
    }

    void performQueryUsingUserFilter(Cursor cur) {
        do {
            boolean added = false;
            String date = cur.getString(dateColumn);
            String path = cur.getString(dataColumn);

            long dateinMilliSec = Long.parseLong(date);
            mCalendar.setTimeInMillis(dateinMilliSec);
            int monthOfYear = mCalendar.get(Calendar.MONTH);
            //if (monthOfYear >= 0 && monthOfYear <= 11 ) {
                if(mUserFilter.toLowerCase().contains(mMonthNames[monthOfYear].toLowerCase())) {
                    added = addtoListIfNotFound(path);
                }

                if (mUserFilter.toLowerCase().contains(mLastWeekEnd.toLowerCase())) {
                    DateRangeManager range = new DateRangeManager();
                    Pair<Long, Long> p = range.getLastWeekEnd();

                    if (DEBUG) {
                      String date2 = "" + mCalendar.get(Calendar.DAY_OF_MONTH) + ":" + mCalendar.get(Calendar.MONTH) + ":" + mCalendar.get(Calendar.YEAR);
                      String time2 = "" + mCalendar.get(Calendar.HOUR_OF_DAY) + ":" + mCalendar.get(Calendar.MINUTE) + ":" + mCalendar.get(Calendar.SECOND);

                      Log.d(TAG , date2 + " " + time2);
                    }

                    if ((dateinMilliSec >= p.first) && (dateinMilliSec <= p.second)) {
                        added = addtoListIfNotFound(path);
                    }

                } if (mUserFilter.toLowerCase().contains(mToday.toLowerCase())) {
                    // TBD: Need to Refactor this code with the above
                    DateRangeManager range = new DateRangeManager();
                    Pair<Long, Long> p = range.getToday();

                    if (DEBUG) {
                      String date2 = "" + mCalendar.get(Calendar.DAY_OF_MONTH) + ":" + mCalendar.get(Calendar.MONTH) + ":" + mCalendar.get(Calendar.YEAR);
                      String time2 = "" + mCalendar.get(Calendar.HOUR_OF_DAY) + ":" + mCalendar.get(Calendar.MINUTE) + ":" + mCalendar.get(Calendar.SECOND);

                      Log.d(TAG , date2 + " " + time2);
                    }

                    if ((dateinMilliSec >= p.first) && (dateinMilliSec <= p.second)) {
                        added = addtoListIfNotFound(path);
                    }
                } else {
                    if (WARN) Log.i(TAG, "Ooops! No results");
                }

                ExifInterface intf = null;
                //String data = null;

                /*{

                   GeoDecoder geoDecoder = null;
                   String city = null;
                   try {
                           geoDecoder = new GeoDecoder(new ExifInterface(path));
                           if (!geoDecoder.isValid()) continue;
                   } catch (IOException e) {
                           // TODO Auto-generated catch block
                           e.printStackTrace();
                   }
                   city = geoDecoder.getAddress(this).get(0).getLocality();
                       if(mUserFilter.replace(" ", "").contains(city.replace(" ","")) && !added) {
                           mList.add(path);
                           //Uri imageUri = Uri.parse(path);
                           //mImageUris.add(imageUri);
                    }
                }*/

            //}
        } while (cur.moveToNext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        setShareIntent(createShareIntent());
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
        private Context mContext ;
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

        /*public View getView(int position, View convertView, ViewGroup parent) {
            final ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setLayoutParams(new GridView.LayoutParams(260, 260));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(4, 4, 4, 4);
            imageView.setImageBitmap(photos.get(position));
            imageView.setBackgroundColor(Color.TRANSPARENT);
            return imageView;
        } */
        public View getView(int position, View convertView, ViewGroup parent) {
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
        }
    }

    Bitmap getNextPicture(String path) {

        ExifInterface intf = null;
        //String data = null;
        Bitmap bitmap = null;
        Bitmap newBitmap = null;

        try {
            intf = new ExifInterface(path);
        } catch(IOException e) {
            e.printStackTrace();
        }

        if(intf == null) {
            //File doesn't exist or isn't an image
        }

        String dateString = intf.getAttribute(ExifInterface.TAG_DATETIME);
           //Log.d("PATH : ", data);
           Log.d(TAG, dateString);
        if (intf.hasThumbnail()) {
               byte[] thumbnail = intf.getThumbnail();
               //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
               bitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
               if (bitmap != null) {
                   newBitmap = Bitmap.createScaledBitmap(bitmap, 240, 240, true);
                   bitmap.recycle();
                   if (newBitmap != null) {
                       return newBitmap;
                   }
               }
               //BitmapDrawable bmd = new BitmapDrawable(getResources(),bmpImg);
               return bitmap;
          } else {

           Uri imageUri = null; ;

           try {
               imageUri = Uri.parse("file://" + path);
               bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
               if (bitmap != null) {
                   newBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
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

    /**
     * Add image(s) to the grid view adapter.
     *
     * @param value Array of Bitmap references
     */
    private void addGridImage(Bitmap... value) {
        for (Bitmap image : value) {
            imageAdapter1.addPhoto(image);
            imageAdapter1.notifyDataSetChanged();
        }
    }

    class LoadImagesInBackGround extends AsyncTask<Object, Bitmap, Object> {

        /**
         * Load images in the background, and display each image on the screen.
         */
        protected String doInBackground(Object... params) {
            setProgressBarIndeterminateVisibility(true);
            for  ( ; mCurrIndex < mList.size() ;mCurrIndex++) {
                String path = mList.get(mCurrIndex);
                Bitmap bmp = getNextPicture(path);
                publishProgress(bmp);
            }
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
        protected void onPostExecute(Object result) {
            setProgressBarIndeterminateVisibility(false);
        }
    }

}
