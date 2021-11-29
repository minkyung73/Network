package omokTest;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Network Term project_ Team 8_ Gomoku Game
 * 
 * Server side for network-based gomoku game
 * 
 * Features:
 * Broadcast messages to client side
 * Synchronization implemented for appropriate server-client message handling
 * 
 * 
 * @author H.Jaeh, K.Minkyung, K.Younjeong, P.Chaerim, H.Younki
 * Last Changed: NOV. 28. 2021 
 *
 */

public class GomokuServer {
	private ServerSocket welcomeSocket;							//listening socket for server
	private Broadcaster messageBroadcaster = new Broadcaster(); //message broadcaster initialize
	
	//main method
	public static void main(String[] args) {
		GomokuServer server = new GomokuServer();
		server.startServer();
	}
	
	//constructor for server
	public GomokuServer() {
		
	}
	
	//start server and make new userSocket thread
	void startServer() {
		try {
			welcomeSocket = new ServerSocket(6789);
			System.out.println("Socket initialized");
			
			while(true) {
				Socket userSocket = welcomeSocket.accept();
				serverThread thread = new serverThread(userSocket);
				thread.start();
				messageBroadcaster.add(thread);
				System.out.println("people connected: " + messageBroadcaster.size());
			}
		}
		catch(Exception e){
			
		}
	}
	
	//userSocket thread implementing Thread class
	class serverThread extends Thread {
		private Socket userSocket;			//user socket
		private boolean isReady = false;	//boolean value for storing user 'game ready' status
		private BufferedReader fromClient;	//reader for client message
		private PrintWriter toClient;		//writer for server message
		
		serverThread(Socket socket){
			userSocket = socket;
		}
		
		Socket getSocket() {
			return userSocket;
		}
		
		boolean isReady() {
			return isReady;
		}
		
		//run thread
		@Override
		public void run() {
			try {
				String messageFromClient;
				
				fromClient = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
				toClient = new PrintWriter(userSocket.getOutputStream(), true);
				
				while((messageFromClient = fromClient.readLine()) != null) {
					//if message is start, pick stone color when all players are ready
					if(messageFromClient.startsWith("[START]")) {
						isReady = true;
						if(messageBroadcaster.isReady() == true) {
							Random rand = new Random();
							int a = rand.nextInt(1);
							
							if(a == 0) {
								toClient.println("[COLOR]BLACK");
								System.out.println("Player 1 stone = BLACK");
								messageBroadcaster.sendToOpponent(this, "[COLOR]WHITE");
							}
							else {
								toClient.println("[COLOR]WHITE");
								System.out.println("Player 1 stone = BLACK");
								messageBroadcaster.sendToOpponent(this, "[COLOR]BLACK");
							}
						}
					}
					
					//if message is stone, broadcast x, y coordinate to opponent client
					else if(messageFromClient.startsWith("[STONE]")) {
						messageBroadcaster.sendToOpponent(this, messageFromClient);
					}
					
					//if message is timeout, skip player turn and start opponent turn
					else if(messageFromClient.startsWith("[TIMEOUT]")) {
						messageBroadcaster.sendToOpponent(this, messageFromClient);
					}
						
					//if message is stopgame, set isReady to false
					else if(messageFromClient.startsWith("[STOPGAME]")) {
						isReady = false;
					}
					
					//if message is dropgame, player resigned the game
					else if(messageFromClient.startsWith("[DROPGAME]")) {
						isReady = false;
						messageBroadcaster.sendToOpponent(this, "[DROPGAME]");
					}
					
					//if message is win, player won the game.
					//Broadcast message to opponent that opponent lost the game
					else if(messageFromClient.startsWith("[WIN]")) {
						isReady = false;
						toClient.println("[WIN]");
						messageBroadcaster.sendToOpponent(this, "[LOSE]");
					}
				}
			}
			catch(IOException e) {
				
			}
			finally {
				messageBroadcaster.remove(this);
				try {
					if(fromClient != null) {
						fromClient.close();
					}
					if(toClient != null) {
						toClient.close();
					}
					if(userSocket != null) {
						userSocket.close();
					}
					fromClient = null;
					toClient = null;
					userSocket = null;
					
					messageBroadcaster.sendToGame("[DISCONNECT]");
				}
				catch(Exception e) {
					
				}
				
			}	
		}
	}
	
	//Broadcaster class for broadcasting messages to opponent player
	//Broadcaster implements ArrayList to contain each player's information(socket value, game ready status)
	class Broadcaster extends ArrayList {
		
		//constructor of Broadcaster
		Broadcaster() {
			
		}
		
		//add thread information of player to Broadcaster list
		void add(serverThread thread) {
			super.add(thread);
		}
		
		//remove thread information of player from Broadcaster list
		void remove(serverThread thread) {
			super.remove(thread);
		}
		
		
		serverThread getThread(int i) {
			return (serverThread)get(i);
		}
		
		
		Socket getSocket(int i) {
			return getThread(i).getSocket();
		}
		
		//send message to client
		void sendToClient(int i, String msg) {
			try {
				PrintWriter toUser = new PrintWriter(getSocket(i).getOutputStream(), true);
				toUser.println(msg);
			}
			catch(IOException e) {
				
			}
		}
		
		//send message to 'opponent' player's client
		void sendToGame(String msg) {
			for(int i = 0; i < size(); i++) {
				sendToClient(i, msg);
			}
		}
		
		//send message to 'opponent' player
		void sendToOpponent(serverThread thread, String msg) {
			for(int i = 0; i < size(); i++) {
				if(getThread(i) != thread) {
					sendToClient(i, msg);
				}
			}
		}		
		
		//check if all players are ready
		//synchronized implemented for tracking ready count appropriately
		synchronized boolean isReady() {
			int count = 0;
			
			for(int i = 0; i < size(); i++) {
				if(getThread(i).isReady()) {
					count++;
				}
			}
			
			if(count == 2) {
				return true;
			}
			return false;
		}
		
	}
	
	
}
