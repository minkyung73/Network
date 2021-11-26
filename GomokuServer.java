package omokTest;

import java.net.*;
import java.io.*;
import java.util.*;

public class GomokuServer {
	private ServerSocket welcomeSocket;
	private Broadcaster messageBroadcaster = new Broadcaster();
	
	public static void main(String[] args) {
		GomokuServer server = new GomokuServer();
		server.startServer();
	}
	
	public GomokuServer() {
		
	}
	
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
	
	
	class serverThread extends Thread{
		private Socket userSocket;
		private boolean isReady = false;
		private BufferedReader fromClient;
		private PrintWriter toClient;
		
		serverThread(Socket socket){
			userSocket = socket;
		}
		
		Socket getSocket() {
			return userSocket;
		}
		
		boolean isReady() {
			return isReady;
		}
		
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
	
	
	class Broadcaster extends ArrayList {
		//constructor of Broadcaster
		Broadcaster() {
			
		}
		
		
		void add(serverThread thread) {
			super.add(thread);
		}
		
		
		void remove(serverThread thread) {
			super.remove(thread);
		}
		
		
		serverThread getThread(int i) {
			return (serverThread)get(i);
		}
		
		
		Socket getSocket(int i) {
			return getThread(i).getSocket();
		}
		
		
		void sendToClient(int i, String msg) {
			try {
				PrintWriter toUser = new PrintWriter(getSocket(i).getOutputStream(), true);
				toUser.println(msg);
			}
			catch(IOException e) {
				
			}
		}
		
		void sendToGame(String msg) {
			for(int i = 0; i < size(); i++) {
				sendToClient(i, msg);
			}
		}
		
		
		void sendToOpponent(serverThread thread, String msg) {
			for(int i = 0; i < size(); i++) {
				if(getThread(i) != thread) {
					sendToClient(i, msg);
				}
			}
		}		
		
		
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