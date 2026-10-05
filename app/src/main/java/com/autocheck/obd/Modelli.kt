package com.autocheck.obd

/** Gravità di un problema, mostrata come semaforo. */
enum class Gravita(val livello: Int, val emoji: String, val etichetta: String, val colore: Int) {
    VERDE(0, "🟢", "Puoi guidare tranquillo", 0xFF2E7D32.toInt()),
    ARANCIONE(1, "🟠", "Puoi guidare con prudenza, ma vai dal meccanico a breve", 0xFFE07B00.toInt()),
    ROSSO(2, "🔴", "Fermati: non usare l'auto finché non è controllata", 0xFFC62828.toInt());
}

/**
 * Scheda di diagnosi pensata per chi non è meccanico:
 * cosa significa, se si può guidare, cause, cosa fare, costi e conseguenze.
 */
data class Scheda(
    val codice: String?,
    val titolo: String,
    val spiegazione: String,
    val gravita: Gravita,
    val guida: String,
    val cause: List<String>,
    val faiDaTe: List<String>,
    val meccanico: String,
    val costo: String,
    val seIgnori: String,
    val nota: String? = null
)

/** Dati grezzi letti dalla centralina. */
data class Letture(
    val memorizzati: List<String>,
    val pendenti: List<String>,
    val permanenti: List<String>,
    val stato: IntArray?,
    val valori: Map<String, Double>,
    val protocollo: String,
    val vin: String? = null
)

/** Una voce di dati motore già tradotta in parole. */
data class DatoMotore(
    val nome: String,
    val valore: String,
    val stato: String,
    val gravita: Gravita
)

/** Rapporto completo di un controllo. */
data class Rapporto(
    val quando: Long,
    val km: Int?,
    val verdetto: String,
    val gravita: Gravita?,
    val schede: List<Scheda>,
    val avvisi: List<String>,
    val datiMotore: List<DatoMotore>,
    val codici: List<String>,
    val spiaAccesa: Boolean,
    val kmDaCancellazione: Int?,
    val protocollo: String,
    val spie: List<String> = emptyList(),
    val centraline: List<Centralina>? = null,
    val notaScansione: String? = null,
    val logTecnico: String? = null,
    val vin: String? = null,
    val marca: String? = null
)
