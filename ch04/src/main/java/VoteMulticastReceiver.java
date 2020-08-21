import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class VoteMulticastReceiver {
    private static final String GATEWAY = "224.0.0.50"; // 网关 UDP 组播地址
    private static int PORT = 9898;


    public static void main(String[] args) throws IOException {

        InetAddress address = InetAddress.getByName(GATEWAY); // Multicast address
        if (!address.isMulticastAddress()) { // Test if multicast address
            throw new IllegalArgumentException("Not a multicast address");
        }

        MulticastSocket sock = new MulticastSocket(PORT); // for receiving
        sock.joinGroup(address); // Join the multicast group

//        String hostAddress = address.getHostAddress();
//        System.out.println(hostAddress);

        // Receive a datagram
        DatagramPacket packet = new DatagramPacket(new byte[136],
            136);
        sock.receive(packet);

        System.out.println("Received Text-Encoded Request (" + packet.getLength()
            + " bytes): ");
        String content = new String(packet.getData());
        System.out.println(content);
        sock.close();
    }
}
