package app.nudroidlabs.waktusolat.data

data class PrayerDay(
    val hijri: String,
    val dateRaw: String,
    val dayRaw: String,
    val imsak: String,
    val subuh: String,
    val syuruk: String,
    val dhuha: String,
    val zohor: String,
    val asar: String,
    val maghrib: String,
    val isyak: String
)

data class PrayerResponse(
    val days: List<PrayerDay>,
    val status: String,
    val serverTime: String,
    val zone: String,
    val bearing: String
)

data class PrayerZone(
    val code: String,
    val state: String,
    val area: String
) {
    val displayName: String get() = "$code · $area"
}
