/* 
   StatusAnalyzer. Analyses current wireless ADB status. 
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

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class StatusAnalyzer {
	public static final int DumbADBPort = -1;
	public static final int DefaultADBPort = 5555;

	public static final String NetworkNameDefault = "wifi";
	public static enum Status {UNDEFINED, UP, DOWN, NO_NETWORK, NO_ADBD}
	
	private Context context; 
	private Status curStatus = Status.UNDEFINED;
	private String ipAddress;
	private int portNumber;
	
	public StatusAnalyzer(Context context) {
		this.context = context;
	}
	
	public Status analyze() {
		// Get port number
		String portNumberStr = Utils.getProp("service.adb.tcp.port");
		portNumber = -1;
		if (!Utils.isEmpty(portNumberStr)) 
			try {
				portNumber = Integer.parseInt(portNumberStr);
			} catch(NumberFormatException ex) {	}

		ipAddress = ipAddressFromWifiManager(context);
		if (ipAddress == null) 	return (curStatus = Status.NO_NETWORK); 

		// Is adbd running?
		if (Utils.getAdbdPid() < 0)	return (curStatus = Status.NO_ADBD);

		curStatus =  (portNumber > 0) ? Status.UP : Status.DOWN;
		return curStatus;
	}

	public Status getStatus() {
		return curStatus; 
	}
	
	public boolean isWirelessActive() {
		return (portNumber != DumbADBPort);
	}
	
	
	public String evaluateADBConnectString() {
		if (curStatus != Status.UP) return null; 

		StringBuilder sb = new StringBuilder();
		sb.append("adb connect ");
		sb.append(ipAddress);
		if (portNumber != DefaultADBPort) {
			sb.append(':');
			sb.append(portNumber);
		}

		return sb.toString();
	}
		

	private static String ipAddressFromWifiManager(Context context) {
		WifiManager wfm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wfm == null) return null;
		WifiInfo wfi = wfm.getConnectionInfo();
		if (wfi == null) return null;
		int ipAddr = wfi.getIpAddress();
		if (ipAddr == 0) return null;
		return ipAddrToString(ipAddr);
	}


	private static String ipAddrToString(int ipAddress) {
		try {
			Class<?> formatterClass =  Class.forName("android.text.format.Formatter");
			return  (String) formatterClass.getMethod("formatIpAddress", int.class).invoke(null, Integer.valueOf(ipAddress));
		} catch (Exception ex) {
			StringBuilder sb = new StringBuilder();
			int count = 4;
			int tmp = ipAddress;
			while (--count >= 0) {
				if (sb.length()>0) sb.append('.');
				sb.append(tmp & 0xff);
				tmp >>>= 8;
			}	
			return sb.toString();
		}
	}


	
}

