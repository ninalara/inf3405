
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;

public class Server {
	private static ServerSocket listener;
	private static String serverAddress = "127.0.0.1";
	private static int port = 5000;
	private static String serverPath = System.getProperty("user.dir") + "\\";

	public static void main(String[] args) throws Exception {
		Scanner in = new Scanner(System.in);
		System.out.println("Enter IP Address of the Server:");
		serverAddress = in.nextLine();
		while (!Server.validateIpAddress(serverAddress)) {
			System.out.println("Wrong IP Address. Enter another one:");
			serverAddress = System.console().readLine();
		}

		// Port
		System.out.println("Enter Port for the server :");
		port = in.nextInt();
		while (!Server.validatePort(port)) {
			System.out.println("Wrong Port. Should be between 5000 and 5050. Enter another one:");
			port = Integer.parseInt(System.console().readLine());
		}
		InetAddress serverIP = InetAddress.getByName(serverAddress);

		listener = new ServerSocket();
		listener.setReuseAddress(true);
		listener.bind(new InetSocketAddress(serverIP, port));

		try {
			int nClients = -1;
			while (true) {
				new ClientHandler(listener.accept(), nClients++).start();
			}

		} finally {
			listener.close();
		}

	}

//    https://stackoverflow.com/Questions/5667371/validate-ipv4-address-in-java
	private static final Pattern PATTERN = Pattern
			.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	public static boolean validatePort(final int port) {
		return port >= 5000 && port <= 5500;
	}

	public static boolean validateIpAddress(final String ipAdress) {
		return PATTERN.matcher(ipAdress).matches();
	}

	private static class ClientHandler extends Thread {
		private Socket socket;
		private int clientNumber;
		private String path = new String(serverPath);

		public ClientHandler(Socket socket, int clientNumber) {
			this.socket = socket;
			this.clientNumber = clientNumber;
			System.out.println("New connection with client#" + clientNumber + " at " + socket);
		}

		public void run() {
			try {
				DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				out.writeUTF("Hello from server - you are client#" + clientNumber);
				commandSelector(in, out);
			} catch (Exception e) {
				System.out.println("Error handling client#" + clientNumber + ": " + e);
			}
		}

		public void commandSelector(DataInputStream in, DataOutputStream out) throws Exception {
			int command = 0;
			String line = "";
			String[] inputs = new String[] {};
			// wait for the client to enter a command
			while (command == 0) {
				
				if (line == "") {
					try {
						// read the command that the client entered and split it
						line = in.readUTF();
						inputs = line.split(" ");

						// write in the server console everything needed
						System.out.println("[" + serverAddress + ":" + port + " - " + java.time.LocalDate.now() + "@"
								+ java.time.LocalTime.now().getHour() + ":" + java.time.LocalTime.now().getMinute()
								+ ":" + java.time.LocalTime.now().getSecond() + "] : " + line);
					} catch (Exception e) {
					}
				}

				if (inputs.length == 0)
					continue;
				// switch case with the different commands available
				// using the split line that the users entered
				switch (inputs[0]) {
				case "cd":
					cdCommand(out, inputs);
					break;
				case "ls":
					lsCommand(out, inputs, false);
					out.writeUTF("\n");
					break;
				case "mkdir":
					mkdirCommand(out, inputs);
					break;
				case "upload":
					uploadCommand(out, inputs);
					break;
				case "download":
					downloadCommand(out, inputs);
					break;
				case "exit":
					System.exit(0);
					break;
				case "help":
					helpCommand(out);
					break;
				default:
					out.writeUTF("Command not found, type help for help\n");
					break;
				}
				// reset the values
				line = "";
				inputs = new String[] {};
			}
		}

		public void cdCommand(DataOutputStream out, String[] inputs) throws Exception {
			// verifies that the user has entered a name of directory
			if (inputs.length == 1) {
				out.writeUTF("No directory name was typed\n");
				return;
			} else if (inputs[1].equals("..")) {
				// splits the path where there are "\" in the path if the second argument is
				// ".."
				String[] splitPath = path.split("\\\\");
				String newPath = "";
				// create a new path with all the split elements except for the last one
				for (int i = 0; i < splitPath.length - 1; i++) {
					newPath += splitPath[i] + "\\";
				}
				path = newPath;
				System.out.println("HEAD on " + splitPath[splitPath.length - 2]);
			} else if (!lsCommand(out, inputs, true).contains(inputs[1])) {
				// error message if the directory is not found
				out.writeUTF("No directory with that name was found\n");
				return;
			} else {
				// sets the new path if there is no problem
				path += inputs[1] + "\\";
				System.out.println("HEAD on " + inputs[1]);
			}
			out.writeUTF("The current directory is now " + path);
		}

