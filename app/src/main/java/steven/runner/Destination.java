package steven.runner;

/**
 * Created by Steven on 2016/8/21.
 */
public class Destination {
	private final String name;
	private final double latitude;
	private final double longitude;


	public Destination(String name, double latitude, double longitude) {
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;

	}

	public String getName() {
		return name;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
}
