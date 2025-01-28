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

	private static final List<String> censoredWords = new ArrayList<>();

	static {
		loadCensoredWords();
	}

	public HiloServidorComunicaciones(Socket clienteSocket, ConcurrentHashMap<String, Player> players,
									  List<HiloServidorComunicaciones> threads) {
		this.clienteSocket = clienteSocket;
		this.players = players;
		this.threads = threads;
	}

	@Override
	public void run() {
		try (BufferedReader lector = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()))) {
			writer = new PrintWriter(clienteSocket.getOutputStream(), true);
			writer.println("[SERVIDOR]: Escribe tu nombre:");
			username = lector.readLine();

			assignedColor = ServerStart.obtainColor();
			players.put(username, new Player(username, assignedColor));
			ServerStart.sendToAll("[SERVIDOR]: "  + username + " se ha unido al chat." );

			String message;
			while ((message = lector.readLine()) != null) {
				if (message.startsWith("/")) {
					commands(message);
				} else {
					String mensajeCensurado = censorMessage(message);
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
		ServerStart.sendToAll(resume);
	}

	private void showInfoPlayer() {
		Player player = players.get(username);
		String message = " Tus datos \n" +
				"-PV: " + player.getPv() + "\n" +
				"-Dinero: " + player.getMoney();
		sendMessage(message);
	}

	private void donate(String[] wordParts) {

		boolean correct=true;
		boolean canDonate=true;
		if (wordParts.length < 3) {
			sendMessage("[SERVIDOR]: incorrecto. El comando es: /dar <cantidad> <jugador>.");
			correct=false;
		}

		try {
			if (correct) {
				int ammount = Integer.parseInt(wordParts[1]);
				String playerWhoGetsDonated = wordParts[2];

				if (!players.containsKey(playerWhoGetsDonated)) {
					sendMessage("[SERVIDOR]: Usuario no encontrado.");
					canDonate=false;
				}

				Player donor = players.get(username);
				Player whoGetsDonated = players.get(playerWhoGetsDonated);

				if (donor.getMoney() < ammount) {
					sendMessage("[SERVIDOR]: Saldo insuficiente para la donación.");
					canDonate=false;
				}

				if (canDonate) {
					donor.reducirDinero(ammount);
					whoGetsDonated.incrementarDinero(ammount);

					for (HiloServidorComunicaciones hilo : threads) {
						if (hilo.username.equals(playerWhoGetsDonated)) {
							hilo.sendMessage("[SERVIDOR]: El jugador " + username + " te ha donado " + ammount + " monedas.");
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



