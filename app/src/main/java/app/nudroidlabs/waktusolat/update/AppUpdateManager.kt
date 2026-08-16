package app.nudroidlabs.waktusolat.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import app.nudroidlabs.waktusolat.BuildConfig
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String
)

object AppUpdateManager {
    const val METADATA_URL =
        "https://raw.githubusercontent.com/nudroid12/WaktuSolatMalaysia/main/update.json"

    private const val USER_AGENT_PREFIX = "WaktuSolatKiblat/"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val MAX_METADATA_BYTES = 256 * 1024
    private const val MAX_APK_BYTES = 200L * 1024L * 1024L
    private const val APK_MIME = "application/vnd.android.package-archive"

    suspend fun checkForUpdate(context: Context): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openConnection(METADATA_URL)
            try {
                val code = connection.responseCode
                require(code in 200..299) { "Pelayan kemas kini memulangkan HTTP $code." }

                val bytes = connection.inputStream.use { input ->
                    readLimitedBytes(input, MAX_METADATA_BYTES)
                }

                val json = JSONObject(bytes.toString(Charsets.UTF_8))
                val info = UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName").trim(),
                    apkUrl = json.optString("apkUrl").trim(),
                    sha256 = json.optString("sha256").trim().lowercase(),
                    releaseNotes = json.optString("releaseNotes").trim()
                )

                if (!UpdatePolicy.isUpdateAvailable(BuildConfig.VERSION_CODE, info.versionCode)) {
                    return@runCatching null
                }

                require(info.versionName.isNotBlank()) {
                    "Nama versi kemas kini tidak sah."
                }
                require(UpdatePolicy.isAllowedApkUrl(info.apkUrl)) {
                    "URL APK kemas kini tidak dibenarkan."
                }
                require(UpdatePolicy.isValidSha256(info.sha256)) {
                    "SHA-256 kemas kini tidak sah."
                }

                info
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun downloadAndVerify(
        context: Context,
        info: UpdateInfo
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(UpdatePolicy.isAllowedApkUrl(info.apkUrl)) {
                "URL APK kemas kini tidak dibenarkan."
            }
            require(UpdatePolicy.isValidSha256(info.sha256)) {
                "SHA-256 kemas kini tidak sah."
            }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { old ->
                if (old.isFile) old.delete()
            }

            val destination = File(updatesDir, "waktu-solat-${info.versionCode}.apk")
            val connection = openConnection(info.apkUrl)

            try {
                val code = connection.responseCode
                require(code in 200..299) { "Muat turun APK gagal dengan HTTP $code." }

                val declaredLength = connection.contentLengthLong
                require(declaredLength <= 0L || declaredLength <= MAX_APK_BYTES) {
                    "Saiz APK melebihi had keselamatan."
                }

                var written = 0L
                BufferedInputStream(connection.inputStream).use { input ->
                    BufferedOutputStream(destination.outputStream()).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            written += read
                            require(written <= MAX_APK_BYTES) {
                                "Saiz APK melebihi had keselamatan."
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }

                require(destination.isFile && destination.length() > 0L) {
                    "Fail APK kosong."
                }

                val actualSha256 = sha256(destination)
                require(actualSha256.equals(info.sha256, ignoreCase = true)) {
                    destination.delete()
                    "SHA-256 APK tidak sepadan."
                }

                validatePackageAndSigner(context, destination, info)
                destination
            } catch (error: Throwable) {
                destination.delete()
                throw error
            } finally {
                connection.disconnect()
            }
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        )

    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun openConnection(value: String): HttpURLConnection {
        val url = URL(value)
        require(url.protocol.equals("https", ignoreCase = true)) {
            "Sambungan kemas kini mesti menggunakan HTTPS."
        }
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json, application/octet-stream")
            setRequestProperty("User-Agent", USER_AGENT_PREFIX + BuildConfig.VERSION_NAME)
        }
    }

