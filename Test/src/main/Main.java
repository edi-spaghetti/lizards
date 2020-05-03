package main;

import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;

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
        log("Looping");
        return 0;
    }

    @Override
    public void onExit() {
        super.onExit();
        log("Exiting");
    }
}
