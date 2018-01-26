package at.jku.pervasive.sedd.actclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import at.jku.pervasive.sedd.util.OptionParser;
import at.jku.pervasive.sedd.util.OptionParser.Quote;

/**
 * A CoordinatorClient handles interaction with an activity coordination server.
 * It will immediately activate the client thread upon object instantiation and
 * connect to the designated coordination server. To terminate the client, call
 * {@linkplain #interrupt()}.
 * 
 * @author matsch, 2012
 */
public class CoordinatorClient extends Thread {
	public static final String DEFAULT_SERVER_HOST = "netadmin.soft.uni-linz.ac.at";
	public static final int DEFAULT_SERVER_PORT = 8891;
	public static final Charset NET_CHARSET = Charset.forName("US-ASCII");

	/**
	 * This class represents the state of a single user, mainly the current
	 * activity.
	 */
	public class UserState {
		private final String userId;
		private long updateAge;
		private ClassLabel activity;

		public UserState(String userId) {
			this.userId = userId;
			this.updateAge = -1;
			this.activity = null;
		}

		/**
		 * The users ID.
		 * 
		 * @return String
		 */
		public String getUserId() {
			return userId;
		}

		/**
		 * Time since the users activity was last updated in ms.
		 * 
		 * @return long
		 */
		public long getUpdateAge() {
			return updateAge;
		}

		/**
		 * The users last known activity.
		 * 
		 * @return ClassLabel
		 */
		public ClassLabel getActivity() {
			return activity;
		}

		/**
		 * Tell the server the user role that was determined for this user.
		 * 
		 * @param role the new user role (UserRole)
		 */
		public void setRole(UserRole role) {
			synchronized (outputQueue) {
				outputQueue.add("U:" + userId + ":" + role);
			}
			// notify client thread that new output is available
			lock.release();
		}

		@Override
		public String toString() {
			return userId + ":" + activity + "-" + updateAge;
		}

	}

	private static final Quote[] BRACKETS = { new Quote('(', ')') };
	private static final Comparator<UserState> COMPARE_USERSTATE = new Comparator<UserState>() {
		@Override
		public int compare(UserState o1, UserState o2) {
			return o1.userId.compareTo(o2.userId);
		}
	};

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private String clientId;
	private Semaphore lock;
	private String host;
	private int port;
	private ArrayDeque<String> outputQueue;
	private Thread inputThread;
	private HashMap<String, UserState> groupState;
	private UserState[] groupStateList;
	private ArrayList<GroupStateListener> groupStateListeners;

	/**
	 * Create a new CoordinatorClient.
	 * 
	 * @param host server host name
	 * @param port server port
	 * @param clientId the ID of this client
	 */
	public CoordinatorClient(String host, int port, String clientId) {
		this.host = host;
		this.port = port;
		this.clientId = clientId;
		outputQueue = new ArrayDeque<String>();
		groupState = new HashMap<String, UserState>();
		groupStateList = null;
		groupStateListeners = new ArrayList<GroupStateListener>();
		inputThread = null;
		lock = new Semaphore(1);
		start();
	}

