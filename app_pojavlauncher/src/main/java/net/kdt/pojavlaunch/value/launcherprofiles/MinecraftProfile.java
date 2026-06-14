package net.kdt.pojavlaunch.value.launcherprofiles;

import androidx.annotation.Keep;

@Keep
public class MinecraftProfile {

	public static String LATEST_RELEASE = "latest-release";
	public static String LATEST_SNAPSHOT= "latest-snapshot";

	public String name;
	public String type;
	public String created;
	public String lastUsed;
	public String icon;
	public String lastVersionId;
	public String gameDir;
	public String javaDir;
	public String javaArgs;
	public String logConfig;
	public boolean logConfigIsXML;
	public String pojavRendererName;
	public String controlFile;
	public MinecraftResolution[] resolution;


	public static MinecraftProfile createTemplate(){
		MinecraftProfile TEMPLATE = new MinecraftProfile();
		TEMPLATE.name = "";
		TEMPLATE.lastVersionId = LATEST_RELEASE;
		return TEMPLATE;
	}

	public static MinecraftProfile getDefaultProfile(){
		MinecraftProfile defaultProfile = new MinecraftProfile();
		defaultProfile.name = "Default";
		defaultProfile.lastVersionId = "1.7.10";
		return defaultProfile;
	}

	public MinecraftProfile(){}

	public MinecraftProfile(MinecraftProfile profile){
		name = profile.name;
		type = profile.type;
		created = profile.created;
		lastUsed = profile.lastUsed;
		icon = profile.icon;
		lastVersionId = profile.lastVersionId;
		gameDir = profile.gameDir;
		javaDir = profile.javaDir;
		javaArgs = profile.javaArgs;
		logConfig = profile.logConfig;
		logConfigIsXML = profile.logConfigIsXML;
		pojavRendererName = profile.pojavRendererName;
		controlFile = profile.controlFile;
		resolution = profile.resolution;
	}

	/**
	 * True when this profile uses OptiFine as its renderer.
	 * Heuristic: the selected version id contains "OptiFine" / "optifine"
	 * (OptiFine install names the version accordingly).
	 */
	public boolean isOptiFine() {
		if (lastVersionId == null) return false;
		String v = lastVersionId.toLowerCase();
		return v.contains("optifine");
	}

	/**
	 * True when this profile's instance gameDir shows no mod-loader footprint
	 * (no mods/ folder, or it is empty) and the profile is not OptiFine.
	 * Best-effort heuristic — it cannot detect loaders that have not yet
	 * dropped a jar into mods/. Callers that need an authoritative answer
	 * for the mod store should additionally check {@link #isOptiFine()}.
	 */
	public boolean isVanilla() {
		if (isOptiFine()) return false;
		try {
			java.io.File gamedir = net.kdt.pojavlaunch.Tools.getGameDirPath(this);
			if (gamedir == null) return true;
			java.io.File modsDir = new java.io.File(gamedir, "mods");
			if (!modsDir.exists()) return true;
			String[] children = modsDir.list();
			return children == null || children.length == 0;
		} catch (Throwable t) {
			return true;
		}
	}

	/**
	 * @return the resolved game directory for this profile, or null on failure.
	 * Caller-side convenience so fragments don't have to catch tools exceptions.
	 */
	public java.io.File resolveGameDir() {
		try {
			return net.kdt.pojavlaunch.Tools.getGameDirPath(this);
		} catch (Throwable t) {
			return null;
		}
	}

	public int getInstalledModsCount() {
		try {
			java.io.File gameDir = resolveGameDir();
			if (gameDir == null) return 0;
			java.io.File modsDir = new java.io.File(gameDir, "mods");
			if (!modsDir.exists() || !modsDir.isDirectory()) return 0;
			java.io.File[] files = modsDir.listFiles(f -> f.isFile() &&
					(f.getName().toLowerCase().endsWith(".jar") || f.getName().toLowerCase().endsWith(".jar.disabled")));
			return files != null ? files.length : 0;
		} catch (Throwable t) {
			return 0;
		}
	}
}
