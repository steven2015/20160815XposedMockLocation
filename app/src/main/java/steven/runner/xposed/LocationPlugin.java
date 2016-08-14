package steven.runner.xposed;

import android.app.PendingIntent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import steven.runner.AppConfig;
import steven.runner.BuildConfig;
import steven.runner.Utils;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


/**
 * Created by Steven on 2016/8/13.
 */
public class LocationPlugin implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        final XSharedPreferences sharedPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, AppConfig.SHARED_PREFERENCES_FILE);
        sharedPreferences.makeWorldReadable();
        hookAllMethods(LocationManager.class, "requestLocationUpdates", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                final Object firstParameter = param.args[0];
                if (firstParameter != null) {
                    if (firstParameter.getClass() == String.class) {
                        Log.d("steven", "requestLocationUpdates [" + Utils.getCaller("android.location.LocationManager") + "] " + firstParameter + " " + param.args[1] + " ms " + param.args[2] + " m " + Utils.identityToString(param.args[3]) + ":" + param.args[3]);
                    } else if (firstParameter.getClass().getName().equals("android.location.LocationRequest")) {
                        Log.d("steven", "requestLocationUpdates [" + Utils.getCaller("android.location.LocationManager") + "] " + Utils.identityToString(param.args[0]) + ":" + param.args[0] + " " + Utils.identityToString(param.args[1]) + ":" + param.args[1]);
                    } else {
                        Log.d("steven", "requestLocationUpdates [" + Utils.getCaller("android.location.LocationManager") + "] " + firstParameter + " ms " + param.args[1] + " m " + param.args[2] + " " + Utils.identityToString(param.args[3]) + ":" + param.args[3]);
                    }
                }
                super.beforeHookedMethod(param);
            }
        });
        findAndHookMethod(LocationManager.class, "removeUpdates", LocationListener.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.d("steven", "removeUpdates [" + Utils.getCaller("android.location.LocationManager") + "] " + Utils.identityToString(param.args[0]) + ":" + param.args[0]);
            }
        });
        findAndHookMethod(LocationManager.class, "removeUpdates", PendingIntent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.d("steven", "removeUpdates [" + Utils.getCaller("android.location.LocationManager") + "] " + Utils.identityToString(param.args[0]) + ":" + param.args[0]);
            }
        });
        findAndHookMethod("com.android.server.LocationManagerService", loadPackageParam.classLoader, "reportLocation", Location.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((Boolean) param.args[1] == false) {
                    Location l = (Location) param.args[0];
                    Utils.unparcel(l);
                    Log.d("steven", "reportLocation " + Utils.identityToString(param.thisObject) + " " + l);
                }
            }
        });
    }
}
