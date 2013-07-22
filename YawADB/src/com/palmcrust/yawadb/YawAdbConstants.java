/* 
   YawAdbConstant. Constants used for communication between processes. 
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


public class YawAdbConstants {
	public static final String AsWidgetExtra = "com.palmcrust.yawadb.extra.FromWidget";
	public static final String ComponentNameExtra = "com.palmcrust.yawadb.extra.ComponentName";
	public static final String OnClickIntentExtra = "com.palmcrust.yawadb.extra.OnClickIntent";
	public static final String ExplicitExtra = "com.palmcrust.yawadb.extra.Explicit";
	public static final String NewAutoUsbExtra = "com.palmcrust.yawadb.extra.NewAutoUsb";
	public static final String NewPortNumberExtra = "com.palmcrust.yawadb.extra.NewePortNumber";

	public static final String OptionsChangedAction = "com.palmcrust.yawadb.action.NEWOPTIONS";
	public static final String RefreshStatusAction = "com.palmcrust.yawadb.action.REFRESH";
	public static final String ProviderRefreshAction = "com.palmcrust.yawadb.action.PROVIDERREFRESH";
	public static final String AdbModeChangedAction = "com.palmcrust.yawadb.action.ADBMODECHANGED";
	public static final String PopupAction = "com.palmcrust.yawadb.action.POPUP";
}
