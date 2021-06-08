import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {
	private static String clientPath = System.getProperty("user.dir") + "\\";
    private static Socket socket;

    public static void main(String[] args) throws Exception {
		// Variables constantes - coordonn�es du serveur
		String serverAddress = "127.0.0.1";
		int serverPort = 5000;

    	Scanner inputScanner = new Scanner(System.in);

		// Input et validation de l'adresse IP
		System.out.println("-- Client side --");    	
		System.out.println("-- Enter the server's IP address:");
		serverAddress = inputScanner.nextLine();
		while (!Client.validateIpAddress(serverAddress)) {
			System.out.println("-- Wrong IP Address. Try again. --");
	        serverAddress = System.console().readLine();
	    }

		// Input et validation du port
	    System.out.println("-- Enter the server's port:");
	    serverPort = inputScanner.nextInt();
	    while (!Client.validatePort(serverPort)) {
	        System.out.println("-- Invalid port: Should be between 5000 and 5050. Try again. --\n");
	        serverPort = Integer.parseInt(System.console().readLine());
	    }
		
		socket = new Socket(serverAddress, serverPort);
		System.out.format("> Server is running on %s:%d%n", serverAddress, serverPort);
		
		// Envoi et r�ception de donn�es
		DataInputStream dataInput = new DataInputStream(socket.getInputStream());
		DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
		System.out.println(dataInput.readUTF());
		
		while (true) {
			System.out.print("$ ");
			String command = inputScanner.nextLine();

			if (command == "") continue;
			dataOutput.writeUTF(command);
			commandCenter(command, dataInput);

			TimeUnit.MILLISECONDS.sleep(100);
			
			while(dataInput.available() != 0)
			{
				String serverComs = dataInput.readUTF();
				if (serverComs.isEmpty()) break;
				System.out.println(serverComs);
			}
		}
    }
    
	public static void commandCenter(String command, DataInputStream dataInput) throws Exception {
		String[] clientInputs = new String[] {};
		clientInputs = command.split(" ");
		
		if(clientInputs.length != 0) {
			switch(clientInputs[0]) {
			case "upload":
				uploadFile(clientInputs[1]);
				break;
			case "download":
				downloadFile(clientInputs[1]);
				break;
			case "exit":
				System.exit(0);
				break;
			default:
				break;
			}			
		} else return;

	}
    
	// https://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets
    private static void uploadFile(String fileName) throws IOException {
    	File file = new File(clientPath + "\\" + fileName);
		FileInputStream in = null;
		OutputStream out = socket.getOutputStream();
		byte[] bytes = new byte[(int) file.length()];
		int count;
		
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("-- Error: file not found. Try again. --");
		}
		
		while((count = in.read(bytes)) > 0) {
			out.write(bytes, 0, count);
		}
		
		in.close();
		out.close();
    }
    
	// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket
    private static void downloadFile(String fileName) throws Exception {
    	byte[] bytesArray = new byte[1];
    	ByteArrayOutputStream bout = new ByteArrayOutputStream();
		FileOutputStream fout = null;
		BufferedOutputStream buffout = null;
		
		try {
			fout = new FileOutputStream(fileName);
			buffout = new BufferedOutputStream(fout);
			
			bout.write(bytesArray);
    		buffout.write(bout.toByteArray());
    		
    		System.out.println("-- File successfully downloaded! --");
    		buffout.flush();
    		buffout.close();
    		fout.flush();
    		fout.close();
		} catch(IOException e) {
			System.out.println("-- Error: an error occured. Please try 12again. --");
    	}
    }
	
	public static boolean validatePort(final int port) {
		return port >= 5000 && port <= 5050;
	}
    
	// https://stackoverflow.com/Questions/5667371/validate-ipv4-address-in-java
	public static boolean validateIpAddress(final String ipAdress) {
		Pattern PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
		return PATTERN.matcher(ipAdress).matches();
	}
}