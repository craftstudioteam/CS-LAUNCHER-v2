package com.craftstudio.launcher.feature.download.enums

/**
 * Mod的依赖类型，并且为每一个类型单独指定一个代表色，便于区分
 * @param curseforge 类型在 CurseForge 上的代称
 * @param modrinth 类型在 Modrinth 上的代称
 * @param color 该类型的代表色
 */
enum class DependencyType(val curseforge: String?, val modrinth: String?, val color: Int) {
    /**
     * 需装：这种依赖对项目来说是必须的，如果缺少这种依赖，项目将无法正常运行
     *
     * CurseForge: "3"
     * Modrinth: "required"
     * 颜色：0x4CFF9800（橙色，Alpha 30%）
     */
    REQUIRED("3", "required", 0x4CFF9800),

    /**
     * 可选：这些依赖不是必须的，但可以为项目添加额外的功能或特性
     *
     * CurseForge: "2"
     * Modrinth: "optional"
     * 颜色：0x4C34C759（浅绿色，Alpha 30%）
     */
    OPTIONAL("2", "optional", 0x4C34C759),

    /**
     * 不兼容：这种依赖表示项目与其他特定的项目或依赖有冲突，不建议同时使用，如果尝试同时使用这些依赖，可能会导致错误或故障
     *
     * CurseForge: "5"
     * Modrinth: "incompatible"
     * 颜色：0x4CEF5350（浅红色，Alpha 30%）
     */
    INCOMPATIBLE("5", "incompatible", 0x4CEF5350),

    /**
     * 嵌入式：这些依赖已经包含在项目内，用户不需要单独安装，它们是项目的一部分，用于确保项目正常运行
     *
     * CurseForge: "1"
     * Modrinth: "embedded"
     * 颜色：0x4CFFD54F（浅黄色，Alpha 30%）
     */
    EMBEDDED("1", "embedded", 0x4CFFD54F),

    /**
     * 工具：这类依赖是用于开发或操作项目的工具，它们本身并不是项目运行所必需的
     *
     * CurseForge: "4"
     * Modrinth: null
     * 颜色：0x4CBDBDBD（灰色，Alpha 30%）
     */
    TOOL("4", null, 0x4CBDBDBD),

    /**
     * 包含：这种依赖是指项目所包含的文件或资源，虽然它们不是项目的核心功能，但可以为项目提供额外的支持或功能
     *
     * CurseForge: "6"
     * Modrinth: null
     * 颜色：0x4C9575CD（紫色，Alpha 30%）
     */
    INCLUDE("6", null, 0x4C9575CD)
}