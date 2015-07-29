/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
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
package com.sitewhere.android.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Dialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.sitewhere.android.protobuf.SiteWhereProtobufActivity;

/**
 * Fragment that implements example application.
 * 
 * @author Derek
 */
public class ExampleFragment extends MapFragment implements LocationListener, SensorEventListener {

	/** Tag used for logging */
	private static final String TAG = "ExampleFragment";

	/** Interval at which locations are sent */
	private static final int SEND_INTERVAL_IN_SECONDS = 5;

	/** Text views for lat, long, altitude */
	private TextView txtLat;
	private TextView txtLon;
	private TextView txtAlt;

	/** Text views for rotation vector */
	private TextView txtRotX;
	private TextView txtRotY;
	private TextView txtRotZ;

	/** Manages location updates */
	protected LocationManager locationManager;

	/** Manages sensors */
	protected SensorManager sensorManager;

	/** Sensor for rotation vector */
	protected Sensor rotationVector;

	/** Last reported location */
	private Location lastLocation;

	/** Last reported rotation vector */
	private float[] lastRotation;

	/** Used to schedule a recurring report to SiteWhere for location */
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup,
	 * android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup example = (ViewGroup) inflater.inflate(R.layout.example, container, false);

		View map = super.onCreateView(inflater, example, savedInstanceState);
		FrameLayout.LayoutParams mapParams = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		map.setLayoutParams(mapParams);
		example.addView(map);

		View overlay = inflater.inflate(R.layout.sensor_overlay, example, false);
		FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		overlay.setLayoutParams(overlayParams);
		example.addView(overlay);

		return example;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPostCreate(android.os.Bundle)
	 */
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Getting Google Play availability status
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity().getBaseContext());

		// If Google Play not available, show dialog for dealing with it.
		if (status != ConnectionResult.SUCCESS) {
			int requestCode = 10;
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, getActivity(), requestCode);
			dialog.show();
		} else {
			hookViews();
			getMap().setMyLocationEnabled(true);
			locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			scheduler.scheduleAtFixedRate(new LocationReporter(), SEND_INTERVAL_IN_SECONDS,
					SEND_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);

			sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
			rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	/**
	 * Get reference to views.
	 */
	protected void hookViews() {
		txtLat = (TextView) getActivity().findViewById(R.id.overlay_lat_value);
		txtLon = (TextView) getActivity().findViewById(R.id.overlay_lon_value);
		txtAlt = (TextView) getActivity().findViewById(R.id.overlay_alt_value);

		txtRotX = (TextView) getActivity().findViewById(R.id.overlay_rotx_value);
		txtRotY = (TextView) getActivity().findViewById(R.id.overlay_roty_value);
		txtRotZ = (TextView) getActivity().findViewById(R.id.overlay_rotz_value);
	}

	/**
	 * Sends most recent location and measurements to SiteWhere.
	 */
	protected void onSendDataToSiteWhere() {
		SiteWhereProtobufActivity sw = (SiteWhereProtobufActivity) getActivity();
		try {
			if (lastLocation != null) {
				sw.sendLocation(sw.getUniqueDeviceId(), null, lastLocation.getLatitude(),
						lastLocation.getLongitude(), lastLocation.getAltitude());
				sw.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(getActivity(), "Sent Location to SiteWhere", Toast.LENGTH_SHORT)
								.show();
					}
				});
			}
			if (lastRotation != null) {
				sw.sendMeasurement(sw.getUniqueDeviceId(), null, "x.rotation", lastRotation[0]);
				sw.sendMeasurement(sw.getUniqueDeviceId(), null, "y.rotation", lastRotation[1]);
				sw.sendMeasurement(sw.getUniqueDeviceId(), null, "z.rotation", lastRotation[2]);
			}
		} catch (Throwable e) {
			Log.e(TAG, "Unable to send location to SiteWhere.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	@Override
	public void onLocationChanged(Location location) {

		if (getMap() == null) {
			return;
		}

		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		LatLng latLng = new LatLng(latitude, longitude);

		// For first location reading, center and zoom to location.
		if (lastLocation == null) {
			CameraPosition position = getMap().getCameraPosition();
			CameraPosition.Builder builder = CameraPosition.builder(position).target(latLng).zoom(15)
					.tilt(45);
			getMap().animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
		} else {
			getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
		}

		txtLat.setText(String.format("%1.6f", latitude));
		txtLon.setText(String.format("%1.6f", longitude));
		txtAlt.setText(String.format("%1.6f", location.getAltitude()));

		this.lastLocation = location;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "Location provider (" + provider + ") disabled.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "Location provider (" + provider + ") enabled.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (LocationManager.GPS_PROVIDER.equals(provider)) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "Sensor accuracy changed for " + sensor.getName() + ".");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		lastRotation = event.values;
		if (lastRotation != null) {
			txtRotX.setText(String.format("%1.6f", lastRotation[0]));
			txtRotY.setText(String.format("%1.6f", lastRotation[1]));
			txtRotZ.setText(String.format("%1.6f", lastRotation[2]));
		}
	}

	/**
	 * Runs location reporting in a separate thread.
	 * 
	 * @author Derek
	 */
	private class LocationReporter implements Runnable {

		@Override
		public void run() {
			onSendDataToSiteWhere();
		}
	}
}