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
        version = 1.002,
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
    private int msPerTile = 0;

    @Override
    public void onStart() {
        super.onStart();

        currentLizard = Lizard.GREEN;
        initialHunterXP =  getSkills().getExperience(Skill.HUNTER);
    }

    @Override
    public int onLoop() {

        // log("looping");

        //         update state
        updateState();

        // do stuff
        if (state == 0) {
            log("taking items");
            takeItems();
        } else if (state == 1) {
            // log("setting trap");
            setTrap();
        } else if (state == 2) {
            // log("checking trap");
            checkTrap();
        } else if (state == 3) {
            // log("releasing lizard");
            releaseLizard();
        } else if (state == -1) {
            // log("waiting");
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

        if (getWalking().isRunEnabled()) {
            msPerTile = 300;
        } else {
            msPerTile = 600;
        }

        // first priority is ensuring we have enough space
        if (getInventory().emptySlotCount() < 3) {
            state = 3;
            return;
        }

        // next override default priority if we're right next to a settable trap
        // most likely because we just checked it
        if (nearestSettableTrap != null) {
            if (getLocalPlayer().distance(nearestSettableTrap) == 1.0 && nearestItem != null) {

                if (nearestSettableTrap.distance(nearestItem) < 1.1) {
                    log("picking up collapsed trap");
                    state = 0;
                    return;
                }

                log("setting this trap first, get those items in sec ");
                state = 1;
                return;
            }
        }


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
        // log("current Hunter: " + currentHunter);
        // log("nearestSettableL: " + nearestSettableTrap);
        // log("nearestCheckable: " + nearestCheckableTrap);
        // log("nearestItem: " + nearestItem);
        // log("releaseableLizard: " + releaseableLizard);
        // log("myTiles: " + getTrapTilesLogString());

    }

    private StringBuilder getTrapTilesLogString() {
        StringBuilder myTrapTilesLog = new StringBuilder();
        for (Tile tile : myTrapTiles) {

            int x = tile.getX();
            int y = tile.getY();

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
                if (nearest == null && obj.distance(tile) < 2) {
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
                            && groundItem.distance(tile) < 2
            );

            if (item != null) {
                // log("actual distance = " + item.distance(tile));
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

        int x = nearestSettableTrap.getX();
        int y = nearestSettableTrap.getY();
        // log("setting trap at: " + x + "," + y);

        // set the trap
        if (nearestSettableTrap.interact("Set-trap")) {

            // wait for trap to be set up
            int numTiles = getWalking().getAStarPathFinder().calculate(
                    getLocalPlayer().getTile(), nearestSettableTrap.getTile()).size();
            int setTrapTime = 1800;
            int sleepMinimum = (numTiles * msPerTile) + setTrapTime;

            sleep(Calculations.random(sleepMinimum, sleepMinimum + 200));

            // add the area surrounding the trap so we don't accidentally pick up someone else's equipment
            Tile tile = nearestSettableTrap.getTile();
            // log("Adding trap tile: " + tile.getX() + "," + tile.getY());
            myTrapTiles.add(tile);
        }
    }

    private void checkTrap() {

        int x = nearestCheckableTrap.getX();
        int y = nearestCheckableTrap.getY();
        // log("checking trap at: " + x + "," + y);

        // cache a copy of this before we remove the object
        Tile tile = nearestCheckableTrap.getTile();

        if (nearestCheckableTrap.interact("Check")) {

            // wait for trap to be checked
            int numTiles = getWalking().getAStarPathFinder().calculate(
                    getLocalPlayer().getTile(), nearestCheckableTrap.getTile()).size();
            int setTrapTime = 600;
            int sleepMinimum = (numTiles * msPerTile) + setTrapTime;

            sleep(Calculations.random(sleepMinimum, sleepMinimum + 200));

            // log("nearest checkable traps exists: " + nearestCheckableTrap.exists());

            int beforeSize = myTrapTiles.size();
            // remove the area from list
            myTrapTiles.removeIf(a -> (a.getX() == tile.getX() && a.getY() == tile.getY()));
            log("removed " + (beforeSize - myTrapTiles.size() + " tiles"));
        }
    }

    private boolean allItemsTaken(List<GroundItem> items) {

        boolean allTaken = true;

        for (GroundItem item : items) {
            if (item.exists()) {
                allTaken = false;
            } else {
                log(item + "does not exist");
            }
        }

        return allTaken;
    }

    private boolean itemsOnSameTile(GroundItem a, GroundItem b) {
        return a.getTile().getX() == b.getTile().getX() && a.getTile().getY() == b.getTile().getY();
    }

    private void takeItems() {

        int x = nearestItem.getX();
        int y = nearestItem.getY();
        // log("taking items at: " + x + "," + y);

        List<GroundItem> items = getGroundItems().all(
                groundItem -> groundItem != null
                        && (groundItem.getID() == currentLizard.netID || groundItem.getID() == currentLizard.ropeID)
                        && itemsOnSameTile(groundItem, nearestItem)
        );

        boolean allTaken = allItemsTaken(items);
        log("allTaken is " + allTaken);

        while (!allTaken) {
            if (items.size() > 0) {
                allTaken = allItemsTaken(items);
                GroundItem nextItem = items.get(items.size() - 1);

                // calculate how long we need to wait before trying to pick up next item
                int numTiles = getWalking().getAStarPathFinder().calculate(
                        getLocalPlayer().getTile(), nextItem.getTile()).size();
                int sleepMinimum = numTiles * msPerTile + 1200;

                if (nextItem.interact("Take")) {
                    sleepUntil(() -> !nextItem.exists(), Calculations.random(
                            sleepMinimum, sleepMinimum + 600
                    ));
                    if (!nextItem.exists()) {
                        items.remove(nextItem);
                    } else {
                        log(String.format("%s still exists!", nextItem.toString()));
                    }
                } else {
                    log("TODO: walker or move cam");
                }
            } else {
                log("all items taken!");
                allTaken = true;
            }
        }

        int beforeSize = myTrapTiles.size();
        myTrapTiles.removeIf(t -> t.distance(nearestItem.getTile()) == 1.0);
        log(String.format("Removed %d tiles", beforeSize - myTrapTiles.size()));

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
