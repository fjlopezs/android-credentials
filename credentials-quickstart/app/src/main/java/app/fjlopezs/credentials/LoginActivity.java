/*
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.fjlopezs.credentials;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import app.fjlopezs.credentials.helpers.Preferences;

/**
 * A minimal example of saving and loading username/password credentials from the Credentials API.
 *
 * @author samstern@google.com
 */
public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final String KEY_IS_RESOLVING = "is_resolving";
    private static final int RC_SAVE = 1;
    private static final int RC_HINT = 2;
    private static final int RC_READ = 3;



    private GoogleApiClient mCredentialsApiClient;
    private Credential mCurrentCredential;
    private boolean mIsResolving = false;

    private Preferences pref;
    private String email;
    private String name;
    private Button botonReintentar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        pref = new Preferences(LoginActivity.this);
        pref.setSharedPreferences("credentials");


        // Buttons
        botonReintentar = ( Button) findViewById(R.id.buttonGoogleSign);
        botonReintentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCredentialsClicked();
            }
        });


        // Instance state
        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING);
        }

        // Instantiate GoogleApiClient.  This is a very simple GoogleApiClient that only connects
        // to the Auth.CREDENTIALS_API, which does not require the user to go through the sign-in
        // flow before connecting.  If you are going to use this API with other Google APIs/scopes
        // that require sign-in (such as Google+ or Google Drive), it may be useful to separate
        // the CREDENTIALS_API into its own GoogleApiClient with separate lifecycle listeners.
        mCredentialsApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Attempt auto-sign in.
        if (!mIsResolving) {
            requestCredentials(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_RESOLVING, mIsResolving);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        hideProgress();

        switch (requestCode) {
            case RC_HINT:
                // Drop into handling for RC_READ
            case RC_READ:
                if (resultCode == RESULT_OK) {
                    boolean isHint = (requestCode == RC_HINT);
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    processRetrievedCredential(credential, isHint);
                } else {
                    Log.e(TAG, "Credential Read: NOT OK");
                    showToast("Credential Read Failed");
                }

                mIsResolving = false;
                break;
            case RC_SAVE:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Credential Save: OK");
                    showToast("Credential Save Success");
                    startActivity();
                } else {
                    Log.e(TAG, "Credential Save: NOT OK");
                    showToast("Credential Save Failed");
                }

                mIsResolving = false;
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        botonReintentar.setEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended:" + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        botonReintentar.setEnabled(false);
    }

    /**
     * Called when the save button is clicked.  Reads the entries in the email and password
     * fields and attempts to save a new Credential to the Credentials API.
     */
    private void saveCredentialClicked() {


        if (email != null){
            pref.savePreferences(Constants.EMAIL, email);
            pref.savePreferences(Constants.NAME, name);
        }




        // Create a Credential with the user's email as the ID and storing the password.  We
        // could also add 'Name' and 'ProfilePictureURL' but that is outside the scope of this
        // minimal sample.
        Log.d(TAG, "Saving Credential:" + email + ":" + name);
        final Credential credential = new Credential.Builder(email)
                .setPassword("password")
                .setName(name)
                .build();

        showProgress();

        // NOTE: this method unconditionally saves the Credential built, even if all the fields
        // are blank or it is invalid in some other way.  In a real application you should contact
        // your app's back end and determine that the credential is valid before saving it to the
        // Credentials backend.
        Auth.CredentialsApi.save(mCredentialsApiClient, credential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "SAVE: OK");
                            showToast("Credential Saved");
                            hideProgress();
                        } else {
                            resolveResult(status, RC_SAVE);
                        }
                    }
                });
    }

    /**
     * Called when the Load Credentials button is clicked. Attempts to read the user's saved
     * Credentials from the Credentials API.  This may show UX, such as a credential picker
     * or an account picker.
     * <p/>
     * <b>Note:</b> in a normal application loading credentials should happen without explicit user
     * action, this is only connected to a 'Load Credentials' button for easier demonstration
     * in this sample.  Make sure not to load credentials automatically if the user has clicked
     * a "sign out" button in your application in order to avoid a sign-in loop. You can do this
     * with the function <code>Auth.CredentialsApi.disableAuthSignIn(...)</code>.
     */
    private void loadCredentialsClicked() {
        requestCredentials(true);
    }

    /**
     * Request Credentials from the Credentials API.
     *
     * @param shouldResolveHint true if resolutions for hints should occur. Setting
     *                          shouldResolveHint to false will not show UI unless there is a known
     *                          Credential and is therefore appropriate for app start.
     */
    private void requestCredentials(final boolean shouldResolveHint) {
        // Request all of the user's saved username/password credentials.  We are not using
        // setAccountTypes so we will not load any credentials from other Identity Providers.
        CredentialRequest request = new CredentialRequest.Builder()
                .setSupportsPasswordLogin(true)
                .build();

        showProgress();

        Auth.CredentialsApi.request(mCredentialsApiClient, request).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult credentialRequestResult) {
                        hideProgress();
                        if (credentialRequestResult.getStatus().isSuccess()) {
                            // Successfully read the credential without any user interaction, this
                            // means there was only a single credential and the user has auto
                            // sign-in enabled.
                            processRetrievedCredential(credentialRequestResult.getCredential(), false);
                        } else {
                            // Reading the credential requires a resolution, which means the user
                            // may be asked to pick among multiple credentials if they exist.
                            Status status = credentialRequestResult.getStatus();
                            if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                                if (!shouldResolveHint) {
                                    Log.d(TAG, "requestCredentials: ignoring hint.");
                                    return;
                                }

                                // This is a "hint" credential, which will have an ID but not
                                // a password.  This can be used to populate the username/email
                                // field of a sign-up form or to initialize other services.
                                resolveResult(status, RC_HINT);
                            } else if (status.getStatusCode() ==
                                    CommonStatusCodes.RESOLUTION_REQUIRED) {
                                // This is most likely the case where the user has multiple saved
                                // credentials and needs to pick one
                                resolveResult(status, RC_READ);
                            } else {
                                if (status.getStatusCode() == CommonStatusCodes.NETWORK_ERROR) {


                                    if (pref.exist(Constants.EMAIL)) {
                                        email = pref.getPreferences(Constants.EMAIL);
                                        name = pref.getPreferences(Constants.NAME);
                                        startActivity();//OK
                                    } else {
                                        showToast("Ninguna conexión a Internet está disponible actualmente");
                                        botonReintentar.setVisibility(View.VISIBLE);
                                    }


                                }
                                Log.w(TAG, "Unexpected status code: " + status.getStatusCode());
                            }
                        }
                    }
                });
    }

    /**
     * Called when the delete credentials button is clicked.  This deletes the last Credential
     * that was loaded using the load button.
     */
    private void deleteLoadedCredentialClicked() {
        if (mCurrentCredential == null) {
            showToast("Error: no credential to delete");
            return;
        }

        showProgress();

        Auth.CredentialsApi.delete(mCredentialsApiClient, mCurrentCredential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        hideProgress();
                        if (status.isSuccess()) {
                            // Credential delete succeeded, disable the delete button because we
                            // cannot delete the same credential twice. Clear text fields.
                            showToast("Credential Delete Success");
                            pref.clearAll();
                            name = null;
                            email = null;

                            mCurrentCredential = null;
                        } else {
                            // Credential deletion either failed or was cancelled, this operation
                            // never gives a 'resolution' so we can display the failure message
                            // immediately.
                            Log.e(TAG, "Credential Delete: NOT OK");
                            showToast("Credential Delete Failed");
                        }
                    }
                });
    }

    /**
     * Attempt to resolve a non-successful Status from an asynchronous request.
     *
     * @param status      the Status to resolve.
     * @param requestCode the request code to use when starting an Activity for result,
     *                    this will be passed back to onActivityResult.
     */
    private void resolveResult(Status status, int requestCode) {
        // We don't want to fire multiple resolutions at once since that can result
        // in stacked dialogs after rotation or another similar event.
        if (mIsResolving) {
            Log.w(TAG, "resolveResult: already resolving.");
            return;
        }

        Log.d(TAG, "Resolving: " + status);
        if (status.hasResolution()) {
            Log.d(TAG, "STATUS: RESOLVING");
            try {
                status.startResolutionForResult(LoginActivity.this, requestCode);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "STATUS: Failed to send resolution.", e);
                hideProgress();
            }
        } else {

            if (pref.exist(Constants.EMAIL)){
                email = pref.getPreferences(Constants.EMAIL);
                name = pref.getPreferences(Constants.NAME);
                startActivity();//??
            }



            Log.e(TAG, "STATUS: FAIL");
            showToast("Could Not Resolve Error");
            hideProgress();
        }
    }

    /**
     * Process a Credential object retrieved from a successful request.
     *
     * @param credential the Credential to process.
     * @param isHint     true if the Credential is hint-only, false otherwise.
     */
    private void processRetrievedCredential(Credential credential, boolean isHint) {
        Log.d(TAG, "Credential Retrieved: " + credential.getId() + ":" + credential.getName());

        email = credential.getId();
        name = credential.getName();

        // If the Credential is not a hint, we should store it an enable the delete button.
        // If it is a hint, skip this because a hint cannot be deleted.
        if (!isHint) {
            botonReintentar.setVisibility(View.GONE);
            showToast("Credential Retrieved");
            mCurrentCredential = credential;
            startActivity();//OK

        } else {
            showToast("Credential Hint Retrieved");
            saveCredentialClicked();

        }

//        deleteLoadedCredentialClicked();

    }



    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showProgress() {
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);

        botonReintentar.setEnabled(false);

    }

    private void hideProgress() {
        findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);

        botonReintentar.setEnabled(true);
    }


    private void startActivity(){
        Intent intentActivity = new Intent(LoginActivity.this, SuccessActivity.class);
        Bundle options = new Bundle();
        options.putString(Constants.NAME, name);
        options.putString(Constants.EMAIL, email);

        intentActivity.putExtras(options);
        startActivity(intentActivity);
        //finish();

    }


}
