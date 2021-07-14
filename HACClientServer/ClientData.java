import java.io.Serializable;
import java.net.InetAddress;

/**
 * The class modeling the client data stored in a packet
 * @author Sam Dodson
 *
 */
class ClientData implements Serializable {
	private InetAddress address;
	private int port;
	private int availability;
	private int idNumber;
	private boolean status;
	
	/**
	 * Constructor for PeerData class
	 * @param address the address of a client node
	 * @param port the port number of a client node
	 * @param availability the availability of a client node
	 * @param idNumber the idNumber of a client node
	 */
	public ClientData(InetAddress address, int port, int availability, int idNumber) {
		this.address = address;
		this.port = port;
		this.availability = availability;
		this.idNumber = idNumber;
		this.status = true;
	}
	
	/**
	 * Getter for IP address
	 * @return IP address of client node
	 */
	public InetAddress getAddress() {
		return this.address;
	}
	
	/**
	 * Getter for port number
	 * @return port number of client node
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * Getter for availability
	 * @return availability of client node
	 */
	public int getAvailability() {
		return this.availability;
	}
	
	/**
	 * Getter for ID number
	 * @return ID number of client node
	 */
	public int getId() {
		return this.idNumber;
	}
	
	/**
	 * Getter for status
	 * @return status of client node
	 */
	public boolean getStatus() {
		return this.status;
	}
	
	/**
	 * Setter for address
	 * @param value to set address to 
	 */
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	/**
	 * Setter for port
	 * @param port value to set port to
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Setter for availability
	 * @param availability value to set availability to
	 */
	public void setAvailability(int availability) {
		this.availability = availability;
	}
	
	/**
	 * Setter for ID
	 * @param id value to set ID to
	 */
	public void setId(int id) {
		this.idNumber = id;
	}
	
	/**
	 * Setter for status
	 * @param status value to set status to
	 */
	public void setStatus(boolean status) {
		this.status = status;
	}
	
    /**
     * Determines equality of two clients based on ID number
     */
	@Override
    public boolean equals(Object o) { 
    	ClientData client = (ClientData) o;
    	if(client.idNumber == this.idNumber) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
}
