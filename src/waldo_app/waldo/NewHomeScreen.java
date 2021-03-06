package waldo_app.waldo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import waldo_app.waldo.helpers.ServerAsyncParent;
import waldo_app.waldo.helpers.ServerCommunicator;
import waldo_app.waldo.infrastructure.BitmapPosition;
import waldo_app.waldo.infrastructure.IconStatus;
import waldo_app.waldo.infrastructure.ListItem;
import waldo_app.waldo.infrastructure.MainListAdapter;
import waldo_app.waldo.infrastructure.MainListCreator;
import waldo_app.waldo.infrastructure.CircleImageView;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
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

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.squareup.picasso.Picasso;

public class NewHomeScreen extends Activity implements ServerAsyncParent {

	private ListView mainContainer;
	private ImageButton btnClosePopup;
	private PopupWindow pwindo;
	public int position, textLength;
	private FrameLayout blur_layout;
	private ArrayList<ListItem> userData, updatedUserData, friendsList;
	private LinearLayout searchBoxLayout, VisbleLayout, visibleButton, invisibleButton;
	private CircleImageView userProfile, userProfileInvisibleMode;
	private EditText searchBox;
	ListAdapter baseListAdapter;
	public MainListAdapter myAdapter, adapter;
	public static Bitmap bitmap;
	public String UserId;
	private String regid, targetID, locationSenderId, locationSenderTag, fb_url;
	public static int Width, Height;
	private GoogleCloudMessaging gcm;
	private Context context;
	private GraphRequest request;

	private static final int SETTINGS_RESULT = 1;
	private int onCampus = 5;
	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private String message = "This is a test GCM message!!";
	private String SENDER_ID = "439243586723"; // This is the project ID number
	public static final String TAG = "GCM for Waldo";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private Boolean usersDataLoaded = false;
	private boolean noFriends = false;
	public SharedPreferences settings = null;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("NHS_flow", "onCreate");

		// Start geofencing service
		if (!isMyServiceRunning(GeofencingService.class)) {
			startService(new Intent(getBaseContext(), GeofencingService.class));
		}

		if (getIntent().getExtras() != null) {
			locationSenderId = getIntent().getExtras().getString("user_id");
			locationSenderTag = getIntent().getExtras().getString("tag");
		}

		// Set the activity, search box, blur, and border color
		settings = getSharedPreferences("UserInfo", 0);
		UserId = settings.getString("uid", "No uid");

		setContentView(R.layout.activity_new_home_screen);
		blur_layout = (FrameLayout) findViewById(R.id.newScreenFrame);
		blur_layout.getForeground().setAlpha(0);
		searchBoxLayout = (LinearLayout) findViewById(R.id.topBarMain);
		context = getApplicationContext();
		VisbleLayout = (LinearLayout) findViewById(R.id.bottomMainLayout);

		if (settings.getInt("main_layout_status", 0) == 1) {
			VisbleLayout.setVisibility(View.INVISIBLE);
		}

		new MainListCreator(UserId, this);
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		Width = size.x;
		Height = size.y;

		/*---------------------------------------------------------- GCM --------------------------------------------*/
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

		/*----------------------------------------------- Geofencing status -----------------------------------------*/

		// Set profile picture
		fb_url = "https://graph.facebook.com/" + UserId + "/picture?type=large";
		userProfile = (CircleImageView) findViewById(R.id.user_profile);
		userProfileInvisibleMode = (CircleImageView) findViewById(R.id.user_profile_invisible_mode);

		onCampus = settings.getInt("on_campus", 0);
		if (onCampus == 1) {
			userProfile.setBorderColor(Color.parseColor("#66CD00"));
		} else {
			userProfile.setBorderColor(Color.parseColor("#CC3232"));
		}

		Picasso.with(context).load(fb_url).into(userProfile);
		Picasso.with(context).load(fb_url).into(userProfileInvisibleMode);

		/*----------------------------------------- Handling search mode --------------------------------------------*/
		textLength = 0;
		searchBox = (EditText) findViewById(R.id.searchBox);
		searchBox.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (usersDataLoaded) {
					// get the text in the EditText
					String searchString = searchBox.getText().toString();
					textLength = searchString.length();
					// clear the initial data set
					updatedUserData.clear();
					for (int i = 0; i < userData.size(); i++) {

						String name = userData.get(i).contact_name;
						if (textLength <= name.length()) {
							// compare the String in EditText with Names in the
							// ArrayList
							if (searchString.equalsIgnoreCase((String) name
									.substring(0, textLength))) {
								updatedUserData.add(userData.get(i));
							}
						}
					}
					adapter.notifyDataSetChanged();
				}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d("NHS_flow", "onStart");

