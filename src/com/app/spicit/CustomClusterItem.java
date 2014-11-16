package com.app.spicit;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class CustomClusterItem implements ClusterItem {
    private final LatLng mPosition;
    private final int mPictID;

    public CustomClusterItem(double lat, double lng, int pictID) {
        mPosition = new LatLng(lat, lng);
        mPictID = pictID;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }
    
    public int getPictureID() {
      return mPictID;
    }
}