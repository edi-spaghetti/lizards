package utils;

public enum Trap {
    GREEN("Swamp lizard"),
    ORANGE("Orange salamander"),
    RED("Red salamander"),
    BLACK("Black salamander");
    
    public int fullNetID;

    public int emptyNetID;
    public String treeName = "Young tree";
    public int emptyTreeID;
    public int bentTreeID;
    public String setActionName = "Set-trap";

    Trap(String lizardname) {
        setFullNetID(lizardname);
    }

    private void setFullNetID(String lizardName) {
        switch (lizardName) {
            case "Swamp lizard":
                this.emptyTreeID = 9341;
                this.bentTreeID = 9257;
                this.emptyNetID = 9343;
                this.fullNetID = 9004;
                break;
            case "Orange salamander":
                this.emptyTreeID = -1;
                this.bentTreeID = -1;
                this.emptyNetID = -1;
                this.fullNetID = -1;
                break;
            case "Red salamander":
                this.emptyTreeID = 8990;
                this.bentTreeID = 8989;
                this.emptyNetID = 8992;
                this.fullNetID = 8986;
                break;
            case "Black salamander":
                this.emptyTreeID = -2;
                this.bentTreeID = -2;
                this.emptyNetID = -2;
                this.fullNetID = -2;
                break;
        }
    }

    public int getfullNetID() {
        return fullNetID;
    }

}
