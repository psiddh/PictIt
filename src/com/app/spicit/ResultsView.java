package com.app.spicit;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.app.spicit.AlertDialogFrag.AlertDialogFragment;
import com.app.spicit.DataBaseManager.SyncState;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ViewAnimator;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;
import android.app.ActivityManager;
import android.view.animation.Animation.AnimationListener;



public class ResultsView extends Activity implements LoaderCallbacks<Cursor>, LogUtils {
    private String TAG = "SpickIt> ResultsView";
    // CPU & connectivity data intensive operation guarded by this flag
    private boolean mSupportGeoCoder = true;

    int id = -1;
    int bucketColumn = 0;
    int dateColumn = 0;
    int titleColumn = 0;
    int dataColumn = 0;
    int mCurrIndex = 0;
    int mGridCount = 0;
    int mScrollTo = 0;

    Calendar mCalendar = Calendar.getInstance((Locale.getDefault()));
    private ShareActionProvider mShareActionProvider;

    String mUserFilter = "";
    
    int[] pictureIds;
    
    boolean bMatchPictureIDsOnly = false;

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
    ResultViewCallback mCallback;
    
    private EditText mEditTextCustomTag;
    
    ImageButton mImgButtonAccept;
    ImageButton mImgButtonCancel;

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
    private UserFilterAnalyzer mAnalyzer;

    private AsyncTask<Object, Bitmap, Object> mLoadImagesInBackground = null;

    ConnectivityManager mConnectivityManager;

    private DataBaseManager mDbHelper;
    private Drawable mDrawable; 

    MenuItem mItem;

    Display mDisplay;
    DisplayMetrics mOutMetrics;
    Bitmap mPlaceHolderBitmap = null;
    //float mDensity;

    ViewSwitcher mViewSwitcher;
    boolean mShowGrid = false;

    private String mShowWarningMenuItem = null;
    private String mShowToastMsg = "Ooops! You may see incorrect or inconsistent results. For more details click 'info' menu item";
    private boolean bShowToastMsg = true;
    private boolean mOOMAlready = false;
    
