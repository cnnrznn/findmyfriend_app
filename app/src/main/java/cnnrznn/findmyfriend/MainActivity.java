package cnnrznn.findmyfriend;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.net.SocketException;
import java.net.UnknownHostException;


public class MainActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private int id;
    private Mode mode;
    private FinderWorker worker = null;
    private GoogleApiClient gac;
    protected String latitude;
    protected String longitude;
    protected Location localLoc;
    protected Location remoteLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        mode = Mode.MAIN;

        gac = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mode == Mode.FIND) {
            worker.startLocationRequest();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    protected void onStart() {
        super.onStart();
        gac.connect();
    }

    @Override
    protected void onStop() {
        gac.disconnect();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        switch(mode) {
            case MAIN:
                super.onBackPressed();
                break;
            default:
                setContentView(R.layout.layout_main);
                mode = Mode.MAIN;
                stopFinder();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void toast(String string) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }

    public void findID(View view) {
        toast("findID called");
        mode = Mode.CONNECT;
        try {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch(Exception e) {}
        int tmpid;
        if (((TextView)findViewById(R.id.editText)).getText().toString().equals("")) {
            tmpid = -1;
        }
        else{
            try {
                tmpid = Integer.parseInt(((TextView) findViewById(R.id.editText))
                        .getText().toString());
            } catch (NumberFormatException nfe) {
                tmpid = -1;
            }
        }
        if (tmpid < 0) {
            toast("Pick a number between 0 and " + Integer.MAX_VALUE + ", please");
        }
        else {
            // TODO check with server that comm id exists
            startFinder(tmpid);
        }
    }

    private int requestNewID() {
        // TODO retrieve a new communication ID from server


        return 0;
    }

    public void getID(View view) {
        toast("getID called!");
        mode = Mode.CONNECT;
        int id = requestNewID();
        startFinder(id);
    }

    private void startFinder(int id) {
        toast("startFinder called!");
        setContentView(R.layout.layout_finder);
        mode = Mode.FIND;
        this.id = id;
        try {
            Log.d("cnnrznn", String.valueOf(this.id));
            worker = new FinderWorker(this.id, gac, this);
        } catch (SocketException e) {
            e.printStackTrace();
            finish();
        } catch (UnknownHostException uhe) {
            Log.e("cnnrznn", "Could not find host");
            uhe.printStackTrace();
            finish();
        }
        (new Thread(worker)).start();
    }

    private void stopFinder() {
        toast("stopFinder called!");
        this.id = -1;
        worker.stop();
    }

    public Runnable setID = new Runnable() {
        @Override
        public void run() {
            try {
                ((TextView)findViewById(R.id.idText))
                        .setText("id: " + String.valueOf(id));
            } catch(Exception e) {}
        }
    };

    public Runnable setLatitude = new Runnable() {
        @Override
        public void run() {
            try {
                latitude = String.valueOf(localLoc.getLatitude());
                ((TextView) findViewById(R.id.latitudeText))
                        .setText("lat: " + latitude);
            } catch(Exception e) {}
        }
    };

    public Runnable setLongitude = new Runnable() {
        @Override
        public void run() {
            try {
                longitude = String.valueOf(localLoc.getLongitude());
                ((TextView) findViewById(R.id.longitudeText))
                        .setText("long: " + longitude);
            } catch (Exception e) {}
        }
    };

    public Runnable setCurrBearing = new Runnable() {
        @Override
        public void run() {
            try {
                ((TextView)findViewById(R.id.currBearingText))
                        .setText(String.valueOf("Your bearing: " +
                                localLoc.getBearing()));
            } catch(Exception e) {}
        }
    };

    public Runnable setTargetBearing = new Runnable() {
        @Override
        public void run() {
            try {
                ((TextView)findViewById(R.id.targetBearingText))
                        .setText(String.valueOf("Target bearing: " +
                                (localLoc.bearingTo(remoteLoc) + 360)%360));
            } catch(Exception e) {}
        }
    };

    public Runnable setDistance = new Runnable() {
        @Override
        public void run() {
            try {
                ((TextView) findViewById(R.id.distanceText))
                        .setText(String.valueOf(localLoc.distanceTo(remoteLoc))
                                + " meters");
            } catch (Exception e) {}
        }
    };
}
