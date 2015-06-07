package waldo_app.waldo;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import waldo_app.waldo.helpers.ServerAsyncParent;
import waldo_app.waldo.helpers.ServerCommunicator;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Spinner;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;

public class FacebookLogInActivity extends FragmentActivity implements
		ServerAsyncParent {

	private final String PENDING_ACTION_BUNDLE_KEY = "com.facebook.samples.hellofacebook:PendingAction";

	SharedPreferences settings = null;
	private PendingAction pendingAction = PendingAction.NONE;
	private boolean canPresentShareDialog;
	private boolean canPresentShareDialogWithPhotos;
	private CallbackManager callbackManager;
	private ProfileTracker profileTracker;
	private ShareDialog shareDialog;
	private FacebookLogInActivity thisRef;
	private FacebookCallback<Sharer.Result> shareCallback = new FacebookCallback<Sharer.Result>() {
		@Override
		public void onCancel() {
			Log.d("HelloFacebook", "Canceled");
		}

		@Override
		public void onError(FacebookException error) {
			Log.d("HelloFacebook", String.format("Error: %s", error.toString()));
			String title = getString(R.string.error);
			String alertMessage = error.getMessage();
			showResult(title, alertMessage);
		}

		@Override
		public void onSuccess(Sharer.Result result) {
			Log.d("HelloFacebook", "Success!");
			if (result.getPostId() != null) {
				String title = getString(R.string.success);
				String id = result.getPostId();
				String alertMessage = getString(
						R.string.successfully_posted_post, id);
				showResult(title, alertMessage);
			}
		}

		private void showResult(String title, String alertMessage) {
			new AlertDialog.Builder(FacebookLogInActivity.this).setTitle(title)
					.setMessage(alertMessage)
					.setPositiveButton(R.string.ok, null).show();
		}
	};

	private enum PendingAction {
		NONE, POST_PHOTO, POST_STATUS_UPDATE
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FacebookSdk.sdkInitialize(this.getApplicationContext());

		settings = getSharedPreferences("UserInfo", 0);

		callbackManager = CallbackManager.Factory.create();

		settings = getSharedPreferences("UserInfo", 0);
		if (settings.contains("uid")) {
			goToHomeActivityAndFinish(settings.getString("uid", "No uid"));
		}
		setContentView(R.layout.activity_facebook_login);
		thisRef = this;
		LoginButton loginButton = (LoginButton) findViewById(R.id.loginBtn);
		loginButton.setReadPermissions("user_friends");

		LoginManager.getInstance().registerCallback(callbackManager,
				new FacebookCallback<LoginResult>() {
					@Override
					public void onSuccess(LoginResult loginResult) {

						handlePendingAction();
						updateUI();
						// sendLogInData();
					}

					@Override
					public void onCancel() {
						if (pendingAction != PendingAction.NONE) {
							showAlert();
							pendingAction = PendingAction.NONE;
						}
						updateUI();
					}

					@Override
					public void onError(FacebookException exception) {
						if (pendingAction != PendingAction.NONE
								&& exception instanceof FacebookAuthorizationException) {
							showAlert();
							pendingAction = PendingAction.NONE;
						}
						updateUI();
					}

					private void showAlert() {
						new AlertDialog.Builder(FacebookLogInActivity.this)
								.setTitle(R.string.cancelled)
								.setMessage(R.string.permission_not_granted)
								.setPositiveButton(R.string.ok, null).show();
					}
				});

		shareDialog = new ShareDialog(this);
		shareDialog.registerCallback(callbackManager, shareCallback);

		if (savedInstanceState != null) {
			String name = savedInstanceState
					.getString(PENDING_ACTION_BUNDLE_KEY);
			pendingAction = PendingAction.valueOf(name);
		}

		profileTracker = new ProfileTracker() {
			@Override
			protected void onCurrentProfileChanged(Profile oldProfile,
					Profile currentProfile) {
				updateUI();
				// It's possible that we were waiting for Profile to be
				// populated in order to
				// post a status update.
				handlePendingAction();
				sendLogInData();
			}
		};

		// Can we present the share dialog for regular links?
		canPresentShareDialog = ShareDialog.canShow(ShareLinkContent.class);

		// Can we present the share dialog for photos?
		canPresentShareDialogWithPhotos = ShareDialog
				.canShow(SharePhotoContent.class);
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

		updateUI();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		callbackManager.onActivityResult(requestCode, resultCode, data);
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
		profileTracker.stopTracking();
	}

	private void updateUI() {
	}

	private void handlePendingAction() {
		PendingAction previouslyPendingAction = pendingAction;
		// These actions may re-set pendingAction if they are still pending, but
		// we assume they
		// will succeed.
		pendingAction = PendingAction.NONE;

		switch (previouslyPendingAction) {
		case NONE:
			break;
		case POST_PHOTO:
			postPhoto();
			break;
		case POST_STATUS_UPDATE:
			postStatusUpdate();
			break;
		}
	}

	private void postStatusUpdate() {
		Profile profile = Profile.getCurrentProfile();
		ShareLinkContent linkContent = new ShareLinkContent.Builder()
				.setContentTitle("Hello Facebook")
				.setContentDescription(
						"The 'Hello Facebook' sample  showcases simple Facebook integration")
				.setContentUrl(
						Uri.parse("http://developers.facebook.com/docs/android"))
				.build();
		if (canPresentShareDialog) {
			shareDialog.show(linkContent);
		} else if (profile != null && hasPublishPermission()) {
			ShareApi.share(linkContent, shareCallback);
		} else {
			pendingAction = PendingAction.POST_STATUS_UPDATE;
		}
	}

	private void postPhoto() {
		Bitmap image = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.icon);
		SharePhoto sharePhoto = new SharePhoto.Builder().setBitmap(image)
				.build();
		ArrayList<SharePhoto> photos = new ArrayList<>();
		photos.add(sharePhoto);

		SharePhotoContent sharePhotoContent = new SharePhotoContent.Builder()
				.setPhotos(photos).build();
		if (canPresentShareDialogWithPhotos) {
			shareDialog.show(sharePhotoContent);
		} else if (hasPublishPermission()) {
			ShareApi.share(sharePhotoContent, shareCallback);
		} else {
			pendingAction = PendingAction.POST_PHOTO;
		}
	}

	private boolean hasPublishPermission() {
		AccessToken accessToken = AccessToken.getCurrentAccessToken();
		return accessToken != null
				&& accessToken.getPermissions().contains("publish_actions");
	}

	/*---------------------------------------------- Send new user data to server -----------------------------------------------------------*/
	public void sendLogInData() {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

		if (savedUserData()) {
			params.add(new BasicNameValuePair("userId", settings.getString(
					"uid", "No FB ID")));
			params.add(new BasicNameValuePair("school", settings.getString(
					"school", "No school")));
			params.add(new BasicNameValuePair("userName", settings.getString(
					"userName", "No user name")));
			params.add(new BasicNameValuePair("fb_token", settings.getString(
					"fb_token", "No FB token")));
		} else {
			Profile profile = Profile.getCurrentProfile();
			AccessToken accessToken = AccessToken.getCurrentAccessToken();
			String school = getIntent().getExtras().getString("school");

			params.add(new BasicNameValuePair("userId", profile.getId()));
			params.add(new BasicNameValuePair("school", school));
			params.add(new BasicNameValuePair("userName", profile.getName()));
			params.add(new BasicNameValuePair("fb_token", accessToken
					.getToken()));
		}
		new ServerCommunicator(this, params, ServerCommunicator.METHOD_POST)
				.execute("http://ram.milab.idc.ac.il/app_send_log_in_details.php");
	}

	public boolean savedUserData() {
		boolean saveSuccessfull = false;

		Profile profile = Profile.getCurrentProfile();
		AccessToken accessToken = AccessToken.getCurrentAccessToken();

		Spinner schoolListSpinner = (Spinner) findViewById(R.id.spinner);
		String school = schoolListSpinner.getSelectedItem().toString();

		SharedPreferences.Editor editor = settings.edit();
		editor.putString("uid", profile.getId());
		editor.putString("school", school);
		editor.putString("userName", profile.getName());
		editor.putString("fb_token", accessToken.getToken());
		editor.commit();

		if (settings.getString("uid", "no uid").equalsIgnoreCase(profile.getId())) {
			saveSuccessfull = !saveSuccessfull;
		}
		return saveSuccessfull;
	}

	public void goToHomeActivityAndFinish(String uid) {

		Intent intent = new Intent(this, NewHomeScreen.class);
		startActivity(intent);
		finish();
	}

	@Override
	public void doOnPostExecute(JSONObject jObj) {
		// TODO Auto-generated method stub
		try {
			goToHomeActivityAndFinish(jObj.getString("uid"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
