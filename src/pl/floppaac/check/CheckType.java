package pl.floppaac.check;

/** Kategoria checka. Decyduje o progach kar w configu. */
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
