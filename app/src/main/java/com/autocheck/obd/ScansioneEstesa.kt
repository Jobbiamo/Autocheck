package com.autocheck.obd

/** Una centralina diversa da quella motore (ABS/ESP, airbag, cambio...). */
data class Centralina(
    val nome: String,
    val tipo: TipoCentralina,
    val indirizzo: String,
    val codici: List<CodiceCentralina>,
    val nota: String
)

/** Codice letto da un'altra centralina. */
data class CodiceCentralina(val codice: String, val dettaglio: String?, val inOsservazione: Boolean)

enum class TipoCentralina(val nome: String) {
    MOTORE("Motore"), CAMBIO("Cambio"), ABS_ESP("ABS / ESP (freni e stabilità)"),
    AIRBAG("Airbag"), CARROZZERIA("Carrozzeria"), QUADRO("Quadro strumenti"),
    STERZO("Servosterzo"), SCONOSCIUTA("Altra centralina")
}

data class EsitoScansione(
    val centraline: List<Centralina>,
    val log: String,
    val nota: String?
)

/**
 * Scansione "best effort" delle altre centraline dell'auto.
 * Lo standard OBD obbliga a rendere leggibile solo la centralina motore:
 * qui si provano gli indirizzi e i comandi diagnostici più diffusi
 * (UDS 19 su CAN, KWP2000 18/13 su CAN e su linea K).
 * Tutto in sola lettura.
 */
