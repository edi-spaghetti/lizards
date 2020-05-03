package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
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
        version = 0.2,
        name = "Poacher"
)


public class Main extends AbstractScript {

    private int state = -1;
    private Lizard currentLizard;

    private int currentHunter = -1;
    private int initialHunterXP = 0;
//        private Skills skills = new Skills(getClient());

    private GameObject nearestSettableTrap;
    private GameObject nearestCheckableTrap;
    private GroundItem nearestItem;
    private Item releaseableLizard;

    private List<GameObject> currentSetTraps = new ArrayList<>();
    private List<Area> trapAreas = new ArrayList<>();

    @Override
    public void onStart() {
        super.onStart();

        currentLizard = Lizard.GREEN;
        //        initialHunterXP =  skills.getExperience(Skill.HUNTER);
    }

    @Override
    public int onLoop() {

        log("looping");

        //         update state
        updateState();

        // do stuff
        if (state == 0) {
            setTrap();
        } else if (state == 1) {
            checkTrap();
        } else if (state == 2) {
            takeItem();
        } else if (state == 3) {
            releaseLizard();
        }

        return Calculations.random(300, 400);
    }

    @Override
    public void onExit() {
        super.onExit();
        //        int finalHunterXP = skills.getExperience(Skill.HUNTER);

        //        log("You gained " + (finalHunterXP - initialHunterXP) + " hunter xp - Congratz!");
    }

    private void updateState() {
        currentHunter = 44;
        // skills.getRealLevel(Skill.HUNTER);

        // iterate traps and check if they have collapsed
        currentSetTraps.removeIf(thisTrap -> !thisTrap.exists());

        // now check what's nearest based on new info from above
        nearestSettableTrap = getNearestSettableTrap();
        nearestCheckableTrap = getNearestCheckableTrap();
        nearestItem = getNearestItem();
        releaseableLizard = getReleaseableLizard();

        // now update main state so we know what action to perform next
        if (nearestSettableTrap != null) {
            state = 0;
        } else if (nearestCheckableTrap != null) {
            state = 1;
        } else if (nearestItem != null) {
            state = 2;
        } else if (releaseableLizard != null) {
            state = 3;
        } else {
            state = -1;
        }

        // log the updated state
        log("currentSetTraps: " + currentSetTraps);
        log("current Hunter: " + currentHunter);
        log("nearestSettableL: " + nearestSettableTrap);
        log("nearestCheckable: " + nearestCheckableTrap);
        log("nearestItem: " + nearestItem);
        log("releaseableLizard: " + releaseableLizard);

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
        return getGameObjects().closest(
                f -> f.getName().equals(currentLizard.getTrapName())
                        && f.hasAction("Set-trap")
                );
    }

    private GameObject getNearestCheckableTrap() {

        GameObject nearest = null;

        for (GameObject trap : currentSetTraps) {

            if (nearest == null
                    && trap.hasAction("Check")
            ) {
                nearest = trap;

            } else if (getLocalPlayer().distance(trap) < getLocalPlayer().distance(nearest)
                    && trap.hasAction("Check")
            ) {
                nearest = trap;
            }
        }

        return nearest;
    }

    private  GameObject getNearestNetTrap() {
        return getGameObjects().closest(
                f -> f.getName().equals("Net trap")
                && f.hasAction("Dismantle")
        );
    }

    private GroundItem getNearestItem() {

        GroundItem return_item = null;

        for (Area trapArea: trapAreas) {

            GroundItem item = getGroundItems().closest(
                    groundItem -> groundItem != null
                            && (groundItem.getID() == 303 || groundItem.getID() == 954)
                            && trapArea.contains(groundItem)
            );

            if (item != null) {
                return_item = item;
            }

        }

        return return_item;
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
        if (currentSetTraps.size() < maxTraps(currentHunter)) {

            if (nearestSettableTrap != null) {

                // set the trap
                if (nearestSettableTrap.interact("Set-trap")) {

                    // wait for trap to be set up
                    sleepUntil(
                            () -> nearestSettableTrap.hasAction("Dismantle"),
                            Calculations.random(4000, 6000)
                    );

                    // add the trap to list so we can find it later
                    GameObject netTrap = getNearestNetTrap();
                    currentSetTraps.add(netTrap);

                    // add the area surrounding the trap so we don't accidentally pick up someone else's equipment
                    Area trapArea = netTrap.getSurroundingArea(1);
                    trapAreas.add(trapArea);
                }
            }
        }
    }

    private void checkTrap() {

        if (nearestCheckableTrap != null) {

            if (nearestCheckableTrap.interact("Check")) {

                // wait for trap to be checked
                sleepUntil(
                        () -> !nearestCheckableTrap.exists(),
                        Calculations.random(4000, 6000)
                );

                // remove the trap from list
                currentSetTraps.remove(nearestCheckableTrap);

            }
        }
    }

    private void takeItem() {
        if (getLocalPlayer().distance(nearestItem) < 5) {
            nearestItem.interact("Take");
            sleepUntil(() -> !nearestItem.exists(), Calculations.random(4000, 6000));
        }
    }

    private void releaseLizard() {

        if (releaseableLizard != null) {
            releaseableLizard.interact("Release");
        }
    }

}
