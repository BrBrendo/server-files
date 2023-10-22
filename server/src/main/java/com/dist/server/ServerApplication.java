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
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.Map;
public class ServerApplication{

	/*D:\ServerFiles*/
	private static final int PORT = 12345; // escolha a porta de operação/escuta do servidor.
	// Local da pasta onde o servidor salvará os arquivos.

	private static final Map<String, String> fileData = new HashMap<>();
	private static final String FILE_STORAGE_PATH = "C:\\dist/"; //subistitua o local para o servidor salvar os arquivos.

	public static void main(String[] args) {
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("Servidor em execução na porta " + PORT);

			File storageDir = new File(FILE_STORAGE_PATH);
			if (!storageDir.exists()) {
				storageDir.mkdirs();
			}

			while (true) {
				Socket clientSocket = serverSocket.accept();
				new ClientHandler(clientSocket).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class ClientHandler extends Thread {
		private final Socket clientSocket;

		public ClientHandler(Socket socket) {
			this.clientSocket = socket;
		}

		@Override
		public void run() {
			try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
				 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

				String request = in.readUTF();

				if (request.equals("UPLOAD")) {
					// Cliente está enviando um arquivo
					String fileName = in.readUTF();
					saveFile(fileName, in);
					System.out.println("Arquivo " + fileName + " foi recebido com sucesso no servidor.");
				} else if (request.equals("DOWNLOAD")) {
					// Cliente está solicitando um arquivo
					String fileName = in.readUTF();
					sendFile(fileName, out);
				} else if (request.equals("LISTAR")) {
					// Cliente está solicitando a lista de arquivos disponíveis
					listarArquivos(out);
				}else if (request.equals("STORE_FILE_INFO")) {
					// Lógica para armazenar informações sobre arquivos
					String fileName = in.readUTF();
					out.writeUTF(fileName); // Envia o nome do arquivo de volta para o cliente
					String ipAddress = clientSocket.getInetAddress().getHostAddress(); // obtém o endereço IP do cliente
					int port = clientSocket.getPort(); // obtém a porta do cliente
					storeFileInfo(fileName, ipAddress, port);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		private void storeFileInfo(String fileName, String ipAddress, int port) {
			// Salvar no arquivo físico
			try {
				Element root = new Element("fileData");
				Document doc = new Document(root);

				Element fileElement = new Element("file");
				fileElement.addContent(new Element("fileName").setText(fileName));
				fileElement.addContent(new Element("ipAddress").setText(ipAddress));
				fileElement.addContent(new Element("port").setText(String.valueOf(port)));
				root.addContent(fileElement);

				XMLOutputter xmlOutput = new XMLOutputter();
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(doc, new FileWriter(FILE_STORAGE_PATH + "fileData.xml"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}



		private void listarArquivos(DataOutputStream out) throws IOException {
			File[] files = new File(FILE_STORAGE_PATH).listFiles();
			if (files != null) {
				StringBuilder fileList = new StringBuilder();
				for (File file : files) {
					fileList.append(file.getName()).append("\n");
				}
				out.writeUTF(fileList.toString());
			} else {
				out.writeUTF("Nenhum arquivo disponível no servidor.");
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