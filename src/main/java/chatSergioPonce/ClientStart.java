package chatSergioPonce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientStart {

	public static void main(String[] args) {
		try (Socket socket = new Socket("localhost", 5555);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			 Scanner scanner = new Scanner(System.in)) {

			Thread receptor = new Thread(() -> {
				try {
					String message;
					while ((message = reader.readLine()) != null) {
							System.out.println(message);

					}
				} catch (IOException e) {
					System.out.println("");
				}
			});
			receptor.start();
			String entry="";
			while (!entry.equalsIgnoreCase("/salir")) {
				 entry = scanner.nextLine();
				writer.println(entry);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
