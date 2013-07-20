/* 
   ConfigActivity. Maintains YawADB configuration page 
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
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigActivity extends Activity {
	public static final int NewConnectionSettings=55;
	protected Resources rsrc;
	protected YawAdbOptions options;
	protected boolean asWidget;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		rsrc = getResources();
		options = new YawAdbOptions(this);
		asWidget = getIntent().getBooleanExtra(YawAdbConstants.AsWidgetExtra, false);
		setContentView(R.layout.config);
		populateFields();
		findViewById(R.id.reset).setOnClickListener(resetClickListener);
	}
	
	private void populateFields() {
		for (YawAdbOptions.Option option : options.allOptions) {
			ViewGroup box = (ViewGroup)findViewById(option.getBoxId());
			if ((option == options.autoRefresh) && !asWidget) { 
				box.setVisibility(View.GONE);
				continue;
			}
			((TextView)box.findViewById(R.id.boxTitle)).setText(option.nameResId);
			TextView valueView = (TextView)box.findViewById(R.id.boxValue);
			valueView.setText(option.getStringValue(rsrc));
			valueView.setTag(option);

			if (valueView instanceof EditText) {
				if (option == options.portNumber) {
					try {
						int classNumber = Class.forName("android.text.InputType").
							getField("TYPE_CLASS_NUMBER").getInt(null);
						TextView.class.getMethod("setInputType", int.class).
							invoke(valueView, Integer.valueOf(classNumber));
					} catch (Exception ex) {
						setInputTypeNumberBase(valueView);
					}
					
					valueView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
				} 

				valueView.setHint(String.valueOf(option.getDefaultValue()));
				valueView.setOnFocusChangeListener(editTextFocusListener);
				
			} else {
				valueView.setOnClickListener(choiceClickListener);
			}
		}
	}

	protected View.OnFocusChangeListener editTextFocusListener = new View.OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (!hasFocus) {
				TextView tv = (TextView)v;
				String newValue = tv.getText().toString().trim();
				if (newValue.length() <= 0)
					newValue = 
						String.valueOf(((YawAdbOptions.Option)v.getTag()).getDefaultValue());
				tv.setText(newValue);
			}
		}
	};
	
	private View.OnClickListener choiceClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			View focused = getCurrentFocus();
			if (focused != null && (focused instanceof EditText))
				editTextFocusListener.onFocusChange(focused, false);
				
			YawAdbOptions.AlternativesOption opt = (YawAdbOptions.AlternativesOption)view.getTag();
			opt.nextValue();
			((TextView)view).setText(opt.getStringValue(rsrc));
		}
	};
	
	private View.OnClickListener resetClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			resetValues();
		}
	};
	
	
	protected void resetValues() {
		for (YawAdbOptions.Option option : options.allOptions) {
			option.setDefaultValue();
			populateFields();
		}
		Utils.showTooltip(this, R.string.msgResetToDefault, Toast.LENGTH_SHORT);
	}
	
	
	private static void setInputTypeNumberBase(TextView tv) {
		tv.addTextChangedListener(new TextWatcher() {
			private int from=0, to=0;
			private CharSequence replacement=null;
			
			@Override
			public void afterTextChanged(Editable s) {
				if (replacement != null) s.replace(from, to, replacement);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				Editable newPortion = new SpannableStringBuilder(s.subSequence(start, start+count));

				int i=count;
				while(--i >= 0) { 
					if (!Character.isDigit(newPortion.charAt(i))) {
						newPortion.delete(i, i+1);
					}
				}

				if (newPortion.length() >= count) 
					replacement = null;
				else {
					replacement = newPortion;
					from = start;
					to = start+count;
				}
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			String oldIface = options.ifaceName.getString();
			int oldPort = options.portNumber.getIntValue();

			if (!applyChanges()) return true;
			
			if (options.portNumber.getIntValue() != oldPort ||
			    !options.ifaceName.getString().equals(oldIface))		
				setResult(ConfigActivity.NewConnectionSettings);
			
			options.savePreferences();
		}
		
		return super.onKeyDown(keyCode, event);
	}

	private boolean applyChanges() {
		boolean result = true;
		boolean hasErrorMessage = false;
		
		for (YawAdbOptions.Option option : options.allOptions) {
			// Alternatives are applied automatically and don't need validation
			if (option instanceof YawAdbOptions.AlternativesOption) continue;

			ViewGroup box = (ViewGroup)findViewById(option.getBoxId());
			TextView valueView = (TextView)box.findViewById(R.id.boxValue);
			String valueStr = valueView.getText().toString().trim();  
			Object value;
			if (option instanceof YawAdbOptions.IntegerOption) {
				// This may be redundant, given that non-digits are filtered out
				try {
					value = Integer.valueOf(valueStr);
				} catch(NumberFormatException ex) {
					value = null;
				}
			} else
				value = valueStr;
			
			if (value == null || !option.setValue(value)) {
				if (!hasErrorMessage) {
					int msgId = option.getErrorMessageId();
					if (msgId != 0) {
						String msg = rsrc.getString(msgId, String.valueOf(value));
						Utils.showTooltip(this, msg, Toast.LENGTH_LONG);
						hasErrorMessage= true;
					}
				}
				result = false;
			}
		}

		
		return result;
	}

}
