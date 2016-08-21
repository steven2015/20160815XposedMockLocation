package steven.runner;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements LocationSentCallback, ActivityCompat.OnRequestPermissionsResultCallback {
	private static final List<Destination> DESTINATIONS = new ArrayList<>();
	private Handler handler;
	private TextView txtLatitude;
	private TextView txtLongitude;
	private Spinner ddlSpeed;
	private Spinner ddlDestination;
	private Button btnUseCurrentLocation;
	private int countDown;

	static {
		DESTINATIONS.add(new Destination("藍田", 22.303001, 114.238930));
		DESTINATIONS.add(new Destination("旺角", 22.319338, 114.169333));
		DESTINATIONS.add(new Destination("荔枝角", 22.338233, 114.146131));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.activity_main);
		Intent intent = new Intent(this, MainService.class);
		super.stopService(intent);
		txtLatitude = (TextView) super.findViewById(R.id.txtLatitude);
		txtLongitude = (TextView) super.findViewById(R.id.txtLongitude);
		ddlSpeed = (Spinner) super.findViewById(R.id.ddlSpeed);
		ddlDestination = (Spinner) super.findViewById(R.id.ddlDestination);
		btnUseCurrentLocation = (Button) super.findViewById(R.id.btnUseCurrentLocation);
		handler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case 1234:
						txtLatitude.setText(String.valueOf(msg.getData().getDouble("LAT")));
						txtLongitude.setText(String.valueOf(msg.getData().getDouble("LNG")));
						break;
				}
			}
		};
		MainService.callback = this;
		ArrayAdapter<String> speedArray = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"WALK", "RUN", "BUS", "MAX"});
		speedArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ddlSpeed.setAdapter(speedArray);
		ddlSpeed.setSelection(0);
		ddlSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					MainService.meterPerSecond = Constants.WALK_SPEED;
				} else if (position == 1) {
					MainService.meterPerSecond = Constants.RUN_SPEED;
				} else if (position == 2) {
					MainService.meterPerSecond = Constants.BUS_SPEED;
				} else if (position == 3) {
					MainService.meterPerSecond = Constants.MAX_SPEED;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				parent.setSelection(0);
				MainService.meterPerSecond = Constants.WALK_SPEED;
			}
		});
		List<String> destinationNames = new ArrayList<>();
		destinationNames.add("請選擇");
		for (Destination d : DESTINATIONS) {
			destinationNames.add(d.getName());
		}
		final ArrayAdapter<String> destinationArray = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, destinationNames);
		destinationArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ddlDestination.setAdapter(destinationArray);
		ddlDestination.setSelection(0);
		ddlDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position > 0) {
					Destination d = DESTINATIONS.get(position - 1);
					txtLatitude.setText(String.valueOf(d.getLatitude()));
					txtLongitude.setText(String.valueOf(d.getLongitude()));
				}
				parent.setSelection(0);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				parent.setSelection(0);
			}
		});
		MainService.meterPerSecond = Constants.WALK_SPEED;
		setNextDirection();
	}

	public void btnToggleServiceClick(View view) {
		Intent intent = new Intent(this, MainService.class);
		Button button = (Button) view;
		if (button.getText().equals("Start")) {
			try {
				MainService.latitude = Double.parseDouble(txtLatitude.getText().toString());
				MainService.longitude = Double.parseDouble(txtLongitude.getText().toString());
			} catch (Exception e) {
				Toast.makeText(this, "Please input location.", Toast.LENGTH_SHORT).show();
				return;
			}
			if (Math.abs(MainService.latitude) < 1 || Math.abs(MainService.longitude) < 1) {
				Toast.makeText(this, "Please input location.", Toast.LENGTH_SHORT).show();
				return;
			}
			super.startService(intent);
			button.setText("Stop");
			txtLatitude.setEnabled(false);
			txtLongitude.setEnabled(false);
			ddlDestination.setEnabled(false);
			btnUseCurrentLocation.setEnabled(false);
		} else {
			super.stopService(intent);
			button.setText("Start");
			txtLatitude.setEnabled(true);
			txtLongitude.setEnabled(true);
			ddlDestination.setEnabled(true);
			btnUseCurrentLocation.setEnabled(true);
		}
	}

	public void btnUseCurrentLocationClick(View view) {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
			return;
		}
		getCurrentLocation();
	}

	@Override
	public void locationSent(Location l) {
		countDown--;
		if (countDown < 0) {
			setNextDirection();
		}
		Bundle bundle = new Bundle();
		bundle.putDouble("LAT", l.getLatitude());
		bundle.putDouble("LNG", l.getLongitude());
		Message msg = new Message();
		msg.what = 1234;
		msg.setData(bundle);
		handler.sendMessage(msg);
	}

	public void setNextDirection() {
		double a = Math.random() * 2 - 1;
		double b = Math.random() * 2 - 1;
		double c = Math.sqrt(a * a + b * b);
		MainService.normalizedLatitudeDelta = a / c;
		MainService.normalizedLongitudeDelta = b / c;
		countDown = (int) (60 + Math.random() * 240);
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		getCurrentLocation();
	}

	public void getCurrentLocation() {
		final LocationManager lm = (LocationManager) super.getSystemService(Service.LOCATION_SERVICE);
		//noinspection MissingPermission
		final LocationListener ll =
				new LocationListener() {
					@Override
					public void onLocationChanged(Location location) {
						txtLatitude.setText(String.valueOf(location.getLatitude()));
						txtLongitude.setText(String.valueOf(location.getLongitude()));
						Toast.makeText(MainActivity.this, "Used Current Location", Toast.LENGTH_SHORT).show();
						//noinspection MissingPermission
						lm.removeUpdates(this);
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
				};
		//noinspection MissingPermission
		lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, ll, null);
		//noinspection MissingPermission
		lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, ll, null);
	}
}
