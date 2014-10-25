package microGrid;

import java.util.ArrayList;
import java.util.List;

import microGrid.Contract.RequestType;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.util.SimUtilities;

//TODO: basic contract - I need power P, who can deliver
//TODO: economic contract - I need power P, who can deliver at price X
//TODO: advanced contract - I need power curve P(), who can deliver

interface Chartable {
	public double getDataForChart(double time);
}

/**
 * master object to set and run model
 * @author shahar
 * @version	0.001.0003	add second chart for temperature <br>
 * 			0.001.0002	temperature, chiller, and pump devices added <br>
 * 			0.001.0001	two PV devices, shows production graph
 */
public class MicroGridModel extends SimModelImpl implements Chartable {

	// Default Values

	private static final int NUMPVDEVICES = 2;

	// private variables

	/** schedule variable to run the simulation */
	private Schedule schedule;

	/** list of all devices, used to call step() */
	private List<Device> devices = new ArrayList<Device>();

	/** list of power devices, used to draw power chart and calc total power */
	private List<PowerDevice> powerDevices = new ArrayList<PowerDevice>();

	/** virtual device to calc outside temp */
	private TempDevice tempDevice;

	/** a chiller in the building */
	private ChillerDevice chillerDevice;

	/** a pump in the building */
	private PumpDevice pumpDevice;

	/** a building */
	private BuildingDevice buildingDevice;

	// for charts
	private DisplaySurface displaySurf;
	private OpenSequenceGraph currentPowerChart;
	private OpenSequenceGraph currentTemperatureChart;

	// model parameters
	private int numPVDevices = NUMPVDEVICES;


	@Override
	public String getName(){
		return "MicroGrid Simulation";
	}

	@Override
	public void setup(){
		System.out.println("Running setup");
		schedule = new Schedule(1);

		// prepare charts

		// Tear down Displays
		if (displaySurf != null) displaySurf.dispose();
		displaySurf = null;

		if (currentPowerChart != null) currentPowerChart.dispose();
		currentPowerChart = null;

		if (currentTemperatureChart != null) currentTemperatureChart.dispose();
		currentTemperatureChart = null;

		// Create Displays
		displaySurf = new DisplaySurface(this, "Simulation Window 1");
		currentPowerChart = new OpenSequenceGraph("Power Chart",this);
		currentTemperatureChart = new OpenSequenceGraph("Temperature Chart",this);

		// Register Displays
		registerDisplaySurface("Simulation Window 1", displaySurf);
		this.registerMediaProducer("Plot", currentPowerChart);	
		this.registerMediaProducer("Plot", currentTemperatureChart);	
	}

	@Override
	public void begin(){
		buildModel();
		buildSchedule();
		buildDisplay();
	}


	/**
	 * build model - add devices
	 */
	public void buildModel() {
		System.out.println("Running BuildModel");

		// add PV devices
		for (int i=0; i<numPVDevices; i++) {
			PowerDevice d = new PVDevice(15);
			devices.add(d);
			powerDevices.add(d);
		}

		// add virtual temp device
		tempDevice = new TempDevice();
		devices.add(tempDevice);

		// add building device with a chiller and pump
		chillerDevice = new ChillerDevice();
		devices.add(chillerDevice);
		powerDevices.add(chillerDevice);

		pumpDevice = new PumpDevice();
		devices.add(pumpDevice);
		powerDevices.add(pumpDevice);

		buildingDevice = new BuildingDevice(tempDevice, chillerDevice, pumpDevice);
		devices.add(buildingDevice);
		powerDevices.add(buildingDevice);
	}

