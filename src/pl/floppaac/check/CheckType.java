package pl.floppaac.check;

public enum CheckType {
    MOVEMENT("movement"),
    COMBAT("combat"),
    PLAYER("player");

    private final String key;

    CheckType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
