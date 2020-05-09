package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
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

    @Override
    public void onStart() {
        super.onStart();
        log("Starting");
    }

    @Override
    public int onLoop() {
        // log("Looping");
        GameObject obj = getGameObjects().closest(o -> o.getID() == 9341);

        Area area = obj.getModel().calculateModelArea();
        Point point = obj.getClickablePoint();
        if (area.contains(point)) {
            getMouse().move(obj);
            log(getClient().getMenu().getDefaultAction());
        }

        return Calculations.random(150, 300);
    }

    @Override
    public void onExit() {
        super.onExit();
        log("Exiting");
    }
}
