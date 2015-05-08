package com.example.demoapp.infrastructure;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;

import com.example.demoapp.NewHomeScreen;
import com.example.demoapp.helpers.ServerAsyncParent;
import com.example.demoapp.helpers.ServerCommunicator;

public class MainListCreator implements ServerAsyncParent {

	SharedPreferences settings = null;
	ArrayList<ListItem> listOfUsers = new ArrayList<ListItem>();
	NewHomeScreen parent;

	public MainListCreator(String uidList, NewHomeScreen parent) {
		this.parent = parent;
		getDataFromServer(uidList);
	}

	public void getDataFromServer(String uidList) {

		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("userId", uidList));
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
			/*
			SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault());
			String date = df.format(Calendar.getInstance().getTime());
			*/
			// looping through All Users prepare the list of all records
			for (int i = 0; i < users.length(); i++) {
				JSONObject row = users.getJSONObject(i);
				
				String imgUrl = "https://graph.facebook.com/" + row.getString("id") + "/picture?type=large";
				String defaultLocation; 
				IconStatus userStatus;
				
				if (row.getInt("oncampus") == 1){
					userStatus = IconStatus.online;
					defaultLocation = "On Campus";
				}else {
					userStatus = IconStatus.offline;
					defaultLocation = "Not on Campus";
				}
				
				ListItem newUser = new ListItem(row.getString("name"),
												defaultLocation,
												imgUrl,
												userStatus,
												row.getString("id"),
												row.getString("gcm_id"),
												row.getString("ubdate_date"),
												row.getString("school"));
				listOfUsers.add(newUser);
			}
			
			parent.onDataLoadeFromServer(listOfUsers);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
