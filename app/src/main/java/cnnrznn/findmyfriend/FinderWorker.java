package cnnrznn.findmyfriend;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/*
 * Created by cnnrznn on 10/25/15.
 * This application is used to determine the bearing and distance
 * to another user via a virtual communication channel by connecting
 * through a server. Users select the channel, then receive periodic
 * updates to distance and bearing to the other user.
 */
public class FinderWorker implements Runnable {

    private static final int BUFSIZE = 195;
    private static final int LOOP_INTERVAL = 500;

    private MainActivity mact;
    private int id;
    private boolean cont;
    private GoogleApiClient gac;
    private LocationListener locationListener;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private byte[] data;

    public FinderWorker(int id, GoogleApiClient googleApiClient, MainActivity mainActivity)
            throws SocketException, UnknownHostException {
        this.id = id;
        this.cont = true;
        mact = mainActivity;
        gac = googleApiClient;
        mact.remoteLoc = LocationServices.FusedLocationApi.getLastLocation(gac);
        socket = new DatagramSocket();
        socket.setSoTimeout(50);
        socket.setReceiveBufferSize(BUFSIZE);
        data = new byte[BUFSIZE];
        packet = new DatagramPacket(data, BUFSIZE);
        packet.setAddress(InetAddress.getByName("74.103.155.78"));
        packet.setPort(1234);

        startLocationRequest();
    }

    public void startLocationRequest() {
        try {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location newLocation) {
                    mact.localLoc = newLocation;
                }
            };

            LocationRequest locationRequest = (new LocationRequest())
                    .setInterval(LOOP_INTERVAL)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(gac, locationRequest, locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (cont) {
            if (gac != null) {
                Log.i("GPS", "Connected to Google Play Services");
                Log.i("avail", String.valueOf(LocationServices.FusedLocationApi
                        .getLocationAvailability(gac)));
                Log.i("cnnrznn", Integer.toString(id));
                if (mact.localLoc != null && mact.remoteLoc != null) {
                    Log.i("latitude", String.valueOf(mact.localLoc.getLatitude()));
                    Log.i("longitude", String.valueOf(mact.localLoc.getLongitude()));
                    mact.runOnUiThread(mact.setID);
                    mact.runOnUiThread(mact.setLatitude);
                    mact.runOnUiThread(mact.setLongitude);
                    mact.runOnUiThread(mact.setCurrBearing);
                    mact.runOnUiThread(mact.setTargetBearing);
                    mact.runOnUiThread(mact.setDistance);
                    Log.e("ui", "Run on UI thread!");
                }

                try {
                    byte[] lat = String.valueOf(mact.localLoc.getLatitude()).getBytes();
                    byte[] lon = String.valueOf(mact.localLoc.getLongitude()).getBytes();
                    System.arraycopy(lat, 0, data, 0, lat.length);
                    System.arraycopy(lon, 0, data, lat.length + 1, lon.length);
                    data[lat.length] = ',';
                    data[lat.length + lon.length + 1] = ',';
                    byte[] bid = ByteBuffer.allocate(4).putInt(id).array();
                    System.arraycopy(bid, 0, data, lat.length + lon.length + 2, 4);
                    data[lat.length + lon.length + 6] = ',';
                    Log.d("data to server", Arrays.toString(data));
                    packet.setData(data);
                    socket.send(packet);
                    socket.receive(packet);
                    Log.d("data from server", Arrays.toString(packet.getData()));
                    String[] targetArr = (new String(packet.getData())).split(",");
                    Log.d("data parsed", Arrays.toString(targetArr));
                    mact.remoteLoc.setLatitude(Double.parseDouble(targetArr[0]));
                    mact.remoteLoc.setLongitude(Double.parseDouble(targetArr[1]));
                } catch (Exception e) {
                    Log.e("comm", "error preparing or sending packet");
                    e.printStackTrace();
                }
            }

            // sleep
            try {
                Thread.sleep(LOOP_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        LocationServices.FusedLocationApi.removeLocationUpdates(gac, locationListener);
        cont = false;
    }
}
