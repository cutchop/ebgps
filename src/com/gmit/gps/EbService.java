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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class EbService extends Service {

	private EbBinder mBinder = new EbBinder();
	/*
	 * private ServerSocket socket = null; private Socket client = null; private
	 * String _tcpMsg;
	 */
	// private Boolean _isclosed = false;
	private Timer _timer;
	private HttpPost httpRequest;
	private HttpResponse httpResponse;
	private String strResult;

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
		_timer = new Timer();
		_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (Global.Locked) {
					handler.sendEmptyMessage(0);
				}
			}
		}, 30000, 30000);
		/*
		 * try { socket = new ServerSocket(8005); listen(); } catch (IOException
		 * e) { e.printStackTrace(); }
		 */
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
							n.defaults |= Notification.DEFAULT_SOUND;// 提示音
							n.sound = Uri.parse("file:///android_asset/tsy.wav");
							n.defaults |= Notification.DEFAULT_VIBRATE; // 震动
							n.defaults |= Notification.DEFAULT_LIGHTS; // 闪烁
							Intent i = new Intent(EbService.this, EbGPSActivity.class);
							i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
							PendingIntent contentIntent = PendingIntent.getActivity(EbService.this, R.string.app_name, i, PendingIntent.FLAG_UPDATE_CURRENT);
							n.setLatestEventInfo(EbService.this, "看车宝", "请注意查看您的电动车！", contentIntent);
							nm.notify(R.string.app_name, n);
						}
					}
				}
			}
		}.execute();
	}

	/*
	 * private void listen() { Log.i("ebservice", "listen start"); new
	 * AsyncTask<Void, Void, Integer>() {
	 * 
	 * @Override protected Integer doInBackground(Void... args) { _tcpMsg =
	 * null; try { client = socket.accept(); } catch (IOException e) {
	 * e.printStackTrace(); SystemClock.sleep(5000); return 0; } try {
	 * client.setSoTimeout(2000); } catch (SocketException e) {
	 * e.printStackTrace(); } BufferedReader in = null; try { in = new
	 * BufferedReader(new InputStreamReader(client.getInputStream(), "gb2312"));
	 * } catch (UnsupportedEncodingException e) { e.printStackTrace(); return 0;
	 * } catch (IOException e) { e.printStackTrace(); return 0; } try { _tcpMsg
	 * = in.readLine(); } catch (IOException e) { e.printStackTrace(); return 0;
	 * } try { client.close(); } catch (IOException e) { e.printStackTrace(); }
	 * return 1; }
	 * 
	 * @Override protected void onPostExecute(Integer result) { if (_tcpMsg !=
	 * null) { if (_tcpMsg.equals("caution")) { NotificationManager nm =
	 * (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	 * Notification n = new Notification(R.drawable.ic_launcher, "请注意查看您的电动车！",
	 * System.currentTimeMillis()); n.flags = Notification.FLAG_AUTO_CANCEL;
	 * Intent i = new Intent(EbService.this, EbGPSActivity.class);
	 * i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
	 * Intent.FLAG_ACTIVITY_NEW_TASK); PendingIntent contentIntent =
	 * PendingIntent.getActivity(EbService.this, R.string.app_name, i,
	 * PendingIntent.FLAG_UPDATE_CURRENT); n.setLatestEventInfo(EbService.this,
	 * "看车宝", "请注意查看您的电动车！", contentIntent); nm.notify(R.string.app_name, n); }
	 * } if (!_isclosed) { listen(); } } }.execute(); }
	 */
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
