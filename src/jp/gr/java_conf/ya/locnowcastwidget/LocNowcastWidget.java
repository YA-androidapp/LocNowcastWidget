package jp.gr.java_conf.ya.locnowcastwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocNowcastWidget extends AppWidgetProvider implements LocationListener, GpsStatus.Listener {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		// サービスの起動
		Intent intent = new Intent(context, LocNowcastWidgetService.class);
		context.startService(intent);
	}

	public void onDeleted(Context context, int[] appWidgetIds) {
		try {
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			locationManager.removeUpdates(this);
			locationManager.removeGpsStatusListener(this);
		} catch (Exception e) {
			Log.v("LocNowcastWidget", e.toString() + " : " + e.getMessage());
		}

		super.onDeleted(context, appWidgetIds);
	}

	public void onDisabled(Context context) {
		try {
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			locationManager.removeUpdates(this);
			locationManager.removeGpsStatusListener(this);
		} catch (Exception e) {
			Log.v("LocNowcastWidget", e.toString() + " : " + e.getMessage());
		}

		super.onDisabled(context);
	}

	@Override
	public void onLocationChanged(Location location) {
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

	@Override
	public void onGpsStatusChanged(int event) {
	}
}

//<!-- Copyright 2014 (c) YA <ya.androidapp@gmail.com> All rights reserved. -->