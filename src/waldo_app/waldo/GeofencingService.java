package waldo_app.waldo;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationServices;

import waldo_app.waldo.helpers.ServerAsyncParent;
import waldo_app.waldo.helpers.ServerCommunicator;
import android.app.PendingIntent;
import android.app.Service;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class GeofencingService extends Service implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		com.google.android.gms.location.LocationListener, ServerAsyncParent {

	private GoogleApiClient mGoogleClient;
	public static int geoStatus;
	private double geoLatitude, geoLongitude;
	private float geoRadius, distance;
	private static Location userLocation, geoLocation;
	static SharedPreferences settings;
	public static boolean onCampus;

	/** indicates how to behave if the service is killed */
	/** interface for clients that bind */
	IBinder mBinder;
	/** indicates whether onRebind should be used */
	boolean mAllowRebind;

	/** Called when the service is being created. */
	@Override
	public void onCreate() {
		mGoogleClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API)
				.build();
		geoLatitude = 32.17644339539441;// 32.177256142836924;
		geoLongitude = 34.835711773484945;// 34.83560096472502;
		geoRadius = 500;
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
		sendCheckInToServer(settings.getString("uid", "No uid"), false);
		return mAllowRebind;
	}

	/** Called when a client is binding to the service with bindService() */
	@Override
	public void onRebind(Intent intent) {

	}

	/** Called when The service is no longer used and is being destroyed */
	@Override
	public void onDestroy() {
		super.onDestroy();
		sendCheckInToServer(settings.getString("uid", "No uid"), false);
		SharedPreferences.Editor editor = settings.edit();
		if (settings.getInt("on_campus", 0) != 3 && settings.getInt("on_campus", 0) != 4) {
			editor.putInt("on_campus", 2);
		} else {
			editor.putInt("on_campus", 4);
		}
		editor.commit();
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
	}

	@Override
	public void onConnected(Bundle arg0) {
		SharedPreferences.Editor editor = settings.edit();

		if (!checkLocationServices())
			return;

		/* update user location */
		userLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleClient);

		/* update the distance between userLocation and geoLocation */
		distance = userLocation.distanceTo(geoLocation);

		/*
		 * LocationRequest locationRequest = LocationRequest.create()
		 * .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)/*priority -
		 * couple of options (have to choose the best one)
		 * .setFastestInterval(5000L) .setInterval(10000L)
		 * .setSmallestDisplacement(75.0F);
		 * LocationServices.FusedLocationApi.requestLocationUpdates
		 * (mGoogleClient, locationRequest, this);
		 */

		/*-------------------- The actual geofence action -----------------*/
		ArrayList<Geofence> geofences = new ArrayList<Geofence>();

		// 1800000, check every 30 minutes (1000 = 1 sec)
		geofences.add(new Geofence.Builder()
				.setExpirationDuration(Geofence.NEVER_EXPIRE)
				.setRequestId("unique-geofence-id")
				.setCircularRegion(geoLatitude, geoLongitude, geoRadius)
				/* coordinate and radius in meters */
				.setTransitionTypes(
						Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL
								| Geofence.GEOFENCE_TRANSITION_EXIT).setLoiteringDelay(5000)
				.build());

		/*--------------------- same as above - with the pendingintent ----------------------*/
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this,
				MyLocationHandler.class), PendingIntent.FLAG_UPDATE_CURRENT);

		LocationServices.GeofencingApi.addGeofences(mGoogleClient, geofences, pendingIntent);

		/* update the geofence status for the first time */
		if (distance < geoRadius) {
			geoStatus = 1;
			sendCheckInToServer(settings.getString("uid", "No uid"), onCampus);
			Log.v("Geo_servis", "AUTO : You are inside the IDC");
			if (settings.getInt("on_campus", 0) != 3 && settings.getInt("on_campus", 0) != 4) {
				editor.putInt("on_campus", 1);
			} else {
				editor.putInt("on_campus", 3);
			}
		} else {
			geoStatus = 2;
			sendCheckInToServer(settings.getString("uid", "No uid"), !onCampus);
			Log.v("Geo_servis", "AUTO : You are outside the IDC");
			if (settings.getInt("on_campus", 0) != 3 && settings.getInt("on_campus", 0) != 4) {
				editor.putInt("on_campus", 2);
			} else {
				editor.putInt("on_campus", 4);
			}
		}
		editor.commit();
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		sendCheckInToServer(settings.getString("uid", "No uid"), false);
		SharedPreferences.Editor editor = settings.edit();
		if (settings.getInt("on_campus", 0) != 3 && settings.getInt("on_campus", 0) != 4) {
			editor.putInt("on_campus", 2);
		} else {
			editor.putInt("on_campus", 4);
		}
		editor.commit();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		sendCheckInToServer(settings.getString("uid", "No uid"), false);
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {

		} else {

		}
	}

	public void sendCheckInToServer(String userId, boolean onCampus) {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

		params.add(new BasicNameValuePair("userId", userId));
		params.add(new BasicNameValuePair("onCampus", Integer.toString(onCampus ? 1 : 0)));

		new ServerCommunicator(this, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/app_send_chekin.php");
	}

	@Override
	public void doOnPostExecute(JSONObject jObj) {
		try {
			if (jObj.getInt("success") == 1) {
			} else {
				Log.e("geoServisUpdateFailed", jObj.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e("geoServisUpdateException", jObj.toString() + e.toString());
		}
	}

	public boolean checkLocationServices() {
		LocationAvailability locAval = LocationServices.FusedLocationApi
				.getLocationAvailability(mGoogleClient);
		return locAval.isLocationAvailable();
	}
}
