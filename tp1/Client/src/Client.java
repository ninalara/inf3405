import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.regex.Pattern;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {
	private static String clientPath = System.getProperty("user.dir") + "\\";
    private static Socket socket;

    public static void main(String[] args) throws Exception {
		// Variables constantes - coordonnées du serveur
		String serverAddress = "127.0.0.1";
		int serverPort = 5000;

    	Scanner inputScanner = new Scanner(System.in);

		// Input et validation de l'adresse IP
		System.out.println("-- Enter the server's IP address:");
		serverAddress = inputScanner.nextLine();
		while (!Client.validateIpAddress(serverAddress)) {		// cas où l'adresse IP est incorrect
			System.out.println("-- Wrong IP Address. Try again. --");
	        serverAddress = System.console().readLine();
	    }

		// Input et validation du port
	    System.out.println("-- Enter the server's port:");
	    serverPort = inputScanner.nextInt();
	    while (!Client.validatePort(serverPort)) {		// cas où le port est incorrect
	        System.out.println("-- Invalid port: Should be between 5000 and 5050. Try again. --\n");
	        serverPort = Integer.parseInt(System.console().readLine());
	    }
		
		socket = new Socket(serverAddress, serverPort);
		System.out.format("-- Server is running on %s:%d%n --", serverAddress, serverPort);
		
		// Envoi et réception de données
		DataInputStream dataInput = new DataInputStream(socket.getInputStream());
		DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
		System.out.println(dataInput.readUTF());
		
		while (true) {
			String command = inputScanner.nextLine();

			if (command == "") continue;
			dataOutput.writeUTF(command);
			commandCenter(command, dataInput, dataOutput);

			TimeUnit.MILLISECONDS.sleep(100);
			inputScanner.close();
			
			while(dataInput.available() != 0)
			{
				String serverComs = dataInput.readUTF();
				if (serverComs.isEmpty()) break;
				System.out.println(serverComs);
			}
		}
    }
    
	public static void commandCenter(String command, DataInputStream dataInput, DataOutputStream dataOutput) throws Exception {
		String[] clientInputs = new String[] {};

		try { clientInputs = command.split(" "); } catch(Exception e) {}
		if (clientInputs.length == 0) return;
			
		switch(clientInputs[0]) {
			case "upload":
				uploadFile(dataOutput, clientInputs[1]);
				break;
			case "download":
				downloadFile(dataInput, clientInputs[1]);
				break;
			case "exit":
				System.exit(0);
				break;
			default:
				break;
		}
	}
    
    private static void uploadFile(DataOutputStream dataOutput, String fileName) throws IOException {
		File file = new File(clientPath + "\\" + fileName);
		FileInputStream fileInput = new FileInputStream(file.toString());
    	System.out.print(file.toString());

		byte[] buffer = new byte[16*2024];
		int fileSize = fileInput.read();
		int read = fileInput.read(buffer);

		// Si le fichier n'est pas un fichier
    	if(!(file.isFile())) {
			return;
		}
		
		while(read > 0 && fileSize > 0) {
			dataOutput.write(buffer, 0, read);
			fileSize -= read;
		}

		fileInput.close();
    }
    
    private static void downloadFile(DataInputStream dataInput, String fileName) throws Exception {
		ObjectInputStream objectOutput = new ObjectInputStream(socket.getInputStream());
		FileOutputStream fileOutput = new FileOutputStream(clientPath + "\\" + fileName);		

		if(dataInput.readBoolean()) {
			System.out.print(objectOutput.available());
			byte [] buffer = (byte[]) objectOutput.readObject();
			File downloadedFile = new File(clientPath + "\\" + fileName);
			Files.write(downloadedFile.toPath(), buffer);
			System.out.print(objectOutput.readObject());
		} else {
			System.out.print("An error occured when downloading the file.");
		}

		objectOutput.close();
		fileOutput.close();
    }
	
	// https://stackoverflow.com/Questions/5667371/validate-ipv4-address-in-java
	private static final Pattern PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    
	public static boolean validateIpAddress(final String ipAdress) {
		return PATTERN.matcher(ipAdress).matches();
	}

	public static boolean validatePort(final int port) {
		return port >= 5000 && port <= 5500;
	}
}
