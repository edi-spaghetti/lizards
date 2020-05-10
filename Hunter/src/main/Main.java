package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.walking.path.impl.LocalPath;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.Entity;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;
import utils.Lizard;
import utils.TrapObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


@ScriptManifest(
        author="RonMan",
        description="Hunter is a filler skill anyway",
        category = Category.HUNTING,
        version = 3.050,
        name = "Poacher"
)


public class Main extends AbstractScript {

    private Lizard prey;

    private int currentHunter = -1;
    private int initialHunterXP = 0;

    private List<TrapObject> myTraps = new ArrayList<>();
    private List<Item> releaseableLizards = new ArrayList<>();
    private TrapObject nextTrap;
    private List<GameObject> objects;
    private Entity nextEntity;

    @Override
    public void onStart() {
        super.onStart();

        getPrey();
        initialHunterXP =  getSkills().getExperience(Skill.HUNTER);
        currentHunter = getSkills().getRealLevel(Skill.HUNTER);
        initialiseTraps();

        getMouse().setAlwaysHop(true);
    }

    @Override
    public int onLoop() {

        // update state
        updateState();
        getNextTrap();

        // do stuff
        // first priority is ensuring we have enough space
        if (getInventory().emptySlotCount() < 3
                && (nextTrap.getState() == nextTrap.COMPLETED
                || nextTrap.getState() == nextTrap.FAILED)) {
             log("prioritising lizard release");
            releaseLizard();
        } else if (nextTrap.getState() == nextTrap.FAILED) {
             log("taking items");
            takeItems();
        } else if (nextTrap.getState() == nextTrap.WAITING) {
             log("setting trap");

            // refresh handler for game object
            nextEntity = getGameObjects().closest(e -> e.getIndex() == nextTrap.getEmptyTree().getIndex()
                    && e.getX() == nextTrap.getEmptyTree().getX()
                    && e.getY() == nextTrap.getEmptyTree().getY()
            );

            setTrap();
        } else if (nextTrap.getState() == nextTrap.COMPLETED) {
             log("checking trap");
            checkTrap();
        } else if (nextTrap.getState() == nextTrap.IN_PROGRESS) {
             log("normally releasing lizards");
            releaseLizard();
        } else if (nextTrap.getState() == nextTrap.NONE) {
             log("nothing to do");
        }

        return Calculations.random(150, 300);
    }

    @Override
    public void onExit() {
        super.onExit();
        int finalHunterXP = getSkills().getExperience(Skill.HUNTER);
        log("You poached " + (finalHunterXP - initialHunterXP) + " xp - Congratz!");
    }

    private void getPrey() {

        NPC lizard = getNpcs().closest(
                l -> l.getID() == Lizard.GREEN.npcID
                || l.getID() == Lizard.ORANGE.npcID
                || l.getID() == Lizard.RED.npcID
                || l.getID() == Lizard.BLACK.npcID
        );

        if (lizard.getID() == Lizard.GREEN.npcID) {
            log("prey is green lizards");
            prey = Lizard.GREEN;
        } else if (lizard.getID() == Lizard.ORANGE.npcID) {
            log("prey is orange lizards");
            prey = Lizard.ORANGE;
        } else if (lizard.getID() == Lizard.RED.npcID) {
            log("prey is red lizards");
            prey = Lizard.RED;
        } else if (lizard.getID() == Lizard.BLACK.npcID) {
            log("prey is black lizards");
            prey = Lizard.BLACK;
        }

    }

    private void initialiseTraps() {

        List<TrapObject> tempTraps = new ArrayList<>();
        objects = getGameObjects().all();
        for (GameObject object : objects) {
            if (object != null) {
                if (object.getID() == prey.trap.emptyTreeID) {
                    TrapObject trap = new TrapObject(object, getLocalPlayer());
                    tempTraps.add(trap);
                }
            }
        }

        // sort traps by distance from player
        tempTraps.sort(TrapObject.TrapDistanceComparator);

        // add as many traps as we have the hunter level for
        int index = 0;
        while (myTraps.size() < maxTraps(currentHunter)) {
            myTraps.add(tempTraps.get(index));
            if (index < tempTraps.size() -1) {
                index += 1;
            } else {
                break;
            }
        }

        log(String.format("Initialised with %d traps", myTraps.size()));
    }

