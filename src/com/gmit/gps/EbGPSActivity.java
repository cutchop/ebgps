package com.gmit.gps;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapabc.mapapi.*;

public class EbGPSActivity extends MapActivity {

	MapView map;
	MapController mapcontrol;
	Button btnLockOrNot;
	View popView;
	TextView popText;
	private Boolean locked = false;
	private float lon, lat;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		lon = 114.3704f;
		lat = 30.5731f;
		map = (MapView) findViewById(R.id.map);
		btnLockOrNot = (Button) findViewById(R.id.btnLockOrNot);
		popView = getLayoutInflater().inflate(R.layout.overlay, null);
		popText = (TextView) popView.findViewById(R.id.poptext);
		map.setBuiltInZoomControls(true);
		mapcontrol = map.getController();
		mapcontrol.setZoom(14);
		mapcontrol.setCenter(new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6)));
		btnLockOrNot.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (locked) {
					locked = false;
					btnLockOrNot.setText("锁定");
					popText.setText("未锁定");
					Toast.makeText(EbGPSActivity.this, "已解锁,现在移动车辆时您将不会收到通知", Toast.LENGTH_LONG).show();
				} else {
					locked = true;
					btnLockOrNot.setText("解锁");
					popText.setText("已锁定");
					Toast.makeText(EbGPSActivity.this, "已锁定,车辆被移动时您将收到警示信息", Toast.LENGTH_LONG).show();
				}
			}
		});
		popText.setText("未锁定");

		map.addView(popView, new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT, MapView.LayoutParams.WRAP_CONTENT, new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6)), MapView.LayoutParams.BOTTOM_CENTER));
		// reLocation();
	}

	private void reLocation() {
		((MapView.LayoutParams) popView.getLayoutParams()).point = new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6));
	}
}