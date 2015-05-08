package com.example.demoapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import com.facebook.appevents.AppEventsLogger;

public class NewLoginScreen extends Activity /*implements ServerAsyncParent*/ {
	
	SharedPreferences settings = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_login_screen);
		
		/*inputName = (EditText) findViewById(R.id.inputName);
		inputId = (EditText) findViewById(R.id.inputId);*/
		
		settings = getSharedPreferences("UserInfo", 0);
		if (settings.contains("uid")) {
			goToHomeActivityAndFinish(settings.getString("uid", "No uid"));
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		// Call the 'activateApp' method to log an app event for use in
		// analytics and advertising
		// reporting. Do so in the onResume methods of the primary Activities
		// that an app may be
		// launched into.
		AppEventsLogger.activateApp(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
	}

	@Override
	public void onPause() {
		super.onPause();

		// Call the 'deactivateApp' method to log an app event for use in
		// analytics and advertising
		// reporting. Do so in the onPause methods of the primary Activities
		// that an app may be
		// launched into.
		AppEventsLogger.deactivateApp(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	// ************* Login Button ****************//
	public void onClickLogin(View view){
		Toast.makeText(this, "Open FB LogIn", Toast.LENGTH_SHORT).show();
		
		Spinner schoolListSpinner = (Spinner) findViewById(R.id.spinner);
		String userSchool = schoolListSpinner.getSelectedItem().toString();
		
		Intent childFBLogIn = new Intent(this, FacebookLogInActivity.class);
		childFBLogIn.putExtra("school", userSchool);
		startActivity(childFBLogIn);

		Toast.makeText(this, userSchool, Toast.LENGTH_SHORT).show();
		finish();
	}

	public void goToHomeActivityAndFinish(String uid) {
		
		Intent intent = new Intent(this, NewHomeScreen.class);
		startActivity(intent);
		finish();
	}
}
