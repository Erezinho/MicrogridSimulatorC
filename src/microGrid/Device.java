package microGrid;

import java.util.TreeMap;

/**
 * a superclass for all devices
 * @author shahar
 */
public class Device implements Chartable {

	/** device's name */
	protected String name;
	
	/** map from time to power, describing base power curve of device */
	protected TreeMap<Double,Double> baseHourlyData;
	
	/** map from time to power, can override base power curve */
	protected TreeMap<Double,Double> overrideEnergyCurve;
	
	
	/**
	 * constructor
	 */
	public Device() {
		
		baseHourlyData = new TreeMap<Double,Double>();
		overrideEnergyCurve = new TreeMap<Double,Double>();
		
	} // public Device

	
	/**
	 * get device's name
	 * @return device's name
	 */
	public String getName() { return name; }
	
	
	/**
	 * set baseEnergyCurve with a 1-hour interval data from an array
	 * @param hourlyData an array of 24 doubles for energy production (or consumption) in each hour
	 */
	public void setHourlyData(double[] hourlyData) { setHourlyData(hourlyData, 1); } // default value for factor
	
	/**
	 * set baseEnergyCurve with a 1-hour interval data from an array
	 * @param hourlyData an array of 25 (0..24) doubles for energy production (or consumption) in each hour
	 * @param factor an optional parameter to multiply each hourlyData entry
	 */
	public void setHourlyData(double[] hourlyData, double factor) {
		for (int h=0; h<25; h++)
			baseHourlyData.put((double)h, hourlyData[h] * factor);
	}
	
	
	/**
	 * calc energy production/consumption at certain time 
	 * @param time time to calc energy at, in hours from midnight
	 * @return power at time 'time' in KW
	 */
	protected double interpoleteHourlyData(double time) {
		
		double energy=0;
		
		// get index of hour below 'time'
		Double t1 = baseHourlyData.floorKey(time);
		Double t2 = baseHourlyData.ceilingKey(time);
		
		assert(t1!=null);
		assert(t2!=null);
		
		// get energy before and after time
		double e1 = baseHourlyData.get(t1);
		double e2 = baseHourlyData.get(t2);
		
		// interpolate energy
		if (time==t1) // time is an exact timestamp
			energy = e1;
		else
			energy = e1 + (e2-e1)*(time-t1); // linear interpolation

		return energy;		
	}
	

	/**
	 * do one time step of simulation <br>
	 * should be overriden
	 * @param time the time (in hours from midnight) 
	 */
	public void step(double time) {
		System.out.println("Device step()");
	}

	
	/**
	 * get data to draw on simulation chart <br>
	 * default is interpolation of hourly data given to device
	 * @param time the time to get power at
	 * @return the power production/consumption at time 'time'
	 */
	public double getDataForChart(double time) {
		return interpoleteHourlyData(time);
	}
}

/**
 * subclass of {@link Device} with cached power consumption/generation
 * @author shahar
 *
 */
abstract class PowerDevice extends Device implements Negotiator {
	/** last time power was calculated */
	private double lastTimePowerCalc;
	
	/** last power calculated */
	private double cachedPower;
	
	/**
	 * get current power production/consumption
	 * @param time the time to get power at
	 * @return the power production/consumption at time 'time'
	 */
	public double getCurrentPower(double time) {
		if (time > lastTimePowerCalc) {
			// should recalc and cache power
			lastTimePowerCalc = time;
			cachedPower = calcCurrentPower(time);
		}
		return cachedPower;
	}


	/**
	 * calc the current power production/consumption
	 * should be overriden
	 * @param time the time to get power at
	 * @return the power production/consumption at time 'time'
	 */
	protected double calcCurrentPower(double time) {
		return interpoleteHourlyData(time);
	}


	/**
	 * get data to draw on simulation chart <br>
	 * default is current power production/consumption
	 * @param time the time to get power at
	 * @return the power production/consumption at time 'time'
	 */
	@Override
	public double getDataForChart(double time) {
		return getCurrentPower(time);
	}
}