		// Store our shared preference
		SharedPreferences sp = getSharedPreferences("OURINFO", MODE_PRIVATE);
		Editor ed = sp.edit();
		ed.putBoolean("active", true);
		ed.commit();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d("NHS_flow", "onStop");

		// Store our shared preference
		SharedPreferences sp = getSharedPreferences("OURINFO", MODE_PRIVATE);
		Editor ed = sp.edit();
		ed.putBoolean("active", false);
		ed.commit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("NHS_flow", "onDestroy");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d("NHS_flow", "onNewIntent");

		if (intent.getExtras() != null) {
			locationSenderId = intent.getExtras().getString("user_id");
			locationSenderTag = intent.getExtras().getString("tag");
		}
		setIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d("NHS_flow", "onResume");

		if (getIntent().getExtras() != null) {
			locationSenderId = getIntent().getExtras().getString("user_id");
			locationSenderTag = getIntent().getExtras().getString("tag");
		}
		new MainListCreator(UserId, this);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	/*---------------------------------------------------------- External methods  --------------------------------------------*/

	public void GetFacebookFriends() {
		// If the activity starts from notification (from type 'location
		// received')
		if (settings.getInt("location_received", 0) == 1) {
			// update location_received to 2
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("location_received", 2);
			editor.commit();

			ArrayList<String> list_of_ids = new ArrayList<String>();
			String IdS = settings.getString("IdS", "null");
			int i = 0;
			int j = 0;
			list_of_ids.add("");
			// insert all the Id'S into arraylist
			while (i < (IdS.length() - 1)) {
				if (IdS.charAt(i) == ',') {
					i++;
					j++;
					if (i < (IdS.length() - 1)) {
						list_of_ids.add("");
					}
				} else {
					list_of_ids.set(j, list_of_ids.get(j) + IdS.charAt(i));
					i++;
				}
			}

			// update the userData
			friendsList = new ArrayList<ListItem>();
			for (i = 0; i < userData.size(); i++) {
				for (j = 0; j < list_of_ids.size(); j++) {
					String id = list_of_ids.get(j);
					if (userData.get(i).uId.compareTo(id) == 0) {
						friendsList.add(userData.get(i));
					}
				}
			}

			// Show the list on the screen
			userData = friendsList;
			updatedUserData = new ArrayList<ListItem>(userData);
			mainContainer = (ListView) findViewById(R.id.mainContainer);
			baseListAdapter = new MainListAdapter(NewHomeScreen.this, userData);
			adapter = new MainListAdapter(NewHomeScreen.this, updatedUserData);
			mainContainer.setAdapter(adapter);
			usersDataLoaded = !usersDataLoaded;

		}
		// If the activity not starts from notification
		else {
			request = GraphRequest.newMyFriendsRequest(AccessToken.getCurrentAccessToken(),
					new GraphRequest.GraphJSONArrayCallback() {

						@Override
						public void onCompleted(JSONArray friends, GraphResponse graphResponse) {
							try {
								SharedPreferences.Editor editor = settings.edit();
								String IdS = "";
								friendsList = new ArrayList<ListItem>();
								for (int i = 0; i < userData.size(); i++) {
									for (int j = 0; j < friends.length(); j++) {
										String id = (String) friends.getJSONObject(j).getString(
												"id");
										if (userData.get(i).uId.compareTo(id) == 0) {
											friendsList.add(userData.get(i));
											IdS += id + ",";
										}
									}
								}

								editor.putString("IdS", IdS);
								editor.commit();
								userData = friendsList;
								updatedUserData = new ArrayList<ListItem>(userData);
								mainContainer = (ListView) findViewById(R.id.mainContainer);
								baseListAdapter = new MainListAdapter(NewHomeScreen.this, userData);
								adapter = new MainListAdapter(NewHomeScreen.this, updatedUserData);
								mainContainer.setAdapter(adapter);
								// myAdapter = adapter;
								usersDataLoaded = !usersDataLoaded;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
			request.executeAsync();
		}
	}

	public void onDataLoadeFromServer(ArrayList<ListItem> listOfUsers) {
		int locationSenderIndex = 0;

		// empty list (for testing)
		// listOfUsers.clear();

		if (listOfUsers.size() != 0) {
			noFriends = false;
			if (locationSenderId != null && locationSenderTag != null) {
				ArrayList<ListItem> tmpListOfUsers = new ArrayList<ListItem>();

				for (int i = 0; i < listOfUsers.size(); i++) {
					if (listOfUsers.get(i).uId.equalsIgnoreCase(locationSenderId)) {
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
			GetFacebookFriends();
		} else {
			noFriends = true;
			TextView title = (TextView) findViewById(R.id.title_invisible_mode);
			title.setText("Welcome to Waldo ");
			TextView subtitle = (TextView) findViewById(R.id.subtitle_invisible_mode);
			subtitle.setText("Invite your friends to find them faster on campus");
			// change to invisible layout
			VisbleLayout.setVisibility(View.INVISIBLE);
		}
	}

	// Menu Button
	public void onClickMenu(View view) {
		Intent i = new Intent(getApplicationContext(), SettingsScreen.class);
		startActivityForResult(i, SETTINGS_RESULT);
	}

	public void onClickUserProfile(View view) {
		// Start visibility popup
		UserVisibilityPopup(view);
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
		searchBox.setText("");
	}

	// Icon button
	public void onClickListIcon(View view) {
		position = ((BitmapPosition) view.getTag()).position;
		int viewId = view.getId();

		switch (viewId) {
		case 1:// Online
			view.setSelected(true);
			Toast.makeText(
					this,
					"Location request was sent to: "
							+ MainListAdapter.items.get(position).contact_name, Toast.LENGTH_SHORT)
					.show();
			view.setId(2);
			sendGcmLocationRquest();
			MainListAdapter.items.get(position).icon_status = IconStatus.request_sent;
			adapter.notifyDataSetChanged();
			break;

		case 2:// Request_sent
			Toast.makeText(this, "Location request was already sent", Toast.LENGTH_SHORT).show();
			adapter.notifyDataSetChanged();
			break;

		case 3:// Request_received
			startActivity(new Intent(this, TagsScreen.class));
			// Remove the above part when we manage to change the similar part
			// in the tag screen
			view.setSelected(true);
			view.setId(1);
			MainListAdapter.items.get(position).icon_status = IconStatus.online;
			adapter.notifyDataSetChanged();
			break;

		case 4:// Answer_received
			view.setSelected(true);
			view.setId(1);
			initiatePopupWindow(view);
			MainListAdapter.items.get(position).icon_status = IconStatus.online;
			adapter.notifyDataSetChanged();
			break;

		default:// Offline
			Toast.makeText(this, "The user is currently not on campus", Toast.LENGTH_SHORT).show();
			break;
		}
	}

	// ListProfile Button
	public void onClickListProfile(View view) {
		position = ((BitmapPosition) view.getTag()).position;
		initiatePopupWindow(view);
	}

	public void onClickUserProfileInvisibleMode(View view) {
		// check if we are in invisible mode
		if (!noFriends) {
			SharedPreferences.Editor editor = settings.edit();
			onCampus = settings.getInt("on_campus", 0);

			// change to visible layout
			VisbleLayout.setVisibility(View.VISIBLE);

			// Update the 'on_campus' field for visible mode(1 or 2) according
			// to the user real status
			if (onCampus == 3) {
				editor.putInt("on_campus", 1);
				userProfile.setBorderColor(Color.parseColor("#66CD00"));
				Picasso.with(context).load(fb_url).into(userProfile);
			} else {
				editor.putInt("on_campus", 2);
			}

			// change layout status to invisible(0)
			editor.putInt("main_layout_status", 0);
			editor.commit();
		}
	}

	/*---------------------------- User Visibility Popup  ----------------------------------*/
	private void UserVisibilityPopup(View view) {

		LayoutInflater inflater = (LayoutInflater) NewHomeScreen.this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.activity_user_visibility_popup,
				(ViewGroup) findViewById(R.id.user_visibility_popup));
		pwindo = new PopupWindow(layout, (int) (NewHomeScreen.Width / 1.8),
				(int) (NewHomeScreen.Height / 7), false);

		pwindo.showAtLocation(layout, Gravity.CENTER, (int) (NewHomeScreen.Width / 8) * (-1),
				(int) (NewHomeScreen.Height / 2.6) * (-1));
		btnClosePopup = (ImageButton) layout.findViewById(R.id.btn_close_visibility_popup);
		btnClosePopup.setOnClickListener(cancel_user_visibility_click_listener);
		visibleButton = (LinearLayout) layout.findViewById(R.id.visible_button);
		visibleButton.setOnClickListener(visible_button_click_listener);
		invisibleButton = (LinearLayout) layout.findViewById(R.id.invisible_button);
		invisibleButton.setOnClickListener(invisible_button_click_listener);

		// If visible
		if (settings.getInt("main_layout_status", 4) == 1) {
			TextView text = (TextView) layout.findViewById(R.id.text_view_visible);
			text.setTextColor(Color.parseColor("#ededed"));
			ImageView image = (ImageView) layout.findViewById(R.id.image_view_visible);
			Drawable drawable = context.getResources().getDrawable(R.drawable.ic_waldo_semi_green);
			image.setImageDrawable(drawable);
		}
		// If invisible
		else {
			TextView text = (TextView) layout.findViewById(R.id.text_view_invisible);
			text.setTextColor(Color.parseColor("#ededed"));
			ImageView image = (ImageView) layout.findViewById(R.id.image_view_invisible);
			Drawable drawable = context.getResources().getDrawable(R.drawable.ic_waldo_semi_red);
			image.setImageDrawable(drawable);
		}

		// blur background and disable layout
		blur_layout.getForeground().setAlpha(150);
		pwindo.setFocusable(true);
		pwindo.update();
	}

	// Exit popup button
	private OnClickListener cancel_user_visibility_click_listener = new OnClickListener() {
		public void onClick(View v) {
			// restore blur and enable layout
			blur_layout.getForeground().setAlpha(0);
			pwindo.showAsDropDown((View) v.getParent());
			pwindo.dismiss();
		}
	};

	// Visible button
	private OnClickListener visible_button_click_listener = new OnClickListener() {
		public void onClick(View v) {
			SharedPreferences.Editor editor = settings.edit();
			onCampus = settings.getInt("on_campus", 0);

			// Check if we are in invisible mode
			if (!noFriends) {
				// change to visible layout
				VisbleLayout.setVisibility(View.VISIBLE);
			} else {
				TextView title = (TextView) findViewById(R.id.title_invisible_mode);
				title.setText("Welcome to Waldo");
				TextView subtitle = (TextView) findViewById(R.id.subtitle_invisible_mode);
				subtitle.setText("Invite your friends to find them faster on campus");
			}

			// Update the 'on_campus' field for visible mode(1 or 2) according
			// to the user real status
			if (onCampus == 3) {
				editor.putInt("on_campus", 1);
				userProfile.setBorderColor(Color.parseColor("#66CD00"));
				Picasso.with(context).load(fb_url).into(userProfile);
			} else {
				editor.putInt("on_campus", 2);
			}

			// change layout status to invisible(0)
			editor.putInt("main_layout_status", 0);
			editor.commit();

			// restore blur and enable layout
			blur_layout.getForeground().setAlpha(0);
			pwindo.showAsDropDown((View) v.getParent());
			pwindo.dismiss();
		}
	};

	// Invisible button
	private OnClickListener invisible_button_click_listener = new OnClickListener() {
		public void onClick(View v) {
			SharedPreferences.Editor editor = settings.edit();
			onCampus = settings.getInt("on_campus", 0);

			TextView title = (TextView) findViewById(R.id.title_invisible_mode);
			title.setText("Your'e Invisible");
			TextView subtitle = (TextView) findViewById(R.id.subtitle_invisible_mode);
			subtitle.setText("Become visible and find your friends with Waldo");

			// change to invisible layout
			VisbleLayout.setVisibility(View.INVISIBLE);

			// Update the 'on_campus' field for invisible mode(3 or 4) according
			// to the user real status
			if (onCampus == 1) {
				editor.putInt("on_campus", 3);
				userProfile.setBorderColor(Color.parseColor("#CC3232"));
				Picasso.with(context).load(fb_url).into(userProfile);
			} else {
				editor.putInt("on_campus", 4);
			}

			// change layout status to visible(1)
			editor.putInt("main_layout_status", 1);
			editor.commit();

			// restore blur and enable layout
			blur_layout.getForeground().setAlpha(0);
			pwindo.showAsDropDown((View) v.getParent());
			pwindo.dismiss();
		}
	};

	/*---------------------------- Profile Popup  ----------------------------------*/
	private void initiatePopupWindow(View view) {

		// We need to get the instance of the LayoutInflater
		LayoutInflater inflater = (LayoutInflater) NewHomeScreen.this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.activity_answer_popup,
				(ViewGroup) findViewById(R.id.popup_element));

		pwindo = new PopupWindow(layout, (int) (Width / 1.1), (int) (Height / 2.5), false);
		pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
		btnClosePopup = (ImageButton) layout.findViewById(R.id.btn_close_popup);
		btnClosePopup.setOnClickListener(cancel_button_click_listener);
		blur_layout.getForeground().setAlpha(190);
		pwindo.setFocusable(true);
		pwindo.update();

		/*
		 * // Set date and date TextView contactDate = (TextView)
		 * layout.findViewById(R.id.answer_location_time);
		 * contactDate.setText(MainListAdapter.items.get(position).tagDateTime);
		 * // Set status message TextView contactStatus = (TextView)
		 * layout.findViewById(R.id.answer_status); if (view.getId() == 5) {
		 * contactStatus.setText("User is currently not on campus"); } else {
		 * contactStatus.setText("User is currently on campus"); }
		 */

		// Set the contact name
		TextView contactName = (TextView) layout.findViewById(R.id.answer_contact_name);
		contactName.setText(MainListAdapter.items.get(position).contact_name);
		// Set the contact location
		TextView Location = (TextView) layout.findViewById(R.id.answer_location);
		Location.setText(MainListAdapter.items.get(position).Location);
		// Set the profile picture
		ImageView profilePicture = (ImageView) layout.findViewById(R.id.answer_profile_picture);
		Drawable profileImageAsDrawable = new BitmapDrawable(NewHomeScreen.this.getResources(),
				((BitmapPosition) view.getTag()).bitmap);
		profilePicture.setImageDrawable(profileImageAsDrawable);
		Picasso.with(NewHomeScreen.this).load(MainListAdapter.items.get(position).profile_pic)
				.into(profilePicture);
	}

	private OnClickListener cancel_button_click_listener = new OnClickListener() {
		public void onClick(View v) {
			// restore blur and enable layout
			blur_layout.getForeground().setAlpha(0);
			pwindo.dismiss();
		}
	};

	/*---------------------------- Google play Services ----------------------------------*/
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

	/*---------------------------- GCM methods ----------------------------------*/
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
					sendRegistrationIdToBackend();
					storeRegistrationId(context, regid);

				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
			}
		}.execute(null, null, null);
	}

	/*
	 * The msg fields: 1. type of message 2. ID 3. name 4. tag (empty in type
	 * '1') For example: 2,301633590,or bokobza,in some place.
	 */
	public void sendGcmLocationRquest() {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		System.out.println("the registration id: " + regid);

		targetID = MainListAdapter.items.get(position).gcm_id;

		StringBuilder gcm_message = new StringBuilder();
		gcm_message.append(1).append(",").append(regid).append(",")
				.append(settings.getString("userName", "Your friend")).append(".");
		message = gcm_message.toString();

		/* here we put the reciever id" */
		params.add(new BasicNameValuePair("target", targetID));
		/* here we put the message we want to sent" */
		params.add(new BasicNameValuePair("message", message));

		new ServerCommunicator(this, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/GCM_send_message.php");
	}

	/*---------------------------- Server communication methods ----------------------------------*/
	@Override
	public void doOnPostExecute(JSONObject jObj) {
		// TODO Auto-generated method stub
		int gcmResponsStatus = 0;

		try {
			gcmResponsStatus = jObj.getInt("success");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (gcmResponsStatus == 1) {
			/*--Do here the change in the friend list item--*/
			Log.v("GCM", "Send location request success!!" + " Status: " + gcmResponsStatus + " "
					+ jObj.toString());
		} else {
			Log.v("GCM", "Send location request failed!!" + " Status: " + gcmResponsStatus + " "
					+ jObj.toString());
		}
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

	/*---------------------------- Geofencing ----------------------------------*/
	// check if the geofencingService is running
	private boolean isMyServiceRunning(Class<GeofencingService> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
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
		return getSharedPreferences(NewHomeScreen.class.getSimpleName(), Context.MODE_PRIVATE);
	}
}
