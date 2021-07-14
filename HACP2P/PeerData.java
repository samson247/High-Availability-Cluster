import java.io.Serializable;
import java.net.InetAddress;

/**
 * The class modeling the peer data stored in a packet
 * @author Sam Dodson
 *
 */
class PeerData implements Serializable {
	private InetAddress address;
	private int port;
	int idNumber;
	private int availability;
	
	/**
	 * Constructor for PeerData class
	 * @param address the address of a peer node
	 * @param port the port number of a peer node
	 * @param idNumber the idNumber of a peer node
	 * @param availability the availability of a peer node
	 */
	public PeerData(InetAddress address, int port, int idNumber, int availability) {
		this.address = address;
		this.port = port;
		this.idNumber = idNumber;
		this.availability = availability;
	}
	
	/**
	 * Getter for IP address
	 * @return IP address for node
	 */
	public InetAddress getAddress() {
		return this.address;
	}
	
	/**
	 * Getter for port number
	 * @return port number of node
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * Getter for ID number of node
	 * @return ID number of node
	 */
	public int getId() {
		return this.idNumber;
	}
		
	/**
	 * Getter for availability of node
	 * @return availability of node
	 */
	public int getAvailability() {
		return this.availability;
	}
	
	/**
	 * Setter for availability
	 * @param availability the new availability score to be set
	 */
	public void setAvailability(int availability) {
		this.availability = availability;
	}
}