	/**
	 * Create a new CoordinatorClient with default server and host.
	 * 
	 * @param clientId the ID of this client
	 */
	public CoordinatorClient(String clientId) {
		this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT, clientId);
	}

	/**
	 * Update the current activity and notify the server. This method will
	 * return immediately, the server update will be performed by the client
	 * thread.
	 * 
	 * @param label the new activity (class label), null for null-class
	 */
	public void setCurrentActivity(ClassLabel label) {
		synchronized (outputQueue) {
			outputQueue.add(String.valueOf(label));
		}
		// notify client thread that new output is available
		lock.release();
	}

	/**
	 * Update the current room state and notify the server.
	 * 
	 * @param state the new room state (RoomState)
	 */
	public void setRoomState(RoomState state) {
		synchronized (outputQueue) {
			outputQueue.add("R:" + state);
		}
		// notify client thread that new output is available
		lock.release();
	}

	/**
	 * Add a new listener, that will be notified about any user activity
	 * changes. The listeners will be notified whenever an activity changes, but
	 * at most every 100 ms and at least every 5000 ms.
	 * 
	 * @param groupStateListener
	 */
	public void addGroupStateListener(GroupStateListener groupStateListener) {
		groupStateListeners.add(groupStateListener);
	}

	/**
	 * Remove a group state listener.
	 * 
	 * @param groupStateListener
	 */
	public void removeGroupStateListener(GroupStateListener groupStateListener) {
		groupStateListeners.remove(groupStateListener);
	}

	protected void notifyGroupStateListeners() {
		// if the number of users changed, update sorted array of all users
		if (groupStateList == null) {
			ArrayList<UserState> gsa = new ArrayList<UserState>(groupState.values());
			Collections.sort(gsa, COMPARE_USERSTATE);
			groupStateList = gsa.toArray(new UserState[gsa.size()]);
		}
		// notify listeners
		for (GroupStateListener l : groupStateListeners)
			l.groupStateChanged(groupStateList);
	}

	@Override
	public void run() {
		try {
			// open connection
			socket = new Socket();
			socket.connect(new InetSocketAddress(host, port), 5000);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), NET_CHARSET));
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), NET_CHARSET), true);
			// send our client id to the server
			out.println(clientId);
			String response = in.readLine();
			System.out.println("server says: " + response);
			if (!response.equals("accepted")) throw new IOException("invalid client id");
			// create input thread
			inputThread = new Thread() {

				@Override
				public void run() {
					String line;
					while (!interrupted()) {
						try {
							line = in.readLine();
							if (line != null) {
								// System.out.println("update: " + line);
								String[] users = OptionParser.split(line, ",", BRACKETS);
								for (String user : users) {
									if (user.length() != 0) {
										String[] up = OptionParser.split(user);
										if (up.length != 3) throw new IOException("invalid server response");
										String usId = up[1];
										UserState us = groupState.get(usId);
										if (us == null) {
											// add new user state object
											us = new UserState(usId);
											groupState.put(usId, us);
											groupStateList = null;
										}
										// update user state
										us.updateAge = Long.parseLong(up[0]);
										us.activity = ClassLabel.parse(up[2]);
									}
								}
								notifyGroupStateListeners();
							} else {
								// this never happens with my java runtime ...
								interrupt();
							}
						} catch (Exception e) {
							System.out.println(e.getMessage());
							interrupt();
						}
					}
				}

			};
			inputThread.start();
			// send current activity to server until this thread is stopped
			// int ln = 1;
			while (!interrupted()) {
				// wait for an activity change
				lock.acquire();
				// send current activity to server
				synchronized (outputQueue) {
					String line;
					while ((line = outputQueue.poll()) != null) {
						// line = String.format("l%5d", ln++)+line;						
						out.println(line);
						// System.out.println(line);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("connection failed: " + e.getMessage());
		} catch (InterruptedException e) {
			// thread is interrupted by a call to interrupt, which we consider
			// to be the "correct" way to terminate the client. do nothing.
		}
		if (inputThread != null) inputThread.interrupt();
		if (!socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Test this class
//	public static void main(String[] args) {
//		// create a test client
//		CoordinatorClient c = new CoordinatorClient("00000001");
//		//CoordinatorClient c = new CoordinatorClient("127.0.0.1", DEFAULT_SERVER_PORT, "00000001");
//		c.addGroupStateListener(new GroupStateListener() {
//
//			@Override
//			public void groupStateChanged(UserState[] groupState) {
//				System.out.println(Arrays.toString(groupState));
//				for (UserState us : groupState) {
//					double r = Math.random();
//					us.setRole(r < 0.25 ? UserRole.transition : r < 0.5 ? UserRole.listener : r < 0.75 ? UserRole.speaker : null);
//				}
//			}
//		});
//		// send a few activity updates
//		try {
//			c.setCurrentActivity(ClassLabel.sitting);
//			Thread.sleep(500);
//			c.setRoomState(RoomState.empty);
//			c.setCurrentActivity(ClassLabel.standing);
//			Thread.sleep(1000);
//			c.setCurrentActivity(null);
//			Thread.sleep(1000);
//			c.setRoomState(RoomState.transition);
//			c.setCurrentActivity(ClassLabel.standing);
//			Thread.sleep(1500);
//			c.setCurrentActivity(ClassLabel.standing);
//			Thread.sleep(2000);
//			c.setCurrentActivity(ClassLabel.standing);
//			c.setRoomState(RoomState.lecture);
//			Thread.sleep(500);
//			c.setCurrentActivity(ClassLabel.sitting);
//			Thread.sleep(500);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// stop the client
//		c.interrupt();
//	}


}
