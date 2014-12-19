package microGrid;

/**
 * an implementation of PV system
 * @author shahar
 */
public class PVDevice extends PowerDevice {

	/**	counter of PV devices, used to give distinct ID for devices */
	private static int numPVDevices = 0;

	/** device unique id */
	private int id;

	/** rated KW for the system */
	private double ratedKW;

	/** standard production in KW for 1KWp system in TA area */
	private double[] hourlyData = {0,0,0,0,0,0.032258065,0.096774194,0.212903226,0.367741935,0.535483871,0.667741935,0.716129032,0.712903226,0.641935484,0.522580645,0.351612903,0.164516129,0.04516129,0.009677419,0,0,0,0,0,0,0};

	/**
	 * constructor
	 * @param kw the KW rating of the system
	 */
	public PVDevice(double kw) {
		super();
		MicroGridModel.debugPrintln("Created a PVDevice, reated at: " + kw + "KW");

		id = ++numPVDevices; // set unique id
		name = "PV-"+id; // set name

		ratedKW = kw;

		setHourlyData(hourlyData, ratedKW); // fill baseEnergyCurve with hourly data (multiplied by rated KW)
	}


	/**
	 * do one time step of simulation
	 * @param schedule the Schedule object 
	 */
	@Override
	public void step(double time) {
		MicroGridModel.debugPrintln("PVDevice " + id + " step(), ts=" + time + ", production=" + interpoleteHourlyData(time));
	}


	// implement Negotiator interface 
	
	/**
	 * implement Negotiator interface
	 * actually, nothing to do for PV
	 * @param schedule the Schedule object 
	 */
	@Override
	public Bid negotiate(Announce a, double time) { return null; } // call with announcement, receive a bid 

	/**
	 * implement Negotiator interface
	 * actually, nothing to do for PV
	 * @param schedule the Schedule object 
	 */
	@Override
	public void negotiate(Award a, double time) {} // award a bid
}
