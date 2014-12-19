package microGrid;

/**
 * an implementation of a virtual device to simulate a building <br>
 * a building has an internal temp <br>
 * it will gain or loose heat as a function of the temp diff between inside and outside temp <br>
 *  Q = K * dt
 * it will also loose heat through chiller operation
 * internal temp is a function of internal heat
 *  Ti = Q / C
 * @author shahar
 */
public class BuildingDevice extends PowerDevice {

	/**	counter of temp devices, used to give distinct ID for devices */
	private static int numBuildingDevices = 0;
	
	/** device unique id */
	private int id;
	
	/** internal temp */
	private double t = 22; // start with 22C

	/** chiller in the building */
	private ChillerDevice chillerDevice;

	/** outside temperature */
	private TemperatureDevice tempDevice;

	/** water pump */
	private PumpDevice pumpDevice;
	
	// DR event handling
	
	/** are we during DR event? */
	private boolean drEventActive;
	
	/** when is the DR event ends */
	private double drEventEndTime;
	
	/** how much power did we commit to DR event */
	private double drEventPowerCommitment;

	/** heat transfer coefficient */
	private static final double K = 10;

	/** heat capacity coefficient. lower value means higher inertia */
	private static final double C = 0.0005;

	/** energy demand in a building, w/o chiller or pump */
	private double[] hourlyData = {11.67,11.67,11.67,11.67,11.67,11.67,11.67,11.67,13.33,18.33,20.00,21.67,21.67,21.67,21.67,21.67,21.67,21.67,21.67,21.67,21.67,21.67,21.67,18.33,11.67};

	/**
	 * constructor
	 * @param pumpDevice 
	 */
	public BuildingDevice(TemperatureDevice temp, ChillerDevice chiller, PumpDevice pump) {
		super();
		MicroGridModel.debugPrintln("Created a BuildingDevice");

		id = ++numBuildingDevices; // set unique id
		name = "Building-"+id; // set name
		
		setHourlyData(hourlyData, -1); // fill baseEnergyCurve with hourly data, data taken from Azuri building base consumption approximation 

		tempDevice = temp; // save outside temperature
		chillerDevice = chiller; // save chiller
		chillerDevice.setBuilding(this); // tell chiller it resides in our building
		pumpDevice = pump; // save pump
		pumpDevice.setBuilding(this); // tell chiller it resides in our building
	}

	
	/**
	 * do one time step of simulation
	 * @param schedule the Schedule object 
	 */
	@Override
	public void step(double time) {
		// calc new t (internal temp) 
		double tOut = tempDevice.getCurrentTemperature(time); // outside temp
		double Qdot = K * (tOut-t); // heat flux into the building
		double Qin = t/C; // internal heat
		Qin += Qdot; // add Q flux
		Qin -= chillerDevice.getCurrentHeatTransfer(time); // substract heat removed by chiller
		t = C * Qin; // calc new internal temp

		// handle end of DR event
		if (drEventActive  && time > drEventEndTime)
			drEventActive = false;

		MicroGridModel.debugPrintln("BuildingDevice " + id + " step(), ts=" + time + ", temp=" + t);
	}


	public double getCurrentInternalTemp(double time) {
		return t; // FIXME: might be called before step(), so t will hold the previous step's t...
	}


	/**
	 * get current power production/consumption
	 * if during a DR event, subtract the committed amount
	 * @param time the time to get power at
	 * @return the power production/consumption at time 'time'
	 */
	@Override
	public double getCurrentPower(double time) {
		double p = super.getCurrentPower(time);
		
		// handle DR event
		if (drEventActive)
			return p-drEventPowerCommitment;
		return p;
	}
	

	// implement Negotiator interface 
	
	/**
	 * implement Negotiator interface
	 * actually, nothing to do for PV
	 * @param schedule the Schedule object 
	 */
	@Override
	public Bid negotiate(Announce a, double time) { // call with announcement, receive a bid 
		double p = getCurrentPower(time);
		return new Bid(p/10, a.time, 0); // commit for 10% of current power consumption
		}  

	/**
	 * implement Negotiator interface
	 * actually, nothing to do for PV
	 * @param schedule the Schedule object 
	 */
	@Override
	public void negotiate(Award a, double time) { // award a bid
		double p = getCurrentPower(time);
		assert(a.amount <= p/10); // don't allow an award higher then 10% of current power
		drEventPowerCommitment = Math.min(p/10, a.amount);
		drEventEndTime = time + a.time;
		drEventActive = true;
	}
}

/**
 * a wrapper class to draw building internal temp chart
 * @author user
 *
 */
class BuildingWrapperForTemperature implements Chartable {
	BuildingDevice buildingDevice;
	BuildingWrapperForTemperature(BuildingDevice building) { buildingDevice = building; }
	@Override
	public double getDataForChart(double time) { return buildingDevice.getCurrentInternalTemp(time); }
}