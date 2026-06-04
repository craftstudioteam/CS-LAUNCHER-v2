package net.kdt.pojavlaunch.modloaders.modpacks.models;

import org.jetbrains.annotations.Nullable;

/**
 * Search filters, passed to APIs
 */
public class SearchFilters {
    public boolean isModpack;
    /** Overrides isModpack for non-standard project types: "resourcepack", "shader", "datapack" */
    public String projectType;
    public String name;
    @Nullable public String mcVersion;
    /** Mod loader filter: "fabric", "forge", "quilt", "neoforge", or null/empty for any */
    @Nullable public String modLoader;
    /** Additional categories facet for Modrinth search (e.g. "adventure" for worlds) */
    @Nullable public String categories;

}