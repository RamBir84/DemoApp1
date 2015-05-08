package com.example.demoapp;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.demoapp.helpers.ServerAsyncParent;
import com.example.demoapp.helpers.ServerCommunicator;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.PendingIntent;
import android.app.Service;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class geofencingService extends Service implements GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, ServerAsyncParent {
   
	private GoogleApiClient mGoogleClient;
	static int geoStatus;
	private double geoLatitude;
	private double geoLongitude;
	private float geoRadius;
	static Location userLocation;
	private Location geoLocation;
	private float distance;
	static SharedPreferences settings; 
	private boolean onCampus;
	
	
   /** indicates how to behave if the service is killed */
// int mStartMode;
   /** interface for clients that bind */
   IBinder mBinder;     
   /** indicates whether onRebind should be used */
   boolean mAllowRebind;

   
   /** Called when the service is being created. */
   @Override
   public void onCreate() {
	  // Toast.makeText(this, "Service created", Toast.LENGTH_SHORT).show();
		mGoogleClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();		
		geoStatus = -1;
		geoLatitude = 32.177256142836924;/*idc*////32.16469634171559;*apartment*32.16744820334117;
		geoLongitude = 34.83560096472502;/*idc*////34.84679650515318;*apartment*34.83503853902221;
		geoRadius = 2000;
		geoLocation = new Location("");
		geoLocation.setLatitude(geoLatitude);
		geoLocation.setLongitude(geoLongitude);
		settings = getSharedPreferences("UserInfo", 0);
		onCampus = true;
	
   }

   /** The service is starting, due to a call to startService() */
   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
	   mGoogleClient.connect();
	   return START_STICKY;
   }

   /** A client is binding to the service with bindService() */
   @Override
   public IBinder onBind(Intent intent) {
      return mBinder;
   }

   /** Called when all clients have unbound with unbindService() */
   @Override
   public boolean onUnbind(Intent intent) {
      return mAllowRebind;
   }

   /** Called when a client is binding to the service with bindService()*/
   @Override
   public void onRebind(Intent intent) {

   }

   /** Called when The service is no longer used and is being destroyed */
   @Override
   public void onDestroy() {
	   super.onDestroy();
	   Toast.makeText(this, "Service Destroyed", Toast.LENGTH_SHORT).show();
   }

	
	@Override
	public void onConnected(Bundle arg0) {
		// Toast
		// Toast.makeText(this, "Service Connected", Toast.LENGTH_SHORT).show();
		
		// update user location
		userLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleClient);
		
		// update the distance between userLocation and geoLocation
		distance = userLocation.distanceTo(geoLocation);
		
	 /* start listening to location updates this is suitable for foreground listening, with the onLocationChanged() invoked for location updates */
	    LocationRequest locationRequest = LocationRequest.create()
								            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)/*priority - couple of options (have to choose the best one)*/
								            .setFastestInterval(5000L) // read
								            .setInterval(10000L) // read
								            .setSmallestDisplacement(75.0F); // read
	    
	    /* activate the requestloactionpdates that activate the method onloactionchanged of the locationlistner that we passed to it */
	    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleClient, locationRequest, this);
	    
		// the actual geofence action
		ArrayList<Geofence> geofences = new ArrayList<Geofence>();
		geofences.add(new Geofence.Builder()
									.setExpirationDuration(Geofence.NEVER_EXPIRE)
									.setRequestId("unique-geofence-id")
									.setCircularRegion(geoLatitude, geoLongitude, geoRadius)
/*coordinate and radius in meters*/ .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
									.setLoiteringDelay(30000) // check every 30 seconds
									.build());
		
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this, MyLocationHandler.class), PendingIntent.FLAG_UPDATE_CURRENT);
		
		// same as above - with the pendingintent
		LocationServices.GeofencingApi.addGeofences(mGoogleClient, geofences, pendingIntent);
		
		// update the geofence status
		if (distance < geoRadius){
			geoStatus = 1;
			sendCheckInToServer(settings.getString("uid", "No uid"), onCampus);
			Toast.makeText(this,"AUTO : You are inside the IDC", Toast.LENGTH_SHORT).show();
		} else {
			geoStatus = 2;
			sendCheckInToServer(settings.getString("uid", "No uid"), !onCampus);
			Toast.makeText(this, "AUTO : You are outside the IDC", Toast.LENGTH_SHORT).show();
		}	
	}
	
	@Override
	public void onConnectionSuspended(int arg0) {
		// this callback will be invoked when the client is disconnected
		// it might happen e.g. when Google Play service crashes
		// when this happens, all requests are canceled,
		// and you must wait for it to be connected again
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {	
	}
	
	@Override
	public void onLocationChanged(Location location) {
		// the toast for the auto update
		if (location != null) {
			//Toast.makeText(this,"Auto Location: "+location.getLatitude()+" : "+location.getLongitude(), Toast.LENGTH_SHORT).show();
		} else {
			//Toast.makeText(this,"AUTO NULL", Toast.LENGTH_SHORT).show();
			}
	}	
	
	public void sendCheckInToServer(String userId, boolean onCampus ) {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		
		params.add(new BasicNameValuePair("userId", userId));
		params.add(new BasicNameValuePair("onCampus", Integer.toString(onCampus? 1 : 0)));
		
		new ServerCommunicator(this, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/app_send_chekin.php");
	}

	@Override
	public void doOnPostExecute(JSONObject jObj) {
		try {
			if (jObj.getInt("success") == 1){
				Toast.makeText(this,"status was updated successfuly", Toast.LENGTH_LONG).show();
			}else {
				Toast.makeText(this,"status FAILED to updated", Toast.LENGTH_LONG).show();
				Log.e("geoServisUpdateFailed", jObj.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(this,"status FAILED to updated on exception", Toast.LENGTH_LONG).show();
			Log.e("geoServisUpdateException", jObj.toString() + e.toString());
		}
	}
}
