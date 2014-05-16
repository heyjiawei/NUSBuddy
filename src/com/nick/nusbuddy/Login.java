package com.nick.nusbuddy;

import java.io.*;
import java.net.*;

import javax.net.ssl.*;

import org.json.*;

import android.os.*;
import android.app.*;
import android.content.*;
import android.content.SharedPreferences.*;
import android.view.*;
import android.widget.*;

public class Login extends Activity {
	
	// login async task to handle network
	public class LoginTask extends AsyncTask<Void, Void, Boolean> {
		
		// exceptions
		Exception exception;
		boolean loginNoExceptions;
		
		// for posting
		URL loginUrl;
		HttpsURLConnection loginHttpsConnection;
		JSONObject loginJsonObject;
		OutputStreamWriter loginOutputWriter;
		
		// recieving
		int responseCode;
		String responseContent;
		
		// login response details
		boolean loginSuccess;
		String loginToken;
		String loginInfo;
		String loginValid;
		String loginValidJs;
		
		@Override
		protected void onPreExecute() {
			loginNoExceptions = true;
			
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			Looper.prepare();
			try {
				
				// setting up the https connection
				loginUrl = new URL(loginUrlString);
				loginHttpsConnection = (HttpsURLConnection)loginUrl.openConnection();
				loginHttpsConnection.setConnectTimeout(60000);
				loginHttpsConnection.setReadTimeout(60000);
				loginHttpsConnection.setRequestProperty("Content-Type", "application/json");
				loginHttpsConnection.setDoInput(true);
				loginHttpsConnection.setDoOutput(true);
				
				// creating the json object
				loginJsonObject = new JSONObject();
				loginJsonObject.put("APIKey", loginApiKey);
				loginJsonObject.put("UserID", loginUserId);
				loginJsonObject.put("Password", loginPassword);
				loginJsonObject.put("Domain", "");
				
				// sending the json object into the output stream as string
				loginOutputWriter = new OutputStreamWriter(loginHttpsConnection.getOutputStream());
				loginOutputWriter.write(loginJsonObject.toString());
				loginOutputWriter.flush();
				loginOutputWriter.close();
				
				// receiving the response from server
				responseCode = loginHttpsConnection.getResponseCode();
				
				if (responseCode == 200) {
					BufferedReader br = new BufferedReader(new InputStreamReader(loginHttpsConnection.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line = br.readLine();
					while(line != null) {
						sb.append(line + "\n");
						line = br.readLine();
					}
					br.close();
					responseContent = sb.toString();
					
					//Log.d("response", sb.toString());
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
				exception = e;
				loginNoExceptions = false;
				
			} finally {
				loginHttpsConnection.disconnect();
			}
			
			return loginNoExceptions;
		}
		
		@Override
		protected void onPostExecute(Boolean noExceptions) {
			pd.dismiss();
			if (noExceptions) {
				if (responseCode == 200 && responseContent.contains("Login_JSONResult")) {
					try {
						JSONObject result = new JSONObject(responseContent);
						result = result.getJSONObject("Login_JSONResult");
						loginToken = result.getString("Token");
						loginSuccess = result.getBoolean("Success");
						loginInfo = result.getString("Info");
						loginValid = result.getString("ValidTill");
						loginValidJs = result.getString("ValidTill_js");
						
						if (loginSuccess) {
							prefsEdit.putString("loginToken", loginToken);
							prefsEdit.commit();
							pd.setMessage(prefs.getString("loginToken", "123456789"));
							pd.show();
							Intent intent = new Intent(context, HomePage.class);
							startActivity(intent);
							finish();
							
						} else {
							pd.setMessage(prefs.getString("loginToken", "Invalid UserID or Password"));
							pd.show();
							
						}
						
					} catch (Exception e) {
						e.printStackTrace();
						StackTraceElement[] a = e.getStackTrace();
						StringBuilder sb = new StringBuilder();
						sb.append(e.getMessage() + "\n");
						for (int i = 0; i < a.length; i++) {
							sb.append(a[i] + "\n");
						}
						pd.setMessage(sb.toString());
						pd.show();
					} 
				} else {
					pd.setMessage(responseCode + ". ok =" + HttpURLConnection.HTTP_OK);
					pd.show();
				}
			} else {
				StackTraceElement[] a = exception.getStackTrace();
				StringBuilder sb = new StringBuilder();
				sb.append(exception.getMessage() + "\n");
				for (int i = 0; i < a.length; i++) {
					sb.append(a[i] + "\n");
				}
				pd.setMessage(sb.toString());
				pd.show();
			}
		}
		
	}
	
	public class VerifyTokenTask extends AsyncTask<String, Void, Boolean> {
		
		// exceptions
		Exception exception;
		boolean verifyNoExceptions;
		
		// for verifying
		URL verifyUrl;
		HttpsURLConnection verifyHttpsConnection;
		
		// stuff for verify
		String verifyToken;
		
		// recieving
		int responseCode;
		String responseContent;
		private String receivedToken;
		private boolean receivedSuccess;
		
		
		@Override
		protected void onPreExecute() {
			verifyNoExceptions = true;
			
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			Looper.prepare();
			
			if (params == null) {
				return false;
			}
			
			verifyToken = params[0];
			try {
				verifyUrl = new URL("https://ivle.nus.edu.sg/api/Lapi.svc/Validate?APIKey=" + getString(R.string.api_key_mine) + "&Token="
						+ verifyToken + "&output=json");
				verifyHttpsConnection = (HttpsURLConnection) verifyUrl.openConnection();
				
				responseCode = verifyHttpsConnection.getResponseCode();
				
				if (responseCode == 200) {
					BufferedReader br = new BufferedReader(new InputStreamReader(verifyHttpsConnection.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line = br.readLine();
					while (line != null) {
						sb.append(line);
						line = br.readLine();
					}
					br.close();
					responseContent = sb.toString();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				exception = e;
				verifyNoExceptions = false;
				
			} finally {
				verifyHttpsConnection.disconnect();
				
			}
			
			return verifyNoExceptions;
		}
		
		@Override
		protected void onPostExecute(Boolean noExceptions) {
			pd.dismiss();
			
			if (noExceptions) {
				
				try {
					JSONObject result = new JSONObject(responseContent);
					receivedToken = result.getString("Token");
					receivedSuccess = result.getBoolean("Success");
					
					if (receivedSuccess) {
						prefsEdit.putString("loginToken", receivedToken);
						prefsEdit.commit();
						
						GetUserIdTask getUserIdTask = new GetUserIdTask();
						getUserIdTask.execute(receivedToken);
					} else {
						pd.setMessage("Stored token invalid, please sign in again");
						pd.show();
					}
					
				} catch (JSONException e) {
					e.printStackTrace();
					pd.setMessage(e.toString());
					pd.show();
				}
				
			} else {
				pd.setMessage("hello "+exception.toString());
				pd.show();
				
			}
			
		}
		
	}
	
	public class GetUserIdTask extends AsyncTask<String, Void, Boolean> {

		// exceptions
		Exception exception;
		boolean userIdNoExceptions;
		
		// for getting
		URL userIdUrl;
		HttpsURLConnection userIdHttpsConnection;
		
		// stuff for get
		String userIdToken;
		
		// recieving
		int responseCode;
		String responseContent;
		boolean receivedSuccess;
		String receivedToken;
		String receivedUserId;
		
		
		
		@Override
		protected void onPreExecute() {
			userIdNoExceptions = true;
			
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			Looper.prepare();
			
			if (params == null) {
				return false;
			}
			
			userIdToken = params[0];
			try {
				userIdUrl = new URL("https://ivle.nus.edu.sg/api/Lapi.svc/UserID_Get?APIKey=" + getString(R.string.api_key_mine) + "&Token="
						+ userIdToken + "&output=json");
				userIdHttpsConnection = (HttpsURLConnection) userIdUrl.openConnection();
				
				responseCode = userIdHttpsConnection.getResponseCode();
				
				if (responseCode == 200) {
					BufferedReader br = new BufferedReader(new InputStreamReader(userIdHttpsConnection.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line = br.readLine();
					while (line != null) {
						sb.append(line);
						line = br.readLine();
					}
					br.close();
					responseContent = sb.toString();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				exception = e;
				userIdNoExceptions = false;
				
			} finally {
				userIdHttpsConnection.disconnect();
				
			}
			
			return userIdNoExceptions;
		}
		
		@Override
		protected void onPostExecute(Boolean noExceptions) {
			pd.dismiss();
			
			if (noExceptions && responseContent != null) {
				prefsEdit.putString("userId", responseContent.substring(1, responseContent.length()-1));
				prefsEdit.commit();
				
				Intent intent = new Intent(context, HomePage.class);
				startActivity(intent);
				finish();
				
				/*try {
					JSONObject result = new JSONObject(responseContent);
					result = result.getJSONObject("Login_JSONResult");
					receivedToken = result.getString("Token");
					receivedSuccess = result.getBoolean("Success");
					
					if (receivedSuccess) {
						prefsEdit.putString("loginToken", receivedToken);
						prefsEdit.commit();
						pd.setMessage(prefs.getString("loginToken", "123456789"));
						pd.show();
						
						GetUserIdTask getUserIdTask = new GetUserIdTask();
						getUserIdTask.execute(receivedToken);
					} else {
						pd.setMessage("Stored token invalid, please sign in again");
						pd.show();
					}
					
				} catch (JSONException e) {
					e.printStackTrace();
					pd.setMessage(e.toString());
					pd.show();
				}*/
				
			} else {
				pd.setMessage("hello " + exception.toString());
				pd.show();
				
			}
			
		}

		
		
	}
	
	// this view and layout
	Context context;
	ProgressDialog pd;
	EditText editTextUserId;
	EditText editTextPassword;
	
	// verify login token
	VerifyTokenTask verifyTokenTask;
	
	// actual login
	LoginTask loginTask;
	String loginUrlString;
	String loginUserId;
	String loginPassword;
	String loginApiKey;
	
	// shared preferences
	SharedPreferences prefs;
	Editor prefsEdit;
	
	// actual user info
	String userId;
	String password;
	String token;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		context = this;
		pd = new ProgressDialog(context);
		editTextUserId = (EditText) findViewById(R.id.EditText_userid);
		editTextPassword = (EditText) findViewById(R.id.EditText_password);
		
		loginUrlString = "https://ivle.nus.edu.sg/api/Lapi.svc/Login_JSON";
		loginApiKey = getString(R.string.api_key_working);
		
		prefs = context.getSharedPreferences("NUSBuddyPrefs", MODE_PRIVATE);
		prefsEdit = prefs.edit();
		
		String oldToken = prefs.getString("loginToken", null);
		Toast.makeText(context, oldToken, Toast.LENGTH_LONG).show();
		
		if (oldToken != null) {
			pd.setMessage("Verifying stored token");
			pd.show();
			
			verifyTokenTask = new VerifyTokenTask();
			verifyTokenTask.execute(oldToken);
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}
	
	// for the testing button there
	public void next(View v) {
    	Intent i = new Intent(this, HomePage.class);
    	startActivity(i);
    }
	
	// when sign in button is pressed
	// runs the async task
	public void onClickButtonLogin(View v) {
		pd.setMessage("Logging in... Please Wait... ");
		pd.show();
		
		loginUserId = editTextUserId.getText().toString();
		loginPassword = editTextPassword.getText().toString();
		loginTask = new LoginTask();
		loginTask.execute((Void) null);
	}

}