class ScansioneEstesa(
    private val invia: (String, Long) -> String,
    private val proto: Int,
    private val nomeProtocollo: String,
    private val progresso: (String) -> Unit
) {

    private val log = StringBuilder()

    private fun cmd(c: String, timeout: Long = 3000): String {
        val r = try { invia(c, timeout) } catch (e: Exception) { "ECCEZIONE: ${e.message}" }
        if (log.length < 60000) log.append("> ").append(c).append('\n').append(r.trim()).append("\n\n")
        return r
    }

    fun esegui(): EsitoScansione {
        log.append("Protocollo motore: ").append(proto).append(" (").append(nomeProtocollo).append(")\n")
        log.append("Versione adattatore: ").append(cmd("ATI").trim()).append("\n\n")

        val trovate = ArrayList<Centralina>()
        var nota: String? = null
        try {
            when (proto) {
                6, 8 -> trovate.addAll(scansioneCan(false))
                7, 9 -> trovate.addAll(scansioneCan(true))
            }
            trovate.addAll(scansioneLineaK())
        } catch (e: Exception) {
            nota = "Scansione estesa interrotta: ${e.message}"
            log.append("ERRORE: ").append(e.message).append('\n')
        } finally {
            ripristina(proto)
        }
        // Una centralina può rispondere sia su CAN sia su linea K: tieni la prima
        val uniche = trovate.distinctBy { it.tipo.name + it.codici.joinToString { c -> c.codice } + it.indirizzo }
        return EsitoScansione(uniche.filter { it.tipo != TipoCentralina.MOTORE || it.codici.isNotEmpty() }, log.toString(), nota)
    }

    // ───────────────────────────── CAN ─────────────────────────────

    private fun scansioneCan(bit29: Boolean): List<Centralina> {
        cmd("ATH1"); cmd("ATS1"); cmd("ATCAF1"); cmd("ATAT0"); cmd("ATST64")

        // 1) scoperta: chi risponde a una richiesta "funzionale" (a tutti)?
        progresso("Cerco le centraline presenti…")
        val rispondenti = LinkedHashSet<String>()
        if (bit29) {
            cmd("ATSH18DB33F1"); cmd("ATCF18DAF100"); cmd("ATCM1FFFFF00")
        } else {
            cmd("ATSH7DF"); cmd("ATCF700"); cmd("ATCM700")
        }
        for (richiesta in listOf("3E00", "1902FF")) {
            for (m in IsoTp.riassembla(cmd(richiesta, 4000))) rispondenti.add(m.id)
        }
        cmd("ATCRA")

        // 2) interrogazione fisica: indirizzi scoperti + indirizzi tipici
        val coppie = LinkedHashMap<String, String>() // richiesta -> risposta
        if (bit29) {
            for (r in rispondenti) if (r.length == 8 && r.startsWith("18DAF1")) {
                val t = r.substring(6); coppie["18DA${t}F1"] = r
            }
            for (t in listOf("10", "11", "18", "28", "29", "2A", "30", "40", "58", "60", "70")) coppie["18DA${t}F1"] = "18DAF1$t"
        } else {
            for (r in rispondenti) if (r.length == 3) {
                val v = r.toInt(16)
                val req = if (v in 0x600..0x6FF) v - 0x400 else v - 8
                if (req > 0) coppie[hex3(req)] = r
            }
            val tipici = (0x7E0..0x7E7) + listOf(0x7B0, 0x7B1, 0x7B2, 0x7B6, 0x7C0, 0x7C4, 0x7D0, 0x7D1, 0x7D2, 0x760, 0x740, 0x750, 0x720, 0x730)
            for (q in tipici) coppie[hex3(q)] = hex3(q + 8)
            for (q in listOf(0x241, 0x243, 0x244, 0x247)) coppie[hex3(q)] = hex3(q + 0x400) // GMLAN
        }

        val out = ArrayList<Centralina>()
        for ((req, rsp) in coppie) {
            if (!bit29 && req == "7E0" && rsp == "7E8") continue // il motore è già letto dal controllo standard
            progresso("Interrogo la centralina $req…")
            cmd("ATSH$req"); cmd("ATCRA$rsp")
            cmd("ATFCSH$req"); cmd("ATFCSD300000"); cmd("ATFCSM1")
            interrogaCan(req, rsp, rsp in rispondenti)?.let { out.add(it) }
        }
        cmd("ATCRA"); cmd("ATFCSM0")
        return out
    }

    private fun interrogaCan(req: String, rsp: String, scoperta: Boolean): Centralina? {
        var esiste = scoperta
        // UDS: 19 02 FF = codici con stato
        var msgs = messaggiDa(cmd("1902FF", 4000), rsp)
        if (attesa(msgs)) { cmd("ATSTFF"); msgs = messaggiDa(cmd("1902FF", 6000), rsp); cmd("ATST64") }
        msgs.firstOrNull { it.data.isNotEmpty() && it.data[0] == 0x59 }?.let {
            return Centralina(nomePer(req, it.dtc()), tipoPer(req, decodificaUds(it.data)), req, decodificaUds(it.data), "UDS")
        }
        if (msgs.isNotEmpty()) esiste = true
        if (!esiste) return null

        // KWP2000 su CAN
        for (richiesta in listOf("1802FF00", "1800FF00", "13")) {
            val m = messaggiDa(cmd(richiesta, 4000), rsp)
            m.firstOrNull { it.data.isNotEmpty() && (it.data[0] == 0x58 || it.data[0] == 0x53) }?.let {
                val codici = decodificaKwp(it.data)
                return Centralina(nomePer(req, codici), tipoPer(req, codici), req, codici, "KWP")
            }
        }
        return Centralina(nomePer(req, emptyList()), tipoPer(req, emptyList()), req, emptyList(),
            "Risponde, ma non ha accettato le richieste di lettura errori")
    }

    private fun messaggiDa(r: String, rsp: String) = IsoTp.riassembla(r).filter { it.id == rsp }

    /** Risposta negativa 78 = "sto elaborando, aspetta". */
    private fun attesa(m: List<IsoTp.Msg>) = m.any { it.data.size >= 3 && it.data[0] == 0x7F && it.data[2] == 0x78 }

    private fun IsoTp.Msg.dtc() = if (data.isNotEmpty() && data[0] == 0x59) decodificaUds(data) else emptyList()

    // ─────────────────────────── LINEA K ───────────────────────────

    private fun scansioneLineaK(): List<Centralina> {
        val out = ArrayList<Centralina>()
        cmd("ATH0"); cmd("ATS1"); cmd("ATAT1")
        cmd("ATSP5")
        val indirizzi = listOf("28" to TipoCentralina.ABS_ESP, "29" to TipoCentralina.ABS_ESP, "58" to TipoCentralina.AIRBAG,
            "18" to TipoCentralina.CAMBIO, "40" to TipoCentralina.CARROZZERIA, "60" to TipoCentralina.QUADRO)
        var guastiInit = 0
        for ((a, tipo) in indirizzi) {
            progresso("Provo la linea K: ${tipo.nome}…")
            cmd("ATPC")
            cmd("ATSH81${a}F1")
            var r = cmd("1802FF00", 9000)
            val u = r.uppercase()
            if (u.contains("BUS INIT") || u.contains("UNABLE") || u.contains("ERROR")) {
                guastiInit++
                // nessuna linea K collegata: inutile insistere con tutti gli indirizzi
                if (guastiInit >= 3 && out.isEmpty()) break
                continue
            }
            var dati = righeDati(r)
            if (dati.none { it.firstOrNull() == 0x58 }) {
                r = cmd("13", 6000)
                dati = righeDati(r)
            }
            val riga58 = dati.firstOrNull { it.firstOrNull() == 0x58 || it.firstOrNull() == 0x53 }
            if (riga58 != null) {
                val codici = dati.filter { it.firstOrNull() == riga58[0] }.flatMap { decodificaKwp(it) }.distinctBy { it.codice }
                out.add(Centralina(tipo.nome, tipo, "K-$a", codici, "KWP linea K"))
            } else if (dati.isNotEmpty()) {
                out.add(Centralina(tipo.nome, tipo, "K-$a", emptyList(), "Risponde, ma non ha accettato le richieste di lettura errori"))
            }
        }
        cmd("ATPC")
        return out
    }

    private fun righeDati(r: String): List<IntArray> =
        r.split('\n', '\r').mapNotNull { riga ->
            val t = riga.trim().uppercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (t.isEmpty() || t.any { it.length != 2 || !it.all { c -> c in '0'..'9' || c in 'A'..'F' } }) null
            else t.map { it.toInt(16) }.toIntArray()
        }

    // ─────────────────────────── RIPRISTINO ───────────────────────────

    private fun ripristina(proto: Int) {
        cmd("ATPC")
        cmd("ATSP$proto")
        cmd("ATH0"); cmd("ATS0"); cmd("ATCAF1"); cmd("ATCRA"); cmd("ATFCSM0"); cmd("ATAT1"); cmd("ATST32")
        when (proto) {
            6, 8 -> cmd("ATSH7DF")
            7, 9 -> cmd("ATSH18DB33F1")
            3 -> cmd("ATSH686AF1")
            4, 5 -> cmd("ATSHC133F1")
        }
        progresso("Ricollego la centralina motore…")
        cmd("0100", 20000)
    }

    companion object {
        fun hex3(v: Int) = String.format("%03X", v)

        /** Risposta UDS 59 02 <maschera> poi gruppi di 4 byte: DTC(3) + stato. */
        fun decodificaUds(d: IntArray): List<CodiceCentralina> {
            val out = ArrayList<CodiceCentralina>()
            var i = 3
            while (i + 3 < d.size) {
                val codice = ObdParser.decodificaDtc(String.format("%02X%02X", d[i], d[i + 1]))
                val ftb = d[i + 2]
                val stato = d[i + 3]
                val confermato = stato and 0x08 != 0
                val inCorso = stato and 0x01 != 0
                val pendente = stato and 0x04 != 0
                if ((confermato || inCorso || pendente) && !(d[i] == 0 && d[i + 1] == 0)) {
                    out.add(CodiceCentralina(codice, if (ftb != 0) String.format("sottotipo %02X", ftb) else null,
                        !confermato && !inCorso))
                }
                i += 4
            }
            return out.distinctBy { it.codice + it.dettaglio }
        }

        /** Risposta KWP 58 <numero> poi gruppi di 3 byte (DTC(2) + stato) oppure 53 con coppie di byte. */
        fun decodificaKwp(d: IntArray): List<CodiceCentralina> {
            val out = ArrayList<CodiceCentralina>()
            if (d.isEmpty()) return out
            if (d[0] == 0x58) {
                var i = 2
                while (i + 2 < d.size) {
                    if (!(d[i] == 0 && d[i + 1] == 0)) {
                        out.add(CodiceCentralina(ObdParser.decodificaDtc(String.format("%02X%02X", d[i], d[i + 1])), null, false))
                    }
                    i += 3
                }
            } else if (d[0] == 0x53) {
                var i = if ((d.size - 1) % 2 == 1) 2 else 1
                while (i + 1 < d.size) {
                    if (!(d[i] == 0 && d[i + 1] == 0)) {
                        out.add(CodiceCentralina(ObdParser.decodificaDtc(String.format("%02X%02X", d[i], d[i + 1])), null, false))
                    }
                    i += 2
                }
            }
            return out.distinctBy { it.codice }
        }

        fun tipoPer(indirizzo: String, codici: List<CodiceCentralina>): TipoCentralina {
            when (indirizzo) {
                "7E0", "241" -> return TipoCentralina.MOTORE
                "7E1" -> return TipoCentralina.CAMBIO
                "243", "760", "7B0" -> return TipoCentralina.ABS_ESP
                "247" -> return TipoCentralina.AIRBAG
                "720", "7C0" -> return TipoCentralina.QUADRO
                "244", "740" -> return TipoCentralina.CARROZZERIA
            }
            if (indirizzo.startsWith("18DA") && indirizzo.length == 8) {
                when (indirizzo.substring(4, 6)) {
                    "10", "11" -> return TipoCentralina.MOTORE
                    "18" -> return TipoCentralina.CAMBIO
                    "28", "29", "2A" -> return TipoCentralina.ABS_ESP
                    "58" -> return TipoCentralina.AIRBAG
                    "40" -> return TipoCentralina.CARROZZERIA
                    "60" -> return TipoCentralina.QUADRO
                    "30" -> return TipoCentralina.STERZO
                }
            }
            val c = codici.map { it.codice.first() }
            return when {
                c.count { it == 'C' } > 0 && c.count { it == 'C' } >= c.count { it == 'B' } -> TipoCentralina.ABS_ESP
                c.any { it == 'B' } -> TipoCentralina.CARROZZERIA
                c.isNotEmpty() && c.all { it == 'P' } -> TipoCentralina.MOTORE
                else -> TipoCentralina.SCONOSCIUTA
            }
        }

        fun nomePer(indirizzo: String, codici: List<CodiceCentralina>): String {
            val t = tipoPer(indirizzo, codici)
            return if (t == TipoCentralina.SCONOSCIUTA) "Centralina all'indirizzo $indirizzo" else t.nome
        }
    }
}

