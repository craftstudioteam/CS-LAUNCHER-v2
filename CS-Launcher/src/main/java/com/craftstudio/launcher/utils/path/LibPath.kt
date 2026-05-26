package com.craftstudio.launcher.utils.path

import com.craftstudio.launcher.utils.path.PathManager.Companion.DIR_DATA
import com.craftstudio.launcher.utils.path.PathManager.Companion.DIR_GAME_HOME
import java.io.File

class LibPath {
    companion object {
        private val COMPONENTS_DIR = File(DIR_DATA, "components")
        private val OTHER_LOGIN_DIR = File(DIR_GAME_HOME, "other_login")

        @JvmField val CACIO_8 = File(DIR_GAME_HOME, "caciocavallo")
        @JvmField val CACIO_17 = File(DIR_GAME_HOME, "caciocavallo17")
        @JvmField val CACIO_17_AGENT = File(CACIO_17, "cacio-agent.jar")

        @JvmField val FORGE_INSTALLER = File(COMPONENTS_DIR, "forge_installer.jar")
        @JvmField val MIO_FABRIC_AGENT = File(COMPONENTS_DIR, "MioFabricAgent.jar")

        @JvmField val MIO_LIB_PATCHER = File(COMPONENTS_DIR, "MioLibPatcher.jar")
        @JvmField val OPTIFINE_RENAMER = File(COMPONENTS_DIR, "OptiFineRenamer.jar")

        @JvmField val AUTHLIB_INJECTOR = File(OTHER_LOGIN_DIR, "authlib-injector.jar")
        @JvmField val NIDE_8_AUTH = File(OTHER_LOGIN_DIR, "nide8auth.jar")

        @JvmField val JAVA_SANDBOX_POLICY = File(COMPONENTS_DIR, "java_sandbox.policy")
        @JvmField val LOG4J_XML_1_7 = File(COMPONENTS_DIR, "log4j-rce-patch-1.7.xml")
        @JvmField val LOG4J_XML_1_12 = File(COMPONENTS_DIR, "log4j-rce-patch-1.12.xml")
        @JvmField val PRO_GRADE = File(COMPONENTS_DIR, "pro-grade.jar")
    }
}
