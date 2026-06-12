package net.kdt.pojavlaunch.yggdrasil;

public class PlayerCape {
    private final byte[] bytes;
    private final String hash;

    public PlayerCape(byte[] bytes, String hash) {
        this.bytes = bytes;
        this.hash = hash;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerCape)) return false;
        PlayerCape that = (PlayerCape) o;
        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }
}
