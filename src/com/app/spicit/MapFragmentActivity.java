package com.app.spicit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.json.JSONException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterClickListener;
import com.google.maps.android.clustering.ClusterManager.OnClusterInfoWindowClickListener;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemInfoWindowClickListener;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

//import com.google.maps.android.utils.demo.CustomMarkerClusteringDemoActivity.PersonRenderer;
//import com.google.maps.android.utils.demo.model.Person;
import com.google.android.gms.maps.CameraUpdateFactory;

//import com.google.maps.android.utils.demo.MyItemReader;
//import com.google.maps.android.utils.demo.R;
//import com.google.maps.android.utils.demo.model.MyItem;






import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
public class MapFragmentActivity extends Activity implements ClusterManager.OnClusterClickListener<CustomClusterItem>, ClusterManager.OnClusterInfoWindowClickListener<CustomClusterItem>, ClusterManager.OnClusterItemClickListener<CustomClusterItem>, ClusterManager.OnClusterItemInfoWindowClickListener<CustomClusterItem> { 
	    GoogleMap googleMap;
	    SharedPreferences sharedPreferences;
	    int locationCount = 0;
	    private DataBaseManager mDbHelper;
	    private ClusterManager<CustomClusterItem> mClusterManager;
	    List<CustomClusterItem> items = new ArrayList<CustomClusterItem>();
	    //new ClusterManager<MyItem>
	    
	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.mapfragactivity);
	        
	        //BubbleIconFactory b ;
	        getActionBar().setHomeButtonEnabled(true);
	        getActionBar().setDisplayHomeAsUpEnabled(true);
	     // Getting Google Play availability status
	        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
	        mDbHelper = DataBaseManager.getInstance(this);
	        
	        final Geocoder geocoder;
        	geocoder = new Geocoder(this, Locale.getDefault());
	 
	        // Showing status
	        if(status!=ConnectionResult.SUCCESS){ // Google Play Services are not available
	 
	            int requestCode = 10;
	            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
	            dialog.show();
	 
	        } else { // Google Play Services are available
	 
	            // Getting reference to the SupportMapFragment of activity_main.xml
	            MapFragment fm =  (MapFragment) getFragmentManager().findFragmentById(R.id.map);
	 
	            // Getting GoogleMap object from the fragment
	            googleMap = fm.getMap();
	            
	            mClusterManager = new ClusterManager<CustomClusterItem>(this, googleMap);

	            // Enabling MyLocation Layer of Google Map
	            //googleMap.setMyLocationEnabled(true);
	 
	            // Opening the sharedPreferences object
	            sharedPreferences = getSharedPreferences("location", 0);
	 
	            // Getting number of locations already stored
	            locationCount = sharedPreferences.getInt("locationCount", 0);
	            locationCount = mDbHelper.mMapLatLongVals.size();
	            // Getting stored zoom level if exists else return 0
	            String zoom = sharedPreferences.getString("zoom", "0");
	 
	            // If locations are already saved
	            if(locationCount!=0){
	 
	                String lat = "";
	                String lng = "";
	                double id = -1;
	 
	                // Iterating through all the locations stored
	                for(int i=0;i<locationCount;i++){
	 
	                	ArrayList<Double> latlong = mDbHelper.mMapLatLongVals.get(i);
	                	if (latlong.size() > 0) {
	                      // Getting the latitude of the i-th location
	                      lat = latlong.get(0) + ""; //sharedPreferences.getString("lat"+i,"0");
	                      if (latlong.size() > 1)
	                        // Getting the longitude of the i-th location
	                        lng = latlong.get(1) + ""; //sharedPreferences.getString("lng"+i,"0");
	                      if (latlong.size() > 2)
	                    	  id = latlong.get(2);
	                	}
	 
	                    // Drawing marker on the map
	                    //drawMarker(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
	                    CustomClusterItem item = new CustomClusterItem(Double.parseDouble(lat), Double.parseDouble(lng), (int) id);
	                    items.add(item);
	            	    
	                }
	 
	                // Moving CameraPosition to last clicked position
	                //googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng))));
	                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), 100));
	                // Setting the zoom level in the map on last position  is clicked
	                googleMap.animateCamera(CameraUpdateFactory.zoomTo(Float.parseFloat("1.0")));
	                
	                mClusterManager.addItems(items);
	                
	            }
	            
	            googleMap.setOnCameraChangeListener(mClusterManager);
                googleMap.setOnCameraChangeListener(mClusterManager);
                googleMap.setOnMarkerClickListener(mClusterManager);
                googleMap.setOnInfoWindowClickListener(mClusterManager);
                mClusterManager.setOnClusterClickListener(this);
                mClusterManager.setOnClusterInfoWindowClickListener(this);
                mClusterManager.setOnClusterItemClickListener(this);
                mClusterManager.setOnClusterItemInfoWindowClickListener(this);
                mClusterManager.cluster();

	           /* googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {

					@Override
					public boolean onMarkerClick(Marker marker) {
	                	Double lat = marker.getPosition().latitude;
	                	Double lng = marker.getPosition().longitude;
	                	int len = mDbHelper.mMapLatLongVals.size();
	                	ArrayList<String> placeList = null;
	                	String place = null;
	                	//ArrayList<int> pictId = new ArrayList<int>();
	                	ArrayList<Integer> pictId = new ArrayList<Integer>();
	                	
	                	Double lati[] = new Double[2];
	                	Double longi[]  = new Double[2];
	                	lati[0] = lat;
	                	longi[0] = lng;
	                	
	                	for (int i = 0; i < len ; i++) {
	                		ArrayList<Double> latlng = mDbHelper.mMapLatLongVals.get(i);
	                		Double latitude = latlng.get(0);
	                		Double longitude = latlng.get(1);
	                		
	                		lati[1] = latitude;
	                		longi[1] = longitude;
	                		
	                		Log.d("GAPIss ", "LATITUDE FROM DB - " + latitude + "  " + longitude + "   :::  marker API - " + lat + "  " + lng );
	                		//if (latitude == lat && longitude == lng) {
	                		if (floatingpointAlmostEqual(lati, longi, 0.000001)) {
	                			// Hack !
	                			double id = latlng.get(2);
	                			pictId.add((int)id);
	                			//break;
	                		}
	                	}
	                	
	                	if (pictId.size() == 0) 
	                		return false;
	                	
	                	//if (place != null) {
	                	    int [] pictureID = new int[pictId.size()];
	                	    for (int i =0; i < pictId.size(); i++) {
	                	    	pictureID[i] = pictId.get(i);
	                	    }
	                        Intent intent = new Intent(getBaseContext(), ResultsView.class);
	                        //intent.putExtra("filter", place);
	                        Bundle myBundle = new Bundle(); 
	                        myBundle.putIntArray("PictIDArray", pictureID);
	                        intent.putExtras(myBundle);
	                        startActivity(intent);
	                        return true;
	                }
	            });*/
	        }
	    
	    
	        /*googleMap.setOnMapClickListener(new OnMapClickListener() {
	    	 
            @Override
            public void onMapClick(LatLng point) {
            	
                locationCount++;
 
                // Drawing marker on the map
                drawMarker(point);
 
                // Opening the editor object to write data to sharedPreferences 
                SharedPreferences.Editor editor = sharedPreferences.edit();
 
                // Storing the latitude for the i-th location
                editor.putString("lat"+ Integer.toString((locationCount-1)), Double.toString(point.latitude));
 
                // Storing the longitude for the i-th location
                editor.putString("lng"+ Integer.toString((locationCount-1)), Double.toString(point.longitude));
 
                // Storing the count of locations or marker count
                editor.putInt("locationCount", locationCount);
 
                //Storing the zoom level to the shared preferences 
                editor.putString("zoom", Float.toString(googleMap.getCameraPosition().zoom));
 
                // Saving the values stored in the shared preferences 
                editor.commit();
 
                Toast.makeText(getBaseContext(), "Marker is added to the Map", Toast.LENGTH_SHORT).show();
 
            }
        });*/
 
        /*googleMap.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
 
                // Removing the marker and circle from the Google Map
                //googleMap.clear();
 
                // Opening the editor object to delete data from sharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
 
                // Clearing the editor
                editor.clear();
 
                // Committing the changes
                editor.commit();
 
                // Setting locationCount to zero
                locationCount=0;
            	
            	
 
            }
        });*/
    }
 
    private boolean floatingpointAlmostEqual(Double p1[], Double p2[], Double EPSILON) {
        return ((Math.abs(p1[0] - p1[1]) < EPSILON) && (Math.abs(p2[0] - p2[1]) < EPSILON));
    }
        
	private void readItems() {
	    //InputStream inputStream = getResources().openRawResource(R.raw.radar_search);
	   // List<CustomClusterItem> items = new MyItemReader().read(inputStream);
	    //mClusterManager.addItems(items);
	}
	    
    private void drawMarker(LatLng point){
        // Creating an instance of MarkerOptions
        MarkerOptions markerOptions = new MarkerOptions();
 
        // Setting latitude and longitude for the marker
        markerOptions.position(point);
        // Adding marker on the Google Map
        googleMap.addMarker(markerOptions);
    }
	 
	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        // Inflate the menu; this adds items to the action bar if it is present.
	        //getMenuInflater().inflate(R.menu.mapfragactivity, menu);
	        return true;
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
	        }
			return false;
	    }

		@Override
		public void onClusterItemInfoWindowClick(CustomClusterItem cluster) {
			// TODO Auto-generated method stub
			//Toast.makeText(this, "Hello from onClusterItemInfoWindowClick",Toast.LENGTH_SHORT).show();
			
		}

		@Override
		public boolean onClusterItemClick(CustomClusterItem item) {
			// TODO Auto-generated method stub
			//????
			//Toast.makeText(this, "Hello from onClusterItemClick",Toast.LENGTH_SHORT).show();
			
        	
        	
        	//if (place != null) {
        	    int [] pictureID = new int[1];
        	    pictureID[0] = item.getPictureID();
        
                Intent intent = new Intent(getBaseContext(), ResultsView.class);
                //intent.putExtra("filter", place);
                Bundle myBundle = new Bundle(); 
                myBundle.putIntArray("PictIDArray", pictureID);
                intent.putExtras(myBundle);
                startActivity(intent);
                return true;
		}

		@Override
		public void onClusterInfoWindowClick(Cluster<CustomClusterItem> cluster) {
			// TODO Auto-generated method stub
			//Toast.makeText(this, "Hello from onClusterInfoWindowClick",Toast.LENGTH_SHORT).show();
			
		}

		@Override
		public boolean onClusterClick(Cluster<CustomClusterItem> cluster) {
			// TODO Auto-generated method stub
			String firstName = cluster.getItems().iterator().next().getPosition().toString();
			//Toast.makeText(this, cluster.getSize() + " (including " + firstName + ")", Toast.LENGTH_SHORT).show();
			ArrayList<Integer> pictId = new ArrayList<Integer>();
			Iterator it = cluster.getItems().iterator();
			while (it.hasNext()) {
				CustomClusterItem element = (CustomClusterItem) it.next();
				//Double lat = cluster.getItems().iterator().next().getPosition().latitude;
	        	//Double lng = cluster.getItems().iterator().next().getPosition().longitude;
	        	//int len = mDbHelper.mMapLatLongVals.size();
	        	Log.d("STEP 1", element.getPictureID() + " picture id is added " +  cluster.getSize());
	        	pictId.add(element.getPictureID());
	        	
	        	//cluster.getItems().iterator().
	        	
	        	
			}
        	if (pictId.size() == 0) 
        		return false;
        	
        	Log.d("STEP 2", "OK there is some size");
        	
        	//if (place != null) {
        	    int [] pictureID = new int[pictId.size()];
        	    for (int i =0; i < pictId.size(); i++) {
        	    	pictureID[i] = pictId.get(i);
        	    	
                	Log.d("STEP 3", "OK Setting up pictArray " + pictureID.length);

        	    }
                Intent intent = new Intent(getBaseContext(), ResultsView.class);
                //intent.putExtra("filter", place);
                Bundle myBundle = new Bundle(); 
                myBundle.putIntArray("PictIDArray", pictureID);
                intent.putExtras(myBundle);
                startActivity(intent);
                return true;
       
		}
		
		/**
		 * Draws profile photos inside markers (using IconGenerator).
		 * When there are multiple people in the cluster, draw multiple photos (using MultiDrawable).
		 */
		private class PersonRenderer extends DefaultClusterRenderer<CustomClusterItem> {
		    private final IconGenerator mIconGenerator = new IconGenerator(getApplicationContext());
		    private final IconGenerator mClusterIconGenerator = new IconGenerator(getApplicationContext());
		    private final ImageView mImageView;
		    private final ImageView mClusterImageView;
		    private final int mDimension;

		    public PersonRenderer() {
		        super(getApplicationContext(), googleMap, mClusterManager);

		        View multiProfile = getLayoutInflater().inflate(R.layout.multi_profile, null);
		        mClusterIconGenerator.setContentView(multiProfile);
		        mClusterImageView = (ImageView) multiProfile.findViewById(R.id.image);

		        mImageView = new ImageView(getApplicationContext());
		        mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
		        mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
		        int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
		        mImageView.setPadding(padding, padding, padding, padding);
		        mIconGenerator.setContentView(mImageView);
		    }

		    @Override
		    protected void onBeforeClusterItemRendered(CustomClusterItem person, MarkerOptions markerOptions) {
		        // Draw a single person.
		        // Set the info window to show their name.
		    	/*String path = mDbHelper.mMapIDPathCache.get(person.getPictureID());
		    	Uri imageUri = Uri.parse("file://" +path);
		        mImageView.setImageURI(imageUri);
		        Bitmap icon = mIconGenerator.makeIcon();
		        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title("Pictures From Berlin Trip");*/
		    }

		    @Override
		    protected void onBeforeClusterRendered(Cluster<CustomClusterItem> cluster, MarkerOptions markerOptions) {
		    	
		    	if (cluster.getSize() == 1) {
		    		
		    		for (CustomClusterItem p : cluster.getItems()) {
		    			String path = mDbHelper.mMapIDPathCache.get(p.getPictureID());
				    	Uri imageUri = Uri.parse("file://" +path);
				        mImageView.setImageURI(imageUri);
				        Bitmap icon = mIconGenerator.makeIcon();
				        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title("Pictures From Berlin Trip");
		    		}
		    	
		    	}
		        
		        // Draw multiple people.
		        // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
		        /*List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
		        int width = mDimension;
		        int height = mDimension;

		        for (CustomClusterItem p : cluster.getItems()) {
		            // Draw 4 at most.
		            if (profilePhotos.size() == 4) break;
		            Drawable drawable = getResources().getDrawable(p.profilePhoto);
		            drawable.setBounds(0, 0, width, height);
		            profilePhotos.add(drawable);
		        }
		        MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
		        multiDrawable.setBounds(0, 0, width, height);

		        mClusterImageView.setImageDrawable(multiDrawable);
		        Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
		        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));*/
		    }

		    @Override
		    protected boolean shouldRenderAsCluster(Cluster cluster) {
		        // Always render clusters.
		        return cluster.getSize() > 1;
		    }
		}
}


	 