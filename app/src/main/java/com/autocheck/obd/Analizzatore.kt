package com.autocheck.obd

import com.autocheck.obd.Gravita.ARANCIONE
import com.autocheck.obd.Gravita.ROSSO
import com.autocheck.obd.Gravita.VERDE
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Trasforma le letture grezze in una diagnosi comprensibile. */
object Analizzatore {

    private val dataFmt = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)

    fun analizza(l: Letture, km: Int?, precedente: Controllo?, adesso: Long = System.currentTimeMillis()): Rapporto {
        val v = l.valori
        val schede = ArrayList<Scheda>()
        val avvisi = ArrayList<String>()

        // ── Stato spia e test di autodiagnosi (modo 01 PID 01) ──
        val st = l.stato
        val spiaAccesa = st != null && st.isNotEmpty() && (st[0] and 0x80) != 0
        val testIncompleti = if (st != null && st.size >= 4) contaTestIncompleti(st) else 0

        // ── Codici errore ──
        for (c in l.memorizzati) schede.add(DatabaseGuasti.trova(c))
        for (c in l.pendenti) if (c !in l.memorizzati) {
            schede.add(DatabaseGuasti.trova(c).copy(
                nota = "IN OSSERVAZIONE: la centralina lo ha visto una volta ma non lo ha ancora confermato. " +
                    "Rifai il controllo tra qualche giorno: se diventa definitivo, va riparato."))
        }
        for (c in l.permanenti) if (c !in l.memorizzati && c !in l.pendenti) {
            schede.add(DatabaseGuasti.trova(c).copy(
                nota = "CODICE PERMANENTE: non si può cancellare con l'app. Sparisce da solo solo quando la centralina " +
                    "verifica, guidando, che il guasto è davvero risolto."))
        }

        // ── Dati motore ──
        schede.addAll(regoleDatiMotore(v))
        val datiMotore = ArrayList<DatoMotore>()
        for ((k, valore) in v) {
            val p = Pids.perChiave(k) ?: continue
            if (k == "km_cancellazione" || k == "km_spia" || k == "tempo_acceso" || k == "map") continue
            val (stato, g) = Interpreta.stato(k, valore, v)
            datiMotore.add(DatoMotore(p.nome, formatta(valore, p), stato, g))
        }

        // ── Controlli anti-fregatura ──
        val kmCanc = v["km_cancellazione"]?.toInt()
        val kmSpia = v["km_spia"]?.toInt() ?: 0
        val tuttiCodici = (l.memorizzati + l.pendenti + l.permanenti).distinct()

        if (spiaAccesa && tuttiCodici.isEmpty()) {
            avvisi.add("La spia motore risulta accesa ma non sono stati letti codici. Potrebbe essere un guasto in un'altra centralina (es. cambio). Fai una diagnosi completa in officina.")
        }
        if (kmSpia > 0) {
            avvisi.add("Hai percorso $kmSpia km con la spia motore accesa.")
        }
        if (kmCanc != null && kmCanc < 150 && l.memorizzati.isEmpty()) {
            avvisi.add("Gli errori sono stati cancellati da poco: solo $kmCanc km fa. Se hai appena fatto una riparazione è normale. " +
                "Se nessuno ti ha detto di aver riparato qualcosa, qualcuno potrebbe aver solo spento la spia. " +
                "Rifai il controllo tra 100–200 km: se l'errore torna, il problema non era risolto.")
        }
        if (testIncompleti > 0 && (kmCanc == null || kmCanc < 300)) {
            val quanti = if (testIncompleti == 1) "1 test di autodiagnosi dell'auto non è ancora completato"
            else "$testIncompleti test di autodiagnosi dell'auto non sono ancora completati"
            avvisi.add("$quanti: succede dopo una cancellazione degli errori o dopo lo stacco della batteria. " +
                "Prima di una revisione o di comprare/vendere l'auto, fai 100–200 km misti (città + extraurbano) e ricontrolla.")
        }
        if (precedente != null) {
            val prima = precedente.codici
            val tornati = tuttiCodici.filter { it in prima }
            val spariti = prima.filter { it !in tuttiCodici }
            val dataPrima = dataFmt.format(Date(precedente.quando))
            if (tornati.isNotEmpty()) {
                avvisi.add("Questi errori erano già presenti al controllo del $dataPrima: ${tornati.joinToString(", ")}. " +
                    "Se nel frattempo l'auto è stata \"riparata\", il problema NON è stato risolto.")
            }
            if (spariti.isNotEmpty() && kmCanc != null && kmCanc < 300) {
                avvisi.add("Al controllo del $dataPrima c'erano: ${spariti.joinToString(", ")}. Ora non ci sono più e la memoria è stata azzerata $kmCanc km fa. " +
                    "Verifica che sia stata fatta davvero una riparazione: chiedi la fattura con i pezzi sostituiti.")
            }
            if (km != null && precedente.km != null && km > precedente.km && kmCanc != null) {
                val percorsi = km - precedente.km
                if (kmCanc < percorsi && spariti.isEmpty() && prima.isEmpty()) {
                    avvisi.add("Dall'ultimo controllo hai fatto $percorsi km, ma gli errori sono stati cancellati $kmCanc km fa: qualcuno ha azzerato la memoria nel frattempo.")
                }
            }
        }

        // ── Verdetto finale ──
        val peggiore = schede.maxByOrNull { it.gravita.livello }?.gravita
        val nRossi = schede.count { it.gravita == ROSSO }
        val nArancio = schede.count { it.gravita == ARANCIONE }
        val verdetto = when {
            peggiore == ROSSO -> "Problema serio: non usare l'auto finché non è controllata." +
                (if (nRossi > 1) " Ci sono $nRossi problemi gravi." else "")
            peggiore == ARANCIONE -> "Puoi usare l'auto con prudenza, ma va portata dal meccanico a breve." +
                (if (nArancio > 1) " Ci sono $nArancio problemi da sistemare." else "")
            peggiore == VERDE -> "Nessun problema serio: solo segnalazioni minori, da sistemare quando ti è comodo."
            else -> "Auto in buono stato: nessun errore trovato."
        }

        return Rapporto(
            quando = adesso, km = km, verdetto = verdetto, gravita = peggiore,
            schede = schede.sortedByDescending { it.gravita.livello },
            avvisi = avvisi, datiMotore = datiMotore, codici = tuttiCodici,
            spiaAccesa = spiaAccesa, kmDaCancellazione = kmCanc, protocollo = l.protocollo
        )
    }

    /** Test di autodiagnosi supportati ma non completati (byte B, C, D del PID 01). */
    private fun contaTestIncompleti(st: IntArray): Int {
        var n = 0
        val b = st[1]
        for (i in 0..2) if (((b shr i) and 1) == 1 && ((b shr (i + 4)) and 1) == 1) n++
        val c = st[2]
        val d = st[3]
        for (i in 0..7) if (((c shr i) and 1) == 1 && ((d shr i) and 1) == 1) n++
        return n
    }

    private fun regoleDatiMotore(v: Map<String, Double>): List<Scheda> {
        val out = ArrayList<Scheda>()
        val giri = v["giri"] ?: 0.0
        val acceso = giri > 400
        val temp = v["temp_liquido"]
        val tempoAcceso = v["tempo_acceso"] ?: 0.0
        val tensione = v["tensione"]

        if (temp != null && temp >= 110) out.add(Scheda(null, "Motore surriscaldato (${temp.toInt()} °C)",
            "La temperatura del liquido di raffreddamento è molto alta. Normalmente sta tra 85 e 100 °C.",
            ROSSO, "FERMATI appena puoi in sicurezza e spegni il motore.",
            listOf("Liquido di raffreddamento basso o perdita", "Ventola del radiatore che non parte", "Termostato bloccato", "Pompa dell'acqua", "Guarnizione della testata"),
            listOf("Aspetta 30 minuti a motore spento. NON aprire il tappo del radiatore a caldo", "A motore freddo controlla il livello del liquido"),
            "Sì, subito. Se il liquido è sparito, fai trainare l'auto.",
            "Da 20 € (rabbocco/tubo) a 600–1.200 € (guarnizione testata).",
            "Motore danneggiato in modo grave: oltre 1.000 €."))
        else if (temp != null && temp >= 104) out.add(Scheda(null, "Temperatura motore alta (${temp.toInt()} °C)",
            "Il motore è più caldo del normale (85–100 °C). Può essere un momento di traffico, ma va tenuto d'occhio.",
            ARANCIONE, "Puoi guidare, ma guarda la lancetta: se sale ancora, fermati.",
            listOf("Liquido di raffreddamento basso", "Ventola che parte in ritardo", "Radiatore sporco o intasato", "Termostato che si apre poco"),
            listOf("A motore freddo controlla il livello del liquido", "Con motore caldo al minimo verifica che la ventola parta"),
            "Sì, se la temperatura resta alta anche in marcia.",
            "Rabbocco: 10–20 €. Ventola/relè: 30–250 €. Termostato: 80–170 €. Radiatore: 150–350 €.",
            "Rischio di surriscaldamento e guarnizione della testata (600–1.200 €)."))
        else if (temp != null && temp < 70 && tempoAcceso > 900) out.add(Scheda(null, "Il motore non si scalda abbastanza (${temp.toInt()} °C)",
            "Dopo più di 15 minuti di funzionamento il motore è ancora freddo. Quasi sempre è il termostato bloccato aperto.",
            VERDE, "Puoi guidare tranquillo.",
            listOf("Termostato bloccato aperto", "Sensore temperatura che legge male"),
            listOf("Niente di pratico"),
            "Sì, quando ti è comodo.",
            "Termostato: 80–170 € in totale.",
            "Consumi più alti, riscaldamento debole e maggiore usura."))

        if (tensione != null) {
            if (acceso && tensione > 15.0) out.add(Scheda(null, "Tensione di ricarica troppo alta (${"%.1f".format(Locale.ITALY, tensione)} V)",
                "A motore acceso l'alternatore dovrebbe dare 13,5–14,7 V. Una tensione così alta può danneggiare batteria ed elettronica.",
                ROSSO, "Evita di guidare a lungo: rischi di danneggiare la batteria e le centraline.",
                listOf("Regolatore di tensione dell'alternatore guasto"),
                listOf("Niente di pratico"),
                "Sì, subito.",
                "Regolatore: 40–100 € + 60–100 €. Alternatore revisionato: 150–300 € + 50–100 €.",
                "Batteria che si gonfia o perde acido, centraline danneggiate."))
            else if (acceso && tensione < 13.0) out.add(Scheda(null, "L'alternatore non carica bene (${"%.1f".format(Locale.ITALY, tensione)} V)",
                "A motore acceso la tensione dovrebbe essere 13,5–14,7 V. Così la batteria si scarica mentre guidi.",
                ARANCIONE, "Puoi guidare, ma rischi di restare a piedi. Evita viaggi lunghi e di notte (le luci consumano).",
                listOf("Alternatore usurato", "Cinghia dei servizi lenta o consumata", "Morsetti ossidati", "Cavo dell'alternatore"),
                listOf("Controlla che i morsetti della batteria siano puliti e stretti", "Ascolta se la cinghia fischia all'avvio"),
                "Sì, entro pochi giorni.",
                "Cinghia servizi: 20–40 € + 30–60 €. Alternatore revisionato: 150–300 € + 50–100 €.",
                "Batteria scarica e auto che si ferma in marcia."))
            else if (!acceso && tensione < 12.0) out.add(Scheda(null, "Batteria scarica (${"%.1f".format(Locale.ITALY, tensione)} V)",
                "A motore spento una batteria carica sta sui 12,4–12,8 V.",
                ARANCIONE, "L'auto potrebbe non partire. Se parte, fai almeno 30 minuti di strada per ricaricarla.",
                listOf("Batteria vecchia (dopo 4–6 anni)", "Luci o accessori lasciati accesi", "Alternatore che non carica", "Consumo elettrico anomalo a auto spenta"),
                listOf("Ricaricala con un caricabatterie o fai un lungo tragitto", "Molti ricambisti testano la batteria gratis"),
                "Solo se si riscarica di nuovo.",
                "Batteria nuova: 70–140 € (montaggio spesso incluso).",
                "Auto che non parte, soprattutto d'inverno."))
        }

        val breve = v["corr_breve"]
        val lunga = v["corr_lunga"]
        if (acceso && breve != null && lunga != null) {
            val tot = breve + lunga
            if (tot > 20) out.add(Scheda(null, "Il motore sta compensando una miscela magra (+${tot.toInt()}%)",
                "La centralina aggiunge molta benzina in più del normale (oltre +20%): probabilmente entra aria di troppo.",
                ARANCIONE, "Puoi guidare, ma fai controllare entro qualche settimana. Presto può comparire il codice P0171.",
                listOf("Tubo del vuoto crepato o staccato", "Guarnizione aspirazione", "Pompa o filtro benzina", "Iniettori sporchi"),
                listOf("Ascolta se c'è un sibilo nel vano motore a motore acceso"),
                "Sì, chiedi la ricerca di prese d'aria.",
                "Tipicamente 30–150 € se è un tubo o una guarnizione.",
                "Mancate accensioni e catalizzatore rovinato (300–800 €)."))
            else if (tot < -20) out.add(Scheda(null, "Il motore sta compensando una miscela grassa (${tot.toInt()}%)",
                "La centralina toglie molta benzina rispetto al normale (oltre −20%).",
                ARANCIONE, "Puoi guidare, ma consumi di più. Fai controllare entro qualche settimana.",
                listOf("Filtro dell'aria intasato", "Iniettore che gocciola", "Sensore MAP o sonda lambda"),
                listOf("Controlla il filtro dell'aria"),
                "Sì, se il filtro è pulito.",
                "Filtro aria: 10–20 €. Sensore: 40–170 €.",
                "Consumi alti e catalizzatore intasato."))
            else if (abs(lunga) > 10) out.add(Scheda(null, "Correzione benzina al limite (${"%.0f".format(Locale.ITALY, lunga)}%)",
                "Il motore funziona, ma la centralina sta già correggendo la miscela più del solito (normale entro ±10%).",
                VERDE, "Puoi guidare tranquillo.",
                listOf(if (lunga > 0) "Piccola presa d'aria" else "Filtro dell'aria sporco", "Sensori un po' stanchi"),
                listOf("Ricontrolla tra qualche settimana per vedere se peggiora"),
                "Non ora: basta tenerlo d'occhio.",
                "Nessun costo per ora.",
                "Se peggiora diventerà un errore di miscela."))
        }
        return out
    }

    fun formatta(valore: Double, p: Pid): String {
        val numero = if (p.decimali == 0) valore.toInt().toString()
        else String.format(Locale.ITALY, "%.${p.decimali}f", valore)
        return "$numero ${p.unita}"
    }

    /** Testo semplice del rapporto, da condividere o salvare. */
    fun testo(r: Rapporto): String {
        val sb = StringBuilder()
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        sb.append("RAPPORTO AUTOCHECK\n")
        sb.append("Data: ${fmt.format(Date(r.quando))}\n")
        r.km?.let { sb.append("Chilometri: $it km\n") }
        sb.append("Spia motore: ${if (r.spiaAccesa) "ACCESA" else "spenta"}\n")
        r.kmDaCancellazione?.let { sb.append("Km dall'ultima cancellazione errori: $it km\n") }
        sb.append("\nGIUDIZIO: ${r.gravita?.emoji ?: "✅"} ${r.verdetto}\n")

        if (r.avvisi.isNotEmpty()) {
            sb.append("\n⚠️ ATTENZIONE\n")
            r.avvisi.forEach { sb.append("• $it\n") }
        }
        r.schede.forEachIndexed { i, s ->
            sb.append("\n━━━━━━━━━━━━━━━━━━━━\n")
            sb.append("${i + 1}. ${s.gravita.emoji} ${s.codice?.let { "$it – " } ?: ""}${s.titolo}\n")
            s.nota?.let { sb.append("Nota: $it\n") }
            sb.append("\nCosa significa: ${s.spiegazione}\n")
            sb.append("\nPosso guidare? ${s.guida}\n")
            sb.append("\nCause probabili (dalla più frequente):\n")
            s.cause.forEachIndexed { j, c -> sb.append("  ${j + 1}) $c\n") }
            if (s.faiDaTe.isNotEmpty()) {
                sb.append("\nCosa puoi fare da solo:\n")
                s.faiDaTe.forEach { sb.append("  • $it\n") }
            }
            sb.append("\nServe il meccanico? ${s.meccanico}\n")
            sb.append("\nCosto indicativo: ${s.costo}\n")
            sb.append("\nSe lo ignori: ${s.seIgnori}\n")
        }
        if (r.datiMotore.isNotEmpty()) {
            sb.append("\n━━━━━━━━━━━━━━━━━━━━\nDATI MOTORE\n")
            r.datiMotore.forEach { sb.append("• ${it.nome}: ${it.valore} – ${it.stato}\n") }
        }
        sb.append("\nI costi sono stime indicative per un'officina indipendente in Italia. La diagnosi è probabile, non certa.\n")
        return sb.toString()
    }
}

