package com.sourav.weatherfore;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.sourav.weatherfore.db.WeatherContract;
import com.sourav.weatherfore.db.WeatherPreferences;
import com.sourav.weatherfore.sync.SyncUtils;
import com.sourav.weatherfore.utilities.WeatherUtils;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

/**
 * Created by Sourav on 11/15/2017.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{


    protected final static int PLACE_PICKER_REQUEST = 9090;

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
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                try {
                    // Start a new Activity for the Place Picker API, this will trigger {@code #onActivityResult}
                    // when a place is selected or with the user cancels.
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    Intent i = builder.build(getActivity());
                    startActivityForResult(i, PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.e(TAG, String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
                } catch (Exception e) {
                    Log.e(TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
                }
                return false;
            }
        });
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
                    // Note --- if the server is down we still assume the value
                    // is valid
                    preference.setSummary(value);
            }
        }else {
            preference.setSummary(value);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity activity = getActivity();
        if (key.equals(getString(R.string.pref_location_key))){
            // we've changed the location
            // Wipe out any potential PlacePicker latlng values so that we can use this text entry.
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(getString(R.string.pref_location_latitude));
            editor.remove(getString(R.string.pref_location_longitude));
            editor.commit();

            WeatherPreferences.resetLocationCoordinates(activity);
            SyncUtils.startImmediateSync(activity);
        }else if (key.equals(getString(R.string.pref_units_key))) {
            // units have changed. update lists of weather entries accordingly
            activity.getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }
        Preference preference = findPreference(key);
        if (null != preference){
            if (!(preference instanceof CheckBoxPreference)){
                setPreferenceSummary(preference, sharedPreferences.getString(key, ""));
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleApiClient client = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(getActivity(), this)
                .build();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check to see if the result is from our Place Picker intent
        if (requestCode == PLACE_PICKER_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(getContext(),data);
                String address = place.getAddress().toString();
                LatLng latLong = place.getLatLng();

                // If the provided place doesn't have an address, we'll form a display-friendly
                // string from the latlng values.
                if (TextUtils.isEmpty(address)) {
                    address = String.format("(%.2f, %.2f)",latLong.latitude, latLong.longitude);
                }

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.pref_location_key), address);

                // Also store the latitude and longitude so that we can use these to get a precise
                // result from our weather service. We cannot expect the weather service to
                // understand addresses that Google formats.
                editor.putFloat(getString(R.string.pref_location_latitude),
                        (float) latLong.latitude);
                editor.putFloat(getString(R.string.pref_location_longitude),
                        (float) latLong.longitude);
                editor.commit();

                // Tell the SyncAdapter that we've changed the location, so that we can update
                // our UI with new values. We need to do this manually because we are responding
                // to the PlacePicker widget result here instead of allowing the
                // LocationEditTextPreference to handle these changes and invoke our callbacks.
                Preference locationPreference = findPreference(getString(R.string.pref_location_key));
                setPreferenceSummary(locationPreference, address);
                WeatherUtils.resetLocationStatus(getContext());
                SyncUtils.syncImmediately(getContext());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
