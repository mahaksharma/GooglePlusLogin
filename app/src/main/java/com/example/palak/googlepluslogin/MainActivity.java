package com.example.palak.googlepluslogin;

import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusOneButton;
import com.google.android.gms.plus.PlusShare;
import com.google.android.gms.plus.model.people.Person;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements
     View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MainActivity";
    private  static final int RC_SIGN_IN = 9001;
    private static final int RC_SHARE_DIALOG = 8031;

    private static final String KEY_IS_RESOLVING = "is_resolving";
    private static final String KEY_SHOULD_RESOLVE = "should_resolve";
    private static final String URL ="https://plus.google.com/u/0/+DelaroyStudios/posts";
    private static final String LABEL_READ_MORE = "READ_MORE";

    private GoogleApiClient mGoogleApiClient;

    private TextView mStatus;
    private TextView mProfileTagLine;
    private ImageView mProfileImage;
    private boolean mIsResolving = false;
    private boolean mShouldResolve = false;
    private static final int PROFILE_PIC_SIZE = 400;
    private static final int PLUS_ONE_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null){
            mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING);
            mShouldResolve = savedInstanceState.getBoolean(KEY_SHOULD_RESOLVE);
        }
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        findViewById(R.id.share_button).setOnClickListener(this);
        findViewById(R.id.interactive_post).setOnClickListener(this);
        ((SignInButton) findViewById(R.id.sign_in_button)).setSize(SignInButton.SIZE_WIDE);

        findViewById(R.id.sign_in_button).setEnabled(false);

        mStatus =(TextView) findViewById(R.id.status);
        mProfileTagLine = (TextView) findViewById(R.id.user_tagLine);
        mProfileImage = (ImageView) findViewById(R.id.user_profileImage);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PROFILE))
                .build();
    }

    private void updateUI(boolean isSignedIn) {
        if (isSignedIn) {
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            if (currentPerson != null) {
                String name = currentPerson.getDisplayName();
                mStatus.setText("Signed in as: (name)");
                String personPhoto = currentPerson.getImage().getUrl();
                String personTagLine = currentPerson.getTagline();
                mProfileTagLine.setText(personTagLine);
                personPhoto = personPhoto.substring(0,personPhoto.length() - 2)
                        + PROFILE_PIC_SIZE;

                new  LoadProfileImage(mProfileImage).execute(personPhoto);
            }else {
                Log.v(TAG,"Error: Plus.PeopleApi.getCurrentPerson returned null. E...");
                mStatus.setText("Error: please check logs.");
            }

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_layout).setVisibility(View.VISIBLE);
        } else{
            mStatus.setText("Signed out");
            mProfileImage.setImageBitmap(null);
            mProfileTagLine.setText("");

            findViewById(R.id.sign_in_button).setEnabled(true);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_layout).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected  void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((PlusOneButton) findViewById(R.id.plus_one_button)).initialize(URL,PLUS_ONE_REQUEST_CODE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_RESOLVING , mIsResolving);
        outState.putBoolean(KEY_SHOULD_RESOLVE,mShouldResolve);
    }

    @Override
    public void onActivityResult(int requestCode , int resultCode , Intent data) {
        super.onActivityResult(requestCode, resultCode,data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode +":" + data);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode != RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGoogleApiClient.connect();

        } else if(requestCode == RC_SHARE_DIALOG){
            if (resultCode != RESULT_OK) {
                Log.e(TAG, "Failed to post");
            }
        }
    }




    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                mStatus.setText("Signing in...");
                mShouldResolve = true;
                mGoogleApiClient.connect();
                break;
            case R.id.sign_out_button:
                if(mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }
                updateUI(false);
                break;
            case R.id.disconnect_button:
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }
                updateUI(false);
                break;
            case R.id.share_button:
                Intent shareIntent = getSharePostIntent();
                startActivityForResult(shareIntent, RC_SHARE_DIALOG);
                break;
            case R.id.interactive_post:
                Intent shareIntent2 = getInteractivePostIntent();
                startActivityForResult(shareIntent2, RC_SHARE_DIALOG);
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG,"onConnected:" + bundle);
        updateUI(true);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG,"onConnectionSuspended:" +i);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG,"onConnectionFailed:" + connectionResult);
        if(!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try{
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Could not resolve ConnectionResult." , e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                showErrorDialog(connectionResult);


            }
        } else {
            updateUI(false);
        }

    }

    private void showErrorDialog(ConnectionResult connectionResult) {
        int errorCode = connectionResult.getErrorCode();
       /* if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
            GooglePlayServicesUtil.getErrorDialog(errorCode, this, RC_SIGN_IN,
                    (dialog) -> {
                        mShouldResolve = false;
                        updateUI(false);
                    }).show();
        } else {
            String errorString = "Google Play Services Error: (errorCode)";
            Toast.makeText(this,errorString,Toast.LENGTH_SHORT).show();
            mShouldResolve = false;
            updateUI(false);
        }
*/
    }

    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) { this.bmImage = bmImage; }

        protected  Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Bitmap mIconll = null;
            try {
                InputStream in = new java.net.URL(urlDisplay).openStream();
                mIconll = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIconll;
        }
        protected  void onPostExecute(Bitmap result) {bmImage.setImageBitmap(result);}
    }

    private  Intent getSharePostIntent() {
        return new PlusShare.Builder(this)
                .setType("text/plain")
                .setText("Welcome")
                .setContentUrl(Uri.parse(URL))
                .getIntent();
    }

    private Intent getInteractivePostIntent() {
        String action = "/?view=true";
        Uri callToActionUrl = Uri.parse("https://plus.google.com/u/0/+DelaroyStudios/about" + action);
        String callToActionDeepLinkId = "https://plus.google.com/u/0/+DelaroyStudios/about" +action;

        PlusShare.Builder builder = new PlusShare.Builder(this);
        builder.addCallToAction(LABEL_READ_MORE, callToActionUrl , callToActionDeepLinkId)
                .setContentUrl(Uri.parse("https://plus.google.com/u/0/+DelaroyStudios/about"))
                .setContentDeepLinkId("https://plus.google.com/u/0/+DelaroyStudios/about",
                        null, null, null)
                .setText("");

        return builder.getIntent();
    }



}
