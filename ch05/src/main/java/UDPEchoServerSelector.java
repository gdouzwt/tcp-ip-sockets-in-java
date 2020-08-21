import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Random;

public class UDPEchoServerSelector {

    private static final String MULTICAST_ADDRESS = "224.0.0.50";
    private static final int PORT = 9898;
    public static final String ON = "\\\"on\\\"";
    public static final String OFF = "\\\"off\\\"";
    public static final String GATEWAY_ADDRESS = "192.168.1.145";
    public static final String WRITE_PLUG = "{\"cmd\": \"write\",\"model\": \"plug\",\"sid\": \"158d000234727c\",\"short_id\": 38455,\"data\": \"{\\\"status\\\":";
    public static final String NEW_RGB_CMD_HEAD = "{\"cmd\" : \"write\", \"sid\":\"7811dcf981c4\",\"short_id\":0,\"data\":\"{\\\"rgb\\\":";
    public static final String KEY_JSON_ATTR = ",\\\"key\\\":\\\"";
    public static final String CMD_TRAILER = "\\\"}\"}";
    public static volatile String encryptedKey;
    public static final long value = 0xFFL << 24;
    public static boolean isOn = true;
    public static final byte[] initializationVector = {0x17, (byte) 0x99, 0x6d, 0x09, 0x3d, 0x28, (byte) 0xdd, (byte) 0xb3, (byte) 0xba, 0x69, 0x5a, 0x2e, 0x6f, 0x58, 0x56, 0x2e};
    public static final InetSocketAddress UNICAST = new InetSocketAddress(GATEWAY_ADDRESS, PORT);
    public static final SecretKey SECRET_KEY = new SecretKeySpec("07wjrkc41typdvae".getBytes(), 0, 16, "AES");

    public static void main(String[] args) throws Exception {

        NetworkInterface ni = NetworkInterface.getByName("eth7");  // ethernet
        InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS); // Multicast address
        if (!multicastAddress.isMulticastAddress()) { // Test if multicast address
            throw new IllegalArgumentException("Not a multicast address");
        }

        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
            .bind(new InetSocketAddress(PORT))
            .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);  // IPv4
        channel.join(multicastAddress, ni);  // !important
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ, new DataRecord());

        while (true) {
            Thread.sleep(3000);
            if (selector.select(5000) == 0) {
                System.out.println("Waiting for heartbeat sync...");
                continue;
            }

            for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isReadable()) {
                    DatagramChannel selectedChannel = (DatagramChannel) selectionKey.channel();
                    DataRecord dataRecord = (DataRecord) selectionKey.attachment();
                    dataRecord.buffer.clear();
                    dataRecord.address = selectedChannel.receive(dataRecord.buffer);
                    if (dataRecord.address != null) {
                        printBufferData(dataRecord.buffer);
                        if (encryptedKey != null) {
                            selectionKey.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                }
                if (selectionKey.isValid() && selectionKey.isWritable()) {
                    if (encryptedKey == null) {
                        selectionKey.interestOps(SelectionKey.OP_READ);
                        break;
                    } else {
                        updateRGB((DatagramChannel) selectionKey.channel());
//                        togglePlug((DatagramChannel) selectionKey.channel());
                        selectionKey.interestOps(SelectionKey.OP_READ);
                    }
                }
                iterator.remove();
            }
        }
    }

    private static void updateRGB(DatagramChannel channel) throws IOException {

        Random random = new Random(System.currentTimeMillis());

        String hex;
        long[] color = new long[3];
        for (int i = 0; i < color.length; i++) {
            color[i] = random.nextInt(256);
            hex = Long.toHexString(color[i]);
            if (hex.length() == 1) {
                hex = 0 + hex;
            }
            System.out.print(hex);
        }
        System.out.println();

        long temp;
        temp = color[0] << 16;
        temp = temp | color[1] << 8;
        temp = temp | color[2];
        System.out.println(Long.toHexString(temp));

        int[] musicId = {0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29};
        int i = random.nextInt(musicId.length);

        String writeRGBData = NEW_RGB_CMD_HEAD + (value | temp) + KEY_JSON_ATTR + encryptedKey + CMD_TRAILER;
        String writeMusic = "{\"cmd\":\"write\",\"model\":\"gateway\",\"sid\":\"7811dcf981c4\",\"short_id\":0,\"data\":\"{\\\"mid\\\":" + musicId[i] + KEY_JSON_ATTR + encryptedKey + CMD_TRAILER;
        ByteBuffer to = ByteBuffer.wrap(writeMusic.getBytes());
        channel.send(to, UNICAST);
        System.out.println(writeMusic);
    }

    private static void togglePlug(DatagramChannel channel) throws IOException {
        isOn = !isOn;
        String writePlugData = WRITE_PLUG + (isOn ? OFF : ON) + KEY_JSON_ATTR + encryptedKey + CMD_TRAILER;
        ByteBuffer to = ByteBuffer.wrap(writePlugData.getBytes());
        System.out.println(writePlugData);
        channel.send(to, UNICAST);
    }

    private static synchronized void printBufferData(Buffer input) throws Exception {

        input.flip();
        byte[] content = new byte[input.limit()];
        while (input.hasRemaining()) {
            content[input.position()] = ((ByteBuffer) input).get();
        }
        String stringContent = new String(content);

        // find token and encrypt it
        if (stringContent.contains("gateway") && stringContent.contains("heartbeat")) {
            int token = stringContent.indexOf("token");
            token += 8;
            String tokenString = stringContent.substring(token, token + 16);
            byte[] cipher = tokenAESCrypto(tokenString);
            encryptedKey = DatatypeConverter.printHexBinary(cipher);
            System.out.println("************************************************************************ KEY UPDATED ******************************************************************");
        }
        System.out.println(stringContent);
        if (stringContent.contains("Invalid key")) {
            encryptedKey = null;
        }
    }

    static class DataRecord {
        public SocketAddress address;
        public ByteBuffer buffer = ByteBuffer.allocate(400);
    }

    private static byte[] tokenAESCrypto(String token) throws Exception {
        return SymmetricEncryptionUtils.performAESEncryption(token, SECRET_KEY, initializationVector);
    }
}
