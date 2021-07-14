/**
 * Driver class for protocol
 * @author Sam Dodson
 *
 */
public class ProtocolDriver {
	/**
	 * Runs the program logic in normal or local test mode
	 * @param args the IP address or "local" and port number
	 */
	public static void main(String[] args) {
		// Node is created and run in normal mode
		if(args.length == 1) {
			Node node;
			node = new Node(args[0]);
			node.getPeer().listen();
		}
		else if(args.length == 2) {
			// Node is created and run in local test mode
			try {
				Node node = new Node(Integer.parseInt(args[1]));
				node.getPeer().listen();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
	}

}
