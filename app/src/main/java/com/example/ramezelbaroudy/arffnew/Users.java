package com.example.ramezelbaroudy.arffnew;

import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import at.jku.pervasive.sedd.actclient.ClassLabel;
import at.jku.pervasive.sedd.actclient.CoordinatorClient;
import at.jku.pervasive.sedd.actclient.GroupStateListener;
import at.jku.pervasive.sedd.actclient.RoomState;
import at.jku.pervasive.sedd.actclient.UserRole;

/**
 * Created by ramezelbaroudy on 1/23/18.
 */

public class Users {
    CoordinatorClient connection;
    ArrayList<OtherUser> usersHistory;
    int numberOfSitting = 0;
    int numberOfStanding = 0;
    int numberOfWalking = 0;
    String theRoomCondition = "Loading data from server";
    boolean addRemove = false;
    ArrayList<String> usersHistoryDisplayList;

    public Users(CoordinatorClient connection) {
        this.connection = connection;
        usersHistory = new ArrayList<OtherUser>();
        usersHistoryDisplayList = new ArrayList<String>();
    }

    public boolean searchUsersHistory(String userID, ClassLabel activity) {
        for (int i = 0; i < usersHistory.size(); i++) {
            if (usersHistory.get(i).userId.equals(userID)) {
                if (!addRemove) {
                    usersHistory.get(i).addHistory(activity); // adding the activity to the user's history
                } else {
                    usersHistory.get(i).addRemoveHistory(activity);
                }
                return true;
            }

        }
        return false;
    }

    public UserRole searchRole(String userID) {
        for (int i = 0; i < usersHistory.size(); i++) {
            if (userID.equals(usersHistory.get(i).userId)) {
                return usersHistory.get(i).currentRole;
            }
        }
        return UserRole.transition;
    }

    public void usersListner() {
        final GroupStateListener g = new GroupStateListener() {
            @Override
            public void groupStateChanged(CoordinatorClient.UserState[] groupState) {
                for (int i = 0; i < groupState.length; i++) {

                    // ignore the user as he is out of the room if not updated for the last 10 seconds
                    if (groupState[i].getUpdateAge() / 1000 < 10 && groupState[i].getActivity() != null) {
                        if (!searchUsersHistory(groupState[i].getUserId(), groupState[i].getActivity())) {

                            OtherUser newUser = new OtherUser(groupState[i].getUserId());

                            newUser.addHistory(groupState[i].getActivity());
                            usersHistory.add(newUser);
                        }
                    }
                    UserRole u = searchRole(groupState[i].getUserId());
                    groupState[i].setRole(u);
                }
            }
        };
        connection.addGroupStateListener(g);
    }

    public ArrayList<String> prepArrayList() {
        ArrayList<String> userIdAndCurrentRole = new ArrayList<String>();
        for (int i = 0; i < usersHistory.size(); i++) {
            userIdAndCurrentRole.add(usersHistory.get(i).userId + " " + usersHistory.get(i).currentRole);
        }
        return userIdAndCurrentRole;
    }

    // checking the status of all users and decide if there is update to any user then usersHistoryDisplayList is updated with the new data
    public void updateArrayDisplay() {
        for (int i = 0; i < usersHistory.size(); i++) {
            for (int j = 0; j < usersHistoryDisplayList.size(); j++) {
                if (usersHistory.get(i).userId.equals(usersHistoryDisplayList.get(j).substring(0, 8))) {
                    if (usersHistory.get(i).currentRole.toString().charAt(0) != usersHistoryDisplayList.get(j).charAt(9)) { // checking if role changed
                        if (usersHistory.get(i).currentRole.toString().charAt(0) == 's') {
                            usersHistoryDisplayList.set(j, usersHistory.get(i).userId + " " + UserRole.speaker);
                        } else if (usersHistory.get(i).currentRole.toString().charAt(0) == 't') {
                            usersHistoryDisplayList.set(j, usersHistory.get(i).userId + " " + UserRole.transition);
                        } else {
                            usersHistoryDisplayList.set(j, usersHistory.get(i).userId + " " + UserRole.listener);
                        }
                    }
                }
            }
        }
    }

    // repeatly evaulate the status of the room
    public void repeatEvaluation() {
        final Timer repeateEvaluation = new Timer();
        repeateEvaluation.schedule(new TimerTask() {
            @Override
            public void run() {

                numberOfWalking = numberOfSitting = numberOfStanding = 0; // reseting the status of users

                for (int i = 0; i < usersHistory.size(); i++) {
                    String answer = usersHistory.get(i).determineRole(); // what the user is doing
                    if (answer.equals("Sitting")) {
                        numberOfSitting++;
                    } else if (answer.equals("Standing")) {
                        numberOfStanding++;
                    } else {
                        numberOfWalking++;
                    }
                }
                RoomState r = RoomState.transition;
                if (numberOfSitting == 0 && numberOfWalking == 0 && numberOfStanding == 0) {
                    r = RoomState.empty;
                }
                if (numberOfStanding == 1 || numberOfWalking == 1) {
                    if (numberOfSitting > 1) {
                        r = RoomState.lecture;
                    }
                }
                theRoomCondition = r.toString();

                connection.setRoomState(r);
                if (!addRemove) {
                    usersHistoryDisplayList = prepArrayList();
                } else {
                    updateArrayDisplay();

                }
                addRemove = true;

            }

        }, 9000, 2000); // delay for 1 mintue, for each 2 seconds calculate the roles
    }
}
