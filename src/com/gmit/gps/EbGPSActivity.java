package com.gmit.gps;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class EbGPSActivity extends Activity {

	private SoundPool sp;
	private HashMap<Integer, Integer> spMap;
	ImageView btnMap, btnLock, btnUnlock, btnMute, btnExit;
	TextView txtName;
	SharedPreferences settings;
	HttpPost httpRequest;
	HttpResponse httpResponse;
	ProgressDialog proDialog;
	private String strResult;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.i("ebgps", "EbGpsActivity onCreate");
		sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		spMap = new HashMap<Integer, Integer>();
		spMap.put(1, sp.load(this, R.raw.lock, 1));
		spMap.put(2, sp.load(this, R.raw.unlock, 1));
		if (!isServiceRunning()) {
			Intent intent = new Intent(EbGPSActivity.this, EbService.class);
			startService(intent);
		}
		Global.Server = getString(R.string.server);
		settings = getSharedPreferences("gmit.gps", 0);
		Global.Deviceid = settings.getString("deviceid", "");
		Global.Password = settings.getString("password", "");
		Global.Locked = settings.getBoolean("locked", false);
		Global.PlaySound = settings.getBoolean("playsound", true);
		btnMap = (ImageView) findViewById(R.id.btnMap);
		btnLock = (ImageView) findViewById(R.id.btnLock);
		btnUnlock = (ImageView) findViewById(R.id.btnUnlock);
		btnMute = (ImageView) findViewById(R.id.btnMute);
		btnExit = (ImageView) findViewById(R.id.btnExit);
		txtName = (TextView) findViewById(R.id.txtName);
		txtName.setText(Global.Deviceid);
		if (!Global.PlaySound) {
			btnMute.setImageResource(R.drawable.soundclose);
		}
		if (Global.Locked) {
			btnLock.setImageResource(R.drawable.lock_selected);
			btnUnlock.setImageResource(R.drawable.unlock_normal);
		} else {
			btnLock.setImageResource(R.drawable.lock_normal);
			btnUnlock.setImageResource(R.drawable.unlock_selected);
		}
		btnMap.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(EbGPSActivity.this, EbMapActivity.class);
				startActivity(intent);
			}
		});
		btnLock.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				lock(true);
			}
		});
		btnUnlock.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				lock(false);
			}
		});
		btnMute.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Global.PlaySound = !Global.PlaySound;
				if (Global.PlaySound) {
					btnMute.setImageResource(R.drawable.soundopen);
				} else {
					btnMute.setImageResource(R.drawable.soundclose);
				}
			}
		});
		btnExit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog alertDialog = new AlertDialog.Builder(EbGPSActivity.this).setTitle("退出后您将不会收到报警信息,确定退出？").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(EbGPSActivity.this, EbService.class);
						stopService(intent);
						EbGPSActivity.this.finish();
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				}).create();
				alertDialog.show();
			}
		});
	}

	// 锁定or解锁
	private void lock(final Boolean bl) {
		proDialog = new ProgressDialog(EbGPSActivity.this);
		proDialog.setMessage("正在处理,请稍候...");
		proDialog.setIndeterminate(true);
		proDialog.show();
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				httpRequest = new HttpPost(Global.Server + "/lock.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(2);
				params.add(new BasicNameValuePair("id", Global.Deviceid));
				params.add(new BasicNameValuePair("t", bl ? "1" : "0"));
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
				proDialog.dismiss();
				if (result == 1) {
					Global.Locked = bl;
					if (Global.Locked) {
						if (Global.PlaySound) {
							playSound(1, 0);
						}
						btnLock.setImageResource(R.drawable.lock_selected);
						btnUnlock.setImageResource(R.drawable.unlock_normal);
						Toast.makeText(EbGPSActivity.this, "已锁定,车辆被移动时您将收到警示信息", Toast.LENGTH_LONG).show();
					} else {
						if (Global.PlaySound) {
							playSound(2, 0);
						}
						btnLock.setImageResource(R.drawable.lock_normal);
						btnUnlock.setImageResource(R.drawable.unlock_selected);
						Toast.makeText(EbGPSActivity.this, "已解锁,现在移动车辆时您将不会收到通知", Toast.LENGTH_LONG).show();
					}
				} else if (result == 2) {
					Toast.makeText(EbGPSActivity.this, strResult.substring(2), Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(EbGPSActivity.this, "网络连接失败", Toast.LENGTH_LONG).show();
				}
			}
		}.execute();
	}

	private boolean isServiceRunning() {
		boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager) EbGPSActivity.this.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(30);
		if (!(serviceList.size() > 0)) {
			return false;
		}
		for (int i = 0; i < serviceList.size(); i++) {
			if (serviceList.get(i).service.getClassName().equals(EbService.class.getName())) {
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}

	public void playSound(int sound, int number) { // 播放声音,参数sound是播放音效的id，参数number是播放音效的次数
		AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);// 实例化AudioManager对象
		float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // 返回当前AudioManager对象的最大音量值
		float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);// 返回当前AudioManager对象的音量值
		float volumnRatio = audioCurrentVolumn / audioMaxVolumn; // 左右声道音量；
		sp.play(spMap.get(sound), // 播放的音乐id
				volumnRatio, // 左声道音量
				volumnRatio, // 右声道音量
				1, // 优先级，0为最低
				number, // 循环次数，0不循环，-1永远循环
				1 // 回放速度 ，该值在0.5-2.0之间，1为正常速度
		);
	}

	@Override
	protected void onPause() {
		Log.i("ebgps", "EbGPSActivity Pause.");
		super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.i("ebgps", "EbGPSActivity Destroy.");
		super.onDestroy();
	}
}