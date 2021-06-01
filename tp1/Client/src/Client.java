
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.regex.Pattern;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket socket;

    public static void main(String[] args) throws Exception {
    	Scanner in = new Scanner(System.in);
		String serverAddress = "127.0.0.1";
		int port = 5000;
		System.out.println("Enter IP Address of the Server:");
		serverAddress = in.nextLine();
		while (!Client.validateIpAddress(serverAddress)){
			System.out.println("Wrong IP Address. Enter another one:");
	        serverAddress = System.console().readLine();
	    }
			
		//Port
	    System.out.println("Enter Port for the server :");
	    port = in.nextInt();
	    while (!Client.validatePort(port)){
	        System.out.println("Wrong Port. Should be between 5000 and 5050. Enter another one:");
	        port = Integer.parseInt(System.console().readLine());
	    }
		
		socket = new Socket(serverAddress, port);
		
		System.out.format("The server is running on %s:%d%n", serverAddress, port);
		
		DataInputStream input = new DataInputStream(socket.getInputStream());
		DataOutputStream output = new DataOutputStream(socket.getOutputStream());

		String helloMessageFromServer = input.readUTF();
		System.out.println(helloMessageFromServer);
	    
    }
    
	private static final Pattern PATTERN = Pattern.compile(
			"^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
	);
    
	public static boolean validatePort(final int port) {
		return port >= 5000 && port <= 5500;
	}

	public static boolean validateIpAddress(final String ipAdress) {
		return PATTERN.matcher(ipAdress).matches();
	}
}
