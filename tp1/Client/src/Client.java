
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
    private static Socket socket;
	private static String clientPath = System.getProperty("user.dir") + "\\";

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
		while (true) {
			System.out.print("> ");
			String currentCommand = "";
			currentCommand = in.nextLine();
			if (currentCommand == "") continue;
			output.writeUTF(currentCommand);
			commandCompletion(input, output, currentCommand);
			TimeUnit.MILLISECONDS.sleep(100);
			while(input.available() != 0)
			{
				String serverComs = input.readUTF();
				if (serverComs.isEmpty()) break;
				System.out.println(serverComs);
			}
		}
    }
    
	public static void commandCompletion(DataInputStream in, DataOutputStream out, String currentCommand) throws Exception {
		String[] inputs = new String[] {};
		try { inputs = currentCommand.split(" "); } catch(Exception e) {}

		if (inputs.length == 0) return;
			
		switch(inputs[0]) {
			case "upload":
				uploadFile(out, inputs[1]);
				break;
			case "download":
				downloadFile(in, inputs[1]);
				break;
			case "exit":
				System.exit(0);
				break;
			default:
				break;
		}
	}
    
    private static void uploadFile(DataOutputStream out, String fileName) throws IOException {
   		
    	File file = new File(clientPath + "\\" + fileName);
    	System.out.print(file.toString());
    	if(!(file.isFile())) {
			return;
		}
		FileInputStream input = new FileInputStream(file.toString());
		byte[] buffer = new byte[16*2024];
		int fileDataSize = input.read();
		int read = 0;
		
		while(fileDataSize > 0 && (read = input.read(buffer)) > 0) {
			out.write(buffer, 0, read);
			fileDataSize -= read;
		}
		input.close();
    }
    
    // Download file from server from where server location to where client jar file is run.
    private static void downloadFile(DataInputStream in, String fileName) throws Exception {
    	
		FileOutputStream fos = new FileOutputStream(clientPath + "\\" + fileName);
		
		//byte[] buffer = new byte[16*2024];
		//byte[] buffer = in.readAllBytes();
		
		//long fileSize = in.readLong();
		//System.out.print(fileSize);
		//int read = 0;
		
		
		//ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(clientPath + "\\" + fileName));
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		
		//flush out
		
		if(in.readBoolean()) {
			System.out.print(ois.available());
			byte [] buffer = (byte[]) ois.readObject();
			File downloadedFile = new File(clientPath + "\\" + fileName);
			Files.write(downloadedFile.toPath(), buffer);
			System.out.print(ois.readObject());
		} else {
			System.out.print("hey ptit queue ca marche pas!!");
		}
		
		
		//fos.write(buffer);
		// While there is bytes in the in, we empty them in the file.
//		while(fileSize > 0 && (read = in.read(buffer)) > 0) {
//			fos.write(buffer, 0, read);
//			fileSize -= read;
//		}
		//ois.writeObject(buffer);
//		int count;
//        while ((count = in.read(buffer)) > 0) {
//            fos.write(buffer, 0, count);
//        }
		ois.close();
		fos.close();
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
