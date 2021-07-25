package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.Entity;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.GroundItem;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;

@ScriptManifest(
        author="RonMan",
        description="Just testing",
        category = Category.MISC,
        version = 0.1,
        name = "Tester"
)

public class Main extends AbstractScript {

    int yaw = 2000;
    ArrayList<ArrayList<Integer>> coords = new ArrayList<>();
    ArrayList<ArrayList<Entity>> entities = new ArrayList<>();
    ArrayList<Integer> entityIDs = new ArrayList<>();
    int emptyTreeID = 8990;
    int bentTreeID = 8989;
    int emptyNetID = 8992;
    int fullNetID = 8986;
    int ropeID = 303;
    int netID = 954;

    @Override
    public void onStart() {
        super.onStart();
        log("Starting");
        setUpCoords();
        setUpObjectIDs();
        setUpEntities();
    }

    @Override
    public int onLoop() {
        // log("Looping");

        long start = System.currentTimeMillis();
        updateEntities();
        long duration = start - System.currentTimeMillis();

        log(String.format("getGameObjects.closest() took %d ms", duration));

        return Calculations.random(150, 300);
    }

    private void updateEntities() {
        GameObject object;
        GroundItem item;

        for (int i = 0; i < coords.size(); i++) {
            for (int j = 0; j < entityIDs.size(); j++) {
                int id = entityIDs.get(j);
                int finalI = i;
                if (id != ropeID && id != netID) {
                    object = getGameObjects().closest(
                            o -> (o.getX() - coords.get(finalI).get(0)) < 1.1
                                    && (o.getY() - coords.get(finalI).get(1) < 1.1)
                                    && o.getID() == id);
                    entities.get(i).set(j, object);
                } else {
                    item = getGroundItems().closest(
                            o -> (o.getX() - coords.get(finalI).get(0)) < 1.1
                                    && (o.getY() - coords.get(finalI).get(1) < 1.1)
                                    && o.getID() == id);
                    entities.get(i).set(j, item);
                }
            }
        }
    }

    private void setUpEntities() {

        GameObject object;
        GroundItem item;

        for (ArrayList<Integer> xy : coords) {
            ArrayList<Entity> ents = new ArrayList<>();
            for (Integer id: entityIDs) {
                if (id != ropeID && id != netID) {
                    object = getGameObjects().closest(
                            o -> (o.getX() - xy.get(0)) < 1.1
                                    && (o.getY() - xy.get(1) < 1.1)
                                    && o.getID() == id);
                    ents.add(object);
                } else {
                    item = getGroundItems().closest(
                            o -> (o.getX() - xy.get(0)) < 1.1
                                    && (o.getY() - xy.get(1) < 1.1)
                                    && o.getID() == id);
                    ents.add(item);

                }
            }
            entities.add(ents);
        }
    }

    private void setUpObjectIDs() {
        entityIDs.add(emptyTreeID);
        entityIDs.add(emptyNetID);
        entityIDs.add(fullNetID);
        entityIDs.add(ropeID);
        entityIDs.add(netID);
    }

    private void setUpCoords() {
        ArrayList<Integer> c1 = new ArrayList<Integer>();
        c1.add(2451);
        c1.add(3225);
        coords.add(c1);

        ArrayList<Integer> c2 = new ArrayList<Integer>();
        c2.add(2447);
        c2.add(3225);
        coords.add(c2);

        ArrayList<Integer> c3 = new ArrayList<Integer>();
        c3.add(2449);
        c3.add(3228);
        coords.add(c3);

        ArrayList<Integer> c4 = new ArrayList<Integer>();
        c4.add(2453);
        c4.add(3219);
        coords.add(c4);
    }

    private int yawToDegrees(int yaw) {
        int maxYaw = 2050;
        return (int) ((double) (maxYaw - yaw) / (double) maxYaw) * 360;
    }

    private int degreesToYaw(int degrees) {
        int maxYaw = 2050;
        return (degrees / 360) * maxYaw;
    }

    private void rotateToTarget(Entity target) {

        // get coords
        int px = getLocalPlayer().getTile().getX();
        int py = getLocalPlayer().getTile().getY();
        int tx = target.getTile().getX();
        int ty = target.getTile().getY();

        // calculate vector to target
        int v1 = px - tx;
        int v2 = py - ty;
        double vMag = Math.sqrt((double) v1 * v1 + v2 * v2);

        // calculate camera vector (magnitude is 1)
        int yaw = getCamera().getYaw();
        int c1 = (int) Math.sin(yawToDegrees(yaw));
        int c2 = (int) Math.cos(yawToDegrees(yaw));
        int cMag = 1;

        // calculate angle between those two vectors
        int dot = v1 * c1 + v2 * c2;
        int det = v1 * c1 - v2 * c2;
        int angle = (int) Math.atan2(det, dot);
        int yawOffset = degreesToYaw(angle);

        log(String.format("%s at %d, %d "));

        getCamera().rotateToYaw(yaw - yawOffset);

    }

    @Override
    public void onExit() {
        super.onExit();
        log("Exiting");
    }
}
