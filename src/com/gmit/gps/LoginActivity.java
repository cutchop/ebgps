package com.gmit.gps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class LoginActivity extends Activity {

	HttpPost httpRequest;
	HttpResponse httpResponse;
	SharedPreferences settings;
	LinearLayout layAni;
	EditText txtDeviceid, txtPassword;
	Button btnLogin;
	private String strResult;
	private ProgressDialog _updateDialog, _loginDialog;
	private File _downLoadFile;
	private int _fileLength, _downedFileLength = 0;
	private static final int H_W_UPDATEDIALOG_MAX = 0x01;
	private static final int H_W_UPDATEDIALOG_NOW = 0x02;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				showlogin();
				break;
			case H_W_UPDATEDIALOG_MAX:
				_updateDialog.setMax(_fileLength);
				break;
			case H_W_UPDATEDIALOG_NOW:
				int x = _downedFileLength * 100 / _fileLength;
				_updateDialog.setMessage("正在下载，已完成" + x + "%");
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
		Global.Server = getString(R.string.server);
		settings = getSharedPreferences("gmit.gps", 0);
		Global.Deviceid = settings.getString("deviceid", "");
		Global.Password = settings.getString("password", "");
		btnLogin.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				login();
			}
		});
		// 检查新版本
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				httpRequest = new HttpPost(Global.Server + "/version.ashx");
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
					if (results.length > 1) {
						if (results[1].equals(getString(R.string.version))) {
							init();
						} else {
							AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).setTitle("发现新版本,是否更新程序？").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("现在更新", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									newversion();
								}
							}).setNegativeButton("下次更新", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									init();
								}
							}).create();
							alertDialog.show();
						}
					}
				} else {
					init();
				}
			}
		}.execute();
	}

	private void init() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (Global.Deviceid.equals("") || Global.Password.equals("")) {
					handler.sendEmptyMessage(0);
					return;
				}

				new AsyncTask<Void, Void, Integer>() {
					@Override
					protected Integer doInBackground(Void... args) {
						httpRequest = new HttpPost(Global.Server + "/login.ashx");
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
								SharedPreferences.Editor editor = settings.edit();
								editor.putFloat("lat", Float.parseFloat(results[1]));
								editor.putFloat("lon", Float.parseFloat(results[2]));
								editor.putBoolean("locked", results[3].equals("1"));
								editor.commit();
								gotoMain();
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
		Global.Deviceid = txtDeviceid.getText().toString().trim();
		if (Global.Deviceid.equals("")) {
			Toast.makeText(LoginActivity.this, "请输入设备号", Toast.LENGTH_SHORT).show();
			return;
		}
		Global.Password = txtPassword.getText().toString();
		if (Global.Password.equals("")) {
			Toast.makeText(LoginActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
			return;
		}
		_loginDialog = new ProgressDialog(LoginActivity.this);
		_loginDialog.setMessage("正在登录,请稍候...");
		_loginDialog.setIndeterminate(true);
		_loginDialog.show();
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				httpRequest = new HttpPost(Global.Server + "/login.ashx");
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
				_loginDialog.dismiss();
				if (result == 1) {
					String[] results = strResult.split("\\|");
					if (results.length > 3) {
						SharedPreferences.Editor editor = settings.edit();
						editor.putString("deviceid", Global.Deviceid);
						editor.putString("password", Global.Password);
						editor.putFloat("lat", Float.parseFloat(results[1]));
						editor.putFloat("lon", Float.parseFloat(results[2]));
						editor.putBoolean("locked", results[3].equals("1"));
						editor.commit();
						gotoMain();
						return;
					}
				} else if (result == 2) {
					Toast.makeText(LoginActivity.this, strResult.substring(2), Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(LoginActivity.this, "网络连接失败", Toast.LENGTH_LONG).show();
				}
			}
		}.execute();
	}

	private void showlogin() {
		HeightAnimation animation = new HeightAnimation(layAni, 300, true);
		animation.setDuration(1000);
		layAni.startAnimation(animation);
	}

	private void gotoMain() {
		Intent intent = new Intent();
		intent.setClass(LoginActivity.this, EbGPSActivity.class);
		startActivity(intent);
		LoginActivity.this.finish();
	}

	private void newversion() {
		_updateDialog = new ProgressDialog(LoginActivity.this);
		_updateDialog.setMessage("正在等待下载新版本...");
		_updateDialog.setIndeterminate(true);
		_updateDialog.show();
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				try {
					URL url = new URL(Global.Server + "/ebgps.apk");
					URLConnection connection = url.openConnection();
					connection.connect();
					InputStream inputStream = connection.getInputStream();
					String savePath = Environment.getExternalStorageDirectory() + "/download";
					File file = new File(savePath);
					if (!file.exists()) {
						file.mkdir();
					}
					String savePathString = Environment.getExternalStorageDirectory() + "/download/ebgps.apk";
					_downLoadFile = new File(savePathString);
					if (_downLoadFile.exists()) {
						_downLoadFile.delete();
					}
					_downLoadFile.createNewFile();
					OutputStream outputStream = new FileOutputStream(_downLoadFile);
					_fileLength = connection.getContentLength();
					handler.sendEmptyMessage(H_W_UPDATEDIALOG_MAX);
					byte[] buffer = new byte[128];
					while (_downedFileLength < _fileLength) {
						int numRead = inputStream.read(buffer);
						_downedFileLength += numRead;
						outputStream.write(buffer, 0, numRead);
						handler.sendEmptyMessage(H_W_UPDATEDIALOG_NOW);
					}
					return 1;
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == 1) {
					_updateDialog.setMessage("下载完成！");
					_updateDialog.dismiss();
					Intent intent = new Intent();
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setAction(android.content.Intent.ACTION_VIEW);
					String type = "application/vnd.android.package-archive";
					intent.setDataAndType(Uri.fromFile(_downLoadFile), type);
					startActivity(intent);
				}
			}
		}.execute();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			LoginActivity.this.finish();
		}
		return false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
