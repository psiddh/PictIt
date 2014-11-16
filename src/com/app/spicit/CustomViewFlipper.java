/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.app.spicit;

import android.content.Context;
import android.graphics.drawable.Drawable;

import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Checkable;
import android.widget.ViewFlipper;

import com.app.spicit.RecyclingBitmapDrawable;

/**
 * Sub-class of ImageView which automatically notifies the drawable when it is
 * being displayed.
 */
public class CustomViewFlipper extends ViewFlipper implements Checkable{

    static final String TAG = "SpickiT> RecyclingImageView";
    private boolean mChecked = false;
    private boolean mInTagView = false;
    private int mPosition = -1;

    public CustomViewFlipper(Context context) {
        super(context);
        //Log.d("CustomViewFlipper", "New CustomViewFlipper...");
    }

    public CustomViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
    	//Log.d("CustomViewFlipper", "New CustomViewFlipper...");

    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        Drawable drawable = null;
        if (checked) {
           drawable = getResources().getDrawable(R.drawable.bggrid);
        } else {
            drawable = null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(drawable);
        } else {
        	setBackground(drawable);
            //setBackgroundDrawable(drawable);
        }
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void toggle() {
        setChecked(!mChecked);
    }
    
    public void setViewState(int position) {
    	showNext();
    	if (getDisplayedChild() == 0) {
    		mInTagView = false;
    		mPosition = -1;
    	} else {
    		Log.d("CustomViewFlipper", "Double clicked and in TagView State ?");
    		mInTagView = true;
    		mPosition = position;
    	}
    	
    	Log.d("CustomViewFlipper", "getDisplayedChild () mInTagView - " + getDisplayedChild() + " :  " + mInTagView);
    		
    }
    
    public boolean isViewInTagViewState(int position) {
    	Log.d("CustomViewFlipper", "isViewInTagViewState () mInTagView -" + mInTagView);
    	if (mPosition == -1 || (position != mPosition))
    	  return false;
    	
    	return mInTagView;
    }
    /**
     * @see android.widget.ImageView#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        // This has been detached from Window, so clear the drawable
        //Log.d(TAG, "In onDetachedFromWindow");
    	Log.d("CustomViewFlipper","onDetachedFromWindow");
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        // This has been detached from Window, so clear the drawable
    	Log.d("CustomViewFlipper","onAttachedToWindow");
        super.onAttachedToWindow();
    }
}
