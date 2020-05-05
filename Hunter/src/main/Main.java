package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.tabs.Tab;
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
        version = 0.4,
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

    private List<Tile> myTrapTiles = new ArrayList<>();

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
            log("taking item");
            takeItem();
        } else if (state == 1) {
            log("setting trap");
            setTrap();
        } else if (state == 2) {
            log("checking trap");
            checkTrap();
        } else if (state == 3) {
            log("releasing lizard");
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
        log("myTiles: " + getTrapTilesLogString());

    }

    private StringBuilder getTrapTilesLogString() {
        StringBuilder myTrapTilesLog = new StringBuilder();
        for (Tile tile : myTrapTiles) {

            int x = tile.getGridX();
            int y = tile.getGridY();

            if (!myTrapTilesLog.toString().equals("")) {
                myTrapTilesLog.append(" ");
            }

            myTrapTilesLog.append(x).append(",").append(y);
        }

        return myTrapTilesLog;
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

        if (myTrapTiles.size() < maxTraps(currentHunter)) {
            return trap;
        } else {
            return null;
        }

    }

    private GameObject getNearestCheckableTrap() {

        GameObject nearest = null;

        List<GameObject> objs = getGameObjects().all(f -> f.getID() == currentLizard.checkableTrapID);

        for (Tile tile : myTrapTiles) {

            for (GameObject obj : objs) {
                if (nearest == null && obj.getTile() == tile.getTile()) {
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

        for (Tile tile: myTrapTiles) {

            GroundItem item = getGroundItems().closest(
                    groundItem -> groundItem != null
                            && (groundItem.getID() == currentLizard.ropeID
                                || groundItem.getID() == currentLizard.netID)
                            && groundItem.getTile() == tile.getTile()
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
        if (myTrapTiles.size() < maxTraps(currentHunter)) {

            if (nearestSettableTrap != null) {

                log("checking trap at: "
                        + nearestSettableTrap.getTile().getGridX()
                        + ", "
                        + nearestSettableTrap.getTile().getGridY()
                );

                // set the trap
                if (nearestSettableTrap.interact("Set-trap")) {

                    // wait for trap to be set up
                    sleep(Calculations.random(2400, 3000));

                    // add the area surrounding the trap so we don't accidentally pick up someone else's equipment
                    Tile tile = nearestSettableTrap.getTile().translate(0, 1);
                    log("Adding trap tile: " + tile.getGridX() + "," + tile.getGridY());
                    myTrapTiles.add(tile);
                }
            }
        }
    }

    private void checkTrap() {

        int x = nearestItem.getGridX();
        int y = nearestItem.getGridY();
        log("checking trap at: " + x + "," + y);

        if (nearestCheckableTrap.interact("Check")) {

            // wait for trap to be checked
            sleep(Calculations.random(2400, 3000));

            log("nearest checkable traps exists: " + nearestCheckableTrap.exists());

            int beforeSize = myTrapTiles.size();
            // remove the area from list
            myTrapTiles.removeIf(a -> a.getTile() == nearestCheckableTrap.getTile());
            log("removed " + (beforeSize - myTrapTiles.size() + " tiles"));
        }
    }

    private boolean allItemsTaken(List<GroundItem> items) {

        boolean allTaken = true;

        for (GroundItem item : items) {
            if (item.exists()) {
                allTaken = false;
            }
        }

        return allTaken;
    }

    private void takeItem() {

        int x = nearestItem.getGridX();
        int y = nearestItem.getGridY();
        log("taking items at: " + x + "," + y);

        List<GroundItem> items = getGroundItems().all(
                groundItem -> groundItem != null
                        && (groundItem.getID() == currentLizard.netID || groundItem.getID() == currentLizard.ropeID)
                        && groundItem.getTile() == nearestItem.getTile()
        );

        boolean allTaken = allItemsTaken(items);

        while (!allTaken) {
            if (items.size() > 0) {
                GroundItem nextItem = items.get(items.size() - 1);
                if (nextItem.interact("Take")) {
                    items.remove(nextItem);
                    allTaken = allItemsTaken(items);
                } else {
                    log("TODO: walker or move cam");
                }
            } else {
                allTaken = true;
            }
        }

    }

    private void releaseLizard() {

        if (!getTabs().isOpen(Tab.INVENTORY)) {
            getTabs().openWithMouse(Tab.INVENTORY);
        } else {
            if (releaseableLizard.interact("Release")) {
                log("Released lizard in slot " + releaseableLizard.getSlot());
            }
        }

    }

}
