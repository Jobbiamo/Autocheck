package com.autocheck.obd

/** Decodifica delle risposte testuali dell'ELM327. Nessuna dipendenza da Android. */
object ObdParser {

    private val HEX = Regex("^[0-9A-F]+$")
    private val FRAME_CAN = Regex("^([0-9A-F]):([0-9A-F]+)$")

    fun righe(risposta: String): List<String> =
        risposta.split('\n', '\r')
            .map { it.replace(" ", "").trim().uppercase() }
            .filter { it.isNotEmpty() && !it.startsWith("SEARCHING") && !it.startsWith("BUSINIT") }

    fun hexToBytes(hex: String): IntArray {
        val pulito = hex.filter { it in '0'..'9' || it in 'A'..'F' }
        val n = pulito.length / 2
        return IntArray(n) { i -> pulito.substring(i * 2, i * 2 + 2).toInt(16) }
    }

    /**
     * Estrae i byte di dati di una risposta di modo 01/09.
     * Esempio: comando "010C", risposta "410C1AF8" -> [0x1A, 0xF8]
     */
    fun estraiBytes(risposta: String, comando: String): IntArray? {
        val c = comando.replace(" ", "").uppercase()
        if (c.length < 4) return null
        val modo = c.substring(0, 2).toIntOrNull(16) ?: return null
        val prefisso = String.format("%02X", modo + 0x40) + c.substring(2, 4)
        for (riga in righe(risposta)) {
            if (!HEX.matches(riga)) continue
            val idx = riga.indexOf(prefisso)
            if (idx == 0 || (idx > 0 && idx % 2 == 0)) {
                return hexToBytes(riga.substring(idx + prefisso.length))
            }
        }
        return null
    }

    /**
     * Estrae i codici errore da una risposta di modo 03 / 07 / 0A.
     * Gestisce sia i protocolli vecchi (ISO 9141 / KWP, 3 codici per riga)
     * sia il CAN (byte di conteggio, eventuali risposte su più frame).
     */
    fun estraiDtc(risposta: String, intestazione: String): List<String> {
        val linee = righe(risposta)
        val codici = LinkedHashSet<String>()

        if (linee.any { FRAME_CAN.matches(it) }) {
            // CAN multi-frame: prima riga = lunghezza, poi "0:....", "1:...."
            var lunghezza = -1
            val dati = StringBuilder()
            for (l in linee) {
                val m = FRAME_CAN.find(l)
                if (m != null) dati.append(m.groupValues[2])
                else if (HEX.matches(l) && l.length <= 3 && lunghezza < 0) lunghezza = l.toInt(16)
            }
            var s = dati.toString()
            if (lunghezza > 0 && s.length > lunghezza * 2) s = s.substring(0, lunghezza * 2)
            if (s.startsWith(intestazione)) {
                s = s.substring(2)
                if (s.length >= 2) s = s.substring(2) // byte con il numero di codici
                aggiungiCodici(s, codici)
            }
            return codici.toList()
        }

        for (l in linee) {
            if (!HEX.matches(l) || !l.startsWith(intestazione)) continue
            var d = l.substring(2)
            // CAN a frame singolo: 43 + conteggio + 2 byte per codice -> lunghezza 2 + 4n
            if (d.length % 4 == 2) d = d.substring(2)
            aggiungiCodici(d, codici)
        }
        return codici.toList()
    }

    private fun aggiungiCodici(dati: String, out: MutableSet<String>) {
        var i = 0
        while (i + 4 <= dati.length) {
            val pezzo = dati.substring(i, i + 4)
            if (pezzo != "0000") out.add(decodificaDtc(pezzo))
            i += 4
        }
    }

    /** "0301" -> "P0301", "C123" -> "U0123"... */
    fun decodificaDtc(quattroHex: String): String {
        val primo = quattroHex[0].digitToInt(16)
        val lettera = "PCBU"[primo shr 2]
        val cifra = primo and 0x3
        return "$lettera$cifra${quattroHex.substring(1)}"
    }

    /** Numero di telaio dalla risposta a 0902 (CAN multi-frame o linea K su più righe). */
    fun vin(risposta: String): String? {
        val linee = righe(risposta)
        val bytes = ArrayList<Int>()
        if (linee.any { FRAME_CAN.matches(it) }) {
            val dati = StringBuilder()
            for (l in linee) FRAME_CAN.find(l)?.let { dati.append(it.groupValues[2]) }
            val s = dati.toString()
            val i = s.indexOf("4902")
            if (i < 0 || i % 2 != 0) return null
            bytes.addAll(hexToBytes(s.substring(i + 6)).toList()) // salta 49 02 e il numero di elementi
        } else {
            for (l in linee) if (HEX.matches(l) && l.startsWith("4902") && l.length > 6) bytes.addAll(hexToBytes(l.substring(6)).toList())
        }
        val testo = bytes.filter { it in 0x30..0x5A }.map { it.toChar() }.joinToString("")
            .filter { it.isLetterOrDigit() }
        return if (testo.length >= 17) testo.takeLast(17) else null
    }

    /** Risposta di ATRV, es. "12.6V" */
    fun tensione(risposta: String): Double? {
        val m = Regex("(\\d{1,2}[.,]\\d)").find(risposta) ?: return null
        return m.groupValues[1].replace(',', '.').toDoubleOrNull()
    }

    fun nomeProtocollo(dpn: String): String {
        val c = dpn.trim().uppercase().removePrefix("A").take(1)
        return when (c) {
            "1" -> "SAE J1850 PWM"
            "2" -> "SAE J1850 VPW"
            "3" -> "ISO 9141-2"
            "4" -> "ISO 14230-4 KWP (5 baud)"
            "5" -> "ISO 14230-4 KWP (veloce)"
            "6" -> "CAN 11 bit 500 kbaud"
            "7" -> "CAN 29 bit 500 kbaud"
            "8" -> "CAN 11 bit 250 kbaud"
            "9" -> "CAN 29 bit 250 kbaud"
            else -> "sconosciuto"
        }
    }

    fun nessunaRisposta(risposta: String): Boolean {
        val u = risposta.uppercase().replace(" ", "")
        return u.isBlank() || u.contains("UNABLETOCONNECT") || u.contains("NODATA") ||
            u.contains("ERROR") || u.contains("STOPPED") || u.contains("TIMEOUT") || u.trim() == "?"
    }
}