		public void mkdirCommand(DataOutputStream out, String[] inputs) throws Exception {
			// verifies that the client entered the name of the new directory
			if (inputs.length == 1) {
				out.writeUTF("No directory name was typed\n");
				return;
			}

			// Creates the new file with the right path
			File file = new File(path + inputs[1]);
			// Created the directory
			if (file.mkdir()) {
				// Success message
				out.writeUTF("Directory created\n");
			} else {
				// Failure message
				out.writeUTF("An Error has Occurred\n");
			}
		}

		// From
		// https://stackoverflow.com/questions/5694385/getting-the-filenames-of-all-files-in-a-folder#:~:text=Create%20a%20File%20object%2C%20passing,method%20to%20get%20the%20filename.
		public List<String> lsCommand(DataOutputStream out, String[] inputs, boolean isCd) throws Exception {
			File currentFolder = new File(path);
			// get all the files
			File[] listOfFiles = currentFolder.listFiles();
			List<String> directories = new ArrayList<String>();
			// loop through all the files
			for (int i = 0; i < listOfFiles.length; i++) {
				// JCOMPRENDS PAS CA
				if (isCd) {
					if (listOfFiles[i].isDirectory())
						directories.add(listOfFiles[i].getName());
				} else if (listOfFiles[i].isFile()) {
					// if it is a file, it prints as a file
					out.writeUTF("File " + listOfFiles[i].getName());
				} else if (listOfFiles[i].isDirectory()) {
					// if it is a directory, it prints as a directory
					out.writeUTF("Directory " + listOfFiles[i].getName());
				}
			}
			return directories;
		}

		public void uploadCommand(DataOutputStream out, String[] inputs) throws Exception {
			// get the input stream form the socket
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			// create the output stream of the file data
			FileOutputStream fos = new FileOutputStream(path + inputs[1]);
			byte[] buffer = new byte[4096];
			long fileSize = dis.readLong();
			int read = 0;
			// while there are bytes in the dis, we read them and write them in the fos
			while (fileSize > 0 && (read = dis.read(buffer)) > 0) {
				fos.write(buffer, 0, read);
				fileSize -= read;
			}
			// close the the dis and the fos
			dis.close();
			fos.close();
		}

		private void downloadCommand(DataOutputStream out, String[] inputs) throws Exception {
			// verifies that the user entered the name of the file to download
			if (inputs.length == 1) {
				out.writeUTF("No file name was typed\n");
				return;
			} else {
				String fileName = inputs[1];
				String filePath = path + fileName;
				File file = new File(filePath);
				// verifies that the user entered an existing file
				if (!(file.isFile())) {
					out.writeUTF(fileName + " does not exist\n");
					return;
				} else {
					byte[] buff = Files.readAllBytes(file.toPath());
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					// FileInputStream input = new FileInputStream(file.toString());

					// int fileDataSize = input.read();
					System.out.print(buff.length);

					output.writeObject(buff);
//	    		while(fileDataSize > 0 && (read = input.read(buff)) > 0) {
//	    			output.write(buff, 0, read);
//	    			fileDataSize -= read;
//	    		}

//	    		int count ;
//	            while ((count = input.read(buff)) >= 0) {
//	                out.write(buff, 0, count);
//	                System.out.write(buff, 0, count);
//	                
//	            }

//	    		input.close();
					output.close();
//	    		System.out.println(fileName + " downloaded successfully.");	
				}
			}
		}

		// prints the different commands that are available
		private void helpCommand(DataOutputStream out) throws Exception {
			out.writeUTF(
					"ls : lists every files in current directory \ncd : change the current directory\nmkdir : create a new directory \ndownload : download a file from the Server to the Client \nupload : upload a file from the Client to the Server\n");
		}
	}
}
