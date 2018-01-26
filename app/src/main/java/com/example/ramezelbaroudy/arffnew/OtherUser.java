package com.example.ramezelbaroudy.arffnew;

import java.util.ArrayDeque;
import java.util.Iterator;

import at.jku.pervasive.sedd.actclient.ClassLabel;
import at.jku.pervasive.sedd.actclient.UserRole;

/**
 * Created by ramezelbaroudy on 1/23/18.
 */

public class OtherUser {
    String userId;
    ArrayDeque<ClassLabel> history;
    UserRole currentRole;

    public OtherUser(String userId) {
        this.userId = userId;
        history = new ArrayDeque<ClassLabel>();
        currentRole = UserRole.listener; // listener by default
    }

    public void addHistory(ClassLabel doing) {
        if (doing != null) { // ignore null for now
            history.add(doing);
        }
    }

    // removing element from history when new element added, after the one minute window
    public void addRemoveHistory(ClassLabel doing) {

        if (doing != null) { // this is just work around still need to be implemented properly
            history.add(doing);
            history.remove();
        }
    }

    // Method returns string which is the expected user activity
    public String determineRole() {
        int numberOfSitting = 0;
        int numberOfWalking = 0;
        int numberOfStanding = 0;
        int numberOfnull = 0;

        Iterator queueIterator = history.iterator();

        while (queueIterator.hasNext()) {
            Object o = queueIterator.next();
            if (o.toString().equals("sitting")) {
                numberOfSitting++;
            } else if (o.toString().equals("standing")) {
                numberOfStanding++;
            } else if (o.toString().equals("walking")) {
                numberOfWalking++;
            } else {
                numberOfnull++;
            }
        }
        int maximumvalue = Math.max(numberOfSitting, Math.max(numberOfStanding, Math.max(numberOfnull, numberOfWalking)));
        if (maximumvalue == numberOfSitting) {
            currentRole = UserRole.listener; // to be tested still need to show it in the other class
            return "Sitting";
        }
        if (maximumvalue == numberOfWalking && numberOfSitting > 0) {
            currentRole = UserRole.transition;
            return "Walking";
        }
        else {
            if (maximumvalue == numberOfStanding || maximumvalue == numberOfWalking) {
                currentRole = UserRole.speaker;
                return "Standing";
            }
        }

        return "Null";

    }

}
