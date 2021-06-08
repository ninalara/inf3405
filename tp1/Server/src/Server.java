import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

public class Server {
	private static ServerSocket serverSocket;
	private static String serverPath = System.getProperty("user.dir") + "\\";

	public static void main(String[] args) throws Exception {
		// Variables constantes - coordonn�es du serveur
		String serverAddress = "127.0.0.1";
		int serverPort = 5000;
		int clientNumber = 0;

		Scanner inputScanner = new Scanner(System.in);

		// Input et validation de l'adresse IP
		System.out.println("-- Server side --");
		System.out.println("-- Enter the server's IP address:");
		serverAddress = inputScanner.nextLine();
		while (!Server.validateIpAddress(serverAddress)) {
			System.out.println("-- Wrong IP Address. Try again. --");
			serverAddress = System.console().readLine();
		}

		// Input et validation du port
		System.out.println("Enter the server's port:");
		serverPort = inputScanner.nextInt();
		while (!Server.validatePort(serverPort)) {
			System.out.println("-- Invalid port: Should be between 5000 and 5050. Try again. --");
			serverPort = Integer.parseInt(System.console().readLine());
		}
		InetAddress serverIP = InetAddress.getByName(serverAddress);

		// Cr�ation du socket
		serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(new InetSocketAddress(serverIP, serverPort));

		try {
			while (true) {
				new ClientHandler(serverSocket.accept(), clientNumber++).start();
			}

		} finally {
			serverSocket.close();
			inputScanner.close();
		}
	}

	private static class ClientHandler extends Thread {
		private Socket socket;
		private int clientNumber = 1;
		private String path = new String(serverPath);

		public ClientHandler(Socket socket, int clientNumber) {
			this.socket = socket;
			this.clientNumber = clientNumber;
			System.out.println("-- New connection with client#" + clientNumber + " at " + socket + " --");
		}