	public void buildSchedule() {
		System.out.println("Running BuildSchedule");

		// build schedule for simulation steps

		/**
		 * class to implement simulation step
		 * @author shahar
		 *
		 */
		class MicroGridStep extends BasicAction {
			/**
			 * execute one simulation step <br>
			 * loop for all devices, and call their step() funciton
			 */
			@Override
			public void execute() {
				// check for end of simulation
				double ts = schedule.getCurrentTime();
				System.out.println("execute ts="+ts);
				if (ts >= 24 * 60 - 1) {
					schedule.removeAction(this);
					return;
				}

				// go on....
				SimUtilities.shuffle(devices);
				for (Device device : devices)
					device.step(ts/60.);
			}
		}

		schedule.scheduleActionBeginning(0, new MicroGridStep());

		// build schedule for charting

		class MicroGridPowerChartStep extends BasicAction {
			@Override
			public void execute(){
				// check for end of simulation
				double ts = schedule.getCurrentTime();
				if (ts >= 24 * 60 - 1) {
					schedule.removeAction(this);
					return;
				}

				currentPowerChart.step();
				currentTemperatureChart.step();
			}
		}

		schedule.scheduleActionAtInterval(30, new MicroGridPowerChartStep());

		/**
		 * class to implement DR event
		 * @author shahar
		 *
		 */
		class DemandResponseEvent extends BasicAction {

			class DREvent {
				public double powerDemand;
				public double period;
				DREvent(double d, double p) { powerDemand=d; period=p; }

				/** negotiate the response to DR event with a power device 
				 * @return true if a bid was received and awarded*/
				boolean negotiate(Negotiator n) {
					double ts = schedule.getCurrentTime();
					Bid bid = n.negotiate(new Announce(RequestType.SHORTAGE, powerDemand, period, 0), ts/60.);
					if (null==bid) return false; // no bid for the announcement

					n.negotiate(new Award(bid.amount, bid.time, bid.minPrice), ts/60.);
					return true;
				}

				/** execute a DR event with given array of power devices */
				public void execute(List<PowerDevice> powerDevices) {
					for (PowerDevice device : powerDevices)
						negotiate(device);
				}
			}

			/**
			 * send DR event
			 */
			@Override
			public void execute() {

				DREvent drEvent = new DREvent(20,360/60.); // DR event of 20KW for 360 mins 

				SimUtilities.shuffle(powerDevices);
				drEvent.execute(powerDevices);
			}
		}

		schedule.scheduleActionAt(800, new DemandResponseEvent());
	}

	public void buildDisplay() {
		System.out.println("Running BuildDisplay");

		for (PowerDevice d : powerDevices)
			currentPowerChart.addSequence(d.getName(), new ChartData(d));
		currentPowerChart.addSequence("Total", new ChartData(this));

		currentTemperatureChart.addSequence("Outside", new ChartData(tempDevice));
		currentTemperatureChart.addSequence("Building", new ChartData(new BuildingWrapperForTemperature(buildingDevice))); // use wrapper class to extract building internal temperature

		displaySurf.display();
		currentTemperatureChart.setSize(400,400);
		currentTemperatureChart.setLocation(100, 100);
		currentTemperatureChart.display();

		currentPowerChart.setSize(500,500);
		currentPowerChart.setLocation(550, 100);
		currentPowerChart.display();
	}

	@Override
	public Schedule getSchedule(){
		return schedule;
	}

	@Override
	public String[] getInitParam(){
		String[] initParams = { "NumPVDevices" };
		return initParams;
	}

	public int getNumPVDevices(){
		return numPVDevices;
	}

	public void setNumPVDevices(int na){
		numPVDevices = na;
	}

	public static void main(String[] args) {
		SimInit init = new SimInit();
		MicroGridModel model = new MicroGridModel();
		init.loadModel(model, "", false);
	}

	// Chartable imterface implementation
	public double getDataForChart(double time)
	{
		double power = 0;
		for (PowerDevice d : powerDevices)
			power += d.getDataForChart(time);

		return power;
	}

	// tool for charting

	class ChartData implements DataSource, Sequence {

		private Chartable chartable;

		ChartData(Chartable c) {chartable = c;}

		@Override
		public Object execute() {
			return new Double(getSValue());
		}

		@Override
		public double getSValue() {
			double ts = schedule.getCurrentTime();
			return chartable.getDataForChart(ts/60.);
		}
	}

}
