package practicachat;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HiloServidorComunicaciones extends Thread {

	public static final String colorChanger = "\u001B[0m";
	private Socket clienteSocket;
	private ConcurrentHashMap<String, Player> jugadores;
	private List<HiloServidorComunicaciones> hilos;
	private String username;
	private String assignedColor;
	private PrintWriter writer;

	// Lista de palabras prohibidas cargadas desde el archivo
	private static final List<String> palabrasProhibidas = new ArrayList<>();

	static {
		cargarPalabrasProhibidas();
	}

	public HiloServidorComunicaciones(Socket clienteSocket, ConcurrentHashMap<String, Player> players,
									  List<HiloServidorComunicaciones> hilos) {
		this.clienteSocket = clienteSocket;
		this.jugadores = players;
		this.hilos = hilos;
	}

	@Override
	public void run() {
		try (BufferedReader lector = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()))) {
			writer = new PrintWriter(clienteSocket.getOutputStream(), true);
			writer.println("[SERVIDOR]: Escribe tu nombre:");
			username = lector.readLine();

			// Asignar color
			assignedColor = ServerStart.obtainColor();
			jugadores.put(username, new Player(username, assignedColor));
			ServerStart.sendToAll("[SERVIDOR]: " + assignedColor + username + " se ha unido al chat." + colorChanger);

			String mensaje;
			while ((mensaje = lector.readLine()) != null) {
				if (mensaje.startsWith("/")) {
					commands(mensaje);
				} else {
					String mensajeCensurado = censurarMensaje(mensaje);
					writer.println(assignedColor + "[" + username + "]: " + mensajeCensurado + colorChanger);
					ServerStart.sendToAllSpaces("\t\t\t\t\t"+assignedColor + "[" + username + "]: " + mensajeCensurado + colorChanger,this);
				}
			}
		} catch (IOException e) {
			System.out.println("");
		} finally {
			jugadores.remove(username);
			ServerStart.removeThreads(this);
			ServerStart.sendToAll("[SERVIDOR]: " + assignedColor + username + " ha abandonado el chat." + colorChanger);

			// Liberar color
			ServerStart.returnColor(assignedColor);
			try {
				clienteSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void commands(String message) throws IOException {
		String[] command = message.split(" ", 3);
		switch (command[0]) {
			case "/atacar":
				attack(command[1]);
				break;
			case "/resumen":
				showInfoAll();
				break;
			case "/mio":
				showInfoPlayer();
				break;
			case "/dar":
				donate(command);
				break;
			case "/salir":
				clienteSocket.close();
				break;
			default:
				sendMessage("[SERVIDOR]: Comando no reconocido.");
		}
	}

	private void attack(String objetivo) {
		boolean userFound=true;
		boolean canAttack=true;
		if (objetivo == null || !jugadores.containsKey(objetivo)) {
			sendMessage("[SERVIDOR]: Usuario no encontrado.");
			userFound=false;
		}
		if (userFound){
		Player attacker = jugadores.get(username);
		Player attacked = jugadores.get(objetivo);

		if (attacker.getMoney() < 5 || attacked.getPv() <= 0) {
			ServerStart.sendToAll("[SERVIDOR]: " + username + " atacó a " + objetivo + " pero no surtió efecto.");
			canAttack=false;
		}
		if (canAttack){
		attacker.reducirDinero(5);
		attacked.reducirPv(10);

		ServerStart.sendToAll("[SERVIDOR]: " + username + " atacó a " + objetivo + " (-10 PV).");
			}
		}
	}

	private void showInfoAll() {
		String resume="ESTADO DE JUGADORES\n";
		for (Player jugador : jugadores.values()) {
			resume=resume+jugador+"\n";
		}
		sendMessage(resume);
	}

	private void showInfoPlayer() {
		Player player = jugadores.get(username);
		String message = " Tus datos \n" +
				"-PV: " + player.getPv() + "\n" +
				"-Dinero: " + player.getMoney();
		sendMessage(message);
	}

	private void donate(String[] partes) {

		boolean correct=true;
		boolean canDonate=true;
		if (partes.length < 3) {
			sendMessage("[SERVIDOR]: Uso incorrecto. Usa /dar <cantidad> <jugador>.");
			correct=false;
		}

		try {
			if (correct) {
				int cantidad = Integer.parseInt(partes[1]);
				String receptor = partes[2];

				if (!jugadores.containsKey(receptor)) {
					sendMessage("[SERVIDOR]: Usuario no encontrado.");
					canDonate=false;
				}

				Player donante = jugadores.get(username);
				Player destinatario = jugadores.get(receptor);

				if (donante.getMoney() < cantidad) {
					sendMessage("[SERVIDOR]: Saldo insuficiente para la donación.");
					canDonate=false;
				}

				if (canDonate) {
					donante.reducirDinero(cantidad);
					destinatario.incrementarDinero(cantidad);

					for (HiloServidorComunicaciones hilo : hilos) {
						if (hilo.username.equals(receptor)) {
							hilo.sendMessage("[SERVIDOR]: El jugador " + username + " te ha donado " + cantidad + " monedas.");
						}
					}
				}
			}

		} catch (NumberFormatException e) {
			sendMessage("[SERVIDOR]: La cantidad debe ser un número entero.");
		}
	}



	public void sendMessage(String message) {
		writer.println(message);

	}

	private static void cargarPalabrasProhibidas() {
		try (BufferedReader lector = new BufferedReader(new FileReader("censored.txt"))) {
			String linea;
			while ((linea = lector.readLine()) != null) {
				palabrasProhibidas.add(linea.trim().toLowerCase());
			}
		} catch (IOException e) {
		e.printStackTrace();
		}
	}

	private String censurarMensaje(String mensaje) {
		String mensajeCensurado = mensaje;
		for (String palabra : palabrasProhibidas) {
			mensajeCensurado = mensajeCensurado.replaceAll("(?i)" + palabra, "*".repeat(palabra.length()));
		}
		return mensajeCensurado;
	}
}



