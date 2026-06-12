package net.kdt.pojavlaunch.yggdrasil;

public class PlayerSkin {
    private final byte[] bytes;
    private final String hash;
    private final SkinModelType model;

    public PlayerSkin(byte[] bytes, String hash, SkinModelType model) {
        this.bytes = bytes;
        this.hash = hash;
        this.model = model;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getHash() {
        return hash;
    }

    public SkinModelType getModel() {
        return model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerSkin)) return false;
        PlayerSkin that = (PlayerSkin) o;
        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }
}
