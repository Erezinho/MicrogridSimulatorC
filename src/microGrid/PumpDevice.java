package microGrid;

/**
 * an implementation of a water tank with pump <br>
 * water leaves the pump at random rate, <br>
 * as water level reaches low threshold, the pump start working.
 * @author shahar
 */
public class PumpDevice extends PowerDevice {

	/**	counter of pump devices, used to give distinct ID for devices */
	private static int numPumpDevices = 0;
	
	/** device unique id */
	private int id;

	/** the building this pump is in */
	private BuildingDevice buildingDevice;

	/** pump power in KW */
	private double power = 2;

	/** pump flow rate into the tank in m^3/s */
	private double flowRate = 0.1;

	/** average flow rate out of the tank in m^3/s */
	private double avgOutFlowRate = 0.05;

	/**	capacity of water tank in m^3 */
	private double tankCapacity = 10;
	
	/** water tank low threshold level in m^3 */
	private double tankLowThreshold = 1;

	/** is the pump now on or off */
	private boolean pumpIsOn = false;

	/** tank current capacity */
	private double tankCurrentCapacity = tankLowThreshold;

	// DR event handling
	
	/** are we during DR event? */
	private boolean drEventActive;
	
	/** when is the DR event ends */
	private double drEventEndTime;
	
	/** how much power did we commit to DR event */
	private double drEventPowerCommitment;

	/**
	 * constructor
	 */
	public PumpDevice() {
		super();
		System.out.println("Created a PumpDevice");

		id = ++numPumpDevices; // set unique id
		name = "Pump-"+id; // set name
	}

	
	/**
	 * do one time step of simulation
	 * @param schedule the Schedule object 
	 */
	@Override
	public void step(double time) {
		
		// handle end of DR event
		if (drEventActive  && time > drEventEndTime)
			drEventActive = false;

		System.out.println("PumpDevice " + id + " step(), ts=" + time + ", P=" + getCurrentPower(time));
	}


	/**
	 * save the building device for later use
	 * @param buildingDevice building to save
	 */
	public void setBuilding(BuildingDevice building) {
		buildingDevice = building;
	}


	/**
	 * get current power consumption
	 * turn pump on or off as needed
	 * @param time the time to get power at
	 * @return the power production/consumption at time 'time'
	 */
	@Override
	protected double calcCurrentPower(double time) {
		double outRate = avgOutFlowRate*(0.9 + Math.random()*0.2);
		tankCurrentCapacity -= outRate;
		assert(tankCurrentCapacity>=0); // make sure we have enough water

		// check if has to turn pump on
		if (tankCurrentCapacity < tankLowThreshold)
			pumpIsOn = true;

		// if pump is on, add water to tank
		double currentCapacity = drEventActive?0.5:1; // during DR event, the pump works at half power
		if (pumpIsOn) {
			tankCurrentCapacity += flowRate * currentCapacity; 
		}
		
		// check if tank is full
		double highWatermark = tankCapacity * (drEventActive?0.8:1); // during DR event fill tank 80%
		if (tankCurrentCapacity >= highWatermark) {
			tankCurrentCapacity = highWatermark;
			pumpIsOn = false;
		}
		
		return pumpIsOn?(-power*currentCapacity):0;
	}


	// implement Negotiator interface 
	
	/**
	 * implement Negotiator interface
	 * actually, nothing to do for PV
	 * @param schedule the Schedule object 
	 */
	@Override
	public Bid negotiate(Announce a, double time) { // call with announcement, receive a bid 
		return new Bid(power/2, a.time, 0); // commit for 50% of pump power consumption. FIXME: however, this is not acurate, since the pump can't reduce that amount all the time!
	}  

	/**
	 * implement Negotiator interface
	 * actually, nothing to do for PV
	 * @param schedule the Schedule object 
	 */
	@Override
	public void negotiate(Award a, double time) { // award a bid
		assert(a.amount <= power/2); // don't allow an award higher then 50% of pump power
		drEventPowerCommitment = Math.min(power/2, a.amount);
		drEventEndTime = time + a.time;
		drEventActive = true;
		
		pumpIsOn = false; // turn pump off for the time being. it'll be turned on when hitting low threshold
	}
}
