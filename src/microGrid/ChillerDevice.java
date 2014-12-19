package microGrid;

/**
 * an implementation of a chiller <br>
 * a chiller takes out heat from a building in several steps, <br>
 * as a function of the temp diff between building temp and set-point temp.
 * @author shahar
 */
public class ChillerDevice extends PowerDevice {

	/**	counter of chiller devices, used to give distinct ID for devices */
	private static int numChillerDevices = 0;
	
	/** device unique id */
	private int id;

	/** chiller COP */
	private static final double COP = 3;

	/** the building this chiller is in */
	private BuildingDevice buildingDevice;

	/** number of compressors in chiller */
	private double numCompressors = 4;

	/** max chiller power in KW (when all compressors are active) */
	private double maxPower = 25;

	/** chiller set-point temperature in C deg*/
	private double setPoint = 20;

	/**
	 * constructor
	 */
	public ChillerDevice() {
		super();
		MicroGridModel.debugPrintln("Created a ChillerDevice");

		id = ++numChillerDevices; // set unique id
		name = "Chiler-"+id; // set name
	}

	
	/**
	 * do one time step of simulation
	 * @param schedule the Schedule object 
	 */
	@Override
	public void step(double time) {
		MicroGridModel.debugPrintln("ChillerDevice " + id + " step(), ts=" + time + ", P=" + getCurrentPower(time));
	}


	/**
	 * save the building device for later use
	 * @param buildingDevice building to save
	 */
	public void setBuilding(BuildingDevice building) {
		buildingDevice = building;
	}


	/**
	 * calc the current power consumption
	 * power in a function of the number of active compressors
	 * compressors are turned on as the difference between internal temp and set point is increased
	 * @param time the time to get power at
	 * @return the power production/consumption at time 'time'
	 */
	protected double calcCurrentPower(double time) {
		double Tin = buildingDevice.getCurrentInternalTemp(time); // building internal temp
		double dT = Math.max(Tin - setPoint, 0); // temperature difference between set point temp and building temp, but don't get below zero
		int numActiveCompressores = (int) Math.min(dT/0.6, numCompressors); // activate a compressor for each 0.6C deg difference
		double P = maxPower/numCompressors * numActiveCompressores; // current power
		return -P; // power used is negative
	}


	/**
	 * calc the heat flux the chiller takes out of the building at time 'time'
	 * @param time	the current time
	 * @return		the heat flux in KWth
	 */
	public double getCurrentHeatTransfer(double time) {
		return -COP * getCurrentPower(time); // power is negative...
	}


	// implement Negotiator interface 
	
	/**
	 * implement Negotiator interface
	 * actually, nothing to do for Chiller
	 * @param schedule the Schedule object 
	 */
	@Override
	public Bid negotiate(Announce a, double time) { // call with announcement, receive a bid 
		// TODO
		return new Bid(0,0,0); 
		}  

	/**
	 * implement Negotiator interface
	 * actually, nothing to do for PV
	 * @param schedule the Schedule object 
	 */
	@Override
	public void negotiate(Award a, double time) { // award a bid
		// TODO
	}
}