    private static void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.d("Uncaught Exception","Uncaught Exception detected in thread {} " + t + " " + e);
                }
            });
        } catch (SecurityException e) {
        	Log.d("Uncaught Exception","Could not set the Default Uncaught Exception Handler", e);
        }
    }

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
     
     public abstract class DoubleClickOnItemListener implements OnItemClickListener {

    	    private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds
    	    private static final int SEND_SINGLE_CLICK_EVT = 1;//milliseconds

    	    long lastClickTime = 0;
    	    private Handler handler = new Handler() {
    	        public void handleMessage (Message msg) {
    	            switch (msg.what) {
    	                case SEND_SINGLE_CLICK_EVT:
    	                	Bundle b = msg.getData();
    	                    int position = b.getInt("position");
    	                    onSingleItemClick(position);
    	                    break;
    	                default:
    	                	removeMessages(SEND_SINGLE_CLICK_EVT);
    	                    break;
    	            }
    	        }
    	    };
    	    
            public void onItemClick(AdapterView<?> parent,
                                    View v, int position, long id) {
    	        long clickTime = System.currentTimeMillis();
    	        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
    	        	onDoubleItemClick(parent,v,position, id);
    	        	handler.removeMessages(SEND_SINGLE_CLICK_EVT);
    	        } else {
    	        	handler.removeMessages(SEND_SINGLE_CLICK_EVT);
                    Message msg = new Message();
                    Bundle b = new Bundle();
                    b.putInt("position",position);
                    msg.setData(b);
                    msg.what = SEND_SINGLE_CLICK_EVT;
                    handler.sendMessageDelayed(msg, DOUBLE_CLICK_TIME_DELTA);
    	        }
    	        lastClickTime = clickTime;
            }

    	    public abstract void onSingleItemClick(int position);
    	    public abstract void onDoubleItemClick(AdapterView<?> parent,
    	                                            View v, int position, long id);
    }

     public static int getBitmapSize(BitmapDrawable value) {
         Bitmap bitmap = value.getBitmap();

         // From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
         // larger than bitmap byte count.
         if (Utils.hasKitKat() &&  bitmap != null && !bitmap.isRecycled()) {
            // return bitmap.getAllocationByteCount();
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
        cacheSize = 10 * 1024 * 1024;//(1024 *  1024 * memClass) / 8;

        Intent intent = getIntent();
        String filter = intent.getExtras().getString("filter");
        if (filter != null)
            mUserFilter = filter;
        pictureIds = intent.getIntArrayExtra("PictIDArray");
        if((pictureIds != null && pictureIds.length > 0) && mUserFilter == "") {
        	bMatchPictureIDsOnly = true;
        }
        mAnalyzer = new UserFilterAnalyzer(this, mUserFilter);
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
        //mDensity = this.getResources().getDisplayMetrics().density;
        mViewSwitcher.setBackgroundColor(Color.DKGRAY);

        setupViews();
        
        mEditTextCustomTag = (EditText) findViewById(R.id.editTextCustomTag);
        mImgButtonAccept = (ImageButton) findViewById(R.id.customTagAccept);
        mImgButtonCancel = (ImageButton) findViewById(R.id.customTagCancel);
        
        mEditTextCustomTag.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //mHandler.removeMessages(SHOW_GRID_AFTER_DELAY);
                //mProgress.setVisibility(View.GONE);
                return false;
           }
        });
        mEditTextCustomTag.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){
                if(s.length() > 0) {
                	mImgButtonAccept.setVisibility(View.VISIBLE);
                	mImgButtonCancel.setVisibility(View.VISIBLE);
                } else {
                	mImgButtonAccept.setVisibility(View.GONE);
                	mImgButtonCancel.setVisibility(View.GONE);
                }
            }
        });
        
        mImgButtonAccept.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Runnable runnable = new Runnable() {
        	        public void run() {     	
        	        	String customtag = mEditTextCustomTag.getText().toString();
                    	if (customtag == null || customtag == "")
                    		return;
                    	if (mImageUris.size() == 0)
                    		return;
                    	for (int i = 0; i < mImageUris.size(); i++) {
                    		ExifInterface intf = null;
                            String path = mImageUris.get(i).getPath();//mImageUris.get(i).toString();
                            
                            try {
                                intf = new ExifInterface(path);
                            } catch(IOException e) {
                                e.printStackTrace();
                            }

                            if(intf == null) {
                                return;
                            }
                            intf.setAttribute("UserTag", customtag);
                            
                            Log.d(TAG, "xxxx PATH : " + path + " customtag - " + customtag );
                            
                    	
                            try {
        						intf.saveAttributes();
        						//mDisplayImages.setChoiceMode(GridView.CHOICE_MODE_NONE);
        					} catch (IOException e) {
        						// TODO Auto-generated catch block
        						e.printStackTrace();
        					}
                    	}
        	        }
              };
              Thread mythread = new Thread(runnable);
        	  mythread.start();
            	/*String customtag = mEditTextCustomTag.getText().toString();
            	if (customtag == null || customtag == "")
            		return;
            	if (mList.size() == 0)
            		return;
            	for (int i = 0; i < mList.size(); i++) {
            		ExifInterface intf = null;
                    String path = mList.get(i).toString();
                    try {
                        intf = new ExifInterface(path);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }

                    if(intf == null) {
                        return;
                    }
                    intf.setAttribute("UserTag", customtag);
                    
            	
                    try {
						intf.saveAttributes();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}*/
            }
        });
        
        mImgButtonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }
        });

        if (mDbHelper.getState() == DataBaseManager.SyncState.SYNC_STATE_COMPLETED) {
            mUserFilterContainsAPLACES = mDbHelper.retreiveAllPlacesFromStringIfExists(mUserFilter);
        } else if (mDbHelper.getState() == DataBaseManager.SyncState.SYNC_STATE_INPROGRESS) {
            TextView txtView = (TextView) mViewSwitcher.findViewById(R.id.displayViewProgressTextView);
            txtView.setText("Please wait, Loading pictures may take a while! Background Sync is still in-progress");
        }
        
        mDrawable = getResources().getDrawable(R.drawable.bggrid);
        
        mImageAdapter = new GridImageAdapter(this);
        
        mPlaceHolderBitmap= BitmapFactory.decodeResource(this.getResources(),  R.drawable.empty_photo);
        setDefaultUncaughtExceptionHandler();
        
        mDisplayImages.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "OnScroll - firstVisibleItem : " + firstVisibleItem + " visibleItemCount : " + visibleItemCount + " totalItemCount :" + totalItemCount);
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
			}
        });
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
        
        mDisplayImages.setOnItemClickListener(new DoubleClickOnItemListener() {
			@Override
			public void onSingleItemClick(int position) {
				Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri imageUri = Uri.parse("file://" + mList.get(position));
                intent.setDataAndType(imageUri, "image/*");
                startActivity(intent);
			}

			@Override
			public void onDoubleItemClick(AdapterView<?> parent, final View v,
					                      int position, long id) {
				ExifInterface intf = null;
				if (mList.size() == 0 || mList == null)
					return;
                String path = mList.get(position);
                Log.d(TAG, "path " + path);
                if (path == null || path == "")
                	return;
                try {
                    intf = new ExifInterface(path);
                } catch(IOException e) {
                    e.printStackTrace();
                }

                if(intf == null) {
                    return;
                }
                String tag = intf.getAttribute("UserTag");
                
                Log.d(TAG, "xxxx PATH : " + path + " tag - " + tag );
                
               
                CustomViewFlipper flipper = (CustomViewFlipper) v; 
                /*if (flipper.getDisplayedChild() == 0) {
                	//flipper.setOutAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.left_in));
                	//flipper.setInAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.left_out));
                	AnimatorSet set = new AnimatorSet(); //(AnimatorSet) AnimatorInflater.loadAnimator(v.getContext(),R.anim.left_in);
                	
                	set.playSequentially(   AnimatorInflater.loadAnimator(v.getContext(),R.animator.right_in),
				                			AnimatorInflater.loadAnimator(v.getContext(),R.animator.right_out),
				                			AnimatorInflater.loadAnimator(v.getContext(),R.animator.left_in),
				                			AnimatorInflater.loadAnimator(v.getContext(),R.animator.left_out));
                    set.setTarget(flipper);
                	set.start();
                } else {
                	AnimatorSet set = new AnimatorSet();
                	set.playSequentially(   AnimatorInflater.loadAnimator(v.getContext(),R.animator.right_in),
                			AnimatorInflater.loadAnimator(v.getContext(),R.animator.right_out),
                			AnimatorInflater.loadAnimator(v.getContext(),R.animator.left_in),
                			AnimatorInflater.loadAnimator(v.getContext(),R.animator.left_out));
                    set.setTarget(flipper);
                	set.start();
                }*/
                /*AnimatorSet set ;
                if (flipper.getDisplayedChild() == 0) {
                    set = (AnimatorSet) AnimatorInflater.loadAnimator(v.getContext(),R.animator.flip_right);

                	
                } else {
                	set = (AnimatorSet) AnimatorInflater.loadAnimator(v.getContext(),R.animator.flip_left);
                }
            	set.setTarget(flipper);
            	set.start();
                flipper.showNext();*/
                //flipper.setOutAnimation(null);
                //flipper.setInAnimation(null);
                ObjectAnimator animation;
                if (flipper.getDisplayedChild() == 0) {
                	animation = ObjectAnimator.ofFloat(v, "rotationY", 0.0f, 180.0f);
            		animation.setRepeatCount(0);
            		animation.setInterpolator(new AccelerateDecelerateInterpolator());
                } else {
                    animation = ObjectAnimator.ofFloat(v, "rotationY",  180f, 0.0f);
            		animation.setRepeatCount(0);
            		animation.setInterpolator(new AccelerateDecelerateInterpolator());
            		
                }
                animation.addUpdateListener(new AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						// TODO Auto-generated method stub
						float val = animation.getAnimatedFraction();
						if (val >= 0.5 ) {
							 CustomViewFlipper flipper = (CustomViewFlipper) v;
							 flipper.setDisplayedChild(1);
							 if (flipper.getDisplayedChild() == 1) {
								 //flipper.getCurrentView().setRotationY(180);
							 }
							 animation.removeAllUpdateListeners();
							 
						}
					}
                	
                });
                
                animation.setDuration(1000);
                animation.start();
                
                TextView tv = (TextView) ((ViewFlipper) v).getChildAt(1);                
                if (tag == ""  || tag == null) {
                	tv.setText("No Tags Yet!");
                }                
			}
        });
        
        /*mDisplayImages.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				Toast.makeText(getBaseContext(),
                        "pic" + (10 + 1) + " selected",
                        Toast.LENGTH_SHORT).show();
				return true;
			}
        });*/
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

            new LoadImagesInBackGround(getParent(), data).execute();
           
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
            if (DEBUG) Log.d(TAG, cur.getPosition() + " : " + dateColumn );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mShowGrid) {
            getMenuInflater().inflate(R.menu.menu_display_view, menu);
            MenuItem item = menu.findItem(R.id.menu_item_pick_all);
            item.setVisible(true);
            MenuItem item1 = menu.findItem(R.id.slideshow);
            item1.setVisible(true);
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
            case R.id.slideshow:
            	Intent intent = new Intent(getBaseContext(), SlideShowActivity.class);
            	ArrayList<Uri> slideShowURIs = new ArrayList<Uri>();
            	for (int index = 0; index < mList.size(); index++) {
                        Uri imageUri = Uri.parse("file://" + mList.get(index));
                        slideShowURIs.add(imageUri);
                    
                }
            	intent.putParcelableArrayListExtra("uriList", slideShowURIs);
                startActivity(intent);
            	break;
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
        // Set the current grid view
        mDisplayImages.setSelection(mScrollTo);

        // Compute 'mOutMetrics' so that getPicture() can use this value.
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display mDisplay = wm.getDefaultDisplay();
        mOutMetrics= new DisplayMetrics ();
        mDisplay.getMetrics(mOutMetrics);

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
        if (DEBUG) Log.d(TAG, "Selected Items " + selectCount);
        if (selectCount > 0) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            SparseBooleanArray checkedItems = mDisplayImages.getCheckedItemPositions();
            if (checkedItems != null) {
                mImageUris.clear();
                for (int index = 0; index < checkedItems.size(); index++) {
                    int position = checkedItems.keyAt(index);
                    if(position < mList.size() && checkedItems.valueAt(index)) {
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
        	mEditTextCustomTag.setVisibility(View.VISIBLE);
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        public void onDestroyActionMode(ActionMode mode) {
        	mEditTextCustomTag.setVisibility(View.GONE);
        	mImgButtonAccept.setVisibility(View.GONE);
        	mImgButtonCancel.setVisibility(View.GONE);
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
    static class ViewHolder {
  	  RecyclingImageView imageView ;
  	  TextView textView ;
  	  int position;
  	}
    
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
                    ((RecyclingBitmapDrawable) value).setIsCached(true);
                }
                if (getBitmapFromMemCache(data) == null) {
                    mMemoryCache.put(data, value);
                    //Log.d(TAG, "mMemoryCache.put Count - " + mMemoryCache.putCount());
                }
                //Log.d(TAG, "xxxx MemoryCache size " + mMemoryCache.size());
            }
        }

        public BitmapDrawable getBitmapFromMemCache(String data) {
            BitmapDrawable memValue = null;
            if (mMemoryCache != null) {
                memValue = mMemoryCache.get(data);
                //Log.d(TAG, "mMemoryCache.put Count - " + mMemoryCache.putCount());
            }

            return memValue;
        }

        public void clearCache() {
            if (mMemoryCache != null) {
                mMemoryCache.evictAll();
                if (DEBUG) Log.d(TAG, "Memory cache cleared");
            }
        }

        public GridImageAdapter(Context application) {
            super();
            mContext = application;
            mResources = mContext.getResources();
            mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
                protected int sizeOf(String key, BitmapDrawable value) {
                    //final int bitmapSize = getBitmapSize(value) / 1024;
                    //return bitmapSize == 0 ? 1 : bitmapSize;
                	
                	//return 1;//value.getBitmap().getByteCount();
                	
                	// The cache size will be measured in kilobytes rather than
                    // number of items.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        return value.getBitmap().getByteCount() ;
                    } else {
                        return value.getBitmap().getRowBytes() * value.getBitmap().getHeight();
                    }
                }


                protected void entryRemoved( boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue ) {
                    if (DEBUG) Log.d(TAG, "Entry Removed with key " + key + " evicted : " + evicted);
                    if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
                        // The removed entry is a recycling drawable, so notify it
                        // that it has been removed from the memory cache
                        ((RecyclingBitmapDrawable) oldValue).setIsCached(false);
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
            //Log.d(TAG, "Adding LRU PHOTO @ POS " + val);
            if (val < mList.size())
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
            ViewHolder viewHolder;
            if (convertView == null) {
            	// inflate the layout
            	convertView = LayoutInflater.from(mContext).inflate(R.layout.item_flipperdisplaygridview, null);
            	viewHolder = new ViewHolder();
            	
            	viewHolder.imageView = (RecyclingImageView) convertView.findViewById(R.id.imgView);
            	viewHolder.textView = (TextView) convertView.findViewById(R.id.textView);
                viewHolder.imageView.setPadding(4, 4, 4, 4);
                convertView.setTag(viewHolder);

            } else {
            	viewHolder = (ViewHolder) convertView.getTag();
            	if (((CustomViewFlipper)convertView).getDisplayedChild() == 1) {
            		((CustomViewFlipper)convertView).setInAnimation(null);
            		((CustomViewFlipper)convertView).setOutAnimation(null);
            	    //((CustomViewFlipper)convertView).showNext();
            	} else if (((CustomViewFlipper)convertView).getDisplayedChild() == 0){
            		((CustomViewFlipper)convertView).stopFlipping();
            	}
            	//Log.d(TAG, "((CustomViewFlipper)convertView).getCurrentView().getRotationY() : " + ((CustomViewFlipper)convertView).getCurrentView().getRotationY());
            	if (((CustomViewFlipper)convertView).getCurrentView().getRotationY() == 180.0 ) {
            		((CustomViewFlipper)convertView).getCurrentView().setRotationY(0);
            	}

            }
            viewHolder.textView.setBackground(mDrawable);
            viewHolder.position = position;
            viewHolder.textView.setText(position+"");
            //mMemoryCache.trimToSize(50);
            BitmapDrawable drawable = getBitmapFromMemCache(position+"");
            if (drawable!= null && !drawable.getBitmap().isRecycled()) {
            	if (((CustomViewFlipper)convertView).getDisplayedChild() != 0) {
            		((CustomViewFlipper)convertView).setDisplayedChild(0);
            	}
            	viewHolder.imageView.setImageDrawable(drawable);
            } else {
            	mScrollTo = mDisplayImages.getFirstVisiblePosition();
            	            	//Log.d(TAG, "NOt Quite Returning from this point mScrollTo : " + mScrollTo + "position - " + position + " Last position " + mDisplayImages.getLastVisiblePosition());
            	if (cancelPotentialWork(position, viewHolder.imageView)) {
            	  BitmapWorkerTask task = new BitmapWorkerTask(viewHolder.imageView, position);
            	  final AsyncDrawable asyncDrawable =
                          new AsyncDrawable(getResources(), mPlaceHolderBitmap, task);
            	  viewHolder.imageView.setImageDrawable(asyncDrawable);
            	  //task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, position+"");
            	  task.execute(position+"");
            	  
            	}
            }
            mScrollTo = mDisplayImages.getFirstVisiblePosition();
            return convertView;
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
            //Log.d(TAG, "addGridImage --> addLRUPhoto");
        }
        mImageAdapter.notifyDataSetChanged();
    }

    boolean addtoListIfNotFound(String path) {
        if (!mList.contains(path)) {
             mList.add(path);
             //Log.d(TAG, "Path : " + path + " already found!");
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

    public static boolean cancelPotentialWork(int data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final int bitmapData = bitmapWorkerTask.data;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == 0 || bitmapData != data) {
                // Cancel previous task
            	//Log.d("CHECKING!", " cancelPotentialWork - data : " + data);
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }
    
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
     if (imageView != null) {
       final Drawable drawable = imageView.getDrawable();
       if (drawable instanceof AsyncDrawable) {
           final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
           return asyncDrawable.getBitmapWorkerTask();
       }
      }
      return null;
    }
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }
    
    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>{
           private final WeakReference<ImageView> imageViewReference;
           private int data = 0;
           boolean show = true;
           int position = -1;
           //CustomViewFlipper flipper;
           public BitmapWorkerTask(ImageView imageView, int position) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            //this.flipper = flipper;
            //this.flipper.setDisplayedChild(2);
            this.position = position;
           }

           @Override
	        protected void onPreExecute() {
	        	// TODO Auto-generated method stub
	        	super.onPreExecute();
	        	if( Math.abs(mScrollTo-position) > 20) {
	            	   Log.d(TAG, "1. CANCEL this task - mScrollTo : " + mScrollTo + " position : " + position);
	        		cancel(true);
	        		//imageViewReference.clear();
	        	}
	        }
           @Override
           protected Bitmap doInBackground(String... params) {
               int index = -1; //mList.indexOf(String.valueOf(params[0]));
               
               try {
            	   index = Integer.parseInt(String.valueOf(params[0]));
            	} catch(NumberFormatException nfe) {
            		Log.d(TAG, "Ooops ! Error ");
            	} 
        	   //int index = -1;
               if( Math.abs(mScrollTo-position) > 20) {
            	   Log.d(TAG, "1. CANCEL this task - mScrollTo : " + mScrollTo + " position : " + position);
	        		cancel(true);
	        		return null;
	        		//imageViewReference.clear();
	        	}
        	   
               if (index == -1 || index > mList.size()) {// (Math.abs(mScrollTo-index) > 20) ) {
            	   //flipper.setDisplayedChild(0);
            	   Log.d(TAG, "2. CANCEL this task - index is -1 ? : " + index);

            	   cancel(true);
            	   return null;
               }
               
               Log.d(TAG, "In BitmapWorkerTask Current POS :  " + mScrollTo + " - This task position : " + position + " - This Index position : " + index + " Math.abs(mScrollTo-position)  - " + Math.abs(mScrollTo-position));
               final Bitmap bitmap = getPicture(mList.get(index));
               //Log.d(TAG, "From BitmapWorkerTask --> AddLruPPhoto @ pos " + index + " bitmap  - " + bitmap);
               if( Math.abs(mScrollTo-position) < 20) {
                 mImageAdapter.addLRUPhoto(bitmap);
               }
               return bitmap;
           }

           @Override
           protected void onPostExecute(Bitmap bitmap) {
        	   if( Math.abs(mScrollTo-position) > 20) {
            	   Log.d(TAG, "3. CANCEL this task - mScrollTo : " + mScrollTo + " position : " + position);
        		   cancel(true);
        	   }
        	   if (isCancelled()) {
        		   //flipper.setDisplayedChild(0);
                   bitmap = null;
               }

            if (imageViewReference != null && bitmap != null) {
             final ImageView imageView = (ImageView)imageViewReference.get();
             final BitmapWorkerTask bitmapWorkerTask =
                     getBitmapWorkerTask(imageView);
             if (this == bitmapWorkerTask && imageView != null) {
            	 //flipper.setDisplayedChild(0);
              imageView.setImageBitmap(bitmap);
              //mImageAdapter.notifyDataSetChanged();
             // mDisplayImages.
             }
            }
           }
          }

    private int dpToPx(int dp)
    {
        float density = getApplicationContext().getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density);
    }
    
    Bitmap scaleDownLargeImageWithAspectRatio(Bitmap image)
    {
        int imaheVerticalAspectRatio,imageHorizontalAspectRatio;
        float bestFitScalingFactor=0;
        float percesionValue=(float) 0.2;

        //getAspect Ratio of Image
        int imageHeight=(int) (Math.ceil((double) image.getHeight()/100)*100);
        int imageWidth=(int) (Math.ceil((double) image.getWidth()/100)*100);
        int GCD=BigInteger.valueOf(imageHeight).gcd(BigInteger.valueOf(imageWidth)).intValue();
        imaheVerticalAspectRatio=imageHeight/GCD;
        imageHorizontalAspectRatio=imageWidth/GCD;
        Log.i("scaleDownLargeImageWIthAspectRatio","Image Dimensions(W:H): "+imageWidth+":"+imageHeight);
        Log.i("scaleDownLargeImageWIthAspectRatio","Image AspectRatio(W:H): "+imageHorizontalAspectRatio+":"+imaheVerticalAspectRatio);

        //getContainer Dimensions
        //int displayWidth = getWindowManager().getDefaultDisplay().getWidth();
        //int displayHeight = getWindowManager().getDefaultDisplay().getHeight();
        int displayWidth = mOutMetrics.heightPixels ;
        int displayHeight  = mOutMetrics.widthPixels ;
       //I wanted to show the image to fit the entire device, as a best case. So my ccontainer dimensions were displayWidth & displayHeight. For your case, you will need to fetch container dimensions at run time or you can pass static values to these two parameters 

        int leftMargin = 0;
        int rightMargin = 0;
        int topMargin = 0;
        int bottomMargin = 0;
        int containerWidth = displayWidth - (leftMargin + rightMargin);
        int containerHeight = displayHeight - (topMargin + bottomMargin);
        Log.i("scaleDownLargeImageWIthAspectRatio","Container dimensions(W:H): "+containerWidth+":"+containerHeight);

        //iterate to get bestFitScaleFactor per constraints
        while((imageHorizontalAspectRatio*bestFitScalingFactor <= containerWidth) && 
                (imaheVerticalAspectRatio*bestFitScalingFactor<= containerHeight))
        {
            bestFitScalingFactor+=percesionValue;
        }

        //return bestFit bitmap
        int bestFitHeight=(int) (imaheVerticalAspectRatio*bestFitScalingFactor);
        int bestFitWidth=(int) (imageHorizontalAspectRatio*bestFitScalingFactor);
        Log.i("scaleDownLargeImageWIthAspectRatio","bestFitScalingFactor: "+bestFitScalingFactor);
        Log.i("scaleDownLargeImageWIthAspectRatio","bestFitOutPutDimesions(W:H): "+bestFitWidth+":"+bestFitHeight);
        image=Bitmap.createScaledBitmap(image, bestFitWidth,bestFitHeight, true);

        //Position the bitmap centre of the container
        int leftPadding=(containerWidth-image.getWidth())/2;
        int topPadding=(containerHeight-image.getHeight())/2;
        Bitmap backDrop=Bitmap.createBitmap(containerWidth, containerHeight, Bitmap.Config.RGB_565);
        Canvas can = new Canvas(backDrop);
        can.drawBitmap(image, leftPadding, topPadding, null);

        return backDrop;
    }
    
    Bitmap getPicture(String path) {
        ExifInterface intf = null;
        Bitmap bitmap = null;
        Bitmap newBitmap = null;

        if (path == null) {
          return null;
        }
        //Log.d(TAG,"xxxxx path - " + path);
        try {
            intf = new ExifInterface(path);
        } catch(IOException e) {
            e.printStackTrace();
        }

        if(intf == null) {
            return null;
        }

        float dpHeight = mOutMetrics.heightPixels / 2;
        float dpWidth  = mOutMetrics.widthPixels / 2;
        int width=(int) (dpWidth);
        int height=(int) (dpHeight);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ) {
            //width = height;
        }
        if (intf.hasThumbnail() ) {
           byte[] thumbnail = intf.getThumbnail();
           BitmapFactory.Options options = new BitmapFactory.Options();
           options.inJustDecodeBounds = true;
           BitmapFactory.decodeByteArray(thumbnail,0,thumbnail.length,options);

           int bounding = dpToPx(250);
           float xScale = ((float) bounding) / width;
           float yScale = ((float) bounding) / height;
           float scale = (xScale <= yScale) ? xScale : yScale;
           
           //Matrix matrix = new Matrix();
          // matrix.postScale(scale, scale);
           
          /// Calculate inSampleSize
           options.inSampleSize = calculateInSampleSize(options, 100, 100);
           // Decode bitmap with inSampleSize set
           options.inJustDecodeBounds = false;
           //options.inSampleSize = 8;
           bitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length, options);
           //newBitmap = scaleDownLargeImageWithAspectRatio(bitmap);
           /*if (bitmap != null) {
               //newBitmap = Bitmap.createScaledBitmap(bitmap, width, width, true);
        	   newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
               if (newBitmap!= bitmap){
                   bitmap.recycle();
                   bitmap = null;
               }
               if (newBitmap != null) {
                   return newBitmap;
               }
           }*/
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
               options.inSampleSize = calculateInSampleSize(options, 100, 100);

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

        LoadImagesInBackGround(Context application, Cursor cur) {
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
            	Bitmap bmp = null;
                if (mCursor.isClosed() || isCancelled())
                    return null;
                if (bMatchPictureIDsOnly) {
                    bmp = getImgBasedOnPictureIDs(mCursor);
                } else {
                  bmp = getImgBasedOnUserFilter(mCursor);
                }
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
                         "TIP 3: Please ensure that pictures have been indeed taken on the specified 'date / month / date range' or 'place' by checking your Gallery / Photo albums\n\n" +
                         "TIP 4: Note that filters are ONLY applied to 'camera' pictures in the photo albums.\n\n" +
                         "TIP 5: There is no need to form a complete sentence. Apply search filters by merely saying the 'date' on which pictures have been taken " +
                         "(or) 'place' at which they have been taken. For more clues just look at the First Activity / Screen's text animation. ";
            }
            setProgressBarIndeterminateVisibility(false);
            if (mShowWarningMenuItem != null) {
                invalidateOptionsMenu();
            }
        }

        @Override
        protected void onCancelled(Object result) {

        }
        
        Bitmap getImgBasedOnPictureIDs(Cursor cur) {
            boolean added = false;
            boolean found = false;
            String path   = null;
            Integer pictId = -1;
            if (!bMatchPictureIDsOnly)
            	return null;
            do {
                if (cur.isClosed()) break;
                if (id != -1) {
                	pictId = cur.getInt(id);
                }
                for (int i = 0; i < pictureIds.length; i ++) {
                	if (pictId == pictureIds[i]) {
                		found = true;
                		break;
                	} 
                }
            } while(false);
            
                if (dataColumn != -1 && found) {
                  path = cur.getString(dataColumn);
                  added = true;
                }
                
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
                        if (!mOOMAlready) {
                            mShowWarningMenuItem = "ERROR: Sorry! Unable to display complete results due to memory issues.\n\n" +
                                                   " This app is still in alpha stage! We will soon provide an update to address this issue. Apologies once again!";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (bShowToastMsg) {
                                        Toast.makeText(getBaseContext(), mShowToastMsg, Toast.LENGTH_LONG).show();
                                        bShowToastMsg = false;
                                    }
                                    invalidateOptionsMenu();
                                }
                            });
                            mOOMAlready = true;
                        }
                        //Log.e("Map", "ResultsView - Out Of Memory Error " + e.getLocalizedMessage());
                        /*try {
                               android.os.Debug.dumpHprofData("/sdcard/dump.hprof");
                             }
                        catch (IOException e1) {
                               e1.printStackTrace();
                        }*/
                    }

                }
           
			return null;
        }

        Bitmap getImgBasedOnUserFilter(Cursor cur) {
            boolean added = false;
            int dateRangeMatchFound = -1;
            String path   = null;
            String curDate = null;
            long dateinMilliSec = 0;
            do {
                if (cur.isClosed()) break;
                if (dateColumn != -1) {
                  curDate = cur.getString(dateColumn);
                }
                if (cur.isClosed()) break;
                if (dataColumn != -1) {
                  path = cur.getString(dataColumn);
                }
                if (bMatchPictureIDsOnly) {
                	//pictureIds
                	break;
                }
                //Log.d(TAG,"xxxxx path - " + path);
                if (curDate == null && curDate == null) break;
                if (curDate != null)
                  dateinMilliSec = Long.parseLong(curDate);
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
                   if (path == null) break;
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
                               if (placeFound == null) placeFound = ""; // To avoid the crash!
                               if (placeList != null && placeList.size() < 2) {
                                  break;
                               }
                               countryFound  = placeList.get(1);
                               if (countryFound == null) countryFound = ""; // To avoid the crash!
                               if (placeList != null && placeList.size() < 3) {
                                  break;
                               }
                               adminAreaFound  = placeList.get(2);
                               if (adminAreaFound == null) adminAreaFound = ""; // To avoid the crash!
                       } while (false);

                       if (mDbHelper.isAtleastSingleValuePresentInList(placeList) == false) {
                           // Place not found in cache or db ,but it has a valid GPS cod-ordinates
                           // Try and fallback on GeoCoder API to retrieve the place.
                       } else if((mUserFilter.toLowerCase().contains(placeFound.toLowerCase()) && (placeFound != ""))    ||
                                 (mUserFilter.toLowerCase().contains(countryFound.toLowerCase()) && (countryFound != "")) ||
                                 (mUserFilter.toLowerCase().contains(adminAreaFound.toLowerCase())) && (adminAreaFound != "")) {
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
                       if (DEBUG) Log.d(TAG,"Ooops No Connection.. Try Later");
                       if (mDbHelper.getState() == DataBaseManager.SyncState.SYNC_STATE_INPROGRESS) {
                           mShowWarningMenuItem = "Warning: You may see Inconsistent / Incorrect Results if you are searching by place. \n\n" +
                                  "Probable Reason: Check your internet connection! It looks like there is no active data connection. ";
                           runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   if (bShowToastMsg) {
                                       Toast.makeText(getBaseContext(), mShowToastMsg, Toast.LENGTH_LONG).show();
                                       bShowToastMsg = false;
                                       invalidateOptionsMenu();
                                   }
                               }
                           });
                       }
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
                                 "Probable Reason:  Geocoding / Reverse Geocoding Service was not available partially for a few / all pictures. As a result, such pictures if filtered / searched by 'place' may not be displayed in the results grid view. Please retry again later if you see incomplete results. " +
                                 "However note that search by dates should not have any issues. \n\n" +
                                 "TIP: If the issue persists, (Though not ideal!) please consider rebooting the device. This issue is outside the scope of the application";
                       runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                               if (bShowToastMsg) {
                                   Toast.makeText(getBaseContext(), mShowToastMsg, Toast.LENGTH_LONG).show();
                                   bShowToastMsg = false;
                                   invalidateOptionsMenu();
                               }
                           }
                       });
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
                    if (!mOOMAlready) {
                        mShowWarningMenuItem = "ERROR: Sorry! Unable to display complete results due to memory issues.\n\n" +
                                               " This app is still in alpha stage! We will soon provide an update to address this issue. Apologies once again!";
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (bShowToastMsg) {
                                    Toast.makeText(getBaseContext(), mShowToastMsg, Toast.LENGTH_LONG).show();
                                    bShowToastMsg = false;
                                }
                                invalidateOptionsMenu();
                            }
                        });
                        mOOMAlready = true;
                    }
                    //Log.e("Map", "ResultsView - Out Of Memory Error " + e.getLocalizedMessage());
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
    
 // The callback interface
    interface ResultViewCallback {
        void ResultsCallback();
    }
} // End of Main class
