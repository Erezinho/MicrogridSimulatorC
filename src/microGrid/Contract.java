package microGrid;

/**
 * a contract object used to negotiate by CNP
 * @author shahar
 * @see http://en.wikipedia.org/wiki/Contract_Net_Protocol
 *
 */
public class Contract {

	public enum RequestType { SHORTAGE, SURPLUS } ;
	public Device originator;

	Contract() { originator=null; }
	Contract(Device orig) { originator=orig; }

};

class Announce extends Contract {
	public RequestType type;
	public double amount;
	public double time;
	public double price;

	Announce(RequestType t, double a, double tm, double p) { type=t; amount=a; time=tm; price=p; }
}

class Bid extends Contract {
	public double amount;
	public double time;
	public double minPrice; // the minimal price the device can provide, might be lower that announce price

	Bid(double a, double tm, double m) { amount=a; time=tm; minPrice=m; }
}

class Award extends Contract {
	public double amount;
	public double time;
	public double price;

	Award(double a, double tm, double p) { amount=a; time=tm; price=p; }
}


interface Negotiator {
	Bid negotiate(Announce a, double time); // call with announcement, receive a bid 
	void negotiate(Award a, double time); // award a bid
}