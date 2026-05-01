package net.droingo.aquietplace.signal;

import net.minecraft.text.Text;

public enum SignalType {
    GOOD("good"),
    BAD("bad"),
    FREEZE("freeze"),
    DANGER("danger"),
    SHAKA("shaka"),
    LOOT("loot"),
    HELP("help"),
    MOVE("move");

    private final String id;

    SignalType(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public Text getDisplayName() {
        return Text.translatable("signal.aquietplace." + this.id);
    }

    public static SignalType fromNetworkId(int networkId) {
        SignalType[] values = values();

        if (networkId < 0 || networkId >= values.length) {
            return MOVE;
        }

        return values[networkId];
    }

    public int toNetworkId() {
        return this.ordinal();
    }
}