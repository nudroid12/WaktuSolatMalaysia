package app.nudroidlabs.waktusolat.data

/**
 * Zone list transcribed from the current official JAKIM e-Solat "Jadual Kod Zon" page.
 * Keep codes unchanged. Refresh against the official page before any production release.
 */
object JakimZones {
    val all: List<PrayerZone> = listOf(
        PrayerZone("JHR01", "Johor", "Pulau Aur dan Pulau Pemanggil"),
        PrayerZone("JHR02", "Johor", "Johor Bahru, Kota Tinggi, Mersing, Kulai"),
        PrayerZone("JHR03", "Johor", "Kluang, Pontian"),
        PrayerZone("JHR04", "Johor", "Batu Pahat, Muar, Segamat, Gemas Johor, Tangkak"),

        PrayerZone("KDH01", "Kedah", "Kota Setar, Kubang Pasu, Pokok Sena (Daerah Kecil)"),
        PrayerZone("KDH02", "Kedah", "Kuala Muda, Yan, Pendang"),
        PrayerZone("KDH03", "Kedah", "Padang Terap, Sik"),
        PrayerZone("KDH04", "Kedah", "Baling"),
        PrayerZone("KDH05", "Kedah", "Bandar Baharu, Kulim"),
        PrayerZone("KDH06", "Kedah", "Langkawi"),
        PrayerZone("KDH07", "Kedah", "Puncak Gunung Jerai"),

        PrayerZone("KTN01", "Kelantan", "Bachok, Kota Bharu, Machang, Pasir Mas, Pasir Puteh, Tanah Merah, Tumpat, Kuala Krai, Mukim Chiku"),
        PrayerZone("KTN02", "Kelantan", "Gua Musang (Daerah Galas Dan Bertam), Jeli, Jajahan Kecil Lojing"),

        PrayerZone("MLK01", "Melaka", "Seluruh Negeri Melaka"),

        PrayerZone("NGS01", "Negeri Sembilan", "Tampin, Jempol"),
        PrayerZone("NGS02", "Negeri Sembilan", "Jelebu, Kuala Pilah, Rembau"),
        PrayerZone("NGS03", "Negeri Sembilan", "Port Dickson, Seremban"),

        PrayerZone("PHG01", "Pahang", "Pulau Tioman"),
        PrayerZone("PHG02", "Pahang", "Kuantan, Pekan, Muadzam Shah"),
        PrayerZone("PHG03", "Pahang", "Jerantut, Temerloh, Maran, Bera, Chenor, Jengka"),
        PrayerZone("PHG04", "Pahang", "Bentong, Lipis, Raub"),
        PrayerZone("PHG05", "Pahang", "Genting Sempah, Janda Baik, Bukit Tinggi"),
        PrayerZone("PHG06", "Pahang", "Cameron Highlands, Genting Highlands, Bukit Fraser"),
        PrayerZone("PHG07", "Pahang", "Zon Khas Daerah Rompin (Mukim Rompin, Mukim Endau, Mukim Pontian)"),

        PrayerZone("PLS01", "Perlis", "Kangar, Padang Besar, Arau"),
        PrayerZone("PNG01", "Pulau Pinang", "Seluruh Negeri Pulau Pinang"),

        PrayerZone("PRK01", "Perak", "Tapah, Slim River, Tanjung Malim"),
        PrayerZone("PRK02", "Perak", "Kuala Kangsar, Sungai Siput, Ipoh, Batu Gajah, Kampar"),
        PrayerZone("PRK03", "Perak", "Lenggong, Pengkalan Hulu, Gerik"),
        PrayerZone("PRK04", "Perak", "Temengor, Belum"),
        PrayerZone("PRK05", "Perak", "Kampung Gajah, Teluk Intan, Bagan Datuk, Seri Iskandar, Beruas, Parit, Lumut, Sitiawan, Pulau Pangkor"),
        PrayerZone("PRK06", "Perak", "Selama, Taiping, Bagan Serai, Parit Buntar"),
        PrayerZone("PRK07", "Perak", "Bukit Larut"),

        PrayerZone("SBH01", "Sabah", "Bahagian Sandakan (Timur), Bukit Garam, Semawang, Temanggong, Tambisan, Bandar Sandakan, Sukau"),
        PrayerZone("SBH02", "Sabah", "Beluran, Telupid, Pinangah, Terusan, Kuamut, Bahagian Sandakan (Barat)"),
        PrayerZone("SBH03", "Sabah", "Lahad Datu, Silabukan, Kunak, Sahabat, Semporna, Tungku, Bahagian Tawau (Timur)"),
        PrayerZone("SBH04", "Sabah", "Bandar Tawau, Balong, Merotai, Kalabakan, Bahagian Tawau (Barat)"),
        PrayerZone("SBH05", "Sabah", "Kudat, Kota Marudu, Pitas, Pulau Banggi, Bahagian Kudat"),
        PrayerZone("SBH06", "Sabah", "Gunung Kinabalu"),
        PrayerZone("SBH07", "Sabah", "Kota Kinabalu, Ranau, Kota Belud, Tuaran, Penampang, Papar, Putatan, Bahagian Pantai Barat"),
        PrayerZone("SBH08", "Sabah", "Pensiangan, Keningau, Tambunan, Nabawan, Bahagian Pedalaman (Atas)"),
        PrayerZone("SBH09", "Sabah", "Beaufort, Kuala Penyu, Sipitang, Tenom, Long Pasia, Membakut, Weston, Bahagian Pedalaman (Bawah)"),

        PrayerZone("SGR01", "Selangor", "Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, Shah Alam"),
        PrayerZone("SGR02", "Selangor", "Kuala Selangor, Sabak Bernam"),
        PrayerZone("SGR03", "Selangor", "Klang, Kuala Langat"),

        PrayerZone("SWK01", "Sarawak", "Limbang, Lawas, Sundar, Trusan"),
        PrayerZone("SWK02", "Sarawak", "Miri, Niah, Bekenu, Sibuti, Marudi"),
        PrayerZone("SWK03", "Sarawak", "Pandan, Belaga, Suai, Tatau, Sebauh, Bintulu"),
        PrayerZone("SWK04", "Sarawak", "Sibu, Mukah, Dalat, Song, Igan, Oya, Balingian, Kanowit, Kapit"),
        PrayerZone("SWK05", "Sarawak", "Sarikei, Matu, Julau, Rajang, Daro, Bintangor, Belawai"),
        PrayerZone("SWK06", "Sarawak", "Lubok Antu, Sri Aman, Roban, Debak, Kabong, Lingga, Engkelili, Betong, Spaoh, Pusa, Saratok"),
        PrayerZone("SWK07", "Sarawak", "Serian, Simunjan, Samarahan, Sebuyau, Meludam"),
        PrayerZone("SWK08", "Sarawak", "Kuching, Bau, Lundu, Sematan"),
        PrayerZone("SWK09", "Sarawak", "Zon Khas (Kampung Patarikan)"),

        PrayerZone("TRG01", "Terengganu", "Kuala Terengganu, Marang, Kuala Nerus"),
        PrayerZone("TRG02", "Terengganu", "Besut, Setiu"),
        PrayerZone("TRG03", "Terengganu", "Hulu Terengganu"),
        PrayerZone("TRG04", "Terengganu", "Dungun, Kemaman"),

        PrayerZone("WLY01", "Wilayah Persekutuan", "Kuala Lumpur, Putrajaya"),
        PrayerZone("WLY02", "Wilayah Persekutuan", "Labuan")
    )

    init {
        check(all.size == 60) { "Expected 60 JAKIM prayer zones, found ${all.size}" }
        check(all.map { it.code }.distinct().size == all.size) { "Duplicate JAKIM zone code" }
    }

    fun byCode(code: String): PrayerZone = all.firstOrNull { it.code == code } ?: all.first { it.code == "WLY01" }
}
