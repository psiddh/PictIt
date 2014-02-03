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
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
public class DisplayViewsExample extends Activity  implements LoaderCallbacks<Cursor>
{
    int bucketColumn = 0;

    int dateColumn = 0;

    int titleColumn = 0;

    int dataColumn = 0;
    int mCurrIndex = 0;
    private GridView mgridView;
    private int mGridCount = 0;
    Calendar mCalendar = Calendar.getInstance();
    String[] mMonthNames = {"January", "february","March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    private ShareActionProvider mShareActionProvider;

    ArrayList<Uri> mImageUris = new ArrayList<Uri>();

    String mUserFilter;

    //Map<String, String> mMap = new HashMap<String, String>();
    ArrayList<String> mList = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaygridview);
        getLoaderManager().initLoader(0, null, this);

        Intent intent = getIntent();
        String filter = intent.getExtras().getString("filter");
        mUserFilter = filter;

        mgridView = (GridView) findViewById(R.id.gridview);
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
        String imagePath = "";
        if (data != null) {
            mGridCount = data.getCount();
            mgridView.setAdapter(new ImageAdapter(this,data));
        } else {
            //imagePath = imageUri.getPath();
        }
        //setupImageView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }


    /*public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

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
     * (non-Javadoc)
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

    public class ImageAdapter extends BaseAdapter
    {
        private Context context;
        private Cursor  cur;

        public ImageAdapter(Context c, Cursor data)
        {
            context = c;
            cur = data;
            setupCursor();
            performQueryUsingUserFilter();
        }

        //---returns the number of images---
        public int getCount() {
            return mList.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        //---returns an ImageView view---
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(200, 200));
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                imageView.setPadding(5, 5, 5, 5);
                //imageView.setImageDrawable(getNextPic());
                Bitmap bmp = getNextPic();
                if (bmp != null)
                  imageView.setImageBitmap(bmp);
            } else {
                imageView = (ImageView) convertView;
            }

            return imageView;
        }

        void setupCursor() {
            if (cur.moveToFirst()) {
                bucketColumn = cur.getColumnIndex(
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

                dateColumn = cur.getColumnIndex(
                    MediaStore.Images.Media.DATE_TAKEN);

                titleColumn = cur.getColumnIndex(
                        MediaStore.Images.Media.TITLE);

                dataColumn = cur.getColumnIndex(
                        MediaStore.Images.Media.DATA);

                Log.d("ListingImages", cur.getPosition() + " : " + dateColumn );
            }
        }

        void performQueryUsingUserFilter() {


            do {
                boolean added = false;
                String date = cur.getString(dateColumn);
                String path = cur.getString(dataColumn);

                //Uri uri = Uri.fromFile(getFileStreamPath(path));
                //mImageUris.add(path);


                long dateinMilliSec = Long.parseLong(date);
                mCalendar.setTimeInMillis(dateinMilliSec);
                int monthOfYear = mCalendar.get(Calendar.MONTH);
                //if (monthOfYear >= 0 && monthOfYear <= 11 ) {
                    if(mUserFilter.toLowerCase().contains(mMonthNames[monthOfYear].toLowerCase())) {
                        mList.add(path);
                        //Uri imageUri = Uri.parse(path);
                        //mImageUris.add(imageUri);
                        File photo = new File(android.os.Environment.DIRECTORY_PICTURES
                                , path);  // .getExternalStorageDirectory()
                        //mImageUris.add(Uri.fromFile(photo));
                        added = true;
                        //mMap.put(path, mMonthNames[monthOfYear]);
                    } else {
                        //Log.i("monthOfYear  : "," mMonthNames[monthOfYear]");
                    }

                    ExifInterface intf = null;
                    //String data = null;

                    {

                       GeoDecoder geoDecoder = null;
                       String city = null;
                       try {
                               geoDecoder = new GeoDecoder(new ExifInterface(path));
                               if (!geoDecoder.isValid()) continue;
                       } catch (IOException e) {
                               // TODO Auto-generated catch block
                               e.printStackTrace();
                       }
                       /*city = geoDecoder.getAddress(context).get(0).getLocality();
                           if(mUserFilter.replace(" ", "").contains(city.replace(" ","")) && !added) {
                               mList.add(path);
                               Uri imageUri = Uri.parse(path);
                               //mImageUris.add(imageUri);
                        }*/
                    }

                //}
            } while (cur.moveToNext());
            Log.d("ListingImages","");

        }

        Bitmap getNextPic() {
            if ( mCurrIndex < mList.size() ) {
                String path = mList.get(mCurrIndex);
                mCurrIndex++;

                do {
                    ExifInterface intf = null;
                    //String data = null;

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
                       Log.d("dateString : ", dateString);
                    if (intf.hasThumbnail()) {
                           byte[] thumbnail = intf.getThumbnail();
                           //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                           Bitmap bmpImg = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
                           BitmapDrawable bmd = new BitmapDrawable(getResources(),bmpImg);
                           return bmpImg;
                       }
                } while (false);
            }

            return null;
        }

        /* String getCurrentImgPath() {
            String bucket;
            String date;
            String title;
            String data;
            if (cur == null) return null;
            // Get the field values
            bucket = cur.getString(bucketColumn);
            date = cur.getString(dateColumn);
            title = cur.getString(titleColumn);
            data = cur.getString(dataColumn);

            // Do something with the values.
            Log.i("ListingImages", " bucket=" + bucket
                   + "  date_taken=" + date + "title = " + title + "data = " + data);

            return data;
        }

        Bitmap getNextPic() {
            if (!cur.moveToNext()) return null;

            Log.d("ListingImages"," query count= "+cur.getCount());
            do {
                ExifInterface intf = null;
                String data = null;

                try {
                    data = getCurrentImgPath();
                    if (null == data) return null;
                    intf = new ExifInterface(data);
                } catch(IOException e) {
                    e.printStackTrace();local
                }

                if(intf == null) {
                    //File doesn't exist or isn't an image
                }

                String dateString = intf.getAttribute(ExifInterface.TAG_DATETIME);
                   Log.d("PATH : ", data);
                   Log.d("dateString : ", dateString);
                if (intf.hasThumbnail()) {
                       byte[] thumbnail = intf.getThumbnail();
                       //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                       Bitmap bmpImg = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
                       BitmapDrawable bmd = new BitmapDrawable(getResources(),bmpImg);
                       return bmpImg;
                   }
            } while (false);

            return null;
        }*/
    }

}
