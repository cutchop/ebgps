package com.gmit.gps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mapabc.mapapi.core.*;
import com.mapabc.mapapi.map.*;

public class EbGPSActivity extends MapActivity {

	MapView map;
	MapController mapcontrol;
	Button btnLockOrNot;
	ImageButton btnCenter;
	View popView;
	TextView popText;
	private Boolean locked = false;
	private float lon, lat;
	private MyOverlay overlay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		lon = 114.3704f;
		lat = 30.5731f;
		map = (MapView) findViewById(R.id.map);
		btnLockOrNot = (Button) findViewById(R.id.btnLockOrNot);
		btnCenter = (ImageButton) findViewById(R.id.btnCenter);
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
		btnCenter.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mapcontrol.animateTo(new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6)));
			}
		});
		overlay = new MyOverlay();
		map.getOverlays().add(overlay);
	}

	private void reLocation() {
		map.getOverlays().clear();
		map.getOverlays().add(overlay);
	}

	public class MyOverlay extends Overlay {
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);
			Point screenPts = new Point();
			mapView.getProjection().toPixels(new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6)), screenPts);
			// ---add the marker---
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.mark);
			canvas.drawBitmap(bmp, screenPts.x - 15, screenPts.y - 50, null);
		}

		@Override
		public boolean onTap(GeoPoint arg0, MapView arg1) {
			return super.onTap(arg0, arg1);
		}
	}

}