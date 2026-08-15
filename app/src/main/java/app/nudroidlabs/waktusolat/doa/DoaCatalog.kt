package app.nudroidlabs.waktusolat.doa

data class DoaEntry(
    val id: String,
    val title: String,
    val category: String,
    val arabic: String,
    val rumi: String,
    val meaningMalay: String,
    val reference: String
)

object DoaCatalog {
    val entries: List<DoaEntry> = listOf(
        DoaEntry(
            id = "before_sleep",
            title = "Sebelum tidur",
            category = "Tidur",
            arabic = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            rumi = "Bismika Allahumma amutu wa ahya.",
            meaningMalay = "Dengan nama-Mu ya Allah, aku mati dan aku hidup.",
            reference = "Sahih al-Bukhari 6324"
        ),
        DoaEntry(
            id = "wake_up",
            title = "Bangun tidur",
            category = "Tidur",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            rumi = "Alhamdu lillahil-ladhi ahyana ba'da ma amatana wa ilaihin-nushur.",
            meaningMalay = "Segala puji bagi Allah yang menghidupkan kami setelah mematikan kami, dan kepada-Nya kebangkitan.",
            reference = "Sahih al-Bukhari 6324"
        ),
        DoaEntry(
            id = "enter_toilet",
            title = "Masuk tandas",
            category = "Kebersihan",
            arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ",
            rumi = "Allahumma inni a'udhu bika minal-khubthi wal-khaba'ith.",
            meaningMalay = "Ya Allah, aku berlindung dengan-Mu daripada keburukan dan syaitan lelaki serta perempuan.",
            reference = "Sahih al-Bukhari 6322"
        ),
        DoaEntry(
            id = "leave_toilet",
            title = "Keluar tandas",
            category = "Kebersihan",
            arabic = "غُفْرَانَكَ",
            rumi = "Ghufranak.",
            meaningMalay = "Aku memohon keampunan-Mu.",
            reference = "Sunan Abi Dawud 30"
        ),
        DoaEntry(
            id = "before_eating",
            title = "Sebelum makan",
            category = "Makan & Minum",
            arabic = "بِسْمِ اللَّهِ",
            rumi = "Bismillah.",
            meaningMalay = "Dengan nama Allah.",
            reference = "Sahih al-Bukhari 5376"
        ),
        DoaEntry(
            id = "after_eating",
            title = "Selepas makan",
            category = "Makan & Minum",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            rumi = "Alhamdulillahil-ladhi at'amani hadha wa razaqanihi min ghairi hawlin minni wa la quwwah.",
            meaningMalay = "Segala puji bagi Allah yang memberi aku makanan ini dan mengurniakannya kepadaku tanpa daya dan kekuatan daripadaku.",
            reference = "Jami' at-Tirmidhi 3458"
        ),
        DoaEntry(
            id = "leave_home",
            title = "Keluar rumah",
            category = "Rumah",
            arabic = "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            rumi = "Bismillah, tawakkaltu 'alallah, la hawla wa la quwwata illa billah.",
            meaningMalay = "Dengan nama Allah, aku bertawakal kepada Allah. Tiada daya dan kekuatan melainkan dengan Allah.",
            reference = "Sunan Abi Dawud 5095"
        ),
        DoaEntry(
            id = "enter_mosque",
            title = "Masuk masjid",
            category = "Masjid",
            arabic = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            rumi = "Allahumma-ftah li abwaba rahmatik.",
            meaningMalay = "Ya Allah, bukakanlah untukku pintu-pintu rahmat-Mu.",
            reference = "Sahih Muslim 713a"
        ),
        DoaEntry(
            id = "leave_mosque",
            title = "Keluar masjid",
            category = "Masjid",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            rumi = "Allahumma inni as'aluka min fadlik.",
            meaningMalay = "Ya Allah, sesungguhnya aku memohon kepada-Mu daripada kurniaan-Mu.",
            reference = "Sahih Muslim 713a"
        ),
        DoaEntry(
            id = "vehicle",
            title = "Menaiki kenderaan",
            category = "Perjalanan",
            arabic = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ ۝ وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ",
            rumi = "Subhanalladhi sakhkhara lana hadha wa ma kunna lahu muqrinin. Wa inna ila rabbina lamunqalibun.",
            meaningMalay = "Maha Suci Dia yang menundukkan ini untuk kami sedangkan kami tidak mampu menguasainya, dan sesungguhnya kepada Tuhan kami, kami akan kembali.",
            reference = "Al-Quran, Az-Zukhruf 43:13-14"
        ),
        DoaEntry(
            id = "parents",
            title = "Untuk ibu bapa",
            category = "Keluarga",
            arabic = "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            rumi = "Rabbir hamhuma kama rabbayani saghira.",
            meaningMalay = "Wahai Tuhanku, rahmatilah mereka berdua sebagaimana mereka mendidikku ketika kecil.",
            reference = "Al-Quran, Al-Isra' 17:24"
        ),
        DoaEntry(
            id = "knowledge",
            title = "Tambah ilmu",
            category = "Ilmu",
            arabic = "رَبِّ زِدْنِي عِلْمًا",
            rumi = "Rabbi zidni ilma.",
            meaningMalay = "Wahai Tuhanku, tambahkanlah ilmuku.",
            reference = "Al-Quran, Taha 20:114"
        ),
        DoaEntry(
            id = "world_hereafter",
            title = "Kebaikan dunia & akhirat",
            category = "Umum",
            arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            rumi = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar.",
            meaningMalay = "Wahai Tuhan kami, berilah kami kebaikan di dunia dan kebaikan di akhirat, serta peliharalah kami daripada azab neraka.",
            reference = "Al-Quran, Al-Baqarah 2:201"
        )
    )
}
