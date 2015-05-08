package com.example.demoapp;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.demoapp.helpers.ServerAsyncParent;
import com.example.demoapp.helpers.ServerCommunicator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class LoginScreen extends Activity implements ServerAsyncParent {

	SharedPreferences settings = null;
	private EditText inputName;
	private EditText inputId;

	// private EditText inputCity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settings = getSharedPreferences("UserInfo", 0);

		if (settings.contains("uid")) {
			goToHomeActivityAndFinish(settings.getInt("uid", -1));
		}

		setContentView(R.layout.activity_login_screen);

		inputName = (EditText) findViewById(R.id.inputName);
		inputId = (EditText) findViewById(R.id.inputId);
	}

	public void sendData(View v) {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("userName", inputName.getText().toString()));
		params.add(new BasicNameValuePair("userId", inputId.getText().toString()));
		new ServerCommunicator(this, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/app_send_details.php");
	}

	@Override
	public void doOnPostExecute(JSONObject jObj) {
		try {
			goToHomeActivityAndFinish(jObj.getInt("uid"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void goToHomeActivityAndFinish(int uid) {

		if (!settings.contains("uid")) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("Username", inputName.getText().toString());
			editor.putString("UserId", inputId.getText().toString());
			editor.putInt("uid", uid);
			editor.commit();
		}

		Intent intent = new Intent(this, HomeScreen.class);
//		intent.putExtra("userId", Integer.valueOf(settings.getString("UserId", "-2")));
//		intent.putExtra("userId", settings.getInt("UserId", -2));
		startActivity(intent);
		finish();
	}
}
