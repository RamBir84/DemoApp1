package com.example.demoapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.demoapp.helpers.ServerAsyncParent;
import com.example.demoapp.helpers.ServerCommunicator;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class HomeScreen extends Activity implements ServerAsyncParent {
	private ListView mainList;
	SharedPreferences settings = null;

	// GCM fields
	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	// this is the project number
	String SENDER_ID = "439243586723";

	// Tag used on log messages.
	public static final String TAG = "GCM for Waldo";
	TextView mDisplay;
	GoogleCloudMessaging gcm;
	AtomicInteger msgId = new AtomicInteger();
	Context context;

	// hold the registration ID of the user
	String regid;
	String targetID;
	String message = "This is a test GCM message!!";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_screen);

		mainList = (ListView) findViewById(R.id.mainContainer);

		// Intent intent = getIntent();
		// start the geofencingService if necessary
		if (!isMyServiceRunning(geofencingService.class)) {
			// Toast.makeText(this, "before", Toast.LENGTH_SHORT).show();
			startService(new Intent(getBaseContext(), geofencingService.class));
			// Toast.makeText(this, "after", Toast.LENGTH_SHORT).show();
		}
		// int UserId = intent.getIntExtra("UserId", -3);
		settings = getSharedPreferences("UserInfo", 0);
		int UserId = settings.getInt("uid", -3);
		getDataFromServer(UserId);

		// GCM
		// mDisplay = (TextView) findViewById(R.id.display);
		context = getApplicationContext();

		// Check device for Play Services APK. If check succeeds, proceed with
		// GCM registration.
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(this);
			regid = getRegistrationId(context);
			if (regid.isEmpty()) {
				registerInBackground();
			}
		} else {
			Log.i(TAG, "No valid Google Play Services APK found.");
		}
		/*---Print MY ID---*/
		Toast.makeText(this, regid, Toast.LENGTH_LONG).show();
		Log.v("REGID", regid);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Check device for Play Services APK.
		checkPlayServices();
	}

	// Method to stop the service
	public void stopService(View view) {
		stopService(new Intent(getBaseContext(), geofencingService.class));
	}

	public void getDataFromServer(int userId) {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("userId", Integer.toString(userId)));
		new ServerCommunicator(this, params, ServerCommunicator.METHOD_GET)
				.execute("http://ram.milab.idc.ac.il/app_get_users.php");
	}

	@Override
	public void doOnPostExecute(JSONObject jObj) {
		try {
			setDataFromServer(jObj.getJSONArray("users"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public void setDataFromServer(JSONArray users) {
		try {

			// create the grid item mapping
			String[] from = new String[] { "UserId", "Name", "OnCampus" };
			int[] to = new int[] { R.id.UserId, R.id.Name, R.id.OnCampus };

			// looping through All Users prepare the list of all records
			List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
			for (int i = 0; i < users.length(); i++) {
				HashMap<String, String> map = new HashMap<String, String>();
				JSONObject row = users.getJSONObject(i);
				map.put("UserId", row.getString("UserId"));
				map.put("Name", row.getString("Name"));
				map.put("OnCampus", row.getString("OnCampus"));
				fillMaps.add(map);
			}

			// fill in the grid_item layout
			SimpleAdapter adapter = new SimpleAdapter(this, fillMaps, R.layout.list_item, from, to);
			mainList.setAdapter(adapter);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * public void sendCheckInToServer(int userId, boolean onCampus) {
	 * ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
	 * params.add(new BasicNameValuePair("userId", Integer.toString(userId)));
	 * params.add(new BasicNameValuePair("onCampus",
	 * Boolean.toString(onCampus))); new ServerCommunicator(this, params,
	 * ServerCommunicator.METHOD_POST)
	 * .execute("http://ram.milab.idc.ac.il/app_get_users.php"); }
	 */

	// Handler the geofence button
	public void getGeofenceStatus(View view) {

		if ((geofencingService.geoStatus == 1) || (geofencingService.geoStatus == 4)) {
			Toast.makeText(this, "MANUAL : You are inside the IDC", Toast.LENGTH_SHORT).show();
		} else if (geofencingService.geoStatus == 2) {
			Toast.makeText(this, "MANUAL : You are outside the IDC", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "geoStatus NULL", Toast.LENGTH_SHORT).show();
		}
	}

	// check if the geofencingService is running
	private boolean isMyServiceRunning(Class<geofencingService> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If it
	 * doesn't, display a dialog that allows users to download the APK from the
	 * Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	/**
	 * Stores the registration ID and the app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param context
	 *            application's context.
	 * @param regId
	 *            registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getGcmPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	/**
	 * Gets the current registration ID for application on GCM service, if there
	 * is one.
	 * <p>
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	// this method checks if the user already registered, and return his
	// registration Id
	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and the app versionCode in the application's
	 * shared preferences.
	 */
	// register for the service (only in the first time)
	private void registerInBackground() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					regid = gcm.register(SENDER_ID);
					msg = "Device registered, registration ID=" + regid;

					// You should send the registration ID to your server over
					// HTTP, so it
					// can use GCM/HTTP or CCS to send messages to your app.
					sendRegistrationIdToBackend();

					// For this demo: we don't need to send it because the
					// device will send
					// upstream messages to a server that echo back the message
					// using the
					// 'from' address in the message.

					// Persist the regID - no need to register again.
					storeRegistrationId(context, regid);
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				// mDisplay.append(msg + "\n");
			}
		}.execute(null, null, null);
	}

	// Send an upstream message.
	public void onClickSendData(final View view) {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		System.out.println("the registration id: " + regid);
		
		/* here we put the reciever id" */
		params.add(new BasicNameValuePair("target", targetID));
		/* here we put the message we want to sent" */
		params.add(new BasicNameValuePair("message", message));
		
		new ServerCommunicator(this, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/GCM_send_message.php");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}


    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(HomeScreen.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
    	
      // Your implementation here.
      // we can update the server here about the registretion ID
    }
    
}
