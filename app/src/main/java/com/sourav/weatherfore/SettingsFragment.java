package com.sourav.weatherfore;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import com.sourav.weatherfore.db.WeatherContract;
import com.sourav.weatherfore.db.WeatherPreferences;
import com.sourav.weatherfore.sync.SyncUtils;

/**
 * Created by Sourav on 11/15/2017.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener{



    @Override
    public void onStop() {
        super.onStop();
        /* Unregister the preference change listener */
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {

        /*register the preference change listener */
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onStart();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        /* Add 'general' preferences, defined in the XML file */
        addPreferencesFromResource(R.xml.pref_general);

        // Set the preference summary on each preference that isn't a CheckBoxPreference
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();
        int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++){
            Preference preference = prefScreen.getPreference(i);
            if (!(preference instanceof CheckBoxPreference)){
                String value = sharedPreferences.getString(preference.getKey(),"");
                setPreferenceSummery(preference,value);
            }
        }

        Preference pref = findPreference(getString(R.string.pref_location_key));
        pref.setOnPreferenceChangeListener(this);

    }

    private void setPreferenceSummery(Preference preference, String value){

        if (preference instanceof ListPreference){
              /* For list preferences, look up the correct display value in */
            /* the preference's 'entries' list (since they have separate labels/values). */
            ListPreference listPreference = (ListPreference)preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0){
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        }else {
            preference.setSummary(value);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity activity = getActivity();
        if (key.equals(getString(R.string.pref_location_key))){
            WeatherPreferences.resetLocationCoordinates(activity);
            SyncUtils.startImmediateSync(activity);
        }else if (key.equals(getString(R.string.pref_units_key))) {
            // units have changed. update lists of weather entries accordingly
            activity.getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }
        Preference preference = findPreference(key);
        if (null != preference){
            if (!(preference instanceof CheckBoxPreference)){
                setPreferenceSummery(preference, sharedPreferences.getString(key, ""));
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        Toast error = Toast.makeText(getContext(),"Please type a Location",Toast.LENGTH_SHORT);

        String locKey = getString(R.string.pref_location_key);
        if (preference.getKey().equals(locKey)){
            String location = ((String)newValue).trim();
            try{
                if (location.isEmpty()){
                    error.show();
                    return false;
                }
            }catch (Exception e){
                e.printStackTrace();
                error.show();
                return false;
            }
        }
        return true;
    }
}
