package waldo_app.waldo.helpers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import waldo_app.waldo.NewHomeScreen;
import waldo_app.waldo.TagsScreen;
import waldo_app.waldo.infrastructure.MainListCreator;


import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class ServerCommunicator extends AsyncTask<String, Void, Boolean> {
	public static final String METHOD_POST = "POST";
	public static final String METHOD_GET = "GET";

	//private ProgressDialog dialog;

	// URL to update user status
	private final String method;
	private List<NameValuePair> requestParams;
	private InputStream is = null;
	private String line = "";
	private String json = "";
	private String url = "";
	private JSONObject jObj = null;
	private ServerAsyncParent parentActivity;

	public ServerCommunicator(ServerAsyncParent activity, List<NameValuePair> params, String method) {
		parentActivity = activity;
		//dialog = new ProgressDialog((Context) parentActivity);
		this.requestParams = params;
		this.method = method;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		/*dialog.setMessage("Sending request to server, please wait...");
		dialog.show();
		*/
	}

	@Override
	protected Boolean doInBackground(String... params) {
		boolean isRequestSucceeded = false;
		try {

			url = params[0];
			System.out.println("url: " + url);
			HttpResponse httpResponse;
			HttpRequestBase httpMethod;
			// create http request
			if (method == METHOD_POST) {
				HttpPost httpPost = new HttpPost(url);
				httpPost.setEntity(new UrlEncodedFormEntity(requestParams, "utf-8"));
				httpMethod = httpPost;
			} else {
				String paramString = URLEncodedUtils.format(requestParams, "utf-8");
				httpMethod = new HttpGet(url + "?" + paramString);
			}

			DefaultHttpClient httpClient = new DefaultHttpClient();

			//Test print
			Log.v("http_to_server", httpMethod.toString());
			
			// execute
			httpResponse = httpClient.execute(httpMethod);

			// get response from server and parse it to json
			HttpEntity httpEntity = httpResponse.getEntity();
			is = httpEntity.getContent();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
			StringBuilder sb = new StringBuilder();
			line = reader.readLine();
			while (line != null) {
				sb.append(line + "\n");
				line = reader.readLine();
			}

			is.close();
			json = sb.toString();
			System.out.println("json:" + json);
			// try parse the string to a JSON object
			jObj = new JSONObject(json);

			// check json success tag
			int success = jObj.getInt("success");

			if (success == 1) {
				isRequestSucceeded = true;
				Log.d("DemoApp_ServerComm", "Request succeeded: " + json.toString());
				System.out.println(":1");
			} else {
				// failed to update product
				Log.d("DemoApp_ServerComm", "Request failed" + json.toString());
				System.out.println(":2");

			}
		} catch (Exception e) {
			Log.d("DemoApp_ServerComm", "Request failed" + e);
			Log.e("ERROR", "json:" + json);
			
			e.printStackTrace();
			System.out.println(":3");
			
			
			/*
			// the problem 
			if (method == "POST"){
			NewHomeScreen home = (NewHomeScreen)parentActivity;
		//	home.settings = home.getSharedPreferences("UserInfo", 0);
		//	home.UserId = home.settings.getString("uid", "No uid");
		//	new MainListCreator(home.UserId, home);
			home.NotifyDataChanged();
		//	home.sendGcmLocationRquest();
			}
			*/
		}
		return isRequestSucceeded;
	}

	@Override
	protected void onPostExecute(Boolean isRequestSucceeded) {
		/*if (dialog.isShowing()) {
			dialog.dismiss();
		}*/

		if (isRequestSucceeded) {
			parentActivity.doOnPostExecute(jObj);
		} else {
			CharSequence text = "Send Data Faild!";
			/*int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText((Context) parentActivity, text, duration);
			toast.show();*/
			Log.v("Server_Comm", text + "To:" + url + requestParams.toString());
		}
	}

}