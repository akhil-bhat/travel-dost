package com.ellalee.travelmaker;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtilLight;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;

import org.w3c.dom.Document;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.widget.Toast.*;
import static com.ellalee.travelmaker.GmailSync.SCOPE;
import static com.google.android.gms.auth.api.credentials.CredentialPickerConfig.Prompt.SIGN_IN;

import android.test.ActivityInstrumentationTestCase2;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private String[] navItems = {"LogIn", "????????????", "GMail ??????"};


    private NavigationView lvNavList;
    private FrameLayout flContainer;
    private DrawerLayout dlDrawer;
    private ImageButton btn;
    static GoogleAccountCredential mCredential;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static  com.google.api.services.calendar.Calendar mService = null;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { "https://www.googleapis.com/auth/calendar" };
    private String mEmail;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount account;
    private GoogleApiClient mGoogleApiClient;
    private static boolean isLogin = false;
    public static boolean isHello = false;

    CalendarSync calendarThread;
    AlertDialog customDialog;
    static ProgressBar pb;
    Handler handler = new Handler(Looper.getMainLooper());
    static TextView ptt;
    static Context mContext;

   PlanSQLiteHelper db;
   Button btnSearch;
   EditText input;

    @Override

    public void onBackPressed() {

        if (dlDrawer.isDrawerOpen(lvNavList)) {
            dlDrawer.closeDrawer(lvNavList);
        } else {
            super.onBackPressed();
        }
    }
    @Override

    protected void onCreate(Bundle savedInstanceState) {
       // setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        pb = new ProgressBar(this);
        pb = (ProgressBar)findViewById(R.id.progressBar);
        db = new PlanSQLiteHelper(getApplicationContext());

        btnSearch = findViewById(R.id.search_area);
        input = findViewById(R.id.EditWhereToGo);
        input.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if( i == KeyEvent.KEYCODE_ENTER ){
                    mapMain(btnSearch);
                }
                return false;
            }
        });

        //SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //SharedPreferences.Editor editor = mPref.edit();
        //editor.clear();

        //editor.commit();

        lvNavList = (NavigationView)findViewById(R.id.lv_activity_main_nav_list);

        flContainer = (FrameLayout)findViewById(R.id.fl_activity_main_container);
        ptt = (TextView)findViewById(R.id.progText);

        btn = (ImageButton)findViewById(R.id.menu_action_button);
        Log.d("Main Access", "Hello Main");

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        account = GoogleSignIn.getLastSignedInAccount(this);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        //getResultsFromApi();
//        SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        SharedPreferences.Editor editor = mPref.edit();
//        editor.clear();
//
//        editor.commit();

        String email = loadSavedPreferences();
            if (email.equals("EmailStuff") && EasyPermissions.hasPermissions(
                    this, Manifest.permission.GET_ACCOUNTS)) {
                navItems[0] = "LogOut";
                //lvNavList.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, navItems));
            } else {
                Intent nextScreen = new Intent(MainActivity.this, LoginPage.class);
                nextScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(nextScreen);
                ActivityCompat.finishAffinity(MainActivity.this);
            }

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlDrawer.openDrawer(lvNavList);

            }
        });
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        pb.setVisibility(View.INVISIBLE);
        //lvNavList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, navItems));
        //lvNavList.setOnItemClickListener(new MainActivity.DrawerItemClickListener());
        dlDrawer = (DrawerLayout)findViewById(R.id.dl_activity_main_drawer);

        lvNavList.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem){
                int id = menuItem.getItemId();
                if(id == R.id.menu_drawer_home){

                }
                else if(id == R.id.menu_drawer_mail){
                    mCredential = GoogleAccountCredential.usingOAuth2(
                            getApplicationContext(), Arrays.asList(SCOPES))
                            .setBackOff(new ExponentialBackOff());
                    //pb.setVisibility(View.VISIBLE);

                    getResultsFromApi();
                }
                return false;
            }
        });
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(getApplicationContext(), "Connection Succeeded", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override

        public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
            switch (position) {
                case 0:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setTitle("Travel Maker");
                    builder.setMessage("logout?");
                    builder.setPositiveButton("yes", dialogListener);

                    builder.setNegativeButton("no", null);
                    customDialog = builder.create();
                    customDialog.show();
                    break;
                case 1:
                    Intent intent = new Intent(getApplicationContext(), Budgeting.class);
                    startActivity(intent);

                    break;
                case 2:
//                    Intent sendIntent = new Intent();
//                    sendIntent.setAction(Intent.ACTION_SEND);
//                    sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
//                    sendIntent.setType("text/plain");
//                    startActivity(sendIntent);



                    break;

                case 3:
                    mCredential = GoogleAccountCredential.usingOAuth2(
                            getApplicationContext(), Arrays.asList(SCOPES))
                            .setBackOff(new ExponentialBackOff());
                    getResultsFromApi();
                    break;

                case 4:
                    createPdf();
                    break;
            }
            dlDrawer.closeDrawer(lvNavList);
        }
    }
    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (dialog == customDialog && which == DialogInterface.BUTTON_POSITIVE) {
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                if (mCredential == null) {
                    navItems[0] = "LogOut";
                    //lvNavList.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, navItems));
                } else {
                    Intent nextScreen = new Intent(MainActivity.this, LoginPage.class);
                    nextScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(nextScreen);
                    ActivityCompat.finishAffinity(MainActivity.this);
                }
                Toast.makeText(getApplicationContext(), "successfully logout", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                });
    }
    public void calenderMain(View v) {
        startActivity(new Intent(getApplicationContext(), CalendarMain.class));
    }

    public void mapMain(View v){

        String city = input.getText().toString();
        input.setText("");
        LatLng center;
        Plan plan;

        Geocoder geocoder = new Geocoder(this);
        List<Address> list = null;
        double latitude, longitude;

        try {
            list = geocoder.getFromLocationName(city, 10);
        } catch (IOException e) {
            e.printStackTrace();
           // makeText(MainActivity.this, "No input value", LENGTH_SHORT).show();
        }

        if (list != null) {
            if (list.size() == 0) {
                makeText(MainActivity.this, "No matching area info", LENGTH_SHORT).show();
            }
            else {
                plan = new Plan();

                latitude = list.get(0).getLatitude();
                longitude = list.get(0).getLongitude();

                center = new LatLng(latitude, longitude);

                plan.setCentre(center);
                plan.setCity(city);
                plan.setTitle(city); //default title is a city name

                long plan_id = db.createPlan(plan);
/*                db = helper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("KEY_ID",plan_id);
                db.insert("TABLE_PLAN",null,values);
*/
                Intent intent = new Intent(getApplicationContext(),MapMain.class);
                intent.putExtra("plan_id",plan_id);

                startActivity(intent);
            }
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case SIGN_IN :
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.", Toast.LENGTH_LONG).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                if(mCredential.getSelectedAccountName() != null){
                    navItems[0] = "LogOut";

                    calendarThread = new CalendarSync(mCredential, getApplicationContext(), pb, handler);
                    Thread calendar = new Thread(calendarThread);
                    calendar.start();
                }
                else {
                    navItems[0] = "LogOut";
                }
                //lvNavList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, navItems));
                //lvNavList.setOnItemClickListener(new MainActivity.DrawerItemClickListener());
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
        }
    }
    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *
    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public void showPlanList(View v){
        Intent intent = new Intent(MainActivity.this,planListActivity.class);
        startActivity(intent);
    }

    public void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(getApplicationContext(), "No network connection available.", Toast.LENGTH_LONG).show();
        } else {

            ptt.setVisibility(View.VISIBLE);

            calendarThread = new CalendarSync(mCredential, getApplicationContext(), pb, handler);
            Thread calendar = new Thread(calendarThread);
            calendar.start();
        }
    }
    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);

    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private String loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = sharedPreferences.getString("email", "EmailStuff");
        return name;
    }
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createPdf() {
//        PdfDocument document = new PdfDocument();
//        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(100, 100, 1).create();
//        PdfDocument.Page page = document.startPage(pageInfo);
//        TextView tv = new TextView(getApplicationContext());
//        tv.setText("Hello World");
//
//        View content = tv;
//        content.draw(page.getCanvas());
//        document.finishPage(page);
//        if(Build.VERSION.SDK_INT>22){
//            requestPermissions(new String[] {"WRITE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE"}, 1);
//        }
//        try {
//            File f = new File(Environment.getExternalStorageDirectory().getPath() + "/pruebaAppModerator.pdf");
//            FileOutputStream fos = new FileOutputStream(f);
//            document.writeTo(fos);
//            document.close();
//            fos.close();
//        } catch (IOException e) {
//            throw new RuntimeException("Error generating file", e);
//        }
    }

}