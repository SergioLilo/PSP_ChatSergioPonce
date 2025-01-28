package chatSergioPonce;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerStart {

	private static final int PORT = 5555;
	public static final int PV_INITIAL = 50;
	public static final int MONEY_INITIAL = 25;

	// Colores ANSI para consola
	private static final String[] COLORS = {
			"\u001B[31m", // Rojo
			"\u001B[32m", // Verde
			"\u001B[34m", // Azul
			"\u001B[33m", // Amarillo
			"\u001B[35m"  // Magenta
	};

	private static final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();
	private static final List<HiloServidorComunicaciones> threads = new ArrayList<>();
	private static final List<String> availableColors = new ArrayList<>();

	public static void main(String[] args) {
		synchronized (availableColors) {
            Collections.addAll(availableColors, COLORS);
		}

		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("[SERVIDOR]: Iniciando en el puerto " + PORT);

			while (true) {
				Socket clienteSocket = serverSocket.accept();
				HiloServidorComunicaciones hilo = new HiloServidorComunicaciones(
						clienteSocket, players, threads);
				synchronized (threads) {
					threads.add(hilo);
				}
				hilo.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static synchronized void sendToAll(String mensaje) {
		synchronized (threads) {
			if (mensaje.contains("[SERVIDOR]")){
				System.out.println(mensaje);
			}
			for (HiloServidorComunicaciones hilo : threads) {
				hilo.sendMessage(mensaje);
			}
		}
	}
	public static synchronized void sendToAllSpaces(String mensaje, HiloServidorComunicaciones h) {
		synchronized (threads) {
			for (HiloServidorComunicaciones hilo : threads) {
				if (!hilo.equals(h)){
				hilo.sendMessage(mensaje);}
			}
		}
	}

	public static synchronized void removeThreads(HiloServidorComunicaciones hilo) {
		synchronized (threads) {
			threads.remove(hilo);
		}
	}

	public synchronized static String obtainColor() {
		if (availableColors.isEmpty()) {
			return "\u001B[37m";// Blanco
		}
		return availableColors.remove(0);
	}

	public synchronized static void returnColor(String color) {
		availableColors.add(0, color);
	}

}