    private fun readLimitedBytes(
        input: java.io.InputStream,
        maxBytes: Int
    ): ByteArray {
        val output = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break

            total += read
            require(total <= maxBytes) {
                "Metadata kemas kini terlalu besar."
            }
            output.write(buffer, 0, read)
        }

        return output.toByteArray()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun validatePackageAndSigner(
        context: Context,
        apk: File,
        info: UpdateInfo
    ) {
        val packageManager = context.packageManager
        val archive = getArchivePackageInfo(packageManager, apk)
            ?: error("APK kemas kini tidak dapat dibaca.")

        require(archive.packageName == context.packageName) {
            "Package APK tidak sepadan dengan aplikasi ini."
        }

        val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archive.versionCode.toLong()
        }

        require(archiveVersionCode == info.versionCode.toLong()) {
            "Version code APK tidak sepadan dengan metadata."
        }
        require(archiveVersionCode > BuildConfig.VERSION_CODE.toLong()) {
            "APK bukan versi yang lebih baharu."
        }

        val current = getInstalledPackageInfo(packageManager, context.packageName)
        val currentSigners = signerDigests(current)
        val archiveSigners = signerDigests(archive)

        require(currentSigners.isNotEmpty() && archiveSigners.isNotEmpty()) {
            "Tandatangan APK tidak dapat disahkan."
        }
        require(currentSigners.intersect(archiveSigners).isNotEmpty()) {
            "APK tidak ditandatangani dengan kunci aplikasi yang sama."
        }
    }

    private fun getArchivePackageInfo(
        packageManager: PackageManager,
        apk: File
    ): PackageInfo? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            getArchivePackageInfoApi33(packageManager, apk)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ->
            getArchivePackageInfoApi28(packageManager, apk)

        else ->
            getArchivePackageInfoLegacy(packageManager, apk)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getArchivePackageInfoApi33(
        packageManager: PackageManager,
        apk: File
    ): PackageInfo? =
        packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.PackageInfoFlags.of(
                PackageManager.GET_SIGNING_CERTIFICATES.toLong()
            )
        )

    @RequiresApi(Build.VERSION_CODES.P)
    @Suppress("DEPRECATION")
    private fun getArchivePackageInfoApi28(
        packageManager: PackageManager,
        apk: File
    ): PackageInfo? =
        packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES
        )

    @Suppress("DEPRECATION")
    private fun getArchivePackageInfoLegacy(
        packageManager: PackageManager,
        apk: File
    ): PackageInfo? =
        packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.GET_SIGNATURES
        )

    private fun getInstalledPackageInfo(
        packageManager: PackageManager,
        packageName: String
    ): PackageInfo = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            getInstalledPackageInfoApi33(packageManager, packageName)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ->
            getInstalledPackageInfoApi28(packageManager, packageName)

        else ->
            getInstalledPackageInfoLegacy(packageManager, packageName)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getInstalledPackageInfoApi33(
        packageManager: PackageManager,
        packageName: String
    ): PackageInfo =
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(
                PackageManager.GET_SIGNING_CERTIFICATES.toLong()
            )
        )

    @RequiresApi(Build.VERSION_CODES.P)
    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfoApi28(
        packageManager: PackageManager,
        packageName: String
    ): PackageInfo =
        packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )

    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfoLegacy(
        packageManager: PackageManager,
        packageName: String
    ): PackageInfo =
        packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_SIGNATURES
        )

    private fun signerDigests(info: PackageInfo): Set<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signerDigestsApi28(info)
        } else {
            signerDigestsLegacy(info)
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun signerDigestsApi28(info: PackageInfo): Set<String> {
        val signingInfo = info.signingInfo ?: return emptySet()
        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners.orEmpty().toList()
        } else {
            signingInfo.signingCertificateHistory.orEmpty().toList()
        }
        return signatureDigests(signatures)
    }

    @Suppress("DEPRECATION")
    private fun signerDigestsLegacy(info: PackageInfo): Set<String> =
        signatureDigests(info.signatures.orEmpty().toList())

    private fun signatureDigests(
        signatures: List<android.content.pm.Signature>
    ): Set<String> =
        signatures.map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
}
