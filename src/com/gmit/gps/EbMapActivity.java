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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mapabc.mapapi.core.GeoPoint;
import com.mapabc.mapapi.map.MapActivity;
import com.mapabc.mapapi.map.MapController;
import com.mapabc.mapapi.map.MapView;
import com.mapabc.mapapi.map.Overlay;

public class EbMapActivity extends MapActivity {

	MapView map;
	MapController mapcontrol;
	ImageButton btnCenter;
	ImageView btnReturn,btnRefresh;
	TextView txtName;
	// View popView;
	// TextView popText;
	SharedPreferences settings;
	HttpPost httpRequest;
	HttpResponse httpResponse;
	private Timer _timerRefresh;
	private String strResult;
	private float lon, lat, defcenterlon, defcenterlat;
	private MyOverlay overlay;
	private Boolean paused;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				if (!paused) {
					reLocation();
				}
				break;
			case 1:
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		Log.i("ebgps", "EbMapActivity onCreate");
		paused = false;
		defcenterlon = 114.3704f;
		defcenterlat = 30.5731f;
		Global.Server = getString(R.string.server);
		settings = getSharedPreferences("gmit.gps", 0);
		Global.Deviceid = settings.getString("deviceid", "");
		Global.Password = settings.getString("password", "");
		lon = settings.getFloat("lon", 0);
		lat = settings.getFloat("lat", 0);
		Global.Locked = settings.getBoolean("locked", false);
		map = (MapView) findViewById(R.id.map);
		btnCenter = (ImageButton) findViewById(R.id.btnCenter);
		btnReturn = (ImageView)findViewById(R.id.btnReturn);
		btnRefresh = (ImageView)findViewById(R.id.btnRefresh);
		txtName = (TextView)findViewById(R.id.txtName);
		txtName.setText(Global.Deviceid);
		// popView = getLayoutInflater().inflate(R.layout.overlay, null);
		// popText = (TextView) popView.findViewById(R.id.poptext);
		map.setBuiltInZoomControls(true);
		mapcontrol = map.getController();
		mapcontrol.setZoom(14);
		if (lat != 0) {
			mapcontrol.setCenter(new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6)));
			overlay = new MyOverlay();
			map.getOverlays().add(overlay);
		} else {
			mapcontrol.setCenter(new GeoPoint((int) (defcenterlat * 1E6), (int) (defcenterlon * 1E6)));
		}
		// popText.setText("未锁定");
		btnCenter.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (lat != 0) {
					mapcontrol.animateTo(new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6)));
				} else {
					Toast.makeText(EbMapActivity.this, "设备没有定位", Toast.LENGTH_SHORT).show();
				}
			}
		});
		btnReturn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EbMapActivity.this.finish();
			}
		});
		_timerRefresh = new Timer();
		_timerRefresh.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(0);
			}
		}, 30000, 30000);
	}

	// 刷新位置
	private void reLocation() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				httpRequest = new HttpPost(Global.Server + "/location.ashx");
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
						lat = Float.parseFloat(results[1]);
						lon = Float.parseFloat(results[2]);
						map.getOverlays().clear();
						if (overlay == null) {
							overlay = new MyOverlay();
						}
						map.getOverlays().add(overlay);
						return;
					}
				}
			}
		}.execute();

	}

	@Override
	protected void onPause() {
		Log.i("ebgps", "pause");
		paused = true;
		super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.i("ebgps", "destory");
		paused = true;
		_timerRefresh.cancel();
		super.onDestroy();
	}

	public class MyOverlay extends Overlay {
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			// Log.i("ebgps", "draw overlay");
			super.draw(canvas, mapView, shadow);
			Point screenPts = new Point();
			mapView.getProjection().toPixels(new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6)), screenPts);
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.mark);
			canvas.drawBitmap(bmp, screenPts.x - 15, screenPts.y - 50, null);
		}

		@Override
		public boolean onTap(GeoPoint arg0, MapView arg1) {
			return super.onTap(arg0, arg1);
		}
	}

}