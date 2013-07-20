/* 
   YawAdbOption. Maintains list of ADB options.  
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
import android.content.SharedPreferences;
import android.content.res.Resources;

@SuppressWarnings("boxing")
public class YawAdbOptions {
	
	//-------------------------------------------------------------------------------
	public abstract static class Option {
		protected String key;
		protected int boxId,  nameResId;
		protected int errMsgId;
		protected Object curValue, defaultValue;
		

		protected Option(String key, int boxId, int nameResId, int errMsgId, Object defaultValue) {
			this.key = key;
			this.boxId = boxId;
			this.nameResId = nameResId;
			this.errMsgId = errMsgId;
			curValue = this.defaultValue = defaultValue;
		}
		
		public String getKey() {
			return key;
		}

		public int getBoxId() {
			return boxId;
		}

		public int getResId() {
			return nameResId;
		}

		public Object getValue() {
			return curValue;
		}

		public Object getDefaultValue() {
			return defaultValue;
		}

		public boolean setDefaultValue() {
			return setValue(defaultValue);
		}

		public boolean setValue(Object newValue) {
			boolean result = validateValue(newValue);
			if (result) curValue = newValue;
			return result;
		}

		public String getStringValue(Resources rsrc) {
			return String.valueOf(curValue);
		}

		public int getErrorMessageId() {
			return errMsgId;
		}

		@SuppressWarnings("static-method")
		public boolean validateValue(Object value) {
			return true;
		}
		
	}

	//-------------------------------------------------------------------------------
	public static class AlternativesOption extends Option {
		protected int[] choiceResIds;
		protected int defaultChoice;
		
		protected AlternativesOption(String key, int boxId, int nameResId, int errMsgId,
					int defaultChoiceIndex, int[] choiceResIds) {
			super(key, boxId, nameResId, errMsgId, defaultChoiceIndex);
			this.choiceResIds = choiceResIds;
		}

		@Override
		public boolean validateValue(Object value) {
			int intValue = (Integer) value;
			return (intValue >= 0 && intValue < choiceResIds.length);
		}

		public int getIndex() {
			return (Integer) curValue;
		}

		@Override
		public String getStringValue(Resources rsrc) {
			return rsrc.getString(choiceResIds[(Integer)curValue]);
		}

		public void nextValue() {
			int curValueInt = (Integer) curValue;
			if (++curValueInt >= choiceResIds.length) curValueInt = 0;
			curValue = curValueInt;
		}

	}

	//-------------------------------------------------------------------------------
	public static class IntegerOption extends Option {
		protected int minValue, maxValue;
		protected int defaultValue;
		

		protected IntegerOption(String key, int boxId, int nameResId, int errMsgId,
				int defaultValue, int minValue, int maxValue) {
			super(key, boxId, nameResId, errMsgId, defaultValue);
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		public int getIntValue() {
			return (Integer)curValue;
		}

		@Override
		public boolean validateValue(Object value) {
			int intValue = (Integer) value;
			return  (intValue >= minValue && intValue <= maxValue);
		}

		
	}

	//-------------------------------------------------------------------------------------
	public static class TextOption extends Option {
		protected TextOption(String key, int boxId,
				int nameResId, int errMsgId, String defaultValue) {
			super(key, boxId, nameResId, errMsgId, defaultValue);
		}

		@Override
		public boolean setValue(Object value) {
			String stringValue = (value==null) ? "" : String.valueOf(value);
			if (stringValue.length() <= 0)	stringValue = (String)defaultValue;
			return super.setValue(stringValue);
		}
		
		public String getString() {
			return (String) curValue;
		}

	}

	public static class PathOption extends TextOption {
		protected PathOption(String key, int boxId,
				int nameResId, int errMsgId, String defaultValue) {
			super(key, boxId, nameResId, errMsgId, defaultValue);
		}
		
		@Override
		public boolean validateValue(Object value) {
			String stringValue = (String) value;
			// We must allow default value to avoid a dead lock.
			return  stringValue.equals((String)defaultValue) ||
					Utils.validateExecPath(stringValue);
		}
	}

	public static class InterfaceNameOption extends TextOption {
		
		protected InterfaceNameOption(String key, int boxId,
				int nameResId, int errMsgId, String defaultValue) {
			super(key, boxId, nameResId, errMsgId, defaultValue);
		}
		
		@Override
		public boolean validateValue(Object value) {
			String name = (String) value;
			// Since a particular network interface is not always available,
			// we allow current value, even though it might be invalid at the moment.
			// We also allow the default value which is not a real interface name.
			if (name.equals((String)defaultValue) ||
				name.equals((String)curValue)) return true;
			
			try {
				return (StatusAnalyzer.checkNetworkName(name));
			} catch (Exception ex) {
				return false;
			}
		}
	}
	
	//=======================================================================================
	private static final int[] refrStringIds =
		{R.string.refrNever, R.string.refr3sec, R.string.refr20sec,
		 R.string.refr1min, R.string.refr10min, R.string.refr30min};    

	private static final int[] refrIntervals = 
		{0, 3000, 20000, 60000, 600000, 1800000};	

	
	private static final int[] adbRestartStringIds =
		{R.string.adbdRestartNormal, R.string.adbdRestartForced}; 

	public IntegerOption portNumber =
			new IntegerOption("PN", R.id.optPortNumber, 
					R.string.optPortNumber, R.string.msgPortNumberError, StatusAnalyzer.DefaultADBPort, 1024, 65535); 

	public AlternativesOption autoRefresh =
			new AlternativesOption("AR", R.id.optAutoRefresh, R.string.optAutoRefresh, 0, 0, refrStringIds);
	
	public PathOption shellPath = 
			new PathOption("SP", R.id.optShellPath, R.string.optShellPath, R.string.msgInvalidPath, "su");

	public InterfaceNameOption ifaceName = 
			new InterfaceNameOption("IF", R.id.optIfaceName, R.string.optIfaceName, 
				R.string.msgInvalidIfaceName, StatusAnalyzer.NetworkNameDefault);

	public AlternativesOption adbdRestartMethod = 
			new AlternativesOption("ARM", R.id.optAdbRestart,  R.string.optAdbdRestart, 0, 0, adbRestartStringIds);   
	
	public Option[] allOptions = {portNumber, autoRefresh, shellPath, ifaceName, adbdRestartMethod};
	
	//-----------------------------------------------------------------------------------------------------
	
	private static final String SharedPrefsName = "YawAdbOptions"; 
	private Context context;
	
	public YawAdbOptions(Context context) {
		this.context = context;
		loadPreferences();
	}
	
	private void loadPreferences() {
		SharedPreferences sprefs = context.getSharedPreferences(SharedPrefsName, Context.MODE_PRIVATE);
		for (Option opt : allOptions) {
			String key = opt.getKey();
			Object dfltValue = opt.getDefaultValue();
			opt.setValue((opt instanceof TextOption)
					? sprefs.getString(key, (String)dfltValue) 
					: sprefs.getInt(key, (Integer)dfltValue)); 
		}
	}
		
	public void savePreferences() {
		SharedPreferences.Editor editor =
				context.getSharedPreferences(SharedPrefsName, Context.MODE_PRIVATE).edit();
		for (Option opt : allOptions) {
			String key = opt.getKey();
			Object value = opt.getValue();
			if (opt instanceof TextOption)
				editor.putString(key, (String)value);
			else
				editor.putInt(key, (Integer)value);
		}
		editor.commit();
	}

	public int getRefreshInterval() {
		return refrIntervals[autoRefresh.getIndex()];
	}
	
}
