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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

//import com.example.android.common.logger.Log;
//import com.example.android.displayingbitmaps.BuildConfig;

/**
 * A BitmapDrawable that keeps track of whether it is being displayed or cached.
 * When the drawable is no longer being displayed or cached,
 * {@link android.graphics.Bitmap#recycle() recycle()} will be called on this drawable's bitmap.
 */
public class RecyclingBitmapDrawable extends BitmapDrawable {

    static final String TAG = "SpickiT> CountingBitmapDrawable";

    private int mCacheRefCount = 0;
    private int mDisplayRefCount = 0;

    private boolean mHasBeenDisplayed  = false;
    private int where =0;

    public RecyclingBitmapDrawable(Resources res, Bitmap bitmap) {
        super(res, bitmap);
    }

    /**
     * Notify the drawable that the displayed state has changed. Internally a
     * count is kept so that the drawable knows when it is no longer being
     * displayed.
     *
     * @param isDisplayed - Whether the drawable is being displayed or not
     */
    public void setIsDisplayed(boolean isDisplayed) {
        synchronized (this) {
            if (isDisplayed) {
                mDisplayRefCount++;
                mHasBeenDisplayed = true;
            } else {
                mDisplayRefCount--;
            }
        }
        // Check to see if recycle() can be called
        checkState();
    }

    /**
     * Notify the drawable that the cache state has changed. Internally a count
     * is kept so that the drawable knows when it is no longer being cached.
     *
     * @param isCached - Whether the drawable is being cached or not
     */
    public void setIsCached(boolean isCached, int where) {
        //Log.d(TAG, "isCached is ::  " +  isCached);
        this.where = where;
        synchronized (this) {
            if (isCached) {
                mCacheRefCount++;
            } else {
                mCacheRefCount--;
            }
        }

        // Check to see if recycle() can be called
        checkState();
        //END_INCLUDE(set_is_cached)
    }

    private synchronized void checkState() {
        if (this.where  == 1)
            Log.d(TAG,"Recycle ? Display : " + (mDisplayRefCount) + " Cache : " + (mCacheRefCount) + "mHasBeenDisplayed  " + mHasBeenDisplayed + "  " + hasValidBitmap());
        if (mCacheRefCount <= 0 && mDisplayRefCount <= 0 && mHasBeenDisplayed
                && hasValidBitmap()) {
            //if (BuildConfig.DEBUG) {
                Log.d(TAG, "YESSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS RECYCLING    No longer being used or cached so recycling. "
                        + toString());
            //}
            Bitmap bitmap = getBitmap();
            bitmap.recycle();
            bitmap = null;
        }
    }

    private synchronized boolean hasValidBitmap() {
        Bitmap bitmap = getBitmap();
        return bitmap != null && !bitmap.isRecycled();
    }

}
