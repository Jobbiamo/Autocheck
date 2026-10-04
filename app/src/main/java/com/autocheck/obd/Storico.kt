package com.autocheck.obd

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Un controllo salvato nello storico. */
data class Controllo(
    val quando: Long,
    val km: Int?,
    val verdetto: String,
    val livello: Int,
    val codici: List<String>,
    val kmDaCancellazione: Int?,
    val testo: String
)

/** Storico dei controlli, salvato SOLO nella memoria interna del telefono. */
class Storico(context: Context) {

    private val file = File(context.filesDir, "storico.json")

    @Synchronized
    fun lista(): List<Controllo> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val cod = o.optJSONArray("codici") ?: JSONArray()
                Controllo(
                    quando = o.getLong("quando"),
                    km = if (o.has("km")) o.getInt("km") else null,
                    verdetto = o.optString("verdetto"),
                    livello = o.optInt("livello", -1),
                    codici = (0 until cod.length()).map { cod.getString(it) },
                    kmDaCancellazione = if (o.has("kmCanc")) o.getInt("kmCanc") else null,
                    testo = o.optString("testo")
                )
            }.sortedByDescending { it.quando }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun salva(r: Rapporto) {
        val tutti = lista().toMutableList()
        tutti.add(0, Controllo(r.quando, r.km, r.verdetto, r.gravita?.livello ?: -1, r.codici,
            r.kmDaCancellazione, Analizzatore.testo(r)))
        scrivi(tutti)
    }

    @Synchronized
    fun elimina(quando: Long) = scrivi(lista().filter { it.quando != quando })

    @Synchronized
    fun cancellaTutto() {
        file.delete()
    }

    private fun scrivi(lista: List<Controllo>) {
        val arr = JSONArray()
        for (c in lista) {
            val o = JSONObject()
            o.put("quando", c.quando)
            c.km?.let { o.put("km", it) }
            o.put("verdetto", c.verdetto)
            o.put("livello", c.livello)
            o.put("codici", JSONArray(c.codici))
            c.kmDaCancellazione?.let { o.put("kmCanc", it) }
            o.put("testo", c.testo)
            arr.put(o)
        }
        val tmp = File(file.parentFile, "storico.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }
}
