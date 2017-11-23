package com.sourav.weatherfore.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Sourav on 11/21/2017.
 */

/**
 * The service which allows the sync adapter framework to access the authenticator.
 */
public class AccountAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private AccountAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new AccountAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
