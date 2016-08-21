package steven.runner;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;

/**
 * Created by steven.lam.t.f on 2016/8/19.
 */
public class MainService extends Service implements Runnable {
	private volatile Thread thread = null;
	private volatile boolean active = false;
	public static volatile double latitude = 0;
	public static volatile double longitude = 0;
	public static volatile double meterPerSecond = 0;
	public static volatile double normalizedLatitudeDelta = 0;
	public static volatile double normalizedLongitudeDelta = 0;
	public static volatile LocationSentCallback callback;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LocationManager lm = (LocationManager) super.getSystemService(LOCATION_SERVICE);
		lm.setTestProviderEnabled("MOCK", true);
		active = true;
		thread = new Thread(this);
		thread.start();
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		LocationManager lm = (LocationManager) super.getSystemService(LOCATION_SERVICE);
		lm.setTestProviderEnabled("MOCK", false);
		active = false;
	}

	@Override
	public void run() {
		LocationManager lm = (LocationManager) super.getSystemService(LOCATION_SERVICE);
		while (active) {
			Location l = new Location(LocationManager.GPS_PROVIDER);
			l.setLatitude(latitude);
			l.setLongitude(longitude);
			l.setAccuracy((float) (5 + Math.random() * 10));
			final double newLatitude = ((int) ((latitude + normalizedLatitudeDelta * 0.00001 * meterPerSecond) * 1000000)) / 1000000.0;
			final double newLongitude = ((int) ((longitude + normalizedLongitudeDelta * 0.00001 * meterPerSecond) * 1000000)) / 1000000.0;
			final float[] results = new float[1];
			Location.distanceBetween(latitude, longitude, newLatitude, newLongitude, results);
			l.setSpeed((float) (((int) (results[0] * 10)) / 10.0));
			l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
			l.setTime(System.currentTimeMillis());
			Bundle bundle = new Bundle();
			bundle.putInt("satellites", 7);
			l.setExtras(bundle);
			lm.setTestProviderLocation("MOCK", l);
			latitude = newLatitude;
			longitude = newLongitude;
			LocationSentCallback callback = MainService.callback;
			if (callback != null) {
				callback.locationSent(l);
			}
			try {
				Thread.sleep(1010);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}
}
