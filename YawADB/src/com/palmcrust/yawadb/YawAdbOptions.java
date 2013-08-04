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

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.content.Context;
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

	//=======================================================================================
	
	private static final int[] autoRefrStringIds =
		{R.string.refrNever, R.string.refr3sec, R.string.refr20sec,
		 R.string.refr1min, R.string.refr10min, R.string.refr30min};    

	private static final int[] autoOffStringIds =
		{R.string.disabled, R.string.enabled};

	private static final int[] refrIntervals = 
		{0, 3000, 20000, 60000, 600000, 1800000};	

	
	private static final int[] adbRestartStringIds =
		{R.string.adbdRestartNormal, R.string.adbdRestartForced}; 

	public IntegerOption portNumber =
			new IntegerOption("PN", R.id.optPortNumber, 
					R.string.optPortNumber, R.string.msgPortNumberError, StatusAnalyzer.DefaultADBPort, 1024, 65535); 

	public AlternativesOption autoRefresh =
			new AlternativesOption("AR", R.id.optAutoRefresh, R.string.optAutoRefresh, 0, 0, autoRefrStringIds);

	public AlternativesOption autoUsb =
			new AlternativesOption("AD", R.id.optAutoUsb, R.string.optAutoUsb, 0, 0, autoOffStringIds);
	
	public PathOption shellPath = 
			new PathOption("SP", R.id.optShellPath, R.string.optShellPath, R.string.msgInvalidPath, "su");

	public AlternativesOption adbdRestartMethod = 
			new AlternativesOption("ARM", R.id.optAdbRestart,  R.string.optAdbdRestart, 0, 0, adbRestartStringIds);   
	
	public Option[] allOptions = {portNumber, autoRefresh, autoUsb, shellPath,  adbdRestartMethod};
	
	//-----------------------------------------------------------------------------------------------------
	
	private static final String SavedOptionsFileName = "options.dat";
	private static final byte[] signature = {'Y', 'A'}; 
	private static final short version = 0; 
//	private static final String SharedPrefsName = "YawAdbOptions"; 
	private Context context;
	
	public YawAdbOptions(Context context) {
		this.context = context;
		loadPreferences();
	}
	
	
	
//	private void loadPreferences() {
//		SharedPreferences sprefs = context.getSharedPreferences(SharedPrefsName, Context.MODE_PRIVATE);
//		for (Option opt : allOptions) {
//			String key = opt.getKey();
//			Object dfltValue = opt.getDefaultValue();
//			opt.setValue((opt instanceof TextOption)
//					? sprefs.getString(key, (String)dfltValue) 
//					: sprefs.getInt(key, (Integer)dfltValue)); 
//		}
//	}

	private void loadPreferences() {
		ObjectInputStream ois = null;
		Map<String, Object> allValues = new HashMap<String, Object>(); 

		try {
			ois = new ObjectInputStream(
					new GZIPInputStream(
						context.openFileInput(SavedOptionsFileName)));
			byte[] sgn = new byte[2];
			ois.read(sgn);
			if (!Arrays.equals(sgn, signature))
				throw new IllegalStateException ("Wrong signature");
			sgn = null;
			
			if (ois.readUnsignedShort() > version)
				throw new IllegalStateException ("Incompatible version");

			for (;;) 
				allValues.put(ois.readUTF(), ois.readObject());
			
		} catch (FileNotFoundException ex) {
		} catch (EOFException ex) {
		} catch (Exception ex) { ex.printStackTrace(); }
			
		if (ois != null)
			try {ois.close();} catch (IOException ex) {}
		
		for (Option opt : allOptions) {
			String key = opt.getKey();
			if (allValues.containsKey(key))
				opt.setValue(allValues.get(key));
			else
				opt.setDefaultValue();
		}
	}
		
//	public void savePreferences() {
//		SharedPreferences.Editor editor =
//				context.getSharedPreferences(SharedPrefsName, Context.MODE_PRIVATE).edit();
//		for (Option opt : allOptions) {
//			String key = opt.getKey();
//			Object value = opt.getValue();
//			if (opt instanceof TextOption)
//				editor.putString(key, (String)value);
//			else
//				editor.putInt(key, (Integer)value);
//		}
//		editor.commit();
//	}

	public void savePreferences() {
		ObjectOutputStream oos = null;

		try {
			oos = new ObjectOutputStream(
					new GZIPOutputStream(
						context.openFileOutput(SavedOptionsFileName, Context.MODE_PRIVATE)));
			oos.write(signature);
			oos.writeShort(version);

			for (Option opt : allOptions) { 
				oos.writeUTF(opt.getKey());
				oos.writeObject(opt.getValue());
			}
			
		} catch (Exception ex) { ex.printStackTrace(); }
			
		if (oos != null)
			try {oos.close();} catch (IOException ex) {}
	}

	
	public int getRefreshInterval() {
		return refrIntervals[autoRefresh.getIndex()];
	}

	public boolean getAutoUsbValue() {
		return (autoUsb.getIndex() != 0);
	}
	
}
