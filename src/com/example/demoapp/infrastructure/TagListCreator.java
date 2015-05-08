package com.example.demoapp.infrastructure;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.location.Location;

import com.example.demoapp.TagsScreen;
import com.example.demoapp.helpers.ServerAsyncParent;
import com.example.demoapp.helpers.ServerCommunicator;

public class TagListCreator implements ServerAsyncParent {

	SharedPreferences settings = null;
	ArrayList<ListTagItem> listOfTags = new ArrayList<ListTagItem>();
	TagsScreen parent;

	public TagListCreator(Location userLocation, TagsScreen parent) {
		this.parent = parent;
		getDataFromServer(userLocation);
	}

	public void getDataFromServer(Location userLocation) {

		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		
		params.add(new BasicNameValuePair("latitude", Double.toString(userLocation.getLatitude())));
		params.add(new BasicNameValuePair("longitude", Double.toString(userLocation.getLongitude())));
		
		new ServerCommunicator(this, params, ServerCommunicator.METHOD_GET)
				.execute("http://ram.milab.idc.ac.il/app_get_tag.php");
	}

	@Override
	public void doOnPostExecute(JSONObject jObj) {
		try {
			setDataFromServer(jObj.getJSONArray("tags"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public void setDataFromServer(JSONArray tags) {
		try {
			
			// looping through All Users prepare the list of all records
			for (int i = 0; i < tags.length(); i++) {
				JSONObject row = tags.getJSONObject(i);
				
				Location tagLocation = new Location("tags_server");
				tagLocation.setLatitude(row.getDouble("latitude"));
				tagLocation.setLongitude(row.getDouble("longitude"));
				tagLocation.setAltitude(row.getDouble("altitude"));
				tagLocation.setAccuracy(row.getInt("accuracy"));
				tagLocation.setTime(row.getLong("timestamp"));
				
				ListTagItem newTag = new ListTagItem(row.getString("name"), tagLocation);
				
				listOfTags.add(newTag);
			}
			
			parent.onDataLoadeFromServer(listOfTags);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
