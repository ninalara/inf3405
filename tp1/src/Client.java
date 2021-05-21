
import java.io.DataInputStream;
import java.net.Socket;

public class Client {
    private static Socket socket;

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }}
