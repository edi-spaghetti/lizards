package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.Entity;
import org.dreambot.api.wrappers.interactive.GameObject;

import java.awt.*;
import java.awt.geom.Area;

@ScriptManifest(
        author="RonMan",
        description="Just testing",
        category = Category.MISC,
        version = 0.1,
        name = "Tester"
)

public class Main extends AbstractScript {

    int yaw = 2000;

    @Override
    public void onStart() {
        super.onStart();
        log("Starting");
        getCamera().rotateToYaw(2400);
    }

    @Override
    public int onLoop() {
        // log("Looping");

        GameObject o = getGameObjects().closest(f -> f.getIndex() == 153524537);
        rotateToTarget(o);

        return Calculations.random(150, 300);
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
