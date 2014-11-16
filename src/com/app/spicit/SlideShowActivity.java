package com.app.spicit;


	import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ViewFlipper;
	 
	public class SlideShowActivity extends Activity implements 
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener{
	 
	    int mFlipping = 0 ; // Initially flipping is off
	    Button mButton ; // Reference to button available in the layout to start and stop the flipper
	    
	    private static final int SWIPE_MIN_DISTANCE = 120;
		private static final int SWIPE_THRESHOLD_VELOCITY = 200;
		private ViewFlipper mViewFlipper;	
		//private Animation.AnimationListener mAnimationListener;
		private Context mContext;
		private GestureDetectorCompat mDetector;
		
		 ArrayList<Parcelable> mImageParcelableURI;
		 
		 private static final int SLIDE_SHOW_DELAY = 1;
		 int mNextImageInterval = 3000;
		
		private Animation.AnimationListener mAnimationListener  = new Animation.AnimationListener() {
			public void onAnimationStart(Animation animation) {
				//animation started event
			}

			public void onAnimationRepeat(Animation animation) {
			}

			public void onAnimationEnd(Animation animation) {
				//TODO animation stopped event
			}
		};
		
		private Handler mShowNextImageInSlideShow = new Handler() {
	        public void handleMessage (Message msg) {
	            switch (msg.what) {
	                case SLIDE_SHOW_DELAY:
	                    //success handling
	                	mViewFlipper.showNext();
	                    Message mesg = new Message();
	                    mesg.what = SLIDE_SHOW_DELAY;
	                    mShowNextImageInSlideShow.removeMessages(SLIDE_SHOW_DELAY);
	                    mShowNextImageInSlideShow.sendMessageDelayed(mesg, mNextImageInterval);
	                    break;
	                default:
	                    //failure handling
	                    break;
	            }
	        }
	    };
		
	 
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.slideshow);
	 
	        getActionBar().setHomeButtonEnabled(true);
	        getActionBar().setDisplayHomeAsUpEnabled(true);
	        
	     // Instantiate the gesture detector with the
	        // application context and an implementation of
	        // GestureDetector.OnGestureListener
	        mDetector = new GestureDetectorCompat(this,this);
	        final Handler handler = new Handler();
	        
	        // Set the gesture detector as the double tap
	        // listener.
	        mDetector.setOnDoubleTapListener(this);
	        
	        mImageParcelableURI = getIntent().getParcelableArrayListExtra("uriList");
	        
	        mViewFlipper = (ViewFlipper) findViewById(R.id.flipper1);
	        
	        
	        
	        for (Parcelable p : mImageParcelableURI) {
	        	Uri uri = (Uri) p;
	        	ImageView image = new ImageView ( this);
	        	
        	    image.setImageBitmap(decodeSampledBitmapFromResource(uri.getPath(), 300, 300));
        	    mViewFlipper.addView( image );
	        }
	 
	        mViewFlipper.startFlipping();
	        Message mesg = new Message();
            mesg.what = SLIDE_SHOW_DELAY;
            mShowNextImageInSlideShow.sendMessageDelayed(mesg, 500);

	       
	   	 
            /*if(mFlipping==0){
                // Start Flipping 
            	mViewFlipper.startFlipping();
                mFlipping=1;
                //mButton.setText("stop");
            }
            else{
                // Stop Flipping 
            	mViewFlipper.stopFlipping();
                mFlipping=0;
               // mButton.setText("start");
            }*/
	 
       
	        /** Getting a reference to the button available in the resource */
	        //mButton = (Button) findViewById(R.id.btn);
	 
	        /** Setting click event listner for the button */
	        //mButton.setOnClickListener(listener);
	 
	    
	}
	    
	    public static int calculateInSampleSize(BitmapFactory.Options options,
	            int reqWidth, int reqHeight) {
	        // Raw height and width of image
	        final int height = options.outHeight;
	        final int width = options.outWidth;
	        int inSampleSize = 1;    
	        if (height > reqHeight || width > reqWidth) {

	            final int halfHeight = height / 2;
	            final int halfWidth = width / 2;

	            // Calculate the largest inSampleSize value that is a power of 2 and
	            // keeps both
	            // height and width larger than the requested height and width.
	            while ((halfHeight / inSampleSize) > reqHeight
	                    && (halfWidth / inSampleSize) > reqWidth) {
	                inSampleSize *= 2;
	            }
	        }
	        return inSampleSize;
	    }

	public static Bitmap decodeSampledBitmapFromResource(String strPath,int reqWidth, int reqHeight) {

	        // First decode with inJustDecodeBounds=true to check dimensions
	        final BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inJustDecodeBounds = true;
	        BitmapFactory.decodeFile(strPath, options);
	        // Calculate inSampleSize
	        options.inSampleSize = calculateInSampleSize(options,reqWidth,
	                reqHeight);
	        // Decode bitmap with inSampleSize set
	        options.inJustDecodeBounds = false;
	        return BitmapFactory.decodeFile(strPath, options);
	}
	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        getMenuInflater().inflate(R.menu.menu_slideshow, menu);
	        return true;
	  }
	    
	    class SwipeGestureDetector extends SimpleOnGestureListener {
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				try {
					// right to left swipe
					if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
						//mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(mContext, R.anim.left_in));
						//mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(mContext, R.anim.left_out));
						// controlling animation
						//mViewFlipper.getInAnimation().setAnimationListener(mAnimationListener);
						mViewFlipper.showNext();
						return true;
					} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
						//mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(mContext, R.anim.right_in));
						//mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(mContext,R.anim.right_out));
						// controlling animation
						//mViewFlipper.getInAnimation().setAnimationListener(mAnimationListener);
						mViewFlipper.showPrevious();
						return true;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}

				return false;
			}
		}

	    @Override 
	    public boolean onTouchEvent(MotionEvent event){ 
	        this.mDetector.onTouchEvent(event);
	        mViewFlipper.stopFlipping();
	        // Be sure to call the superclass implementation
	        return super.onTouchEvent(event);
	    }
	    
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					//mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(mContext, R.anim.left_in));
					//mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(mContext, R.anim.left_out));
					// controlling animation
					//mViewFlipper.getInAnimation().setAnimationListener(mAnimationListener);
					mViewFlipper.showNext();
					return true;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					//mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(mContext, R.anim.right_in));
					//mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(mContext,R.anim.right_out));
					// controlling animation
					//mViewFlipper.getInAnimation().setAnimationListener(mAnimationListener);
					mViewFlipper.showPrevious();
					return true;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}

			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}
	}