import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class Server {
	private static ServerSocket serverSocket;
	private static String serverPath = System.getProperty("user.dir") + "\\";

	public static void main(String[] args) throws Exception {
		// Variables constantes - coordonnées du serveur
		String serverAddress = "127.0.0.1";
		int serverPort = 5000;
		int clientNumber = 0;

		Scanner inputScanner = new Scanner(System.in);

		// Input et validation de l'adresse IP
		System.out.println("-- Enter the server's IP address:");
		serverAddress = inputScanner.nextLine();
		while (!Server.validateIpAddress(serverAddress)) {		// cas où l'adresse IP est incorrect
			System.out.println("-- Wrong IP Address. Try again. --");
			serverAddress = System.console().readLine();
		}

		// Input et validation du port
		System.out.println("Enter the server's port:");
		serverPort = inputScanner.nextInt();
		while (!Server.validatePort(serverPort)) {		// cas où le port est incorrect
			System.out.println("-- Invalid port: Should be between 5000 and 5050. Try again. --");
			serverPort = Integer.parseInt(System.console().readLine());
		}
		InetAddress serverIP = InetAddress.getByName(serverAddress);

		// Création du socket
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
		private int clientNumber;
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
			int hours = java.time.LocalTime.now().getHour(),
				minutes = java.time.LocalTime.now().getMinute(),
				seconds = java.time.LocalTime.now().getSecond(),
				commandInput = 0;
			
			// Variables constantes - coordonnées du serveur
			String serverAddress = "127.0.0.1";
			int serverPort = 5000;

			String input = "";
			String[] clientInputs = new String[] {};

			while (commandInput == 0) {
				try {
					input = dataInput.readUTF();
					clientInputs = input.split(" ");

					System.out.println(
						"[" + serverAddress + ":" + serverPort + " - " 
							+ date + "@" + hours + ":" + minutes + ":" + seconds + "] : " + input);
					} catch (Exception e) {
				}

				// Si l'usager n'a rien écrit
				if (clientInputs.length == 0)
					continue;

				// Les différentes commandes que l'usager peut écrire
				switch (clientInputs[0]) {
				case "cd":
					changeDirectory(dataOutput, clientInputs);
					break;
				case "ls":
					listFilesAndDirectories(dataOutput, false);
					dataOutput.writeUTF("\n");
					break;
				case "mkdir":
					createNewDirectory(dataOutput, clientInputs);
					break;
				case "upload":
					uploadFile(clientInputs);
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

				// Remise à zéro des valeurs initiales
				input = "";
				clientInputs = new String[] {};
			}
		}

		public void changeDirectory(DataOutputStream out, String[] clientInputs) throws Exception {
			if (clientInputs.length == 1) {
				out.writeUTF("-- Please enter a directory name\n");
				return;
			}

			// Si le client veut revenir au répertoire précédent
			if (clientInputs[1].equals("..")) {
				String[] splitPath = path.split("\\\\");
				String newPath = "";
				// create a new path with all the split elements except for the last one
				for (int i = 0; i < splitPath.length - 1; i++) {
					newPath += splitPath[i] + "\\";
				}
				path = newPath;
				System.out.println("HEAD on " + splitPath[splitPath.length - 2]);
			} else if (!listFilesAndDirectories(out, true).contains(clientInputs[1])) {
				// error message if the directory is not found
				out.writeUTF("No directory with that name was found\n");
				return;
			} else {
				// sets the new path if there is no problem
				path += clientInputs[1] + "\\";
				System.out.println("HEAD on " + clientInputs[1]);
			}
			out.writeUTF("The current directory is now " + path);
		}

		// https://stackoverflow.com/questions/5694385/getting-the-filenames-of-all-files-in-a-folder#:~:text=Create%20a%20File%20object%2C%20passing,method%20to%20get%20the%20filename.
		public List<String> listFilesAndDirectories(DataOutputStream dataOutput, boolean isDirectory) throws Exception {
			File currentFolder = new File(path);
			List<String> listOfDirectories = new ArrayList<String>();
			File[] listOfFiles = currentFolder.listFiles();
			
			for (int i = 0; i < listOfFiles.length; i++) {
				if (isDirectory && listOfFiles[i].isDirectory()) {
					listOfDirectories.add(listOfFiles[i].getName());
				} else if (listOfFiles[i].isDirectory()) {
					dataOutput.writeUTF(listOfFiles[i].getName());
				} else if (listOfFiles[i].isFile()) {
					dataOutput.writeUTF(listOfFiles[i].getName());
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
				dataOutput.writeUTF("-- Directory created! --\n");
			} else {
				dataOutput.writeUTF("-- An Error has occurred: no directory was created. --\n");
			}
		}

		public void uploadFile(String[] clientInputs) throws Exception {
			ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());
			FileOutputStream fileOutput = new FileOutputStream(path + clientInputs[1]);

			byte[] buffer = new byte[4096];
			long fileSize = objectInput.readLong();
			int read = objectInput.read(buffer);

			while (read > 0 && fileSize > 0) {
				fileOutput.write(buffer, 0, read);
				fileSize -= read;
			}

			objectInput.close();
			fileOutput.close();
		}

		private void downloadFile(DataOutputStream dataOutput, String[] clientInputs) throws Exception {
			String fileName = clientInputs[1];
			String filePath = path + fileName;
			File file = new File(filePath);

			if (clientInputs.length == 1) {
				dataOutput.writeUTF("-- Please enter a file name --\n");
				return;
			} 
			
			if (!(file.isFile())) {
				dataOutput.writeUTF("-- " + fileName + " does not exist! --\n");
				return;
			} else {
				byte[] buff = Files.readAllBytes(file.toPath());
				ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());

				System.out.print(buff.length);

				output.writeObject(buff);
				output.close();
			}
		}
	}

	// https://stackoverflow.com/Questions/5667371/validate-ipv4-address-in-java
	private static final Pattern PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	public static boolean validatePort(final int port) {
		return port >= 5000 && port <= 5500;
	}

	public static boolean validateIpAddress(final String ipAdress) {
		return PATTERN.matcher(ipAdress).matches();
	}
}