    private void updateState() {

        // collect game data
        currentHunter = getSkills().getRealLevel(Skill.HUNTER);
        objects = getGameObjects().all();
        List<GroundItem> groundItems = getGroundItems().all();
        releaseableLizards = getReleaseableLizards();

        // update trap state and priority from game data
        for (TrapObject trap : myTraps) {

            // check game objects
            for (GameObject object : objects) {
                if (object.getIndex() == trap.getEmptyTree().getIndex() && trap.getItems().size() == 0) {
                    trap.setEmptyTree(object);
                    trap.setState(trap.WAITING);
                } else if (object.getID() == prey.trap.emptyNetID && trap.getItems().size() == 0) {
                    if (trap.setEmptyNet(object) >= 0) {
                        trap.setState(trap.IN_PROGRESS);
                    }
                } else if (object.getID() == prey.trap.fullNetID && trap.getItems().size() == 0) {
                    if (trap.setFullNet(object) >= 0) {
                        trap.setState(trap.COMPLETED);
                    }
                }
            }

            // check ground items
            for (GroundItem item : groundItems) {
                if (item.getID() == prey.ropeID || item.getID() == prey.netID) {
                    if (trap.addItem(item) >= 0) {
                        log(String.format("setting failed with %s -> %d", item.toString(), trap.getPriority()));
                        trap.setState(trap.FAILED);
                    }
                }
            }

            for (GroundItem item: trap.getItems()) {
                if (!item.exists()) {
                    trap.removeItem(item);
                }
            }
        }
    }

    private void getNextTrap() {
        // sort traps by priority and pick out the top priority trap
        myTraps.sort(TrapObject.TrapPriorityComparator);
        nextTrap = myTraps.get(myTraps.size() - 1);

        logInfo("    X     |    Y     |      state      |   priority  ");
        for (TrapObject trap : myTraps) {
            logInfo(String.format("%05d | %05d | %s | %13d",
                    trap.getEmptyTree().getX(), trap.getEmptyTree().getY(),
                    trap.statuses.get(trap.getState()), trap.getPriority()
            ));
        }
    }

