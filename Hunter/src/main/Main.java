package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;
import utils.Lizard;

import java.util.ArrayList;
import java.util.List;


@ScriptManifest(
        author="RonMan",
        description="Hunter is a filler skill anyway",
        category = Category.HUNTING,
        version = 0.32,
        name = "Poacher"
)


public class Main extends AbstractScript {

    private int state = -1;
    private Lizard currentLizard;

    private int currentHunter = -1;
    private int initialHunterXP = 0;

    private GameObject nearestSettableTrap;
    private GameObject nearestCheckableTrap;
    private GroundItem nearestItem;
    private Item releaseableLizard;

    private List<Area> myTrapAreas = new ArrayList<>();

    @Override
    public void onStart() {
        super.onStart();

        currentLizard = Lizard.GREEN;
        initialHunterXP =  getSkills().getExperience(Skill.HUNTER);
    }

    @Override
    public int onLoop() {

        log("looping");

        //         update state
        updateState();

        // do stuff
        if (state == 0) {
            takeItem();
        } else if (state == 1) {
            setTrap();
        } else if (state == 2) {
            checkTrap();
        } else if (state == 3) {
            releaseLizard();
        }

        return Calculations.random(300, 400);
    }

    @Override
    public void onExit() {
        super.onExit();
        int finalHunterXP = getSkills().getExperience(Skill.HUNTER);
        log("You gained " + (finalHunterXP - initialHunterXP) + " hunter xp - Congratz!");
    }

    private void updateState() {
       currentHunter = getSkills().getRealLevel(Skill.HUNTER);

        // now check what's nearest based on new info from above
        nearestSettableTrap = getNearestSettableTrap();
        nearestCheckableTrap = getNearestCheckableTrap();
        nearestItem = getNearestItem();
        releaseableLizard = getReleaseableLizard();

        // now update main state so we know what action to perform next
        if (nearestItem != null) {
            state = 0;
        } else if (nearestSettableTrap != null) {
            state = 1;
        } else if (nearestCheckableTrap != null) {
            state = 2;
        } else if (releaseableLizard != null) {
            state = 3;
        } else {
            state = -1;
        }

        // log the updated state
        log("current Hunter: " + currentHunter);
        log("nearestSettableL: " + nearestSettableTrap);
        log("nearestCheckable: " + nearestCheckableTrap);
        log("nearestItem: " + nearestItem);
        log("releaseableLizard: " + releaseableLizard);
        log("myAreas: " + myTrapAreas);

    }

    private int maxTraps(int hunterLevel) {
        if (hunterLevel >= 80) {
            return 5;
        } else if (hunterLevel >= 60) {
            return 4;
        } else if (hunterLevel >= 40) {
            return 3;
        } else if (hunterLevel >= 20) {
            return 2;
        } else {
            return 1;
        }
    }

    private GameObject getNearestSettableTrap() {

        GameObject trap = getGameObjects().closest(
                f -> f.getName().equals(currentLizard.getTrapName())
                        && f.hasAction("Set-trap")
                );

        if (myTrapAreas.size() < maxTraps(currentHunter)) {
            return trap;
        } else {
            return null;
        }

    }

    private GameObject getNearestCheckableTrap() {

        GameObject nearest = null;

        List<GameObject> objs = getGameObjects().all(f -> f.getID() == currentLizard.checkableTrapID);

        for (Area area : myTrapAreas) {

            for (GameObject obj : objs) {
                if (nearest == null && area.contains(obj)) {
                    nearest = obj;
                } else if (getLocalPlayer().distance(obj) < getLocalPlayer().distance(nearest)) {
                    nearest = obj;
                }
            }
        }

        return nearest;
    }

    private GroundItem getNearestItem() {

        GroundItem nearestItem = null;

        for (Area trapArea: myTrapAreas) {

            GroundItem item = getGroundItems().closest(
                    groundItem -> groundItem != null
                            && (groundItem.getID() == currentLizard.ropeID
                                || groundItem.getID() == currentLizard.netID)
                            && trapArea.contains(groundItem)
            );

            if (item != null) {
                nearestItem = item;
            }

        }

        return nearestItem;
    }

    private Item getReleaseableLizard() {

        Item capturedLizard = null;

        if (getInventory().contains(currentLizard.getLizardName())) {
            capturedLizard = getInventory().get(currentLizard.getLizardName());
        }

        return  capturedLizard;

    }

    private void setTrap() {

        // check if we have the level to set more traps
        if (myTrapAreas.size() < maxTraps(currentHunter)) {

            if (nearestSettableTrap != null) {

                log("checking trap at: "
                        + nearestSettableTrap.getTile().getGridX()
                        + ", "
                        + nearestSettableTrap.getTile().getGridY()
                );

                // set the trap
                if (nearestSettableTrap.interact("Set-trap")) {

                    // wait for trap to be set up
                    sleepUntil(
                            () -> !nearestSettableTrap.exists() && !getLocalPlayer().isMoving(),
                            Calculations.random(1000, 2000)
                    );

                    // add the area surrounding the trap so we don't accidentally pick up someone else's equipment
                    Area trapArea = nearestSettableTrap.getSurroundingArea(2);
                    myTrapAreas.add(trapArea);
                }
            }
        }
    }

    private void checkTrap() {

        if (nearestCheckableTrap.interact("Check")) {

            log("checking trap at: "
                    + nearestCheckableTrap.getTile().getGridX()
                    + ", "
                    + nearestCheckableTrap.getTile().getGridY()
            );

            // wait for trap to be checked
            sleepUntil(
                    () -> !nearestCheckableTrap.exists(),
                    Calculations.random(4000, 6000)
            );

            // remove the area from list
            myTrapAreas.removeIf(a -> a.contains(getLocalPlayer()));

        }
    }

    private void takeItem() {

        log("checking trap at: "
                + nearestItem.getTile().getGridX()
                + ", "
                + nearestItem.getTile().getGridY()
        );

        if (nearestItem.isOnScreen()) {

            nearestItem.interact("Take");

            // sleep until item arrives in invent
            int slots = getInventory().emptySlotCount();
            sleepUntil(() -> getInventory().emptySlotCount() == slots - 1, Calculations.random(4000, 6000));
        }

        List<GroundItem> moreItems = getGroundItems().all(
                groundItem -> groundItem != null
                            && (groundItem.getID() == currentLizard.ropeID
                                || groundItem.getID() == currentLizard.netID)
                            && nearestItem.getTile() == groundItem.getTile()
        );

        log("Found " + moreItems.size() + " more items");

        // remove the area if we've already picked everything up
        if (moreItems.size() == 0) {
            myTrapAreas.removeIf(a -> a.contains(nearestItem));
        }
    }

    private void releaseLizard() {

        if (releaseableLizard != null) {
            releaseableLizard.interact("Release");
        }
    }

}
