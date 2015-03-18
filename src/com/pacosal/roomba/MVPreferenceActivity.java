package com.pacosal.roomba;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class MVPreferenceActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences); 

        Preference preferenceIp =  (Preference)findPreference("ip");
        preferenceIp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Util.serverIP = (String) newValue;
				return true;
			}
		});
        
        CheckBoxPreference preference2 = (CheckBoxPreference)findPreference("debug");
        preference2.setChecked(Util.debugMode);
        preference2.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Util.debugMode = (Boolean)newValue;
                if (Util.debugMode)
                	Util.logDebug("Debug activado");
                
                grabar();
                
                return true;
            }
        });

        
    }

    protected void grabar() { 
    	
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    	SharedPreferences.Editor editor = settings.edit();
    	
    	editor.putBoolean("debug", Util.debugMode);
    	editor.putString("ip", Util.serverIP);
    	editor.commit();
    	
    }	



}
