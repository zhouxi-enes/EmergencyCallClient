package org.enes.lanvideocall.services;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.enes.lanvideocall.activities.InitActivity;

public class AuthenticationService extends Service {

    private AccountAuthenticator accountAuthenticator;

    private AccountAuthenticator getAccountAuthenticator() {
        if(accountAuthenticator == null) {
            accountAuthenticator = new AccountAuthenticator(this);
        }
        return accountAuthenticator;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        accountAuthenticator = new AccountAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return accountAuthenticator.getIBinder();
    }

    class AccountAuthenticator extends AbstractAccountAuthenticator {


        public AccountAuthenticator(Context context) {
            super(context);
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
            return null;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
            final Bundle bundle = new Bundle();
            final Intent intent = new Intent(AuthenticationService.this, InitActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,response);
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public String getAuthTokenLabel(String authTokenType) {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
            return null;
        }

    }
}
