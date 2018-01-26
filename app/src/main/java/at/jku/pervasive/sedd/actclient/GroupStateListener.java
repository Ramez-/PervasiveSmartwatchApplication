package at.jku.pervasive.sedd.actclient;

import at.jku.pervasive.sedd.actclient.CoordinatorClient.UserState;

/**
 * A GroupStateListener will be notified of any user activity changes.
 */
public interface GroupStateListener {
	void groupStateChanged(UserState[] groupState);
}