    private int getMSPerTile(Entity target) {
        if (getWalking().isRunEnabled()) {

            // if we're right adjacent to the object the player only walks
            int numTiles = getWalking().getAStarPathFinder().calculate(
                    getLocalPlayer().getTile(), target.getTile()).size();
            if (numTiles == 1) {
                return 600;
            }

            return 300;
        } else {
            return 600;
        }
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

    private List<Item> getReleaseableLizards() {

        List<Item> inventory = getInventory().getCollection();
        List<Item> lizards = new ArrayList<>();
        for (Item item : inventory) {
            if (item != null) {
                if (item.getID() == prey.invID) {
                    lizards.add(item);
                }
            }
        }

        return lizards;
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

        // TODO: make this work

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

        getCamera().rotateToYaw(yaw - yawOffset);

    }

    private void getCloser(TrapObject trap) {

        Entity entity = trap.getEmptyTree();
        if (trap.getEmptyNet() != null) {
            entity = trap.getEmptyNet();
        }

        if (getLocalPlayer().distance(entity) > 3) {

            getWalking().walk(entity.getTile());
            Entity finalEntity = entity;
            sleepUntil(() -> getLocalPlayer().distance(finalEntity.getTile()) < 1, Calculations.random(1200));

        } else {
            log("entity is already close enough");
        }
    }

    private boolean customClick(Entity entity, String targetAction) {

        Point point = entity.getClickablePoint();
        getMouse().move(point);
        sleep(Calculations.random(50, 60));
        boolean clickable = getClient().getMenu().getDefaultAction().equals(targetAction);

        if (clickable) {
            getMouse().click();
        }
        return clickable;
    }

    private void setTrap() {

        Point centrePoint = nextEntity.getCenterPoint();
        boolean onScreen = getClient().getViewport().isOnGameScreen(centrePoint);

        // check the trap is actually on screen
        if (onScreen) {

            // set the actual trap
            if (customClick(nextEntity, prey.trap.setActionName)) {

                // wait until the empty net object has been spawned
                GameObject emptyNet = null;
                while (emptyNet == null) {
                    emptyNet = getGameObjects().closest(o -> o != null
                            && o.getID() == prey.trap.emptyNetID
                            && nextEntity.distance(o) == 1.0
                    );
                    sleep(Calculations.random(100, 150));
                }

                // calculate a reasonable timeout
                int numTiles = getWalking().getAStarPathFinder().calculate(
                        getLocalPlayer().getTile(), nextEntity.getTile()).size();
                int setTrapTime = 1800;
                int buffer = 50;
                int sleepMinimum = (numTiles * getMSPerTile(nextEntity)) + setTrapTime + buffer;
                int sleepTime = Calculations.random(sleepMinimum, sleepMinimum + 200);
                log(String.format(
                        "Player at %d, %d - Sleeping %d ms for trap to set at %d, %d",
                        getLocalPlayer().getX(), getLocalPlayer().getY(),
                        sleepTime,
                        nextEntity.getX(), nextEntity.getY()
                ));

                GameObject finalEmptyNet = emptyNet;
                sleepUntil(() -> getLocalPlayer().getAnimation() == -1
                        && getLocalPlayer().distance(finalEmptyNet) == 1.0, sleepTime);

                // add human-ish reaction time
                // sleep(Calculations.random(300, 600));

            }
        } else {
            getCloser(nextTrap);
        }
    }

    private void checkTrap() {

        GameObject nearestCheckableTrap = nextTrap.getFullNet();

        Point centrePoint = nearestCheckableTrap.getCenterPoint();
        boolean onScreen = getClient().getViewport().isOnGameScreen(centrePoint);

        if (onScreen) {

            int currentHunterXP = getSkills().getExperience(Skill.HUNTER);
            if (customClick(nearestCheckableTrap, prey.trap.checkActionName)) {

                // calculate a reasonable timeout
                int numTiles = getWalking().getAStarPathFinder().calculate(
                        getLocalPlayer().getTile(), nearestCheckableTrap.getTile()).size();
                int setTrapTime = 600;
                int buffer = 50;
                int sleepMinimum = (numTiles * getMSPerTile(nearestCheckableTrap)) + setTrapTime + buffer;
                sleepUntil(
                        () -> currentHunterXP < getSkills().getExperience(Skill.HUNTER),
                        Calculations.random(sleepMinimum, sleepMinimum + 200)
                );

            }
        } else {
            getCloser(nextTrap);
        }
    }

    private void takeItems() {

        List<GroundItem> itemsToTake = nextTrap.getItems();
        GroundItem nextItemToTake = itemsToTake.get(itemsToTake.size() - 1);
        log(String.format("taking %s at %d, %d",
                nextItemToTake.toString(),
                nextItemToTake.getTile().getX(), nextItemToTake.getTile().getY()
        ));

        int x = nextItemToTake.getX();
        int y = nextItemToTake.getY();

        // calculate how long we need to wait before trying to pick up next item
        int numTiles = getWalking().getAStarPathFinder().calculate(
                getLocalPlayer().getTile(), nextItemToTake.getTile()).size();
        int actionTime = 600;
        int buffer = 50;
        int sleepMinimum = (numTiles * getMSPerTile(nextItemToTake)) + actionTime + buffer;
        int sleepTime = Calculations.random(
                sleepMinimum, sleepMinimum + 200
        );


        Point centrePoint = nextItemToTake.getCenterPoint();
        boolean onScreen = getClient().getViewport().isOnGameScreen(centrePoint);

        if (onScreen) {

            long start = System.currentTimeMillis();
            if (customClick(nextItemToTake, "Take")) {
                long end = System.currentTimeMillis();

                // sometimes duration is more than sleepTime, in which case just make it 0
                int duration;
                if ((end - start) < 0) {
                    duration = (int) (end - start);
                } else {
                    duration = 0;
                }

                 log(String.format("Take interaction took %d ms", end - start));

                sleepTime = sleepTime - duration;

                log(String.format(
                        "Player at %d, %d - Sleeping %d ms to take %s at %d, %d",
                        getLocalPlayer().getX(), getLocalPlayer().getY(),
                        sleepTime,
                        nextItemToTake.toString(),
                        x, y
                ));
                sleepUntil(() -> !nextItemToTake.exists(), sleepTime);

                if (!nextItemToTake.exists()) {
                    nextTrap.removeItem(nextItemToTake);
                } else {
                    log(String.format("%s still exists!", nextItemToTake.toString()));
                }
            }
        } else {
            getCloser(nextTrap);
        }
    }

    private void releaseLizard() {

        if (!getTabs().isOpen(Tab.INVENTORY)) {
            getTabs().openWithMouse(Tab.INVENTORY);
        } else {

            if (releaseableLizards.size() > 0) {

                getKeyboard().pressShift();

                int numRelease = Calculations.random(releaseableLizards.size());
                int numReleased = 0;
                int index = 0;

                logInfo(String.format("releasing %d lizards", numRelease));
                while (numReleased <= numRelease) {
                    releaseableLizards.get(index).interact();
                    sleep(150, 300);
                    index += 1;
                    numReleased += 1;
                }

                getKeyboard().releaseShift();
            }

        }

    }

}
