package com.example.onpointdemoconsumer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.room.Database;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class MainActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {
    private static final String TAG = "main";
    private static final int RC_SIGN_IN = 100;
    private static final String API_KEY = "5e4f9445e441da0017cde648";
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount account;
    private Button SendBTN;
    private TextView DisplayTXT;
    private static int NORMAL_DURATION = 5*1000;
    private LocationManager locationManager;
    static MainActivity instance;
    private RequestQueue queue;
    private static final String DatabaseURL = "https://onpoint-backend.herokuapp.com/api";
    private String userId;
    private SharedPreferences sp;
    private SharedPreferences.Editor edit;
    private int requestCode = 233;
    private DataSender DS;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        SendBTN = (Button)findViewById(R.id.button);
        DisplayTXT = (TextView)findViewById(R.id.display);
        queue = Volley.newRequestQueue(this);
        // Get Permissions
        requestLocationPermissions();
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        account = GoogleSignIn.getLastSignedInAccount(this);
        if(account==null)
            signIn();
        else updateUI(account);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        SendBTN = findViewById(R.id.button);
        SendBTN.setOnClickListener(this);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        edit = sp.edit();

        edit.putString("inputExtra", "Collecting your Geodata");
        edit.putString("DatabaseURL",DatabaseURL);
        edit.apply();
        userId = sp.getString("userId",null);
    }

    private void requestLocationPermissions() {
        boolean permissionAccessCoarseLocationApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessCoarseLocationApproved) {
            boolean backgroundLocationPermissionApproved =
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;

            if (backgroundLocationPermissionApproved) {
                // App can access location both in the foreground and in the background.
                // Start your service that doesn't have a foreground service type
                // defined.
            } else {
                // App can only access location in the foreground. Display a dialog
                // warning the user that your app must have all-the-time access to
                // location in order to function properly. Then, request background
                // location.
                ActivityCompat.requestPermissions(this, new String[] {
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        requestCode);
            }
        } else {
            // App doesn't have access to the device's location at all. Make full request
            // for permission.
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    },
                    requestCode);
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        if(account == null)
            signIn();
        else {
            Log.d(TAG, "updateUI: logged in - "+account.getDisplayName()+" | IdToken - "+account.getIdToken());
            //Get Location
            //fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            //getLocation();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                if(userId == null)
                {
                    logOnPoint(account.getEmail(),0000,API_KEY);
                }
                else{
                    Intent serviceIntent = new Intent(this, BackgroundService.class);
                    serviceIntent.putExtra("inputExtra", "Collecting your Geodata");
                    serviceIntent.putExtra("userId",userId);
                    ContextCompat.startForegroundService(this, serviceIntent);
                }
            }


        }
    }

    private void logOnPoint(String email, int password, String apiKey) {
        JSONObject user = new JSONObject();
        try {
            user.put("email", email);
            user.put("password", password);
            user.put("apiKey", apiKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }
        final String mRequestBody = user.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, DatabaseURL+"/users/addUser", response -> Log.i("LOG_RESPONSE", response), error -> Log.e("LOG_RESPONSE", error.toString())) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString ;
                String responseCode = "";
                if (response != null) {
                    responseCode = String.valueOf(response.statusCode);
                    responseString = new String(response.data);
                    try{
                        JSONObject obj = new JSONObject(responseString);
                        userId = obj.getString("_id");
                        edit.putString("userId",userId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        userId =responseString.substring(responseString.indexOf("_id")+6,responseString.indexOf("name")-3);
                        edit.putString("userId",userId);
                    }
                    Intent serviceIntent = new Intent(MainActivity.this, BackgroundService.class);
                    edit.apply();
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                }
                return Response.success(responseCode, HttpHeaderParser.parseCacheHeaders(response));
            }
        };
        queue.add(stringRequest);

    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        DisplayTXT.setText("your current location: ("+location.getLatitude()+" , "+location.getLongitude()+")");

    }

    public void updateTextView(final String value){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DisplayTXT.setText("your current location: ("+value+")");
                Log.d(TAG, "run: "+value);
            }
        });
    }

    public static MainActivity getInstance(){
        return instance;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button:
                if(!DisplayTXT.getText().toString().isEmpty()) {
                    if(DS==null)
                    {
                        DS = new DataSender(this, DatabaseURL,userId);
                    }
                    DS.uploadLocation(DisplayTXT.getText().toString(),"");
                    //TODO get latlong from displaytxt
                }
                break;
        }
    }
}
