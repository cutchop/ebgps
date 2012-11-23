package com.gmit.gps;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LoginActivity extends Activity {

	HttpPost httpRequest;
	HttpResponse httpResponse;
	SharedPreferences settings;
	LinearLayout layAni;
	EditText txtDeviceid, txtPassword;
	Button btnLogin;
	private String deviceid, password, server, strResult;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				showlogin();
				break;
			case 1:
				Intent intent = new Intent();
				intent.setClass(LoginActivity.this, EbGPSActivity.class);
				startActivity(intent);
				LoginActivity.this.finish();
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		layAni = (LinearLayout) findViewById(R.id.layAni);
		txtDeviceid = (EditText) findViewById(R.id.txtDeviceid);
		txtPassword = (EditText) findViewById(R.id.txtPassword);
		btnLogin = (Button) findViewById(R.id.btnLogin);
		settings = getSharedPreferences("gmit.gps", 0);
		deviceid = settings.getString("deviceid", "");
		password = settings.getString("password", "");
		btnLogin.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				login();
			}
		});
		init();
	}

	private void init() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (deviceid.equals("") || password.equals("")) {
					handler.sendEmptyMessage(0);
					return;
				}

				new AsyncTask<Void, Void, Integer>() {
					@Override
					protected Integer doInBackground(Void... args) {
						httpRequest = new HttpPost(server + "/login.ashx");
						List<NameValuePair> params = new ArrayList<NameValuePair>(2);
						params.add(new BasicNameValuePair("id", deviceid));
						params.add(new BasicNameValuePair("psd", password));
						try {
							httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
							return 0;
						}
						try {
							httpResponse = new DefaultHttpClient().execute(httpRequest);
						} catch (ClientProtocolException e) {
							e.printStackTrace();
							return 0;
						} catch (IOException e) {
							e.printStackTrace();
							return 0;
						}
						if (httpResponse.getStatusLine().getStatusCode() == 200) {
							try {
								strResult = EntityUtils.toString(httpResponse.getEntity());
							} catch (ParseException e) {
								e.printStackTrace();
								return 0;
							} catch (IOException e) {
								e.printStackTrace();
								return 0;
							}
							if (strResult.startsWith("s|")) {
								return 1;
							} else if (strResult.startsWith("f|")) {
								return 2;
							}
						}
						return 0;
					}

					@Override
					protected void onPostExecute(Integer result) {
						if (result == 1) {
							String[] results = strResult.split("\\|");
							if (results.length > 2) {
								SharedPreferences.Editor editor = settings.edit();
								editor.putFloat("lat", Float.parseFloat(results[1]));
								editor.putFloat("lon", Float.parseFloat(results[2]));
								editor.commit();
								handler.sendEmptyMessage(1);
								return;
							}
						}
						handler.sendEmptyMessage(0);
					}
				}.execute();
			}
		}, 1000);
	}

	private void login() {
		// deviceid =
		/*
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (deviceid.equals("") || password.equals("")) {
					handler.sendEmptyMessage(0);
					return;
				}

				new AsyncTask<Void, Void, Integer>() {
					@Override
					protected Integer doInBackground(Void... args) {
						httpRequest = new HttpPost(server + "/login.ashx");
						List<NameValuePair> params = new ArrayList<NameValuePair>(2);
						params.add(new BasicNameValuePair("id", deviceid));
						params.add(new BasicNameValuePair("psd", password));
						try {
							httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
							return 0;
						}
						try {
							httpResponse = new DefaultHttpClient().execute(httpRequest);
						} catch (ClientProtocolException e) {
							e.printStackTrace();
							return 0;
						} catch (IOException e) {
							e.printStackTrace();
							return 0;
						}
						if (httpResponse.getStatusLine().getStatusCode() == 200) {
							try {
								strResult = EntityUtils.toString(httpResponse.getEntity());
							} catch (ParseException e) {
								e.printStackTrace();
								return 0;
							} catch (IOException e) {
								e.printStackTrace();
								return 0;
							}
							if (strResult.startsWith("s|")) {
								return 1;
							} else if (strResult.startsWith("f|")) {
								return 2;
							}
						}
						return 0;
					}

					@Override
					protected void onPostExecute(Integer result) {
						if (result == 1) {
							String[] results = strResult.split("\\|");
							if (results.length > 2) {
								SharedPreferences.Editor editor = settings.edit();
								editor.putString("deviceid", deviceid);
								editor.putString("password", password);
								editor.putFloat("lat", Float.parseFloat(results[1]));
								editor.putFloat("lon", Float.parseFloat(results[2]));
								editor.commit();
								handler.sendEmptyMessage(1);
								return;
							}
						}
						handler.sendEmptyMessage(0);
					}
				}.execute();
			}
		}, 1000);
		*/
		handler.sendEmptyMessage(1);
	}

	private void showlogin() {
		//layContent.setVisibility(View.VISIBLE);
		
		HeightAnimation animation = new HeightAnimation(layAni, 300, true);
		animation.setDuration(1000);
		layAni.startAnimation(animation);
		//layAni.getLayoutParams().height = 300;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			LoginActivity.this.finish();
		}
		return false;
	}
}
