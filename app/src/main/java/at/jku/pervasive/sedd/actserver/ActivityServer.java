package at.jku.pervasive.sedd.actserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import at.jku.pervasive.sedd.actclient.ClassLabel;
import at.jku.pervasive.sedd.actclient.RoomState;
import at.jku.pervasive.sedd.actclient.UserRole;

public class ActivityServer {

	public static final Charset NET_CHARSET = Charset.forName("US-ASCII");
	
	public static final long SERVER_STARTUP_TIME = System.currentTimeMillis();

	private static class UserState {
		final String userId;
		long timestamp;
		ClassLabel activity;
		RoomState roomState;
		HashMap<String, UserRole> userRoles;

		public UserState(String userId) {
			this.userId = userId;
			userRoles = new HashMap<String, UserRole>();
		}
	}

	private static HashMap<String, UserState> groupState = new HashMap<String, UserState>();
	private static int lastGroupUpdateId = 0;
	private static int emulatedClientCounter = 0;
	private static List<EmulatedClient> emulatedClients = new ArrayList<EmulatedClient>();

	public static void setUserActivity(String userId, ClassLabel activity) {
		synchronized (groupState) {
			UserState us = groupState.get(userId);
			if (us == null) groupState.put(userId, us = new UserState(userId));
			us.timestamp = System.currentTimeMillis();
			us.activity = activity;
			lastGroupUpdateId++;
		}
	}

	public static void setRoomState(String userId, RoomState rs) {
		synchronized (groupState) {
			UserState us = groupState.get(userId);
			if (us == null) groupState.put(userId, us = new UserState(userId));
			us.roomState = rs;
			lastGroupUpdateId++;
		}
	}
	
	public static void setUserRole(String userId, String targetId, UserRole ur) {
		synchronized (groupState) {
			UserState us = groupState.get(userId);
			if (us == null) groupState.put(userId, us = new UserState(userId));
			us.userRoles.put(targetId, ur);
			lastGroupUpdateId++;
		}
	}
	
	public static int getGroupUpdateId() {
		return lastGroupUpdateId;
	}

	public static String getGroupStateString() {
		StringBuilder s = new StringBuilder(256);
		synchronized (groupState) {
			long time = System.currentTimeMillis();
			for (UserState us : groupState.values()) {
				if (s.length() > 0) s.append(',');
				s.append('(');
				s.append(Math.max(0, time - us.timestamp));
				s.append(",'");
				s.append(us.userId);
				s.append("','");
				s.append(us.activity);
				s.append("')");
			}
		}
		return s.toString();
	}
	
	public static String getGuiStateString(String userId) {
		StringBuilder s = new StringBuilder(256);
		synchronized (groupState) {
			long time = System.currentTimeMillis();
			UserState rus = groupState.get(userId);
			if (rus != null)	s.append(rus.roomState); else s.append("null");
			for (UserState us : groupState.values()) {
				s.append(',');
				s.append('(');
				s.append(Math.max(0, time - us.timestamp));
				s.append(",'");
				s.append(us.userId);
				s.append("','");
				s.append(us.activity);
				s.append("','");
				if (rus != null) {
					UserRole ur = rus.userRoles.get(us.userId);
					if (ur != null) s.append(ur); else s.append("null");
				} else s.append("null");
				s.append("')");
			}
		}
		return s.toString();
	}
	
	public static void addEmulatedClient() {
		synchronized (emulatedClients) {
			emulatedClients.add(new EmulatedClient(String.format("%08d", ++emulatedClientCounter)));
		}
	}
	
	public static void clearEmulatedClients() {
		synchronized (emulatedClients) {
			for (EmulatedClient ec: emulatedClients) ec.interrupt();
			emulatedClients.clear();
		}
	}
	
	public static void clearDataStore() {
		synchronized (groupState) {
			groupState.clear();
		}
	}

	public static void startActivityUpdateServer(int port) {
		try {
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.socket().bind(new InetSocketAddress(port));
			System.out.println("listening for user clients on port " + port + "...");
			while (true) {
				SocketChannel clientChannel = serverChannel.accept();
				ActivityUpdateHandler server = new ActivityUpdateHandler(clientChannel);
				Thread serverThread = new Thread(server);
				serverThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void startGuiUpdateServer(int port) {
		try {
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.socket().bind(new InetSocketAddress(port));
			System.out.println("listening for GUI clients on port " + port + "...");
			while (true) {
				SocketChannel clientChannel = serverChannel.accept();
				GuiUpdateHandler server = new GuiUpdateHandler(clientChannel);
				Thread serverThread = new Thread(server);
				serverThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// start several emulated clients
		/*new EmulatedClient("0000001");
		new EmulatedClient("0000002");
		new EmulatedClient("0000003");
		new EmulatedClient("0000004");
		new EmulatedClient("0000005"); */
		// start the activity update server
		(new Thread() {
			@Override
			public void run() {
				startActivityUpdateServer(8891);
			}
		}).start();
		// start the gui update server
		startGuiUpdateServer(8892);
	}

}
