/* 
   PopupActivity. Maintains YawADB pop-up list of tasks
   This is the main activity for YawADB application. 
   Copyright (C) 2013 Michael Glickman (Australia) <palmcrust@gmail.com>

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>
*/    


package com.palmcrust.yawadb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class PopupActivity extends Activity {
	private static final int ConfigActivityRequestCode = 666;
	private Thread adbThread;
	private boolean asWidget;
	private String ifaceName;
	private BroadcastReceiver bcastReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.list);
		adbThread = null;
		asWidget = getIntent().getBooleanExtra(YawAdbConstants.AsWidgetExtra, false);
//		if (asWidget) refreshStatus();
		
		evaluateIfaceName();
		
		refreshText();

		bcastReceiver= new PopupActivityBroadcastReceiver(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(bcastReceiver, filter);

		findViewById(R.id.actConfig).setOnClickListener(clickListener);
		findViewById(R.id.actInfo).setOnClickListener(clickListener);
	}

	private void evaluateIfaceName() {
		YawAdbOptions options = new YawAdbOptions(this);
		ifaceName = new String(options.ifaceName.getString());
	}
	
	protected void refreshStatus() {
		Intent bcastedIntent = new Intent(YawAdbConstants.RefreshStatusAction);
		sendBroadcast(bcastedIntent);
	}
	
	protected void refreshText() {	
		Resources rsrc = getResources();
		
		StatusAnalyzer analyzer = new StatusAnalyzer(this, ifaceName);
		StatusAnalyzer.Status stat = analyzer.analyze();
				
		TextView tv = (TextView) findViewById(R.id.status);
		boolean toggleModeEnabled = true; 
		int colorResId = R.color.itemDisabledBkgr;

		switch(stat) {
			case UP:
				tv.setText(rsrc.getString(R.string.actStatUp,
						analyzer.evaluateADBConnectString()));
				colorResId = R.color.itemEnabledBkgr;
				break;
				
			case DOWN:
				tv.setText(R.string.actStatDown);
				break;
				
			case NO_ADBD:
				tv.setText(R.string.actNoAdbd);
				toggleModeEnabled = false;
				break;
				
			default:
				tv.setText(R.string.actNoNetwork);
				toggleModeEnabled = analyzer.isWirelessActive();
				break;
		}
		
		analyzer = null;
		tv.setOnClickListener(clickListener);
		tv.setBackgroundColor(rsrc.getColor(colorResId));
		
		tv = (TextView) findViewById(R.id.toggleMode);
		if (toggleModeEnabled) {
			boolean tag;
			if (stat == StatusAnalyzer.Status.DOWN) {
				tv.setText(R.string.actEnable);
				colorResId = R.color.itemEnabledBkgr;
				tag = true;
			} else { 
				tv.setText(R.string.actDisable);
				colorResId = R.color.itemDisabledBkgr;
				tag = false; 
			}

			tv.setTag(Boolean.valueOf(tag));
			tv.setOnClickListener(clickListener);
			tv.setBackgroundColor(rsrc.getColor(colorResId));
		} else
			tv.setVisibility(View.GONE);
		
	}


	private final View.OnClickListener clickListener = new View.OnClickListener() {

		@Override
		public void onClick(View view) {
			switch(view.getId()) {
				case R.id.status:
					Utils.showTooltip(
						PopupActivity.this, R.string.msgRefreshing, Toast.LENGTH_SHORT);
					refreshText();
					break;
					
				case R.id.toggleMode:
					changeAdbConnection(((Boolean)view.getTag()).booleanValue());
					break;
					
				case R.id.actConfig:
					startConfigActivity();
					break;

				case R.id.actInfo:
					startInfoActivity();
			}
		}
		
	};

	protected void startConfigActivity() {
		Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
		intent.putExtra(YawAdbConstants.AsWidgetExtra, asWidget);
		startActivityForResult(intent, ConfigActivityRequestCode);
	}
	
	protected void startInfoActivity() {
		Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
		startActivity(intent);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && adbThread != null) {
			adbThread.interrupt();
			adbThread = null;
		}
		return super.onKeyDown(keyCode, event);
	}

	
	@Override
	public void onDestroy() {
		unregisterReceiver(bcastReceiver);
		if (adbThread != null) adbThread.interrupt();
		super.onDestroy();
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ConfigActivityRequestCode) {
			sendBroadcast(new Intent(YawAdbConstants.OptionsChangedAction));
			if (resultCode == ConfigActivity.NewConnectionSettings) {
				StatusAnalyzer analyzer = new StatusAnalyzer(this, ifaceName);
				evaluateIfaceName();
				if (analyzer.analyze() == StatusAnalyzer.Status.UP)
					changeAdbConnection(true);
			}
				
		}

	}

	protected synchronized void changeAdbConnection(boolean enable) {
		if (adbThread == null || !adbThread.isAlive()) {
			adbThread = new AdbConnectionChangeThread(this, enable, asWidget);
			adbThread.start();
		}
	}		 

	private static class PopupActivityBroadcastReceiver extends BroadcastReceiver {
		PopupActivity activity;
		public PopupActivityBroadcastReceiver(PopupActivity activity) {
			this.activity = activity;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			activity.refreshText();
		}
	}
	
	
	protected static class ThreadHandler extends android.os.Handler {
		public static final int WHAT_REFRESH_TEXT = 1;
		public static final int WHAT_SHOW_TOOLTIP = 2;
		
		private PopupActivity activity;
		public ThreadHandler(PopupActivity activity) {
			this.activity = activity;
		}
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case WHAT_REFRESH_TEXT:
					activity.refreshText();
					break;
					
				case WHAT_SHOW_TOOLTIP:
					Utils.showTooltip(activity, msg.arg1, msg.arg2);
			}
		}
		
	}


	private static class AdbConnectionChangeThread extends Thread {
		private static final int AfterUpdateTimeout = 3000;
		private int port;
		private PopupActivity activity;
		private String shellPath;
		private boolean asWidget;
		private boolean forceKill;
		private ThreadHandler handler;

		public AdbConnectionChangeThread (PopupActivity activity, boolean enable, boolean asWidget) {
			this.activity = activity;
			this.asWidget = asWidget;
			handler = new ThreadHandler(activity);
			processOptions(enable);
		}

		private void processOptions(boolean enable) {
			YawAdbOptions options = new YawAdbOptions(activity);
			port = enable ? options.portNumber.getIntValue() : StatusAnalyzer.DumbADBPort;
			forceKill = (options.adbdRestartMethod.getIndex() != 0);
			shellPath = options.shellPath.getString();
		}
		
		
		public void run() {
			String[] cmd = new String[3];
			cmd[0] = "setprop service.adb.tcp.port " + port;
			int pid = 0;
			if (forceKill) pid = Utils.getAdbdPid();
			cmd[1] = (pid <= 0) ? "stop adbd" : ("kill -9 " + pid);
			cmd[2] = "start adbd";
			
			try {
				if (!Utils.runBatchSequence(shellPath, cmd)) 
					Message.obtain(handler, ThreadHandler.WHAT_SHOW_TOOLTIP, 
							R.string.msgCouldntExecute, Toast.LENGTH_LONG).sendToTarget();
				
				// Refresh anyway (just to be up to date)	

				// Wait 3sec for the daemon to come up
				if (port != StatusAnalyzer.DumbADBPort) {
					int countDown = 15;
					while(--countDown>=0 && Utils.getAdbdPid()<0) 
						Thread.sleep(200);
				}
				
				
				Message.obtain(handler, ThreadHandler.WHAT_REFRESH_TEXT).sendToTarget();
					
				if (asWidget) {
					activity.refreshStatus();
					if (!activity.isFinishing()) {
						if (port != StatusAnalyzer.DumbADBPort)
							Thread.sleep(AfterUpdateTimeout);
						activity.finish();
					}
					
				}
			} catch(InterruptedException ex) {}
			
		}
	}

}
