package microGrid;

/**
 * an implementation of a virtual device to supply temperature data
 * @author shahar
 */
public class TemperatureDevice extends Device {

	/**	counter of temp devices, used to give distinct ID for devices */
	private static int numTempDevices = 0;
	
	/** device unique id */
	private int id;
	
	/** temp in july from Dalia Kibbutz */
	private double[] hourlyData = {22.51044907,22.21877315,21.93483333,21.79383333,21.64320833,21.55037037,22.97372685,24.33451389,25.76952778,26.72823611,27.66768056,28.60111111,28.70008333,28.80197222,28.89086111,28.14302778,27.48191667,26.75135185,25.57820833,24.39914815,23.24544444,22.84444444,22.44933333,22.05,21.6};

	/**
	 * constructor
	 */
	public TemperatureDevice() {
		super();
		MicroGridModel.debugPrintln("Created a TemperatureDevice");

		id = ++numTempDevices; // set unique id
		name = "Temp-"+id; // set name
		
		setHourlyData(hourlyData); // fill baseEnergyCurve with hourly data 
	}

	
	/**
	 * a wrapper to interpoleteHourlyData
	 * @param time time to calc temp at, in hours from midnight
	 * @return temp at time 'time' in C
	 */
	public double getCurrentTemperature(double time) {
		return interpoleteHourlyData(time);
	}

	
	/**
	 * do one time step of simulation
	 * @param schedule the Schedule object 
	 */
	@Override
	public void step(double time) {
		MicroGridModel.debugPrintln("TempDevice " + id + " step(), ts=" + time + ", temp=" + getCurrentTemperature(time));
	}
}
