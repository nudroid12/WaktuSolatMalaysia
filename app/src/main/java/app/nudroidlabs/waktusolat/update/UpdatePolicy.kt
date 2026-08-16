package app.nudroidlabs.waktusolat.update

import java.net.URI

object UpdatePolicy {
    private val sha256Regex = Regex("^[0-9a-fA-F]{64}$")

    fun isUpdateAvailable(currentVersionCode: Int, remoteVersionCode: Int): Boolean =
        remoteVersionCode > currentVersionCode

    fun isValidSha256(value: String): Boolean = sha256Regex.matches(value.trim())

    fun isAllowedApkUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("github.com", ignoreCase = true)
    }.getOrDefault(false)
}
