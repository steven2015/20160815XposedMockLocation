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
			l.setAccuracy(5.0f);
			l.setSpeed(1);
			l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
			l.setTime(System.currentTimeMillis());
			Bundle bundle = new Bundle();
			bundle.putInt("satellites", 10);
			l.setExtras(bundle);
			lm.setTestProviderLocation("MOCK", l);
			latitude = latitude + 0.000001;
			longitude = longitude + 0.000001;
			try {
				Thread.sleep(1010);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}
}
