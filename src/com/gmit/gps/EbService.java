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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class EbService extends Service {

	private EbBinder mBinder = new EbBinder();
	private Timer _timer;
	private HttpPost httpRequest;
	private HttpResponse httpResponse;
	private String strResult;
	private SoundPool sp;
	private Integer spmusic;
	private static int Delay = 5000;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				read();
				break;
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.i("ebservice", "service start");
		sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		spmusic = sp.load(this, R.raw.caution, 1);
		_timer = new Timer();
		_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (Global.Locked) {
					handler.sendEmptyMessage(0);
				}
			}
		}, Delay, Delay);
	}

	private void read() {
		Log.i("ebservice", "read caution");
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				httpRequest = new HttpPost(Global.Server + "/caution.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(2);
				params.add(new BasicNameValuePair("id", Global.Deviceid));
				params.add(new BasicNameValuePair("psd", Global.Password));
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
						if (results[1].equals("0")) {
							Global.Locked = false;
							return;
						}
						if (results[2].equals("1")) {
							NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
							Notification n = new Notification(R.drawable.icon, "看车宝提示", System.currentTimeMillis());
							n.flags = Notification.FLAG_AUTO_CANCEL;
							n.defaults |= Notification.DEFAULT_VIBRATE; // 震动
							n.defaults |= Notification.DEFAULT_LIGHTS; // 闪烁
							Intent i = new Intent(EbService.this, EbGPSActivity.class);
							i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
							PendingIntent contentIntent = PendingIntent.getActivity(EbService.this, R.string.app_name, i, PendingIntent.FLAG_UPDATE_CURRENT);
							n.setLatestEventInfo(EbService.this, "看车宝", "请注意查看您的电动车！", contentIntent);
							nm.notify(R.string.app_name, n);
							if (Global.PlaySound)
							{
								playSound(2);
							}
						}
					}
				}
			}
		}.execute();
	}

	private void playSound(int number) { // 播放声音,参数sound是播放音效的id，参数number是播放音效的次数
		AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);// 实例化AudioManager对象
		float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // 返回当前AudioManager对象的最大音量值
		float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);// 返回当前AudioManager对象的音量值
		float volumnRatio = audioCurrentVolumn / audioMaxVolumn; // 左右声道音量；
		sp.play(spmusic, // 播放的音乐id
				volumnRatio, // 左声道音量
				volumnRatio, // 右声道音量
				1, // 优先级，0为最低
				number, // 循环次数，0不循环，-1永远循环
				1 // 回放速度 ，该值在0.5-2.0之间，1为正常速度
		);
	}

	@Override
	public void onDestroy() {
		// _isclosed = true;
		Log.i("ebservice", "service destroy");
		_timer.cancel();
		super.onDestroy();
	}

	public class EbBinder extends Binder {
		EbService getService() {
			return EbService.this;
		}
	}
}
