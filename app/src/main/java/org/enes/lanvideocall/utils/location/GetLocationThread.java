package org.enes.lanvideocall.utils.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import org.enes.lanvideocall.application.MyApplication;

public class GetLocationThread implements LocationListener {

    public interface GetLocationListener {

        void onGotLocation(Location location);

    }

    private GetLocationListener listener;

    public void setLocationListener(GetLocationListener l) {
        listener = l;
    }

    private AlertDialog alertDialog;

    private LocationManager locationManager;

    private LocationManager getLocationManager() {
        if (locationManager == null) {
            LocationManager tmp = (LocationManager) MyApplication.getInstance().
                    getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            locationManager = tmp;
        }
        return locationManager;
    }

    public GetLocationThread(AlertDialog alertDialog) {
        super();
        this.alertDialog = alertDialog;
    }

    private boolean isStart;

    public synchronized void start() {
        isStart = true;
        LocationManager locationManager = getLocationManager();
        try {
            if (MyApplication.getInstance().checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    MyApplication.getInstance().checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                return;
            }
            locationManager.
                    requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, 0, 0, this);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        getLocationManager().removeUpdates(this);
        if(alertDialog != null) {
            if(alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
        }
        if(listener != null) {
            listener.onGotLocation(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public synchronized void stop() {
        if(isStart) {
            locationManager.removeUpdates(this);
        }
    }

}