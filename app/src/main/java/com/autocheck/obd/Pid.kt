package com.autocheck.obd

/** Un valore leggibile dalla centralina (modo 01) o dall'adattatore (ATRV). */
class Pid(
    val chiave: String,
    val nome: String,
    val comando: String,
    val unita: String,
    val decimali: Int,
    val byteMinimi: Int,
    val formula: (IntArray) -> Double
)

object Pids {
    val GIRI = Pid("giri", "Giri motore", "010C", "giri/min", 0, 2) { (it[0] * 256 + it[1]) / 4.0 }
    val VELOCITA = Pid("velocita", "Velocità", "010D", "km/h", 0, 1) { it[0].toDouble() }
    val TEMP_LIQUIDO = Pid("temp_liquido", "Temperatura motore", "0105", "°C", 0, 1) { it[0] - 40.0 }
    val TENSIONE = Pid("tensione", "Tensione batteria", "ATRV", "V", 1, 0) { 0.0 }
    val CARICO = Pid("carico", "Sforzo del motore", "0104", "%", 0, 1) { it[0] * 100.0 / 255 }
    val FARFALLA = Pid("farfalla", "Acceleratore (farfalla)", "0111", "%", 0, 1) { it[0] * 100.0 / 255 }
    val TEMP_ARIA = Pid("temp_aria", "Temperatura aria aspirata", "010F", "°C", 0, 1) { it[0] - 40.0 }
    val CORR_BREVE = Pid("corr_breve", "Correzione benzina (istantanea)", "0106", "%", 1, 1) { (it[0] - 128) * 100.0 / 128 }
    val CORR_LUNGA = Pid("corr_lunga", "Correzione benzina (appresa)", "0107", "%", 1, 1) { (it[0] - 128) * 100.0 / 128 }
    val PRESSIONE_ASP = Pid("map", "Pressione aspirazione", "010B", "kPa", 0, 1) { it[0].toDouble() }
    val MOTORE_ACCESO_DA = Pid("tempo_acceso", "Motore acceso da", "011F", "s", 0, 2) { it[0] * 256.0 + it[1] }
    val KM_DA_CANCELLAZIONE = Pid("km_cancellazione", "Km da ultima cancellazione errori", "0131", "km", 0, 2) { it[0] * 256.0 + it[1] }
    val KM_CON_SPIA = Pid("km_spia", "Km percorsi con spia accesa", "0121", "km", 0, 2) { it[0] * 256.0 + it[1] }

    val CONTROLLO = listOf(
        GIRI, VELOCITA, TEMP_LIQUIDO, TENSIONE, CARICO, FARFALLA, TEMP_ARIA,
        CORR_BREVE, CORR_LUNGA, PRESSIONE_ASP, MOTORE_ACCESO_DA, KM_DA_CANCELLAZIONE, KM_CON_SPIA
    )
    val LIVE = listOf(GIRI, VELOCITA, TEMP_LIQUIDO, TENSIONE, CARICO, FARFALLA, TEMP_ARIA, CORR_BREVE, CORR_LUNGA)

    fun perChiave(k: String): Pid? = CONTROLLO.firstOrNull { it.chiave == k }
}