/** Ricompone i messaggi CAN (ISO-TP) stampati dall'ELM327 con intestazioni attive (ATH1 ATS1). */
object IsoTp {
    class Msg(val id: String, val data: IntArray)

    private val HEX = Regex("^[0-9A-F]+$")

    fun riassembla(risposta: String): List<Msg> {
        val parziali = LinkedHashMap<String, Pair<Int, MutableList<Int>>>()
        val out = ArrayList<Msg>()
        for (raw in risposta.split('\n', '\r')) {
            val t = raw.trim().uppercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (t.size < 2 || t.any { !HEX.matches(it) }) continue
            val id: String
            val resto: List<String>
            if (t[0].length == 3) {
                id = t[0]; resto = t.drop(1)
            } else if (t.size >= 6 && t.take(4).all { it.length == 2 }) {
                id = t.take(4).joinToString(""); resto = t.drop(4)
            } else continue
            if (resto.isEmpty() || resto.any { it.length != 2 }) continue
            val b = resto.map { it.toInt(16) }
            when (b[0] shr 4) {
                0 -> {
                    val n = b[0] and 0x0F
                    if (n > 0 && b.size >= 1 + n) out.add(Msg(id, b.subList(1, 1 + n).toIntArray()))
                }
                1 -> if (b.size >= 2) {
                    val n = ((b[0] and 0x0F) shl 8) or b[1]
                    parziali[id] = n to b.drop(2).toMutableList()
                }
                2 -> {
                    val p = parziali[id] ?: continue
                    p.second.addAll(b.drop(1))
                    if (p.second.size >= p.first) {
                        out.add(Msg(id, p.second.take(p.first).toIntArray()))
                        parziali.remove(id)
                    }
                }
            }
        }
        for ((id, p) in parziali) if (p.second.isNotEmpty()) out.add(Msg(id, p.second.toIntArray()))
        return out
    }
}
