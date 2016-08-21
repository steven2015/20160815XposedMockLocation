package steven.runner;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;

/**
 * Created by Steven on 2016/8/14.
 */
public class Utils {
	public static final String identityToString(Object o) {
		if (o == null) {
			return "null";
		} else {
			return o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
		}
	}

	public static final String getCaller(String previousClassName) {
		boolean flag = false;
		for (StackTraceElement trace : Thread.currentThread().getStackTrace()) {
			if (trace.getClassName().equals(previousClassName)) {
				flag = true;
			} else if (flag) {
				return trace.toString();
			}
		}
		return "[unknown caller]";
	}

	public static final void unparcel(Location l) {
		if (l.getExtras() != null) {
			final Bundle extras = l.getExtras();
			extras.size();
			for (String key : extras.keySet()) {
				final Object o = extras.get(key);
				if (o instanceof Location) {
					unparcel((Location) o);
				}
			}
		}
	}

	public static final boolean isLocationMocking(Location l) {
		return l.getExtras() != null && l.getExtras().get("steven") != null;
	}

	public static final void setLocationMocking(Location l) {
		if (l.getExtras() == null) {
			l.setExtras(new Bundle());
		}
		l.getExtras().putString("steven", "steven");
	}

	public static final void setLocationNotMocking(Location l) {
		if (l.getExtras() != null) {
			l.getExtras().remove("steven");
		}
	}

	public static final boolean isServiceRunning(final Context context, final Class<? extends Service> clazz) {
		final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (final ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
			if (clazz.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