/** Traduce un singolo valore in parole semplici. */
object Interpreta {
    fun stato(chiave: String, v: Double, ctx: Map<String, Double>): Pair<String, Gravita> {
        val acceso = (ctx["giri"] ?: 0.0) > 400
        val fermo = (ctx["velocita"] ?: 0.0) < 1
        return when (chiave) {
            "temp_liquido" -> when {
                v >= 110 -> "Surriscaldamento! Fermati" to ROSSO
                v >= 104 -> "Alta, tienila d'occhio" to ARANCIONE
                v >= 75 -> "Normale" to VERDE
                acceso -> "Motore ancora freddo" to VERDE
                else -> "Motore freddo" to VERDE
            }
            "tensione" -> if (acceso) when {
                v > 15.0 -> "Troppo alta: regolatore dell'alternatore" to ROSSO
                v >= 13.3 -> "Normale: l'alternatore carica" to VERDE
                v >= 13.0 -> "Un po' bassa" to ARANCIONE
                else -> "L'alternatore non carica" to ARANCIONE
            } else when {
                v >= 12.4 -> "Batteria carica" to VERDE
                v >= 12.0 -> "Batteria un po' scarica" to VERDE
                else -> "Batteria scarica" to ARANCIONE
            }
            "giri" -> when {
                v < 1 -> "Motore spento" to VERDE
                fermo && v < 550 -> "Minimo basso, potrebbe spegnersi" to ARANCIONE
                fermo && v <= 1000 -> "Minimo regolare" to VERDE
                fermo && v <= 1400 && (ctx["temp_liquido"] ?: 90.0) < 70 -> "Minimo alto: normale a motore freddo" to VERDE
                fermo && v > 1100 && (ctx["temp_liquido"] ?: 0.0) >= 75 -> "Minimo un po' alto a motore caldo" to VERDE
                else -> "In funzione" to VERDE
            }
            "corr_breve", "corr_lunga" -> when {
                !acceso -> "Disponibile a motore acceso" to VERDE
                abs(v) <= 10 -> "Normale" to VERDE
                abs(v) <= 20 -> (if (v > 0) "Al limite: un po' magra" else "Al limite: un po' grassa") to VERDE
                else -> (if (v > 0) "Troppo magra: entra aria?" else "Troppo grassa") to ARANCIONE
            }
            "temp_aria" -> (if (v > 70) "Molto calda (motore caldo, auto ferma)" else "Normale") to VERDE
            "carico" -> (if (!acceso) "Motore spento" else if (fermo && v > 50) "Alto per il minimo" else "Normale") to VERDE
            "farfalla" -> (if (v < 25 || !acceso) "Pedale rilasciato" else "Pedale premuto") to VERDE
            "velocita" -> (if (v < 1) "Auto ferma" else "In marcia") to VERDE
            else -> "" to VERDE
        }
    }
}
