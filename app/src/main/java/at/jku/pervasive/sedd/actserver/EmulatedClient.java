package at.jku.pervasive.sedd.actserver;

import at.jku.pervasive.sedd.actclient.ClassLabel;
import at.jku.pervasive.sedd.actclient.RoomState;
import at.jku.pervasive.sedd.actclient.UserRole;

public class EmulatedClient extends Thread {

	private String userId;

	public EmulatedClient(String userId) {
		this.userId = userId;
		start();
	}

	@Override
	public void run() {
		try {
			// repeat emulated sequence every 15 minutes
			long interval = 15 * 60 * 1000;
			while (true) {
				// get current time within interval, in minutes
				long time = System.currentTimeMillis() - ActivityServer.SERVER_STARTUP_TIME;
				int seqTime = (int) (time % interval) / (60 * 1000);
				// within first 2 minutes, do something random
				// within next 8 minutes, sit still
				// within next 2 minutes, do something random
				// within last 3 minutes, do nothing (leave room)
				if (seqTime < 2) {
					ActivityServer.setUserActivity(userId, randomActivity());
					ActivityServer.setRoomState(userId, RoomState.transition);
					ActivityServer.setUserRole(userId, userId, UserRole.transition);
				} else if (seqTime < 10) {
					ActivityServer.setUserActivity(userId, sittingActivity());
					ActivityServer.setRoomState(userId, RoomState.lecture);
					ActivityServer.setUserRole(userId, userId, UserRole.listener);
				} else if (seqTime < 12) {
					ActivityServer.setUserActivity(userId, randomActivity());
					ActivityServer.setRoomState(userId, RoomState.transition);
					ActivityServer.setUserRole(userId, userId, UserRole.transition);
				} else {
					ActivityServer.setRoomState(userId, RoomState.empty);
					//ActivityServer.setUserRole(userId, userId, UserRole.transition);
				}
				// wait for at least 100 ms + random factor (max 3 sec)
				sleep(100 + (int) (Math.random() * 3000));
			}
		} catch (InterruptedException e) {
			// thats ok, normal way of terminating the thread
		}
	}

	// return some random activity (equal distribution)
	private static ClassLabel randomActivity() {
		double r = Math.random();
		return r < 0.25 ? ClassLabel.sitting : r < 0.5 ? ClassLabel.standing : r < 0.75 ? ClassLabel.walking : null;
	}

	// return sitting activity, with sporadic errors
	private static ClassLabel sittingActivity() {
		double r = Math.random();
		return r < 0.05 ? randomActivity() : ClassLabel.sitting;
	}

}
