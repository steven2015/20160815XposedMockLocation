package steven.runner.xposed;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import steven.runner.Utils;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


/**
 * Created by Steven on 2016/8/13.
 */
public class LocationPlugin implements IXposedHookLoadPackage {
	public static volatile Object locationManagerService = null;
	public static volatile boolean mocking = false;
	public static final Map<String, Location> LOCATION_MAP = new HashMap<>();
	public static final byte[] LOCK = new byte[0];

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
		final Class<?> locationManagerServiceClazz = XposedHelpers.findClassIfExists("com.android.server.LocationManagerService", loadPackageParam.classLoader);
		if (locationManagerServiceClazz == null) {
			// application
			hookAllMethods(LocationManager.class, "requestLocationUpdates", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					final Object firstParameter = param.args[0];
					if (firstParameter != null) {
						if (firstParameter.getClass() == String.class) {
							Log.d("steven", "requestLocationUpdates [" + Utils.getCaller(LocationManager.class.getName()) + "] " + firstParameter + " " + param.args[1] + " ms " + param.args[2] + " m " + Utils.identityToString(param.args[3]) + ":" + param.args[3]);
						} else if (firstParameter.getClass().getName().equals("android.location.LocationRequest")) {
							Log.d("steven", "requestLocationUpdates [" + Utils.getCaller(LocationManager.class.getName()) + "] " + Utils.identityToString(param.args[0]) + ":" + param.args[0] + " " + Utils.identityToString(param.args[1]) + ":" + param.args[1]);
						} else {
							Log.d("steven", "requestLocationUpdates [" + Utils.getCaller(LocationManager.class.getName()) + "] " + firstParameter + " ms " + param.args[1] + " m " + param.args[2] + " " + Utils.identityToString(param.args[3]) + ":" + param.args[3]);
						}
					}
				}
			});
			findAndHookMethod(LocationManager.class, "removeUpdates", LocationListener.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Log.d("steven", "removeUpdates [" + Utils.getCaller(LocationManager.class.getName()) + "] " + Utils.identityToString(param.args[0]));
				}
			});
			findAndHookMethod(LocationManager.class, "removeUpdates", PendingIntent.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Log.d("steven", "removeUpdates [" + Utils.getCaller(LocationManager.class.getName()) + "] " + Utils.identityToString(param.args[0]));
				}
			});
		} else {
			// system
			XposedHelpers.findAndHookConstructor(locationManagerServiceClazz, Context.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Log.d("steven", "initialize LocationManagerService " + Utils.identityToString(param.thisObject) + " " + Utils.identityToString(this));
					locationManagerService = param.thisObject;
				}
			});
			XposedHelpers.findAndHookMethod(locationManagerServiceClazz, "reportLocation", Location.class, boolean.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if ((Boolean) param.args[1] == false) {
						// not passive
						final Location l = (Location) param.args[0];
						if (mocking) {
							// mocking
							if (Utils.isLocationMocking(l)) {
								// proceed
								Utils.setLocationNotMocking(l);
								synchronized (LOCK) {
									LOCATION_MAP.put("MOCK", l);
								}
								// debug
								Utils.unparcel(l);
								Log.d("steven", "reportLocation mock " + Utils.identityToString(param.thisObject) + " " + l);
							} else {
								// ignore
								param.setResult(null);
								synchronized (LOCK) {
									LOCATION_MAP.put("MOCK_" + l.getProvider(), l);
								}
								// debug
								Utils.unparcel(l);
								Log.d("steven", "reportLocation real " + Utils.identityToString(param.thisObject) + " " + l);
							}
						} else {
							// not mocking
							if (Utils.isLocationMocking(l) == false) {
								// location also not mocking
								synchronized (LOCK) {
									LOCATION_MAP.put("MOCK_" + l.getProvider(), l);
								}
								// debug
								Utils.unparcel(l);
								Log.d("steven", "reportLocation real " + Utils.identityToString(param.thisObject) + " " + l);
							}
						}
					}
				}
			});
			XposedHelpers.findAndHookMethod(locationManagerServiceClazz, "setTestProviderEnabled", String.class, boolean.class, String.class, new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							if ("MOCK".equals(param.args[0])) {
								mocking = (Boolean) param.args[1];
								param.setResult(null);
								Log.d("steven", "current mock status is " + mocking);
							}
						}
					}
			);
			final Method reportLocationMethod = XposedHelpers.findMethodExactIfExists(locationManagerServiceClazz, "reportLocation", Location.class, boolean.class);
			XposedHelpers.findAndHookMethod(locationManagerServiceClazz, "setTestProviderLocation", String.class, Location.class, String.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if ("MOCK".equals(param.args[0])) {
						// send from mock
						if (locationManagerService == null) {
							Log.d("steven", "cannot mock since locationManagerService is null");
						} else {
							final Location l = (Location) param.args[1];
							Utils.setLocationMocking(l);
							final long identity = Binder.clearCallingIdentity();
							reportLocationMethod.invoke(locationManagerService, l, false);
							Binder.restoreCallingIdentity(identity);
							param.setResult(null);
							// debug
							Utils.unparcel(l);
							Log.d("steven", "trigger mock to " + l);
						}
					}
				}
			});
			final Class<?> locationRequestClazz = XposedHelpers.findClassIfExists("android.location.LocationRequest", loadPackageParam.classLoader);
			final Method getProviderMethod = XposedHelpers.findMethodExactIfExists(locationRequestClazz, "getProvider");
			XposedHelpers.findAndHookMethod(locationManagerServiceClazz, "getLastLocation", locationRequestClazz, String.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					final String provider = (String) getProviderMethod.invoke(param.args[0]);
					if (provider != null && provider.startsWith("MOCK")) {
						synchronized (LOCK) {
							param.setResult(LOCATION_MAP.get(provider));
						}
					}
				}
			});
		}
	}
}
