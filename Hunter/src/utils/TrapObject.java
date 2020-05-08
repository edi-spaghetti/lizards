package utils;

import java.util.*;

import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.GroundItem;

public class TrapObject {

    // enums
    public int NONE = -1;
    public int WAITING = 0;
    public int IN_PROGRESS = 1;
    public int COMPLETED = 2;
    public int FAILED = 3;

    private GameObject emptyTree;

    private GameObject emptyNet;
    private GameObject fullNet;
    private List<GroundItem> items = new ArrayList<>();

    private int state = -1;
    private long stateChangedAt = System.currentTimeMillis();
    private double distanceFromPlayer = -1;
    private int priority = -1;

    public TrapObject(GameObject tree, Player player) {
        setUp(tree, player);
    }

    public void setUp(GameObject tree, Player player) {
        this.emptyTree = tree;
        this.distanceFromPlayer = player.distance(tree);
    }

    public void setState(int newState) {

        // keep a record of when state changed
        if (this.state != newState) {
            this.stateChangedAt = System.currentTimeMillis();
        }

        // update priority based on state and state change
        if(this.state > WAITING && newState == WAITING) {
            // this usually means we've just picked up a
            // collapsed trap or checked a successful one, so
            // resetting should take precedence
            this.priority = 0;
        } else if (newState == IN_PROGRESS) {
            // if the trap is currently set there's nothing to do anyway
            this.priority = 999999999;
        } else {
            // this should mean traps that have been waiting longer
            // get seen to first
            this.priority = (int) (System.currentTimeMillis() - this.stateChangedAt) / 1000;
        }

        // update the actual state
        this.state = newState;

    }

    public int getState() {
        return this.state;
    }

    public void setEmptyTree(GameObject object) {
        this.emptyTree = object;
    }

    public GameObject getEmptyTree() {
        return this.emptyTree;
    }

    public int setEmptyNet(GameObject object) {
        if (object.distance(this.emptyTree) <= 1.0) {
            if (this.emptyNet == null || !this.emptyNet.exists()) {
                this.emptyNet = object;
                // new net added
                return 1;
            } else {
                // already have it
                return 0;
            }
        } else {
            // not my net
            return -1;
        }
    }

    public GameObject getEmptyNet() {
        return this.emptyNet;
    }

    public int setFullNet(GameObject object) {
        if (object.distance(this.emptyTree) <= 1.0) {
            if (this.fullNet == null || !this.fullNet.exists()) {
                this.fullNet = object;
                // new net added
                return 1;
            } else {
                // already have it
                return 0;
            }
        } else {
            // not my net
            return -1;
        }
    }

    public GameObject getFullNet() {
        return this.fullNet;
    }

    public int addItem(GroundItem item) {
        if (item.distance(this.emptyTree) <= 1.0) {
            if (this.items.isEmpty()) {
                this.items.add(item);
                // new item added
                return 1;
            } else if (!this.items.contains(item)) {
                // another item added
                this.items.add(item);
                return 0;
            } else {
                return -2;
            }
        } else {
            // not my item
            return -1;
        }
    }

    public int removeItem(GroundItem item) {
        this.items.remove(item);
        return 1;
    }

    public List<GroundItem> getItems() {
        return this.items;
    }

    public void setDistanceFromPlayer(Player player) {
        this.distanceFromPlayer = player.distance(this.emptyTree);
    }

    public double getDistanceFromPlayer() {
        return this.distanceFromPlayer;
    }

    // this function allows us to sort a list of traps by distance from player on init
    public static Comparator<TrapObject> TrapDistanceComparator = new Comparator<TrapObject>() {

        @Override
        public int compare(TrapObject o1, TrapObject o2) {
            return (int) (o1.distanceFromPlayer - o2.distanceFromPlayer);
        }
    };

    public static Comparator<TrapObject> TrapPriorityComparator = new Comparator<TrapObject>() {
        @Override
        public int compare(TrapObject o1, TrapObject o2) {
            return o1.priority - o2.priority;
        }
    };
}
