package steven.runner;

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
}
