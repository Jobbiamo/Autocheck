package com.autocheck.obd

/** Riconoscimento della marca dal numero di telaio (VIN) e indirizzi da provare per primi. */
object Marche {

    /** Prime 3 lettere del VIN (WMI) -> marca. Elenco dei costruttori più diffusi in Europa. */
    private val WMI = mapOf(
        "ZFA" to "Fiat", "ZFB" to "Fiat", "ZFC" to "Fiat", "ZAR" to "Alfa Romeo", "ZLA" to "Lancia", "ZAC" to "Jeep",
        "WVW" to "Volkswagen", "WV1" to "Volkswagen", "WV2" to "Volkswagen", "WV3" to "Volkswagen", "WAU" to "Audi", "WUA" to "Audi",
        "TMB" to "Skoda", "VSS" to "Seat", "W0L" to "Opel", "W0V" to "Opel", "VXK" to "Opel", "W0S" to "Opel",
        "VF1" to "Renault", "VF6" to "Renault", "UU1" to "Dacia", "VF3" to "Peugeot", "VF7" to "Citroen", "VR3" to "Peugeot", "VR7" to "Citroen",
        "WF0" to "Ford", "WF1" to "Ford", "1FA" to "Ford", "WBA" to "BMW", "WBS" to "BMW", "WMW" to "Mini",
        "WDD" to "Mercedes-Benz", "WDB" to "Mercedes-Benz", "W1K" to "Mercedes-Benz", "WME" to "Smart", "YV1" to "Volvo",
        "SB1" to "Toyota", "VNK" to "Toyota", "NMT" to "Toyota",
        "KMH" to "Hyundai", "TMA" to "Hyundai", "NLH" to "Hyundai", "KNA" to "Kia", "KNE" to "Kia", "U5Y" to "Kia",
        "SJN" to "Nissan", "VSK" to "Nissan", "VWA" to "Nissan", "JMZ" to "Mazda", "SHH" to "Honda", "SHS" to "Honda",
        "JMB" to "Mitsubishi", "XMC" to "Mitsubishi", "JF1" to "Subaru", "JSA" to "Suzuki", "JS2" to "Suzuki", "TSM" to "Suzuki",
        "MA3" to "Suzuki", "VSE" to "Suzuki", "5YJ" to "Tesla", "LRW" to "Tesla", "XP7" to "Tesla", "SAL" to "Land Rover", "SAJ" to "Jaguar"
    )

    /** Prefissi per costruttori con molti WMI (Toyota, Honda, Nissan, Mazda giapponesi). */
    private val PREFISSI = listOf("JT" to "Toyota", "JH" to "Honda", "JN" to "Nissan", "JM" to "Mazda", "JS" to "Suzuki", "KN" to "Kia", "KM" to "Hyundai")

    val ELENCO = listOf("Alfa Romeo", "Audi", "BMW", "Citroen", "Cupra", "Dacia", "Fiat", "Ford", "Honda", "Hyundai", "Jeep", "Kia",
        "Lancia", "Land Rover", "Mazda", "Mercedes-Benz", "Mini", "Mitsubishi", "Nissan", "Opel", "Peugeot", "Renault", "Seat", "Skoda",
        "Smart", "Subaru", "Suzuki", "Tesla", "Toyota", "Volkswagen", "Volvo")

    fun daVin(vin: String?): String? {
        if (vin == null || vin.length < 3) return null
        val w = vin.substring(0, 3).uppercase()
        WMI[w]?.let { return it }
        return PREFISSI.firstOrNull { w.startsWith(it.first) }?.second
    }

    /** Gruppi che condividono l'elettronica: le tabelle si sommano. */
    private fun famiglia(marca: String): List<String> = when (marca) {
        "Volkswagen", "Audi", "Skoda", "Seat", "Cupra" -> listOf("Volkswagen", "Audi", "Seat", "Cupra")
        "Peugeot", "Citroen", "DS" -> listOf("Peugeot", "Opel")
        "Opel" -> listOf("Opel", "Peugeot")
        "Fiat", "Alfa Romeo", "Lancia", "Jeep" -> listOf("Fiat", "Jeep")
        "Renault", "Dacia" -> listOf("Renault", "Dacia", "Nissan")
        "Nissan" -> listOf("Nissan", "Renault")
        "Hyundai", "Kia" -> listOf("Hyundai", "Kia")
        "BMW", "Mini" -> listOf("BMW", "Mini")
        "Toyota" -> listOf("Toyota")
        "Land Rover", "Jaguar" -> listOf("Land-Rover")
        else -> listOf(marca)
    }

    /** Indirizzi noti (da OBDb e dalle convenzioni dei costruttori) per la marca. */
    fun indirizzi(marca: String?): List<TabellaIndirizzi.C> {
        if (marca == null) return emptyList()
        val base = famiglia(marca).flatMap { TabellaIndirizzi.PER_MARCA[it].orEmpty() }
        val extra = when (marca) {
            // Convenzioni note: gruppo VW indirizzo 03 = ABS (713/77D), 15 = airbag (715/77F)
            "Volkswagen", "Audi", "Skoda", "Seat", "Cupra" -> listOf(
                TabellaIndirizzi.C("713", "77D", TipoCentralina.ABS_ESP), TabellaIndirizzi.C("715", "77F", TipoCentralina.AIRBAG))
            "Ford" -> listOf(TabellaIndirizzi.C("760", "768", TipoCentralina.ABS_ESP), TabellaIndirizzi.C("737", "73F", null))
            "Hyundai", "Kia" -> listOf(TabellaIndirizzi.C("7D1", "7D9", TipoCentralina.ABS_ESP), TabellaIndirizzi.C("7D2", "7DA", null))
            "Toyota", "Mazda", "Subaru" -> listOf(TabellaIndirizzi.C("7B0", "7B8", TipoCentralina.ABS_ESP))
            "Fiat", "Alfa Romeo", "Lancia", "Jeep" -> listOf(TabellaIndirizzi.C("18DA28F1", "18DAF128", null),
                TabellaIndirizzi.C("18DA58F1", "18DAF158", null))
            else -> emptyList()
        }
        // le voci con tipo noto hanno la precedenza
        return (extra + base).distinctBy { it.richiesta + it.risposta }
    }
}
