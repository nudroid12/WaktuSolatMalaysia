package app.nudroidlabs.waktusolat.location

import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.PrayerZone
import java.text.Normalizer
import java.util.Locale

/**
 * Converts an administrative address into a JAKIM prayer-zone suggestion.
 *
 * This deliberately returns null when more than one zone is plausible.
 * It does not use nearest-coordinate guessing because JAKIM zones are
 * administrative prayer zones and some official zones have special boundaries.
 */
object JakimZoneResolver {
    private val aliasesByZone: Map<String, Set<String>> = mapOf(
        "JHR01" to setOf("pulau aur", "pulau pemanggil"),
        "JHR02" to setOf("johor bahru", "kota tinggi", "mersing", "kulai"),
        "JHR03" to setOf("kluang", "pontian"),
        "JHR04" to setOf("batu pahat", "muar", "segamat", "gemas", "tangkak"),

        "KDH01" to setOf("kota setar", "kubang pasu", "pokok sena"),
        "KDH02" to setOf("kuala muda", "yan", "pendang"),
        "KDH03" to setOf("padang terap", "sik"),
        "KDH04" to setOf("baling"),
        "KDH05" to setOf("bandar baharu", "kulim"),
        "KDH06" to setOf("langkawi"),
        "KDH07" to setOf("puncak gunung jerai"),

        "KTN01" to setOf(
            "bachok", "kota bharu", "machang", "pasir mas", "pasir puteh",
            "tanah merah", "tumpat", "kuala krai", "chiku"
        ),
        "KTN02" to setOf("gua musang", "galas", "bertam", "jeli", "lojing"),

        "NGS01" to setOf("tampin", "jempol"),
        "NGS02" to setOf("jelebu", "kuala pilah", "rembau"),
        "NGS03" to setOf("port dickson", "seremban"),

        "PHG01" to setOf("pulau tioman", "tioman"),
        "PHG02" to setOf("kuantan", "pekan", "muadzam shah"),
        "PHG03" to setOf("jerantut", "temerloh", "maran", "bera", "chenor", "jengka"),
        "PHG04" to setOf("bentong", "lipis", "kuala lipis", "raub"),
        "PHG05" to setOf("genting sempah", "janda baik", "bukit tinggi"),
        "PHG06" to setOf("cameron highlands", "genting highlands", "bukit fraser", "fraser's hill", "fraser hill"),
        "PHG07" to setOf("mukim rompin", "mukim endau", "mukim pontian"),

        "PRK01" to setOf("tapah", "slim river", "tanjung malim"),
        "PRK02" to setOf("kuala kangsar", "sungai siput", "ipoh", "batu gajah", "kampar"),
        "PRK03" to setOf("lenggong", "pengkalan hulu", "gerik"),
        "PRK04" to setOf("temengor", "belum"),
        "PRK05" to setOf(
            "kampung gajah", "teluk intan", "bagan datuk", "seri iskandar",
            "beruas", "parit", "lumut", "sitiawan", "pulau pangkor", "pangkor"
        ),
        "PRK06" to setOf("selama", "taiping", "bagan serai", "parit buntar"),
        "PRK07" to setOf("bukit larut"),

        // Sabah has several official split zones. Broad names such as "Sandakan"
        // and "Tawau" are intentionally omitted when they can be ambiguous.
        "SBH01" to setOf("bukit garam", "semawang", "temanggong", "tambisan", "bandar sandakan", "sukau"),
        "SBH02" to setOf("beluran", "telupid", "pinangah", "terusan", "kuamut"),
        "SBH03" to setOf("lahad datu", "silabukan", "kunak", "sahabat", "semporna", "tungku"),
        "SBH04" to setOf("bandar tawau", "balong", "merotai", "kalabakan"),
        "SBH05" to setOf("kudat", "kota marudu", "pitas", "pulau banggi", "banggi"),
        "SBH06" to setOf("gunung kinabalu"),
        "SBH07" to setOf("kota kinabalu", "ranau", "kota belud", "tuaran", "penampang", "papar", "putatan"),
        "SBH08" to setOf("pensiangan", "keningau", "tambunan", "nabawan"),
        "SBH09" to setOf("beaufort", "kuala penyu", "sipitang", "tenom", "long pasia", "membakut", "weston"),

        "SGR01" to setOf("gombak", "petaling", "sepang", "hulu langat", "hulu selangor", "shah alam"),
        "SGR02" to setOf("kuala selangor", "sabak bernam"),
        "SGR03" to setOf("klang", "kuala langat"),

        "SWK01" to setOf("limbang", "lawas", "sundar", "trusan"),
        "SWK02" to setOf("miri", "niah", "bekenu", "sibuti", "marudi"),
        "SWK03" to setOf("pandan", "belaga", "suai", "tatau", "sebauh", "bintulu"),
        "SWK04" to setOf("sibu", "mukah", "dalat", "song", "igan", "oya", "balingian", "kanowit", "kapit"),
        "SWK05" to setOf("sarikei", "matu", "julau", "rajang", "daro", "bintangor", "belawai"),
        "SWK06" to setOf("lubok antu", "sri aman", "roban", "debak", "kabong", "lingga", "engkelili", "betong", "spaoh", "pusa", "saratok"),
        "SWK07" to setOf("serian", "simunjan", "samarahan", "sebuyau", "meludam"),
        "SWK08" to setOf("kuching", "bau", "lundu", "sematan"),
        "SWK09" to setOf("kampung patarikan", "patarikan"),

        "TRG01" to setOf("kuala terengganu", "marang", "kuala nerus"),
        "TRG02" to setOf("besut", "setiu"),
        "TRG03" to setOf("hulu terengganu"),
        "TRG04" to setOf("dungun", "kemaman"),

        "WLY01" to setOf("kuala lumpur", "putrajaya"),
        "WLY02" to setOf("labuan")
    ).mapValues { (_, values) -> values.map(::normalise).toSet() }

