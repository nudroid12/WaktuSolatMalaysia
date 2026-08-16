package app.nudroidlabs.waktusolat.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    @Test
    fun onlyNewerVersionIsAnUpdate() {
        assertTrue(UpdatePolicy.isUpdateAvailable(18, 19))
        assertFalse(UpdatePolicy.isUpdateAvailable(18, 18))
        assertFalse(UpdatePolicy.isUpdateAvailable(18, 17))
    }

    @Test
    fun sha256MustBeExactly64HexCharacters() {
        assertTrue(UpdatePolicy.isValidSha256("a".repeat(64)))
        assertTrue(UpdatePolicy.isValidSha256("ABCDEF".repeat(10) + "ABCD"))
        assertFalse(UpdatePolicy.isValidSha256("a".repeat(63)))
        assertFalse(UpdatePolicy.isValidSha256("g".repeat(64)))
    }

    @Test
    fun apkUrlMustUseHttpsGitHub() {
        assertTrue(
            UpdatePolicy.isAllowedApkUrl(
                "https://github.com/nudroid12/WaktuSolatMalaysia/releases/download/v1.0.1/app.apk"
            )
        )
        assertFalse(UpdatePolicy.isAllowedApkUrl("http://github.com/example/app.apk"))
        assertFalse(UpdatePolicy.isAllowedApkUrl("https://example.com/app.apk"))
    }
}
