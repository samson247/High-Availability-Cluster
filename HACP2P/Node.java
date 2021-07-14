/**
 * The class representing a peer node in the HAC protocol
 * @author Sam Dodson
 *
 */
public class Node {
	private Peer peer;
	
	/**
	 * Constructor to create a peer node 
	 * @param address the IP address of the peer node
	 */
	public Node(String address) {
		peer = new Peer(address);
	}
	
	/**
	 * Constructor for peer in local test mode
	 * @param port the port number a peer is running on
	 */
	public Node(int port) {
		peer = new Peer(port);
	}
			
	/**
	 * Getter for peer
	 * @return the peer associated with this node
	 */
	public Peer getPeer() {
		return this.peer;
	}
}