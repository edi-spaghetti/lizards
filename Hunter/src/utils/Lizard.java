package utils;

public enum Lizard {
    GREEN("Swamp lizard"),
    ORANGE("Orange salamander"),
    RED("Red salamander"),
    BLACK("Black salamander");

    // coming soon
    // ORANGE, RED, BLACK;

    public String name;
    public Trap trap;
    public int npcID;
    public int invID;
    
    public int ropeID = 303;
    public int netID = 954;
    
    Lizard(String lizardName) {
        setName(lizardName);
        switch (lizardName) {
            case "Swamp lizard":
                setTrap(Trap.GREEN);
                setNpcID(-1);
                setInvID(-1);
                break;
            case "Orange salamander":
                setTrap(Trap.ORANGE);
                setNpcID(-1);
                setInvID(-1);
                break;
            case "Red salamander":
                setTrap(Trap.RED);
                setNpcID(2904);
                setInvID(10147);
                break;
            case "Black salamander":
                setTrap(Trap.BLACK);
                setNpcID(-1);
                setInvID(-1);
                break;
        }
    }

    public void setNpcID(int id) {
        this.npcID = id;
    }

    public int getNpcID() {
        return this.npcID;
    }

    public void setInvID(int id) {
        this.invID = id;
    }

    public int getInvID() {
        return this.invID;
    }

    public void setName(String lizardName) {
        this.name = lizardName;
    }

    public String getName() {
        return this.name;
    }

    public void setTrap(Trap trap) {
        this.trap = trap;
    }

    public Trap getTrap() {
        return this.trap;
    }

}
