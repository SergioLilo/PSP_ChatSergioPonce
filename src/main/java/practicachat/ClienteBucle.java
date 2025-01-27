package practicachat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClienteBucle {

	public static void main(String[] args) {
		try (Socket socket = new Socket("localhost", 5555);
			 BufferedReader lector = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			 PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);
			 Scanner scanner = new Scanner(System.in)) {

			Thread receptor = new Thread(() -> {
				try {
					String mensaje;
					while ((mensaje = lector.readLine()) != null) {
							System.out.println(mensaje);

					}
				} catch (IOException e) {
					System.out.println("");
				}
			});
			receptor.start();
			String entrada="";
			while (!entrada.equalsIgnoreCase("/salir")) {
				 entrada = scanner.nextLine();
				escritor.println(entrada);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
