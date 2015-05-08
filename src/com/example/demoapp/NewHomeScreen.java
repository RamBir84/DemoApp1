package com.example.demoapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.demoapp.helpers.ServerAsyncParent;
import com.example.demoapp.helpers.ServerCommunicator;
import com.example.demoapp.infrastructure.BitmapPosition;
import com.example.demoapp.infrastructure.IconStatus;
import com.example.demoapp.infrastructure.ListItem;
import com.example.demoapp.infrastructure.MainListAdapter;
import com.example.demoapp.infrastructure.MainListCreator;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.squareup.picasso.Picasso;

public class NewHomeScreen extends Activity implements ServerAsyncParent {
	// Design fields
	private ListView mainContainer;
	ImageButton btnClosePopup;
	private PopupWindow pwindo;
	private int position, textLength;
	FrameLayout blur_layout;
	ArrayList<ListItem> userData, updatedUserData;
	LinearLayout searchBoxLayout;
	EditText searchBox;
	ListAdapter baseListAdapter;
	MainListAdapter myAdapter;
	public static Bitmap bitmap;
	private static final int SETTINGS_RESULT = 1;
	MainListAdapter adapter;
	Boolean usersDataLoaded = false;
	String UserId;
	
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
	String targetID = "APA91bGhtJwtxwvFSbq0GLk1lbuL_D92jJTjojfFs4wbg_bEdc_Q_gqt0AJEauUG55YdvQpEuxcBop6Yb4iKYb3RjKXtTJSAollo8EwgtZxvkkqXoQTMwOxxX5NVXFM3JXE6L6q08xa6rh9En9AK8S5kRJcQ7fxQ";
	String message = "This is a test GCM message!!";
	private String locationSenderId;
	private String locationSenderTag;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getIntent().getExtras() != null){
			locationSenderId = getIntent().getExtras().getString("user_id");	
			locationSenderTag = getIntent().getExtras().getString("tag");
		}
		
		setContentView(R.layout.activity_new_home_screen);
		blur_layout = (FrameLayout) findViewById(R.id.newScreenFrame);
		blur_layout.getForeground().setAlpha(0);
		searchBoxLayout = (LinearLayout) findViewById(R.id.topBarMain);
		
		context = getApplicationContext();
		
		// Some data
		/*fakeData = new ArrayList<ListItem>();
		fakeData.add(new ListItem("Or Bokobza", "Last seen in the library", "http://a2zhdwallpapers.com/wp-content/uploads/ktzbulkwallpaper-1423499592/profile-dp-for-boys-facebook-6.jpg", "answer_received", "ID", "10.04.15,  10:04", "IDC Herzliya"));
		fakeData.add(new ListItem("Clara Lutzky", "Last seen in the cafeteria", "http://sms.latestsms.in/wp-content/uploads/profile-pictures209.jpg", "request_sent", "ID", "10.04.15,  12:00", "IDC Herzliya"));
		fakeData.add(new ListItem("Ram Birbrayer", "Last seen in class L101", "http://wallpaperszoo.com/wp-content/uploads/2014/02/cool-profile-pictures-for-boys-on-facebook-9.jpg", "request_received", "ID", "10.04.15,  13:08", "IDC Herzliya"));
		fakeData.add(new ListItem("Jesse Ritz", "Last seen in the main entrance", "http://im2.peldata.com/bl7/66932/22bg.jpg", "online", "ID", "10.04.15,  14:04", "IDC Herzliya"));
		fakeData.add(new ListItem("Barr Solnik", "", "http://www.theprofilepictures.com/wp-content/uploads/2011/11/Boys-profile-pictures-facebook-7.jpg", "request_received", "ID", "", "IDC Herzliya"));
		fakeData.add(new ListItem("Maya Klein", "Last seen in some place", "http://www.trickspk.com/wp-content/uploads/2015/01/cute-girls-profile-photo-for-facebook-4-e851c.jpg", "offline", "ID", "09.04.15,  17:52", "IDC Herzliya"));
		fakeData.add(new ListItem("Dotan Jakoby", "Last seen in some place", "http://www.paklatest.com/wp-content/uploads/2014/12/cool-images-for-facebook-profile-for-boys-5mmm1.jpg", "offline", "ID", "09.04.15,  14:34", "IDC Herzliya"));
		fakeData.add(new ListItem("Irina Afanasyeva", "Last seen in some place", "http://www.paklatest.com/wp-content/uploads/2014/12/cool-images-for-facebook-profile-for-boys-5mmm1.jpg", "offline", "ID", "08.04.15,  20:40", "IDC Herzliya"));
		fakeData.add(new ListItem("Hagar Bass", "Last seen in some place", "http://www.paklatest.com/wp-content/uploads/2014/12/cool-images-for-facebook-profile-for-boys-5mmm1.jpg", "offline", "ID", "09.04.15,  10:04", "IDC Herzliya"));
		fakeData.add(new ListItem("Ariel Ben Moshe", "Last seen in some place", "http://www.paklatest.com/wp-content/uploads/2014/12/cool-images-for-facebook-profile-for-boys-5mmm1.jpg", "offline", "ID", "08.04.15,  10:04", "IDC Herzliya"));
		
		
		mainContainer = (ListView) findViewById(R.id.mainContainer);
		
		baseListAdapter = new MainListAdapter(this, userData);
		
		updatedUserData = new ArrayList<ListItem>(userData);
		adapter = new MainListAdapter(this, updatedUserData);
		mainContainer.setAdapter(adapter);
		myAdapter = adapter;*/
		
		settings = getSharedPreferences("UserInfo", 0);
		UserId = settings.getString("uid", "No uid");
		
		new MainListCreator(UserId, this);
		
		
		/*--------------------------------------------------------------- GCM ----------------------------------------------------------------------------*/
		// mDisplay = (TextView) findViewById(R.id.display);

		// Check device for Play Services APK. If check succeeds, proceed with GCM registration.
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(this);
			regid = getRegistrationId(context);
			if (regid.isEmpty()) {
				registerInBackground();
			}
		} else {
			Log.i(TAG, "No valid Google Play Services APK found.");
		}
		
		// Print MY ID
		Toast.makeText(this, regid, Toast.LENGTH_LONG).show();
		Log.v("REGID", regid);
		
		/*---------------------------------------------------------- Start geofencing service --------------------------------------------------------------*/
		if (!isMyServiceRunning(geofencingService.class)) {
			startService(new Intent(getBaseContext(), geofencingService.class));
		}

		/*---------------------------------------------------------- Handling search mode --------------------------------------------------------------*/
		textLength = 0;
		searchBox = (EditText) findViewById(R.id.searchBox);
		searchBox.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (usersDataLoaded){
					// get the text in the EditText
					String searchString = searchBox.getText().toString();
					textLength = searchString.length();
					
					// clear the initial data set
					updatedUserData.clear();
					for (int i = 0; i < userData.size(); i++) {
						
						String name = userData.get(i).contact_name;
						if (textLength <= name.length()) {
							// compare the String in EditText with Names in the ArrayList
							if (searchString.equalsIgnoreCase((String) name.substring(0, textLength))) {
								updatedUserData.add(userData.get(i));
							}
						}
					}
					adapter.notifyDataSetChanged();
				}
			}
		});
	}

	
	
	public void onDataLoadeFromServer(ArrayList<ListItem> listOfUsers) {
		/**----------------------    TEST    ------------------------**/
		//listOfUsers.get(1).icon_status = IconStatus.request_received;
		/**----------------------    TEST    ------------------------**/
		
		int locationSenderIndex = 0;
		
		if (locationSenderId != null && locationSenderTag != null) {
			
			ArrayList<ListItem> tmpListOfUsers = new ArrayList<ListItem>();
			
			for (int i = 0; i < listOfUsers.size(); i++) {
				if (listOfUsers.get(i).uId.equalsIgnoreCase(locationSenderId)){
					listOfUsers.get(i).Location = locationSenderTag;
					listOfUsers.get(i).icon_status = IconStatus.answer_received;
					locationSenderIndex = i;
				}
			}
			if (locationSenderIndex != 0) {
				ListItem locationSenderItem = listOfUsers.get(locationSenderIndex);
				listOfUsers.remove(locationSenderIndex);
				tmpListOfUsers.add(locationSenderItem);
				for (int i = 0; i < listOfUsers.size(); i++) {
					tmpListOfUsers.add(listOfUsers.get(i));
				}
				listOfUsers = tmpListOfUsers;
			}
		}
		
		
		userData = listOfUsers;
		updatedUserData = new ArrayList<ListItem>(userData);
		
		mainContainer = (ListView) findViewById(R.id.mainContainer);
		
		baseListAdapter = new MainListAdapter(this, userData);
		adapter = new MainListAdapter(this, updatedUserData);
		mainContainer.setAdapter(adapter);
		myAdapter = adapter;
		
		usersDataLoaded = !usersDataLoaded;
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		finish();
	}

	// Menu Button
	public void onClickMenu(View view) {
		Toast.makeText(this, "Open menu", Toast.LENGTH_SHORT).show();
		// triggerNotification();
		// *********
		Intent i = new Intent(getApplicationContext(), SettingsScreen.class);
		startActivityForResult(i, SETTINGS_RESULT);
	}

	// Search Button
	public void onClickSearch(View view) {
		searchBoxLayout.setVisibility(View.INVISIBLE);
		EditText yourEditText = (EditText) findViewById(R.id.searchBox);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(yourEditText, InputMethodManager.SHOW_IMPLICIT);
	}

	// Exit search button
	public void onClickExitSearch(View view) {
		searchBoxLayout.setVisibility(View.VISIBLE);
	}

	// Icon button
	public void onClickListIcon(View view) {
		position = ((BitmapPosition) view.getTag()).position;
		
		int viewId = view.getId();
		
		switch (viewId) {
		case 1:// Online
			view.setSelected(true);
			Toast.makeText(this, "Location request was sent to: " + MainListAdapter.items.get(position).contact_name, Toast.LENGTH_SHORT).show();
			view.setId(2);
			
			sendGcmLocationRquest(view);
			
			MainListAdapter.items.get(position).icon_status = IconStatus.request_sent;
			myAdapter.notifyDataSetChanged();
			break;
			
		case 2:// Request_sent
			Toast.makeText(this, "Location request was already sent", Toast.LENGTH_SHORT).show();
			myAdapter.notifyDataSetChanged();
			break;

		case 3:// Request_received
			startActivity(new Intent(this, TagsScreen.class));
			// Remove the above part when we manage to change the similar part
			// in the tag screen
			view.setSelected(true);
			view.setId(1);
			MainListAdapter.items.get(position).icon_status = IconStatus.online;
			myAdapter.notifyDataSetChanged();
			break;
			
		case 4:// Answer_received
			view.setSelected(true);
			view.setId(1);
			initiatePopupWindow(view);
			MainListAdapter.items.get(position).icon_status = IconStatus.online;
			myAdapter.notifyDataSetChanged();
			break;
			
		default:// Offline
			Toast.makeText(this, "The user is currently not on campus",	Toast.LENGTH_SHORT).show();
			break;
		}
		
		
		/*if (view.getId() == 1) { // Online
			view.setSelected(true);
			Toast.makeText(this, "Location request was sent to: " + MainListAdapter.items.get(position).contact_name, Toast.LENGTH_SHORT).show();
			view.setId(2);
			MainListAdapter.items.get(position).icon_status = IconStatus.request_sent;
			myAdapter.notifyDataSetChanged();

		} else {
			if (view.getId() == 2) { // Request_sent
				Toast.makeText(this, "Location request was already sent", Toast.LENGTH_SHORT).show();
				myAdapter.notifyDataSetChanged();
			}
		}
		if (view.getId() == 3) { // Request_received
			startActivity(new Intent(this, TagsScreen.class));
			// Remove the above part when we manage to change the similar part
			// in the tag screen
			view.setSelected(true);
			view.setId(1);
			MainListAdapter.items.get(position).icon_status = "online";
			myAdapter.notifyDataSetChanged();
		}
		if (view.getId() == 4) { // Answer_received
			view.setSelected(true);
			view.setId(1);
			initiatePopupWindow(view);
			MainListAdapter.items.get(position).icon_status = "online";
			myAdapter.notifyDataSetChanged();
		}
		if (view.getId() == 5) { // Offline
			Toast.makeText(this, "The user is currently not on campus",
					Toast.LENGTH_SHORT).show();
		}*/
	}

	// ListProfile Button
	public void onClickListProfile(View view) {
		position = ((BitmapPosition) view.getTag()).position;
		initiatePopupWindow(view);
	}

	// Profile popup
	private void initiatePopupWindow(View view) {

		// We need to get the instance of the LayoutInflater
		LayoutInflater inflater = (LayoutInflater) NewHomeScreen.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.activity_answer_popup,(ViewGroup) findViewById(R.id.popup_element));

		pwindo = new PopupWindow(layout, 700, 500, false);
		pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
		btnClosePopup = (ImageButton) layout.findViewById(R.id.btn_close_popup);
		btnClosePopup.setOnClickListener(cancel_button_click_listener);
		// blur background and disable layout
		blur_layout.getForeground().setAlpha(190);
		pwindo.setFocusable(true);
		pwindo.update();

		// Set the contact name
		TextView contactName = (TextView) layout.findViewById(R.id.answer_contact_name);
		contactName.setText(MainListAdapter.items.get(position).contact_name);

		// Set date and date
		TextView contactDate = (TextView) layout.findViewById(R.id.answer_location_time);
		contactDate.setText(MainListAdapter.items.get(position).tagDateTime);

		// Set status message
		TextView contactStatus = (TextView) layout.findViewById(R.id.answer_status);
		if (view.getId() == 5) {
			contactStatus.setText("User is currently not on campus");
		} else {
			contactStatus.setText("User is currently on campus");
		}

		// Set the contact location
		TextView Location = (TextView) layout.findViewById(R.id.answer_location);
		Location.setText(MainListAdapter.items.get(position).Location);

		// Set the profile picture
		ImageView profilePicture = (ImageView) layout.findViewById(R.id.answer_profile_picture);
		Drawable profileImageAsDrawable = new BitmapDrawable(NewHomeScreen.this.getResources(),((BitmapPosition) view.getTag()).bitmap);
		profilePicture.setImageDrawable(profileImageAsDrawable);
		Picasso.with(NewHomeScreen.this).load(MainListAdapter.items.get(position).profile_pic).into(profilePicture);
	}

	private OnClickListener cancel_button_click_listener = new OnClickListener() {
		public void onClick(View v) {
			// restore blur and enable layout
			blur_layout.getForeground().setAlpha(0);
			pwindo.dismiss();
		}
	};

	/*-----------------------------------------This part is for Google play Services-----------------------------------------------------*/

	/**
	 * Check the device to make sure it has the Google Play Services APK. If it
	 * doesn't, display a dialog that allows users to download the APK from the
	 * Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
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

	/*--------------------------------------------------This part is for GCM-------------------------------------------------------------*/

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
	// this method checks if the user already registered, and return his registration Id
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
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
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

	/*------------------------------------------ Send a GCM location request. --------------------------------------------------*/
	public void sendGcmLocationRquest(final View view) {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		System.out.println("the registration id: " + regid);
		
		targetID = MainListAdapter.items.get(position).gcm_id;
		
	/*  The msg fields: 1. type of message 2. ID 3. name 4. tag (empty in type '1')
		For example: 2,301633590,or bokobza,in some place.*/
		
		StringBuilder gcm_message = new StringBuilder();
		gcm_message.append(1).append(",")
					.append(regid).append(",")
					.append(settings.getString("userName", "Your friend")).append(".");
		message = gcm_message.toString();
		
		
		/* here we put the reciever id" */
		params.add(new BasicNameValuePair("target", targetID));
		/* here we put the message we want to sent" */
		params.add(new BasicNameValuePair("message", message));

		new ServerCommunicator(this, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/GCM_send_message.php");
	}

	@Override
	public void doOnPostExecute(JSONObject jObj) {
		// TODO Auto-generated method stub
		int gcmResponsStatus = 0;

		try {
			gcmResponsStatus = jObj.getInt("header");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (gcmResponsStatus == 200) {
			/*--Do here the change in the friend list item--*/
		} else {
			Log.v("GCM", "Send location request failed" + jObj.toString());
		}
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
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
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
		// This sample app persists the registration ID in shared preferences,
		// but
		// how you store the regID in your app is up to you.
		return getSharedPreferences(NewHomeScreen.class.getSimpleName(),
				Context.MODE_PRIVATE);
	}

	/**
	 * Sends the registration ID to your server over HTTP, so it can use
	 * GCM/HTTP or CCS to send messages to your app. Not needed for this demo
	 * since the device sends upstream messages to a server that echoes back the
	 * message using the 'from' address in the message.
	 */
	private void sendRegistrationIdToBackend() {

			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("userId", UserId));
			params.add(new BasicNameValuePair("gcm_id", regid));
			new ServerCommunicator(new ServerAsyncParent() {
				
				@Override
				public void doOnPostExecute(JSONObject jObj) {
					// TODO Auto-generated method stub
					
				}
			}, params, ServerCommunicator.METHOD_POST)
					.execute("http://ram.milab.idc.ac.il/app_send_gcmID.php");
		
	}

	// check if the geofencingService is running
	private boolean isMyServiceRunning(Class<geofencingService> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
