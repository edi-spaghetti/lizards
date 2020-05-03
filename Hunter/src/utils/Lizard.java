package utils;

public enum Lizard {
    GREEN("Swamp lizard");

    // coming soon
    // ORANGE, RED, BLACK;

    private String lizardName;
    private String trapName;

    Lizard(String lizardName) {
        setLizardName(lizardName);
        setTrapName("Young tree");
    }

    public String getLizardName() {
        return lizardName;
    }

    public void setLizardName(String lizardName) {
        this.lizardName = lizardName;
    }

    public String getTrapName() {
        return trapName;
    }

    public void setTrapName(String trapName) {
        this.trapName = trapName;
    }
}
