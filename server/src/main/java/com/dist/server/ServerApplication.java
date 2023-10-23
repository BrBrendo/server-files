package com.dist.server;

/**
 * Federal University of Mato Grosso
 * Computer Institute
 *
 * Course: Distributed Systems
 *
 * @author Prof. PhD. Luís Cézar Darienzo Alves
 * @date: 09/02/2023
 *
 * Code developed for teaching UDP and TCP Sockets. In this way, many errors and
 * error handling were ignored in order to keep the code simple.
 *
 * This software send text messages from App to TCPServer and UDPServer classes,
 * using TCP and UDP sockets, respectively.
 */

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ServerApplication{

	/*D:\ServerFiles*/
	private static final int PORT = 12345;

	private static final String FILE_STORAGE_PATH = "C:\\server/";

	public static void main(String[] args) {
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("Servidor em execução na porta " + PORT);

			File storageDir = new File(FILE_STORAGE_PATH);
			if (!storageDir.exists()) {
				storageDir.mkdirs();
			}

			while (true) {
				Socket clientSocket = serverSocket.accept();
				new ClientConnection(clientSocket).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class ClientConnection extends Thread {
		private final Socket clientSocket;

		public ClientConnection(Socket socket) {
			this.clientSocket = socket;
		}

		@Override
		public void run() {
			try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
				 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

				String request = in.readUTF();

				if (request.equals("UPLOAD")) {
					// Cliente envia um arquivo
					String fileName = in.readUTF();
					saveFile(fileName, in);
					System.out.println("Arquivo " + fileName + " foi recebido com sucesso no servidor.");
				} else if (request.equals("DOWNLOAD")) {
					// Cliente requisita um arquivo
					String fileName = in.readUTF();
					sendFile(fileName, out);
				}else if (request.equals("STORE_FILE_INFO")) {
					int numFiles = in.readInt(); // Lê o número de arquivos que serão enviados
					for (int i = 0; i < numFiles; i++) {
						String fileName = in.readUTF(); // Lê cada nome de arquivo da lista
						String ipAddress = clientSocket.getInetAddress().getHostAddress(); // Obtém o endereço IP do cliente
						int port = in.readShort(); // Obtém a porta do cliente
						storeFileInfo(fileName, ipAddress, port); // Chama o método para armazenar as informações do arquivo
					}
				}else if (request.equals("REQUEST_FILE_INFO")) {
				// Lógica para armazenar informações sobre arquivos
					String fileName = "fileData.xml";
					sendFile(fileName, out);
			}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		private void storeFileInfo(String fileName, String ipAddress, int port) {
			try {
				File file = new File(FILE_STORAGE_PATH + "fileData.xml");
				Element root;
				Document doc;
				if (file.exists()) {
					SAXBuilder saxBuilder = new SAXBuilder();
					doc = saxBuilder.build(file);
					root = doc.getRootElement();
				} else {
					root = new Element("fileData");
					doc = new Document(root);
				}

				List<Element> fileList = root.getChildren("file");
				for (Element fileElement : fileList) {
					String existingFileName = fileElement.getChildText("fileName");
					String existingIpAddress = fileElement.getChildText("ipAddress");
					if (existingFileName.equals(fileName) && existingIpAddress.equals(ipAddress)) {
						System.out.println("A entrada para o arquivo " + fileName + " e endereço IP " + ipAddress + " já existe.");
						return; // Não adicione uma nova entrada se já existir
					}
				}

				Element fileElement = new Element("file");
				fileElement.addContent(new Element("fileName").setText(fileName));
				fileElement.addContent(new Element("ipAddress").setText(ipAddress));
				fileElement.addContent(new Element("port").setText(String.valueOf(port)));
				root.addContent(fileElement);

				XMLOutputter xmlOutput = new XMLOutputter();
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(doc, new FileWriter(file));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		private void saveFile(String fileName, DataInputStream in) throws IOException {
			Path filePath = Path.of(FILE_STORAGE_PATH + fileName);
			try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					fileOutputStream.write(buffer, 0, bytesRead);
				}
			}
		}

		private void sendFile(String fileName, DataOutputStream out) throws IOException {
			Path filePath = Path.of(FILE_STORAGE_PATH + fileName);
			if (Files.exists(filePath)) {
				out.writeBoolean(true); // Indica que o arquivo existe
				byte[] fileData = Files.readAllBytes(filePath);
				out.writeInt(fileData.length); // Tamanho do arquivo
				out.write(fileData);
				System.out.println("Enviado arquivo " + fileName + " para o cliente.");
			} else {
				out.writeBoolean(false); // Indica que o arquivo não existe
			}
		}



		//here
	}
}