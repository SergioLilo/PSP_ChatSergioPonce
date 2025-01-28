package chatSergioPonce;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HiloServidorComunicaciones extends Thread {

	public static final String colorChanger = "\u001B[0m";
	private Socket clienteSocket;
	private ConcurrentHashMap<String, Player> players;
	private List<HiloServidorComunicaciones> threads;
	private String username;
	private String assignedColor;
	private PrintWriter writer;

	// Lista de palabras prohibidas cargadas desde el archivo
	private static final List<String> censoredWords = new ArrayList<>();

	static {
		loadCensoredWords();
	}

	public HiloServidorComunicaciones(Socket clienteSocket, ConcurrentHashMap<String, Player> players,
									  List<HiloServidorComunicaciones> hilos) {
		this.clienteSocket = clienteSocket;
		this.players = players;
		this.threads = hilos;
	}

	@Override
	public void run() {
		try (BufferedReader lector = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()))) {
			writer = new PrintWriter(clienteSocket.getOutputStream(), true);
			writer.println("[SERVIDOR]: Escribe tu nombre:");
			username = lector.readLine();

			// Asignar color
			assignedColor = ServerStart.obtainColor();
			players.put(username, new Player(username, assignedColor));
			ServerStart.sendToAll("[SERVIDOR]: " + assignedColor + username + " se ha unido al chat." + colorChanger);

			String mensaje;
			while ((mensaje = lector.readLine()) != null) {
				if (mensaje.startsWith("/")) {
					commands(mensaje);
				} else {
					String mensajeCensurado = censorMessage(mensaje);
					writer.println(assignedColor + "[" + username + "]: " + mensajeCensurado + colorChanger);
					ServerStart.sendToAllSpaces("\t\t\t\t\t"+assignedColor + "[" + username + "]: " + mensajeCensurado + colorChanger,this);
				}
			}
		} catch (IOException e) {
			System.out.println("");
		} finally {
			players.remove(username);
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
		if (objetivo == null || !players.containsKey(objetivo)) {
			sendMessage("[SERVIDOR]: Usuario no encontrado.");
			userFound=false;
		}
		if (userFound){
		Player attacker = players.get(username);
		Player attacked = players.get(objetivo);

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
		for (Player jugador : players.values()) {
			resume=resume+jugador+"\n";
		}
		sendMessage(resume);
	}

	private void showInfoPlayer() {
		Player player = players.get(username);
		String message = " Tus datos \n" +
				"-PV: " + player.getPv() + "\n" +
				"-Dinero: " + player.getMoney();
		sendMessage(message);
	}

	private void donate(String[] partes) {

		boolean correct=true;
		boolean canDonate=true;
		if (partes.length < 3) {
			sendMessage("[SERVIDOR]: incorrecto.El comando es: /dar <cantidad> <jugador>.");
			correct=false;
		}

		try {
			if (correct) {
				int cantidad = Integer.parseInt(partes[1]);
				String receptor = partes[2];

				if (!players.containsKey(receptor)) {
					sendMessage("[SERVIDOR]: Usuario no encontrado.");
					canDonate=false;
				}

				Player donante = players.get(username);
				Player destinatario = players.get(receptor);

				if (donante.getMoney() < cantidad) {
					sendMessage("[SERVIDOR]: Saldo insuficiente para la donación.");
					canDonate=false;
				}

				if (canDonate) {
					donante.reducirDinero(cantidad);
					destinatario.incrementarDinero(cantidad);

					for (HiloServidorComunicaciones hilo : threads) {
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

	private static void loadCensoredWords() {
		try (BufferedReader lector = new BufferedReader(new FileReader("src/main/java/chatSergioPonce/censored"))) {
			String linea;
			while ((linea = lector.readLine()) != null) {
				censoredWords.add(linea.trim().toLowerCase());
			}
		} catch (IOException e) {
		e.printStackTrace();
		}
	}

	private String censorMessage(String message) {
		String censoredMessage = message;
		for (String word : censoredWords) {
			censoredMessage = censoredMessage.replaceAll(word, "*".repeat(word.length()));
		}
		return censoredMessage;
	}
}



