package edu.kit.iti.formal.flavium

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.regex.Pattern

/**
 *
 * @author Alexander Weigl
 * @version 1 (16.12.22)
 */

@Serializable
class Config(val tenants: List<TenantConfig> = arrayListOf()) {
}

@Serializable
class TenantConfig(
    val id: String = "unique_name",
    val title: String = "Flavium",
    val introduction: String = "empty",
    val leaderboardHints: String = "empty",
    val uploadFilename: String = "MyKuromasuSolver.java",
    val resetScript: String = "reset.sh",
    val runScript: String = "run.sh",
    val privacyNote: String = "",
    @SerialName("regexScore") val _regexScore: String = "success (.*?) %",
    @SerialName("workFolder") val _workFolder: String = "work",
) {
    val regexScore by lazy { Pattern.compile(_regexScore) }
    val workFolder by lazy {
        File(_workFolder).absoluteFile
            .apply {
                mkdirs()
            }
    }
}