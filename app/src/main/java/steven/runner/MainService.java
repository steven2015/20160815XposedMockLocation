package steven.runner;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by steven.lam.t.f on 2016/8/19.
 */
public class MainService extends Service implements Runnable {
	private volatile Thread thread = null;
	private volatile boolean active = false;
	public static volatile double latitude = 0;
	public static volatile double longitude = 0;
	public static volatile double meterPerSecond = 0;
	public static volatile LocationSentCallback callback;
	public static volatile boolean stay = false;
	public static volatile double direction = 0; // -pi to pi

	private View backgroundCircle;
	private TextView joystick;
	private LinearLayout window;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		createUI();
		LocationManager lm = (LocationManager) super.getSystemService(LOCATION_SERVICE);
		lm.setTestProviderEnabled("MOCK", true);
		active = true;
		thread = new Thread(this);
		thread.start();
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		WindowManager windowManager = (WindowManager) super.getSystemService(Context.WINDOW_SERVICE);
		//windowManager.removeView(this.window);
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
			final double newLatitude;
			final double newLongitude;
			if (stay) {
				newLatitude = latitude;
				newLongitude = longitude;
				l.setSpeed(0f);
			} else {
				newLatitude = ((int) ((latitude + Math.cos(direction) * 0.00001 * meterPerSecond) * 1000000)) / 1000000.0;
				newLongitude = ((int) ((longitude + Math.sin(direction) * 0.00001 * meterPerSecond) * 1000000)) / 1000000.0;
				final float[] results = new float[1];
				Location.distanceBetween(latitude, longitude, newLatitude, newLongitude, results);
				l.setSpeed((float) (((int) (results[0] * 10)) / 10.0));
			}
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

	private void createUI() {
		// create element
		backgroundCircle = new View(this);
		joystick = new TextView(this);
		window = new LinearLayout(this);
		// initialize
		backgroundCircle.setBackgroundResource(R.drawable.circle);
		joystick.setBackgroundResource(R.drawable.circle);
		// touch listener
		joystick.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
		});
		// display
		window.addView(backgroundCircle, 360, 360);
		window.addView(joystick, 120, 120);
		WindowManager windowManager = (WindowManager) super.getSystemService(Context.WINDOW_SERVICE);
		final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
		layoutParams.gravity = Gravity.TOP | Gravity.START;
		layoutParams.x = 0;
		layoutParams.y = 100;
		//windowManager.addView(this.window, layoutParams);
	}
}
