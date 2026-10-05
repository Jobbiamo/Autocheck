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
    private val marca: String? = null,
    private val profonda: Boolean = false,
    private val progresso: (String) -> Unit
) {

    private val log = StringBuilder()
    /** ID CAN usati dall'auto per il suo funzionamento: non ci si scrive mai sopra. */
    private val traffico = HashSet<String>()

    private fun cmd(c: String, timeout: Long = 3000): String {
        val r = try { invia(c, timeout) } catch (e: Exception) { "ECCEZIONE: ${e.message}" }
        if (log.length < 120000) log.append("> ").append(c).append('\n').append(r.trim().take(3000)).append("\n\n")
        return r
    }

    fun esegui(): EsitoScansione {
        log.append("Protocollo motore: ").append(proto).append(" (").append(nomeProtocollo).append(")\n")
        log.append("Marca: ").append(marca ?: "non riconosciuta").append(" · Scansione profonda: ").append(profonda).append("\n")
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
        val uniche = trovate.distinctBy { it.tipo.name + it.codici.joinToString { c -> c.codice } + it.indirizzo }
        return EsitoScansione(uniche.filter { it.tipo != TipoCentralina.MOTORE || it.codici.isNotEmpty() }, log.toString(), nota)
    }

    // ───────────────────────────── CAN ─────────────────────────────

    private fun ascoltaTraffico() {
        progresso("Ascolto il traffico dell'auto (2 secondi)…")
        val r = cmd("MONITOR", 2500)
        for (riga in r.split('\n', '\r')) {
            val t = riga.trim().uppercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (t.size < 2 || t.any { !it.all { c -> c in '0'..'9' || c in 'A'..'F' } }) continue
            if (t[0].length == 3) traffico.add(t[0])
            else if (t.size >= 5 && t.take(4).all { it.length == 2 }) traffico.add(t.take(4).joinToString(""))
        }
        if (traffico.isNotEmpty()) log.append("ID di traffico esclusi: ").append(traffico.sorted().joinToString(" ")).append("\n\n")
    }

    private fun scansioneCan(bit29: Boolean): List<Centralina> {
        cmd("ATH1"); cmd("ATS1"); cmd("ATCAF1"); cmd("ATAT0"); cmd("ATST64")
        ascoltaTraffico()

        // 1) scoperta funzionale
        progresso("Cerco le centraline presenti…")
        val rispondenti = LinkedHashSet<String>()
        if (bit29) {
            cmd("ATSH18DB33F1"); cmd("ATCF18DAF100"); cmd("ATCM1FFFFF00")
        } else {
            cmd("ATSH7DF"); cmd("ATCF600"); cmd("ATCM600")
        }
        for (richiesta in listOf("3E00", "1902FF")) {
            for (m in IsoTp.riassembla(cmd(richiesta, 4000))) {
                val sid = m.data.firstOrNull() ?: continue
                if (m.id !in traffico && (sid == 0x7E || sid == 0x59 || sid == 0x7F)) rispondenti.add(m.id)
            }
        }

        // 2) coppie richiesta -> risposta da provare
        val coppie = LinkedHashMap<String, String>()
        val suggerimenti = HashMap<String, TipoCentralina>()
        for (c in Marche.indirizzi(marca)) {
            if ((c.richiesta.length == 8) == bit29) {
                coppie[c.richiesta] = c.risposta
                c.tipo?.let { suggerimenti[c.richiesta] = it }
            }
        }
        if (bit29) {
            for (r in rispondenti) if (r.length == 8 && r.startsWith("18DAF1")) coppie.putIfAbsent("18DA${r.substring(6)}F1", r)
            for (t in listOf("10", "11", "18", "28", "29", "2A", "30", "40", "58", "60", "70")) coppie.putIfAbsent("18DA${t}F1", "18DAF1$t")
        } else {
            for (r in rispondenti) if (r.length == 3) {
                val v = r.toInt(16)
                val req = if (v in 0x600..0x6FF) v - 0x400 else v - 8
                if (req > 0) coppie.putIfAbsent(hex3(req), r)
            }
            val tipici = (0x7E0..0x7E7) + listOf(0x7B0, 0x7B1, 0x7B2, 0x7B6, 0x7C0, 0x7C4, 0x7D0, 0x7D1, 0x7D2, 0x760, 0x740, 0x750, 0x720, 0x730)
            for (q in tipici) coppie.putIfAbsent(hex3(q), hex3(q + 8))
            for (q in listOf(0x241, 0x243, 0x244, 0x247)) coppie.putIfAbsent(hex3(q), hex3(q + 0x400)) // GMLAN
        }

        // 3) scansione profonda: prova tutti gli indirizzi diagnostici e annota chi risponde
        if (profonda) coppie.putAll(scopriConSweep(bit29, coppie.keys))

        // mai scrivere su un ID che l'auto usa per il suo funzionamento
        coppie.keys.removeAll { it in traffico }

        val out = ArrayList<Centralina>()
        var n = 0
        for ((req, rsp) in coppie) {
            n++
            if (!bit29 && req == "7E0" && rsp == "7E8") continue // il motore è già letto dal controllo standard
            if (bit29 && req == "18DA10F1") continue
            progresso("Interrogo la centralina $req ($n di ${coppie.size})…")
            cmd("ATSH$req"); cmd("ATCRA$rsp")
            cmd("ATFCSH$req"); cmd("ATFCSD300000"); cmd("ATFCSM1")
            interrogaCan(req, rsp, rsp in rispondenti, suggerimenti[req])?.let { out.add(it) }
        }
        cmd("ATCRA"); cmd("ATFCSM0")
        return out
    }

    /** Invia un "ci sei?" (3E 00) a ogni indirizzo diagnostico e registra chi risponde e da quale ID. */
    private fun scopriConSweep(bit29: Boolean, gia: Set<String>): Map<String, String> {
        val trovate = LinkedHashMap<String, String>()
        cmd("ATST19")
        val richieste: List<String> = if (bit29) {
            cmd("ATCF18DAF100"); cmd("ATCM1FFFFF00")
            (0x00..0xFF).filter { it != 0xF1 && it != 0x33 }.map { String.format("18DA%02XF1", it) }
        } else {
            cmd("ATCF600"); cmd("ATCM600")
            (0x600..0x7FF).filter { it != 0x7DF && it !in 0x7E8..0x7EF }.map { hex3(it) }
        }
        val daProvare = richieste.filter { it !in gia && it !in traffico }
        daProvare.forEachIndexed { i, req ->
            if (i % 16 == 0) progresso("Scansione profonda: ${i * 100 / daProvare.size}% (indirizzo $req)…")
            cmd("ATSH$req")
            // valida solo risposte vere a "3E 00": 7E 00 (positiva) o 7F 3E xx (negativa), mai traffico normale
            val ids = IsoTp.riassembla(cmd("3E00", 1500))
                .filter { it.id !in traffico && it.data.isNotEmpty() && (it.data[0] == 0x7E || (it.data[0] == 0x7F && it.data.size >= 2 && it.data[1] == 0x3E)) }
                .map { it.id }.distinct()
            // un solo rispondente = indirizzo fisico; più rispondenti = indirizzo "a tutti", da ignorare
            if (ids.size == 1 && ids[0] != req) trovate[req] = ids[0]
        }
        cmd("ATST64")
        log.append("Sweep: trovate ").append(trovate.size).append(" centraline: ").append(trovate.entries.joinToString { "${it.key}->${it.value}" }).append("\n\n")
        return trovate
    }

    private fun interrogaCan(req: String, rsp: String, scoperta: Boolean, suggerito: TipoCentralina?): Centralina? {
        var esiste = scoperta
        var msgs = messaggiDa(cmd("1902FF", 4000), rsp)
        if (attesa(msgs)) { cmd("ATSTFF"); msgs = messaggiDa(cmd("1902FF", 6000), rsp); cmd("ATST64") }
        msgs.firstOrNull { it.data.isNotEmpty() && it.data[0] == 0x59 }?.let {
            val codici = decodificaUds(it.data)
            val tipo = suggerito ?: tipoPer(req, codici)
            return Centralina(nomeDi(tipo, req), tipo, req, codici, "UDS")
        }
        if (msgs.isNotEmpty()) esiste = true
        if (!esiste) return null

        for (richiesta in listOf("1802FF00", "1800FF00", "13")) {
            val m = messaggiDa(cmd(richiesta, 4000), rsp)
            m.firstOrNull { it.data.isNotEmpty() && (it.data[0] == 0x58 || it.data[0] == 0x53) }?.let {
                val codici = decodificaKwp(it.data)
                val tipo = suggerito ?: tipoPer(req, codici)
                return Centralina(nomeDi(tipo, req), tipo, req, codici, "KWP")
            }
        }
        val tipo = suggerito ?: tipoPer(req, emptyList())
        return Centralina(nomeDi(tipo, req), tipo, req, emptyList(), "Risponde, ma non ha accettato le richieste di lettura errori")
    }

    private fun nomeDi(t: TipoCentralina, req: String) =
        if (t == TipoCentralina.SCONOSCIUTA) "Centralina all'indirizzo $req" else t.nome

    private fun messaggiDa(r: String, rsp: String) = IsoTp.riassembla(r).filter { it.id == rsp }

    private fun attesa(m: List<IsoTp.Msg>) = m.any { it.data.size >= 3 && it.data[0] == 0x7F && it.data[2] == 0x78 }

    // ─────────────────────────── LINEA K ───────────────────────────

    private fun scansioneLineaK(): List<Centralina> {
        val out = ArrayList<Centralina>()
        cmd("ATH0"); cmd("ATS1"); cmd("ATAT1")
        cmd("ATSP5")
        val base = listOf("28" to TipoCentralina.ABS_ESP, "29" to TipoCentralina.ABS_ESP, "58" to TipoCentralina.AIRBAG,
            "18" to TipoCentralina.CAMBIO, "40" to TipoCentralina.CARROZZERIA, "60" to TipoCentralina.QUADRO)
        val extra = if (!profonda) emptyList() else listOf("2A", "2B", "2C", "2D", "2E", "2F", "59", "5A", "19", "1A", "41", "61", "30", "31", "70", "57")
            .map { it to TipoCentralina.SCONOSCIUTA }
        var guastiInit = 0
        for ((a, tipo) in base + extra) {
            progresso("Provo la linea K: indirizzo $a…")
            cmd("ATPC")
            cmd("ATSH81${a}F1")
            var r = cmd("1802FF00", 9000)
            val u = r.uppercase()
            if (u.contains("BUS INIT") || u.contains("UNABLE") || u.contains("ERROR")) {
                guastiInit++
                if (guastiInit >= 3 && out.isEmpty() && !profonda) break
                if (guastiInit >= 6 && out.isEmpty()) break
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
                val t = if (tipo == TipoCentralina.SCONOSCIUTA) tipoPer("K-$a", codici) else tipo
                out.add(Centralina(nomeDi(t, "K-$a"), t, "K-$a", codici, "KWP linea K"))
            } else if (dati.isNotEmpty()) {
                out.add(Centralina(nomeDi(tipo, "K-$a"), tipo, "K-$a", emptyList(), "Risponde, ma non ha accettato le richieste di lettura errori"))
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
