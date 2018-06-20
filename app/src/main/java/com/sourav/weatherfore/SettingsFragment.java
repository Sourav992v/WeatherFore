package com.sourav.weatherfore;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.sourav.weatherfore.db.WeatherContract;
import com.sourav.weatherfore.db.WeatherPreferences;
import com.sourav.weatherfore.sync.SyncUtils;
import com.sourav.weatherfore.utilities.WeatherUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static com.sourav.weatherfore.Constants.LOCATION_DATA;
import static com.sourav.weatherfore.Constants.RESULT_CODE;

/**
 * Created by Sourav on 11/15/2017.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener,Preference.OnPreferenceChangeListener{

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
                setPreferenceSummary(preference,value);
            }
        }

        Preference pref = findPreference(getString(R.string.pref_location_key));
        pref.setOnPreferenceChangeListener(this);
    }

    private void setPreferenceSummary(Preference preference, String value){

        String key = preference.getKey();

        if (preference instanceof ListPreference){
              /* For list preferences, look up the correct display value in */
            /* the preference's 'entries' list (since they have separate labels/values). */
            ListPreference listPreference = (ListPreference)preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0){
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (key.equals(getString(R.string.pref_location_key))) {
            @SyncUtils.LocationStatus int status = WeatherUtils.getLocationStatus(getContext());
            switch (status) {
                case SyncUtils.LOCATION_STATUS_OK:
                    preference.setSummary(value);
                    break;
                case SyncUtils.LOCATION_STATUS_UNKNOWN:
                    preference.setSummary(getString(R.string.pref_location_unknown_description, value));
                    break;
                case SyncUtils.LOCATION_STATUS_INVALID:
                    preference.setSummary(getString(R.string.pref_location_error_description, value));
                    break;
                default:
                    preference.setSummary(value);
            }
        } else {
            preference.setSummary(value);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity activity = getActivity();
        if (key.equals(getString(R.string.pref_location_key))){

            WeatherUtils.resetLocationStatus(getActivity());
            SyncUtils.startImmediateSync(activity);
        }else if (key.equals(getString(R.string.pref_units_key))) {
            // units have changed. update lists of weather entries accordingly
            getActivity().getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }else if (key.equals(getString(R.string.pref_location_status_key))){
            Preference locationPreference = findPreference(getString(R.string.pref_location_key));
            setPreferenceSummary(locationPreference,sharedPreferences.getString(locationPreference.getKey(),""));
        }
        Preference preference = findPreference(key);
        if (null != preference){
            if (!(preference instanceof CheckBoxPreference)){
                setPreferenceSummary(preference, sharedPreferences.getString(key, ""));
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        Toast error = Toast.makeText(getContext(),"Please type a Location",Toast.LENGTH_SHORT);

        setPreferenceSummary(preference, newValue.toString());

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