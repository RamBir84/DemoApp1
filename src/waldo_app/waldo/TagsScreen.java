package waldo_app.waldo;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationServices;

import waldo_app.waldo.helpers.ServerAsyncParent;
import waldo_app.waldo.helpers.ServerCommunicator;
import waldo_app.waldo.helpers.Utils;
import waldo_app.waldo.infrastructure.ListTagItem;
import waldo_app.waldo.infrastructure.TagListAdapter;
import waldo_app.waldo.infrastructure.TagListCreator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

public class TagsScreen extends Activity implements ServerAsyncParent,
		GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
		com.google.android.gms.location.LocationListener {

	private ListView mainTagContainer;
	private ImageButton btnClosePopup, btnSendTag;
	private PopupWindow pwindo;
	private FrameLayout blur_layout;
	private Location userLocation;
	private ArrayList<ListTagItem> TagsList; // change name!!
	private String targetID, message;
	private EditText tagEdit;
	private int position;
	private GoogleApiClient mGoogleClient;
	private TagsScreen thisRef;

	SharedPreferences settings = null;
	public static boolean isOnline = false;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_tags_screen);
		thisRef = this;
		blur_layout = (FrameLayout) findViewById(R.id.tagScreenFrame);
		blur_layout.getForeground().setAlpha(0);
		mGoogleClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API)
				.build();
		mGoogleClient.connect();

		// Start geofencing service
		if (!isMyServiceRunning(GeofencingService.class)) {
			startService(new Intent(getBaseContext(), GeofencingService.class));
		}

		settings = getSharedPreferences("UserInfo", 0);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Store our shared preference
		SharedPreferences sp = getSharedPreferences("OURINFO", MODE_PRIVATE);
		Editor ed = sp.edit();
		ed.putBoolean("active", true);
		ed.commit();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Store our shared preference
		SharedPreferences sp = getSharedPreferences("OURINFO", MODE_PRIVATE);
		Editor ed = sp.edit();
		ed.putBoolean("active", false);
		ed.commit();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		targetID = intent.getExtras().getString("gcm_id");
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		finish();
	}

	/*---------------------------------------------------------- External methods  --------------------------------------------*/
	private void chooseTagsAndDisplay() {
		float radius = userLocation.getAccuracy();
		Location tagLocation;
		ArrayList<ListTagItem> tagsInUserLocation = new ArrayList<ListTagItem>();

		for (int i = 0; i < TagsList.size(); i++) {
			tagLocation = TagsList.get(i).tag_location;
			if ((userLocation.distanceTo(tagLocation) - tagLocation.getAccuracy()) <= (radius + 100)) {
				tagsInUserLocation.add(new ListTagItem(TagsList.get(i).tag, tagLocation));
			}
		}
		mainTagContainer = (ListView) findViewById(R.id.mainTagContainer);
		ListAdapter listAdapter = new TagListAdapter(this, tagsInUserLocation);
		mainTagContainer.setAdapter(listAdapter);

	}

	// Menu Button
	public void onClickTagMenu(final View view) {
		triggerNotification();
	}

	// Add New Tag Button
	public void onClickAdd(final View view) {
		// Open add tag popup
		initiatePopupWindow();
	}

	// Choosing the tag
	public void onClickItem(final View view) {

		targetID = getIntent().getExtras().getString("gcm_id");
		settings = getSharedPreferences("UserInfo", 0);
		position = (Integer) view.getTag();
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

		StringBuilder gcm_message = new StringBuilder();
		gcm_message.append(2).append(",").append(settings.getString("uid", "Your friend"))
				.append(",").append(settings.getString("userName", "Your friend")).append(",")
				.append(TagListAdapter.items.get(position).tag).append(".");
		message = gcm_message.toString();

		/* here we put the receiver id" */
		params.add(new BasicNameValuePair("target", targetID));
		/* here we put the message we want to sent" */
		params.add(new BasicNameValuePair("message", message));

		new ServerCommunicator(new ServerAsyncParent() {

			@Override
			public void doOnPostExecute(JSONObject jObj) {
				int gcmResponsStatus = 0;

				try {
					gcmResponsStatus = jObj.getInt("success");
				} catch (JSONException e) {
					e.printStackTrace();
				}

				if (gcmResponsStatus == 1) {
					Log.v("GCM", "Send tag respons success!!" + " Status: " + gcmResponsStatus
							+ " " + jObj.toString());
				} else {
					Log.v("GCM", "Send tag respons failed!!" + " Status: " + gcmResponsStatus + " "
							+ jObj.toString());
				}
			}
		}, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/GCM_send_message.php");

		startActivity(new Intent(this, NewHomeScreen.class));
		finish();
	}

	/*---------------------------- Add tag popup  ----------------------------------*/
	private void initiatePopupWindow() {

		LayoutInflater inflater = (LayoutInflater) TagsScreen.this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.activity_add_tag_popup,
				(ViewGroup) findViewById(R.id.add_tag_popup));
		pwindo = new PopupWindow(layout, (int) (NewHomeScreen.Width / 1.1),
				(int) (NewHomeScreen.Height / 2.5), false);

		pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
		btnClosePopup = (ImageButton) layout.findViewById(R.id.btn_close_add_Tag);
		btnClosePopup.setOnClickListener(cancel_add_tag_click_listener);
		btnSendTag = (ImageButton) layout.findViewById(R.id.btn_send_tag);
		btnSendTag.setOnClickListener(send_tag_listener);
		tagEdit = (EditText) layout.findViewById(R.id.searchBoxAdd);

		// blur background and disable layout
		blur_layout.getForeground().setAlpha(190);
		pwindo.setFocusable(true);
		pwindo.update();
	}

	private OnClickListener cancel_add_tag_click_listener = new OnClickListener() {
		public void onClick(View v) {
			// restore blur and enable layout
			blur_layout.getForeground().setAlpha(0);
			pwindo.showAsDropDown((View) v.getParent());
			pwindo.dismiss();
		}
	};

	private OnClickListener send_tag_listener = new OnClickListener() {
		public void onClick(View v) {
			// close popup and reset blur
			blur_layout.getForeground().setAlpha(0);
			pwindo.dismiss();
			// Take tag name
			String tag = (tagEdit.getText().toString());
			// Send the tag
			sendTag(tag, userLocation);
			Toast.makeText(TagsScreen.this, "The new tag: " + tag + " was swent.",
					Toast.LENGTH_SHORT).show();
			startActivity(new Intent(TagsScreen.this, NewHomeScreen.class));
			finish();
		}
	};

	/*---------------------------- Geofencing  ----------------------------------*/
	private boolean isMyServiceRunning(Class<GeofencingService> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/*---------------------------- Server communication methods  ----------------------------------*/
	public void sendTag(String tag, Location tagLocation) {
		targetID = getIntent().getExtras().getString("gcm_id");
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		// Send tag to friend
		StringBuilder gcm_message = new StringBuilder();
		gcm_message.append(2).append(",").append(settings.getString("uid", "Your friend"))
				.append(",").append(settings.getString("userName", "Your friend")).append(",")
				.append(tag).append(".");
		message = gcm_message.toString();

		/* here we put the receiver id" */
		params.add(new BasicNameValuePair("target", targetID));
		/* here we put the message we want to sent" */
		params.add(new BasicNameValuePair("message", message));

		new ServerCommunicator(new ServerAsyncParent() {

			@Override
			public void doOnPostExecute(JSONObject jObj) {
				int gcmResponsStatus = 0;

				try {
					gcmResponsStatus = jObj.getInt("header");
				} catch (JSONException e) {
					e.printStackTrace();
				}

				if (gcmResponsStatus == 1) {
				} else {
					Log.v("GCM_New_Tag", "Send new Tag to friend failed" + jObj.toString());
				}
			}
		}, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/GCM_send_message.php");

		// Send tag to the server
		params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("name", tag));
		params.add(new BasicNameValuePair("latitude", Double.toString(tagLocation.getLatitude())));
		params.add(new BasicNameValuePair("longitude", Double.toString(tagLocation.getLongitude())));
		params.add(new BasicNameValuePair("altitude", Double.toString(tagLocation.getAltitude())));
		params.add(new BasicNameValuePair("bearing", Float.toString(tagLocation.getBearing())));
		params.add(new BasicNameValuePair("accuracy", Float.toString(tagLocation.getAccuracy())));
		params.add(new BasicNameValuePair("timestamp", Long.toString(tagLocation.getTime())));
		new ServerCommunicator(this, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/app_send_tag.php");
	}

	@Override
	public void doOnPostExecute(JSONObject jObj) {
		try {
			if (jObj.getInt("success") == 1) {
				Log.v("New_Tag", "Send new Tag to server success" + jObj.toString());
			} else {
				Log.v("New_Tag", "Send new Tag to server failed" + jObj.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void triggerNotification() {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setSmallIcon(R.drawable.ic_notification);
		mBuilder.setContentTitle("Waldo Notification!");
		mBuilder.setContentText("Hi, This is a Test Notification");
		mBuilder.setDefaults(Notification.DEFAULT_ALL);

		Intent resultIntent = new Intent(this, TagsScreen.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(TagsScreen.class);

		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// notificationID allows you to update the notification later on.
		mNotificationManager.notify(123, mBuilder.build());
	}
	
	public void checkLocationServices(Activity runningActivity) {

		LocationAvailability locAval = LocationServices.FusedLocationApi
				.getLocationAvailability(mGoogleClient);
		boolean isLocAval = locAval.isLocationAvailable();

		if (!isLocAval) {
			Utils.displayPromptForEnablingGPS(runningActivity);
		} else {
			userLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleClient);
			new TagListCreator(userLocation, this);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 100) {
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					checkLocationServices(thisRef);
				}
			}, 3000);
		}
	}
	
	public void onDataLoadeFromServer(ArrayList<ListTagItem> listOfTags) {
		TagsList = listOfTags;
		chooseTagsAndDisplay();
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		// TODO Auto-generated method stub
		checkLocationServices(this);

	}
	
	@Override
	public void onConnectionSuspended(int cause) {
		// TODO Auto-generated method stub
	}
}