		public void run() {
			try {
				DataInputStream dataInput = new DataInputStream(socket.getInputStream());
				DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());

				dataOutput.writeUTF("-- Hello from server, client #" + clientNumber + " --");
				commandSelector(dataInput, dataOutput);
				clientNumber++;
			} catch (Exception e) {
				System.out.println("-- Error handling client#" + clientNumber + ": " + e + " --");
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println("-- Unable to close a socket. --");
				}
				System.out.println("-- Connection with client#" + clientNumber + " is closed --");
			}

		}

		public void commandSelector(DataInputStream dataInput, DataOutputStream dataOutput) throws Exception {
			// Date et temps pour l'affichage
			LocalDate date = java.time.LocalDate.now();
			int hours = java.time.LocalTime.now().getHour(), minutes = java.time.LocalTime.now().getMinute(),
					seconds = java.time.LocalTime.now().getSecond(), commandInput = 0;

			// Variables constantes - coordonn�es du serveur
			String serverAddress = "127.0.0.1";
			int serverPort = 5000;

			String input = "";
			String[] clientInputs = new String[] {};

			while (commandInput == 0) {
				try {
					input = dataInput.readUTF();
					clientInputs = input.split(" ");

					System.out.println("[" + serverAddress + ":" + serverPort + " - " + date + "@" + hours + ":" + minutes + ":" + seconds + "] : " + input);
					} catch (Exception e) {}

				// Si l'usager n'a rien �crit
				if (clientInputs.length == 0)
					continue;

				// Les diff�rentes commandes que l'usager peut �crire
				switch (clientInputs[0]) {
				case "cd":
					changeDirectory(dataOutput, clientInputs);
					break;
				case "ls":
					listFilesAndDirectories(dataOutput, false);
					break;
				case "mkdir":
					createNewDirectory(dataOutput, clientInputs);
					break;
				case "upload":
					uploadFile(dataOutput, clientInputs);
					break;
				case "download":
					downloadFile(dataOutput, clientInputs);
					break;
				case "exit":
					System.exit(0);
					break;
				default:
					dataOutput.writeUTF("Command not found, please try again.\n");
					break;
				}

				// Remise � z�ro des valeurs initiales
				input = "";
				clientInputs = new String[] {};
			}
		}

		public void changeDirectory(DataOutputStream dataOutput, String[] clientInputs) throws Exception {
			if (clientInputs.length == 1) {
				dataOutput.writeUTF("-- Please enter a directory name: \n");
				return;
			}

			if (clientInputs[1].equals("..")) {
				String[] splitPath = path.split("\\\\");
				String newPath = "";

				for (int i = 0; i < splitPath.length - 1; i++) {
					newPath += splitPath[i] + "\\";
				}
				
				path = newPath;
				System.out.println("HEAD on " + splitPath[splitPath.length - 2]);
				
			} else if (!listFilesAndDirectories(dataOutput, true).contains(clientInputs[1])) {
				dataOutput.writeUTF("No directory with that name was found\n");
				return;
			} else {
				path += clientInputs[1] + "\\";
			}
			dataOutput.writeUTF("New current directory: " + path);
		}

		// https://stackoverflow.com/questions/5694385/getting-the-filenames-of-all-files-in-a-folder
		public List<String> listFilesAndDirectories(DataOutputStream dataOutput, boolean isDirectory) throws Exception {
			File currentFolder = new File(path);
			File[] listOfCurrentFiles = currentFolder.listFiles();
			List<String> listOfDirectories = new ArrayList<String>();
			
			for (int i = 0; i < listOfCurrentFiles.length; i++) {
				if (isDirectory && listOfCurrentFiles[i].isDirectory()) {
					listOfDirectories.add(listOfCurrentFiles[i].getName());
				} else {
					dataOutput.writeUTF(listOfCurrentFiles[i].getName());
				}
			}
			return listOfDirectories;
		}

		public void createNewDirectory(DataOutputStream dataOutput, String[] clientInputs) throws Exception {
			if (clientInputs.length == 1) {
				dataOutput.writeUTF("-- Please enter a directory name --\n");
				return;
			}

			File fileDirectory = new File(path + clientInputs[1]);
			boolean directoryCreated = fileDirectory.mkdir();

			if (directoryCreated) {
				dataOutput.writeUTF("-- Directory " + clientInputs[1] + " is created! --\n");
			} else {
				dataOutput.writeUTF("-- Error: no directory was created. Try again. --\n");
			}
		}

		// https://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets
		public void uploadFile(DataOutputStream dataOutput, String[] clientInputs) throws Exception {
			InputStream in = socket.getInputStream();
			FileOutputStream out = null;
			byte[] bytes = new byte[1024];
			int count;
			
			if (clientInputs.length == 1) {
				dataOutput.writeUTF("-- Please enter a valid file name --\n");
				in.close();
				return;
			}
			
			try {
				out = new FileOutputStream(path + clientInputs[1]);
			} catch (IOException e) {
				dataOutput.writeUTF("-- Error: file not found. Try again. --\n");
			}

			while((count = in.read(bytes)) >= 0) {
				out.write(bytes, 0, count);
			}
			
			dataOutput.writeUTF("-- File " + clientInputs[1] + " uploaded successfully! --");
			
			in.close();
			out.close();
		}

		// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket
		private void downloadFile(DataOutputStream dataOutput, String[] clientInputs) throws Exception {
			File file = new File(path + clientInputs[1]);
			FileInputStream in = null;
			BufferedInputStream buffin = new BufferedInputStream(in);
			byte[] bytes = new byte[(int) file.length()];
			
			if (clientInputs.length == 1) {
				dataOutput.writeUTF("-- Please enter a valid file name --\n");
				return;
			}
			
			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				dataOutput.writeUTF("-- Error: file not found. Try again. --");
			}
			
			try {
				buffin.read(bytes, 0, bytes.length);
				buffin.close();
				return;
			} catch (IOException e) {
				dataOutput.writeUTF("-- An error occured. Please try again. --");
			}
			
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