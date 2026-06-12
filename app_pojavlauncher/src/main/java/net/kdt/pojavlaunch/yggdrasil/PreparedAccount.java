package net.kdt.pojavlaunch.yggdrasil;

public class PreparedAccount {
    private final String username;
    private final String profileId;
    private final String formattedUuid;
    private final SkinModelType skinModel;

    public PreparedAccount(String username, String profileId, String formattedUuid, SkinModelType skinModel) {
        this.username = username;
        this.profileId = profileId;
        this.formattedUuid = formattedUuid;
        this.skinModel = skinModel;
    }

    public String getUsername() {
        return username;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getFormattedUuid() {
        return formattedUuid;
    }

    public SkinModelType getSkinModel() {
        return skinModel;
    }
}