    fun resolve(
        stateRaw: String?,
        placeParts: List<String?>
    ): PrayerZone? {
        val state = resolveState(stateRaw, placeParts) ?: return null
        val zonesInState = JakimZones.all.filter { it.state == state }

        if (zonesInState.size == 1) return zonesInState.single()

        val parts = (placeParts + stateRaw)
            .mapNotNull { it?.takeIf(String::isNotBlank) }
            .map(::normalise)
            .flatMap { part -> variants(part) }
            .filter { it.length >= 3 }
            .distinct()

        val matchedCodes = buildSet {
            zonesInState.forEach { zone ->
                val aliases = aliasesByZone[zone.code].orEmpty()
                if (aliases.any { alias -> parts.any { part -> matches(part, alias) } }) {
                    add(zone.code)
                }
            }
        }

        return matchedCodes.singleOrNull()?.let(JakimZones::byCode)
    }

    private fun resolveState(stateRaw: String?, placeParts: List<String?>): String? {
        val joined = (listOf(stateRaw) + placeParts)
            .mapNotNull { it?.takeIf(String::isNotBlank) }
            .joinToString(" ")
            .let(::normalise)

        return when {
            "johor" in joined -> "Johor"
            "kedah" in joined -> "Kedah"
            "kelantan" in joined -> "Kelantan"
            "melaka" in joined || "malacca" in joined -> "Melaka"
            "negeri sembilan" in joined -> "Negeri Sembilan"
            "pahang" in joined -> "Pahang"
            "perlis" in joined -> "Perlis"
            "pulau pinang" in joined || "penang" in joined -> "Pulau Pinang"
            "perak" in joined -> "Perak"
            "sabah" in joined -> "Sabah"
            "selangor" in joined -> "Selangor"
            "sarawak" in joined -> "Sarawak"
            "terengganu" in joined -> "Terengganu"
            "kuala lumpur" in joined || "putrajaya" in joined || "labuan" in joined ||
                "wilayah persekutuan" in joined -> "Wilayah Persekutuan"
            else -> null
        }
    }

    private fun variants(value: String): List<String> {
        val cleaned = value
            .replace(Regex("\\b(daerah|district|jajahan|mukim|bahagian|division|negeri|state|wilayah persekutuan)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return listOf(value, cleaned).filter(String::isNotBlank).distinct()
    }

    private fun matches(part: String, alias: String): Boolean {
        if (part == alias) return true
        if (part.endsWith(" $alias")) return true
        if (part.startsWith("$alias ")) return true
        return alias.length >= 5 && part.contains(" $alias ")
    }

    private fun normalise(raw: String): String {
        val ascii = Normalizer.normalize(raw.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return ascii
            .replace("&", " dan ")
            .replace(Regex("[^a-z0-9']+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
