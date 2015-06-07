package waldo_app.waldo;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import waldo_app.waldo.helpers.ServerAsyncParent;
import waldo_app.waldo.helpers.ServerCommunicator;

import com.google.android.gms.location.GeofencingEvent;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class MyLocationHandler extends IntentService implements ServerAsyncParent {

	private boolean onCampus = true;
	SharedPreferences settings = GeofencingService.settings;

	public MyLocationHandler() {
		super("");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences.Editor editor = settings.edit();

		GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
		GeofencingService.geoStatus = geofencingEvent.getGeofenceTransition();

		if ((GeofencingService.geoStatus == 1) || (GeofencingService.geoStatus == 4)) {
			this.sendCheckInToServer(settings.getString("uid", "no uid"), onCampus);
			Log.v("Geo_handler", "AUTO : You are inside the IDC");

			if (settings.getInt("on_campus", 0) != 3 && settings.getInt("on_campus", 0) != 4) {
				editor.putInt("on_campus", 1);
			} else {
				editor.putInt("on_campus", 3);
			}

		} else if (GeofencingService.geoStatus == 2) {
			if (settings.getInt("on_campus", 0) != 3 && settings.getInt("on_campus", 0) != 4) {
				editor.putInt("on_campus", 2);
			} else {
				editor.putInt("on_campus", 4);
			}

			this.sendCheckInToServer(settings.getString("uid", "no uid"), !onCampus);
			Log.v("Geo_handler", "AUTO : You are outside the IDC");

		} else {
			Log.v("Geo_handler", "AUTO : geoStatus NULL");
		}
		editor.commit();
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
				Log.e("Geo_Status", "status FAILED to updated " + jObj.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e("Geo_Status", "status FAILED to updated on exaption " + jObj.toString());
		}
	}
}