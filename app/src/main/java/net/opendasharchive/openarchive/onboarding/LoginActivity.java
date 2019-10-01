package net.opendasharchive.openarchive.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.services.WebDAVSiteController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.scal.secureshareui.model.Account;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private final static String TAG = "Login";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mNameView, mEmailView, mServerView, mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Account mAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAccount = new Account(LoginActivity.this, WebDAVSiteController.SITE_NAME);

        // Set up the login form.
        mNameView = findViewById(R.id.servername);
        mEmailView = findViewById(R.id.email);
        mServerView = findViewById(R.id.server);

        if (mAccount != null)
        {

            if (!TextUtils.isEmpty(mAccount.getName()))
                mNameView.setText(mAccount.getName());

            if (!TextUtils.isEmpty(mAccount.getSite()))
                mServerView.setText(mAccount.getSite());


            if (!TextUtils.isEmpty(mAccount.getUserName()))
                mEmailView.setText(mAccount.getUserName());

        }

        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String name = mNameView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String server = mServerView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (!server.toLowerCase().startsWith("http"))
        {
            //auto add nextcloud defaults
            server = "https://" + server + "/remote.php/webdav/";
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mAuthTask = new UserLoginTask(name, email, password, server);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email);
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 0;
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mServer;
        private final String mName;

        UserLoginTask(String name, String email, String password, String server) {
            mName = name;
            mEmail = email;
            mPassword = password;
            mServer = server;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            Account account = new Account(LoginActivity.this, WebDAVSiteController.SITE_NAME);
            account.setName(mName);
            account.setUserName(mEmail);
            account.setCredentials(mPassword);
            account.setSite(mServer);
            account.setAuthenticated(false);
            account.saveToSharedPrefs(LoginActivity.this, WebDAVSiteController.SITE_NAME);

            StringBuffer siteUrl = new StringBuffer();
            siteUrl.append(mServer);
            if (!mServer.endsWith("/"))
                siteUrl.append("/");

            Sardine sardine = new OkHttpSardine();
            sardine.setCredentials(account.getUserName(),account.getCredentials());

            try {
                try
                {
                    sardine.getQuota(siteUrl.toString());

                    account.setAuthenticated(true);
                    account.saveToSharedPrefs(LoginActivity.this, WebDAVSiteController.SITE_NAME);

                    return true;
                }
                catch (Exception e) {

                    siteUrl.append("remote.php/dav/");
                    sardine.getQuota(siteUrl.toString());

                    account.setSite(siteUrl.toString());
                    account.setAuthenticated(true);
                    account.saveToSharedPrefs(LoginActivity.this, WebDAVSiteController.SITE_NAME);

                    return true;
                }


            } catch (IOException e) {

                Log.e(TAG,"error on login: " + siteUrl.toString(),e);

                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
          //  showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
          //  showProgress(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_login, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_in:
                attemptLogin();
                return true;

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

