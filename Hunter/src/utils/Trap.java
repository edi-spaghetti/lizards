package utils;

public enum Trap {
    GREEN("Swamp lizard"),
    ORANGE("Orange salamander"),
    RED("Red salamander"),
    BLACK("Black salamander");
    
    public int fullNetID;

    public int emptyNetID = 8992;
    public String treeName = "Young tree";
    public int emptyTreeID = 8990;
    public int bentTreeID = 8989;

    Trap(String lizardname) {
        setFullNetID(lizardname);
    }

    private void setFullNetID(String lizardName) {
        switch (lizardName) {
            case "Swamp lizard":
                this.fullNetID = 9004;
                break;
            case "Orange salamander":
                this.fullNetID = -1;
                break;
            case "Red salamander":
                this.fullNetID = 8986;
                break;
            case "Black salamander":
                this.fullNetID = -2;
                break;
        }
    }

    public int getfullNetID() {
        return fullNetID;
    }

}
