package net.kdt.pojavlaunch.yggdrasil;

public enum SkinModelType {
    NONE(-1),
    STEVE(0),
    ALEX(1);

    private final int targetParity;

    SkinModelType(int targetParity) {
        this.targetParity = targetParity;
    }

    public int getTargetParity() {
        return targetParity;
    }
}
