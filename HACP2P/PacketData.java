import java.io.Serializable;
import java.util.BitSet;

/**
 * The class representing the packet being sent between cluster nodes
 * @author Sam Dodson
 *
 */
public class PacketData implements Serializable {
	private byte version;
	private byte mode;
	private byte code;
	private BitSet flags = new BitSet(8);
	private PeerData peerData;
	
	/**
	 * Constructor for the PacketData class that initializes instance data values
	 * @param version the version of the protocol the packet is being sent in
	 * @param mode the mode the cluster is running in
	 * @param code the type of packet being created
	 * @param peerData the data availability data associated with this peer
	 */
	public PacketData(byte version, byte mode, byte code, PeerData peerData) {
		this.version = version;
		this.mode = mode;
		this.code = code;
		this.peerData = peerData;
	}
		
	/**
	 * Getter for the version field
	 * @return the version of the packet
	 */
	public byte getVersion() {
		return this.version;
	}
	
	/**
	 * Getter for the mode field
	 * @return the mode of the packet
	 */
	public byte getMode() {
		return this.mode;
	}
	
	/**
	 * Getter for the code field
	 * @return the code of the packet
	 */
	public byte getCode() {
		return this.code;
	}
	
	/**
	 * Getter for the data field 
	 * @return the peer data of the packet
	 */
	public PeerData getPeer() {
		return this.peerData;
	}
	
	/**
	 * Getter for the flags field
	 * @return the flags of the packet
	 */
	public BitSet getFlags() {
		return this.flags;
	}
	
	/**
	 * Setter for the flags field
	 * @param canBeSplit flag specifying if a packet's data will require it to be split into multiple packets
	 * @param lastPacket flag specifying if a packet is the last one to be sent
	 */
	public void setFlags(boolean canBeSplit, boolean lastPacket) {
		flags.set(6, canBeSplit);
		flags.set(7, lastPacket);
	}
}
