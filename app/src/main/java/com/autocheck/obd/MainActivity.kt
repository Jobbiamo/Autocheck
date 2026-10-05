package com.autocheck.obd

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission", "SetTextI18n")
class MainActivity : Activity() {

    private val obd = ObdConnection()
    private lateinit var storico: Storico
    private lateinit var contenuto: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var statoTv: TextView
    private val schede = ArrayList<TextView>()
    private var tabCorrente = 0
    @Volatile private var liveAttivo = false
    private var ultimoRapporto: Rapporto? = null
    private val prefs by lazy { getSharedPreferences("autocheck", MODE_PRIVATE) }
    private val fmtData = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

    // Schede
    private val T_CONTROLLO = 0
    private val T_SPIE = 1
    private val T_LIVE = 2
    private val T_STORICO = 3
    private val T_ADATTATORE = 4

    // Selezioni del controllo
    private val spieAccese = LinkedHashSet<String>()
    private var scansioneEstesa = true

    // Colori
    private val BLU = 0xFF1E3A5F.toInt()
    private val ARANCIO = 0xFFE8772E.toInt()
    private val SFONDO = 0xFFF2F4F7.toInt()
    private val TESTO = 0xFF1F2937.toInt()
    private val GRIGIO = 0xFF6B7280.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storico = Storico(this)
        costruisciInterfaccia()
        mostraTab(if (obd.connesso) T_CONTROLLO else T_ADATTATORE)
    }

    override fun onDestroy() {
        liveAttivo = false
        Thread { obd.chiudi() }.start()
        super.onDestroy()
    }

    // ═════════════════════ STRUTTURA ═════════════════════

    private fun costruisciInterfaccia() {
        val radice = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SFONDO)
        }
        val testata = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BLU)
            setPadding(dp(16), dp(14), dp(16), dp(12))
        }
        testata.addView(testo("AutoCheck", 22f, true, Color.WHITE))
        statoTv = testo("", 13f, false, 0xFFCBD5E1.toInt())
        testata.addView(statoTv)
        radice.addView(testata)

        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
        }
        listOf("Controllo", "Spie", "Live", "Storico", "Adattatore").forEachIndexed { i, nome ->
            val t = TextView(this).apply {
                text = nome
                textSize = 12.5f
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(dp(2), dp(14), dp(2), dp(14))
                setOnClickListener { mostraTab(i) }
            }
            barra.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            schede.add(t)
        }
        radice.addView(barra)

        scroll = ScrollView(this)
        contenuto = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(32))
        }
        scroll.addView(contenuto)
        radice.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(radice)
        aggiornaStato()
    }

    private fun aggiornaStato() {
        statoTv.text = if (obd.connesso)
            "● Collegato a ${obd.nomeDispositivo}" + (if (obd.protocollo.isNotEmpty()) " · ${obd.protocollo}" else "")
        else "○ Adattatore non collegato"
    }

    private fun mostraTab(i: Int) {
        liveAttivo = false
        tabCorrente = i
        schede.forEachIndexed { j, t ->
            t.setTextColor(if (j == i) ARANCIO else GRIGIO)
            t.setTypeface(null, if (j == i) Typeface.BOLD else Typeface.NORMAL)
        }
        contenuto.removeAllViews()
        scroll.scrollTo(0, 0)
        when (i) {
            T_CONTROLLO -> tabControllo()
            T_SPIE -> tabSpie()
            T_LIVE -> tabLive()
            T_STORICO -> tabStorico()
            T_ADATTATORE -> tabAdattatore()
        }
    }

    // ═════════════════════ TAB CONTROLLO ═════════════════════

    private fun tabControllo() {
        if (!obd.connesso) {
            val c = card()
            c.addView(testo("Prima collega l'adattatore", 17f, true))
            c.addView(testo("Inserisci l'ELM327 nella presa OBD della macchina, accendi il quadro e collegalo dalla scheda \"Adattatore\".", 15f).margine(6))
            c.addView(bottone("Vai al collegamento", BLU) { mostraTab(T_ADATTATORE) }.margine(12))
            aggiungi(c)
            ultimoRapporto?.let { mostraRapporto(it) }
            return
        }

        val c = card()
        c.addView(testo("Controllo completo dell'auto", 17f, true))
        c.addView(testo("Per il risultato migliore: motore acceso e caldo (dopo almeno 10 minuti di guida), auto ferma.", 14f, false, GRIGIO).margine(4))
        c.addView(testo("Chilometri del contachilometri (facoltativo, serve per lo storico):", 14f).margine(12))
        val kmEdit = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "es. 98500"
            prefs.getInt("ultimi_km", -1).takeIf { it > 0 }?.let { setText(it.toString()) }
        }
        c.addView(kmEdit)

        c.addView(testo("Quali spie sono accese sul cruscotto adesso? (tocca per selezionare)", 14f).margine(14))
        c.addView(selettoreSpie().margine(6))

        val estesaTv = testo("", 14f)
        fun aggiornaEstesa() {
            estesaTv.text = (if (scansioneEstesa) "☑" else "☐") + "  Scansione estesa: prova a leggere anche ABS/ESP, airbag e altre centraline (sperimentale, +30–60 secondi)"
        }
        aggiornaEstesa()
        estesaTv.setPadding(0, dp(8), 0, dp(8))
        estesaTv.setOnClickListener { scansioneEstesa = !scansioneEstesa; aggiornaEstesa() }
        c.addView(estesaTv.margine(10))

        c.addView(bottone("AVVIA CONTROLLO", ARANCIO) {
            val km = kmEdit.text.toString().trim().toIntOrNull()
            km?.let { prefs.edit().putInt("ultimi_km", it).apply() }
            avviaControllo(km)
        }.margine(12))
        aggiungi(c)

        ultimoRapporto?.let { mostraRapporto(it) }
    }

    private fun avviaControllo(km: Int?) {
        val attesa = dialogoAttesa("Sto leggendo la centralina motore…\nCi vogliono circa 20–40 secondi.")
        val spie = spieAccese.toSet()
        val estesaRichiesta = scansioneEstesa
        Thread {
            try {
                val letture = obd.leggiTutto()
                val estesa = if (estesaRichiesta) {
                    runOnUiThread { attesa.setMessage("Centralina motore letta.\nOra provo le altre centraline (ABS/ESP, airbag…)…") }
                    try {
                        obd.scansioneEstesa { msg -> runOnUiThread { attesa.setMessage("Scansione estesa\n$msg") } }
                    } catch (e: Exception) {
                        EsitoScansione(emptyList(), "ERRORE: ${e.message}", "Scansione estesa non riuscita: ${e.message}")
                    }
                } else null
                val precedente = storico.lista().firstOrNull()
                val r = Analizzatore.analizza(letture, km, precedente, spie = spie, estesa = estesa)
                storico.salva(r)
                ultimoRapporto = r
                runOnUiThread {
                    attesa.dismiss()
                    if (tabCorrente == T_CONTROLLO) mostraTab(T_CONTROLLO)
                    scroll.post { scroll.smoothScrollTo(0, dp(260)) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    attesa.dismiss()
                    aggiornaStato()
                    errore("Controllo non riuscito", e.message ?: "Errore di comunicazione con l'adattatore.")
                }
            }
        }.start()
    }

    private fun mostraRapporto(r: Rapporto) {
        // Giudizio finale
        val colore = r.gravita?.colore ?: 0xFF2E7D32.toInt()
        val g = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = sfondo(colore, colore)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        g.addView(testo("RISULTATO DEL CONTROLLO · ${fmtData.format(Date(r.quando))}", 12f, true, 0xDDFFFFFF.toInt()))
        g.addView(testo("${r.gravita?.emoji ?: "✅"}  ${r.verdetto}", 18f, true, Color.WHITE).margine(6))
        val riepilogo = buildString {
            append("Spia motore: ${if (r.spiaAccesa) "accesa" else "spenta"}")
            append(" · Problemi trovati: ${r.schede.size}")
            r.kmDaCancellazione?.let { append("\nErrori cancellati l'ultima volta $it km fa") }
        }
        g.addView(testo(riepilogo, 13f, false, Color.WHITE).margine(6))
        aggiungi(g, 16)

        // Centraline lette
        r.centraline?.let { lista ->
            val c = card()
            c.addView(testo("CENTRALINE LETTE", 12f, true, GRIGIO))
            c.addView(rigaDato("Motore", "letta", "Codici: " + (r.codici.filter { it.startsWith("P") }.joinToString(", ").ifEmpty { "nessuno" }), Gravita.VERDE.colore))
            if (lista.isEmpty()) c.addView(testo("Nessun'altra centralina ha risposto alla scansione estesa.", 14f, false, GRIGIO).margine(6))
            for (ct in lista) {
                val col = if (ct.codici.isEmpty()) Gravita.VERDE.colore else Gravita.ARANCIONE.colore
                c.addView(rigaDato(ct.nome, ct.indirizzo,
                    if (ct.codici.isEmpty()) (if (ct.nota.startsWith("Risponde")) ct.nota else "Nessun errore") else "Errori: " + ct.codici.joinToString(", ") { it.codice }, col))
            }
            r.notaScansione?.let { c.addView(testo(it, 13f, false, 0xFF9A5B00.toInt()).margine(6)) }
            aggiungi(c)
        }
        if (r.spie.isNotEmpty()) {
            aggiungi(testo("Spie che hai indicato: ${r.spie.joinToString(", ")}", 13f, false, GRIGIO))
        }

        // Avvisi anti-fregatura
        for (a in r.avvisi) {
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = sfondo(0xFFFFF7E6.toInt(), 0xFFF5B041.toInt())
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            c.addView(testo("⚠️ Attenzione", 14f, true, 0xFF9A5B00.toInt()))
            c.addView(testo(a, 14f).margine(4))
            aggiungi(c)
        }

        // Schede dei problemi
        if (r.schede.isNotEmpty()) aggiungi(testo("I PROBLEMI, SPIEGATI", 13f, true, GRIGIO), 20)
        for (s in r.schede) aggiungi(vistaScheda(s))

        // Dati motore
        if (r.datiMotore.isNotEmpty()) {
            aggiungi(testo("DATI DEL MOTORE AL MOMENTO DEL CONTROLLO", 13f, true, GRIGIO), 20)
            val c = card()
            r.datiMotore.forEach { d ->
                c.addView(rigaDato(d.nome, d.valore, d.stato, d.gravita.colore))
            }
            aggiungi(c)
        }

        // Azioni
        aggiungi(bottone("Condividi il rapporto (WhatsApp, email…)", BLU) {
            condividi(Analizzatore.testo(r))
        }, 16)
        r.logTecnico?.let { log ->
            aggiungi(bottone("Invia rapporto tecnico (per migliorare l'app)", 0xFF6B7280.toInt()) {
                condividi("RAPPORTO TECNICO AUTOCHECK\nProtocollo: ${r.protocollo}\nSpie: ${r.spie.joinToString(", ")}\n\n$log")
            })
        }
        if (obd.connesso && (r.codici.isNotEmpty() || r.spiaAccesa)) {
            aggiungi(bottone("Cancella errori e spegni la spia", 0xFF9CA3AF.toInt()) { confermaCancellazione() })
        }
        aggiungi(testo("I costi sono stime indicative per un'officina indipendente in Italia. La diagnosi è probabile, non certa: serve a capire il problema e a valutare se un preventivo è onesto.",
            12f, false, GRIGIO), 12)
    }

    private fun vistaScheda(s: Scheda): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = sfondo(Color.WHITE, s.gravita.colore, 2)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val titolo = (s.codice?.let { "$it · " } ?: "") + s.titolo
        c.addView(testo("${s.gravita.emoji}  $titolo", 17f, true))
        c.addView(testo(s.gravita.etichetta, 13f, true, s.gravita.colore).margine(4))
        s.nota?.let {
            c.addView(testo(it, 13f, false, 0xFF9A5B00.toInt()).apply {
                background = sfondo(0xFFFFF7E6.toInt(), 0xFFFFF7E6.toInt())
                setPadding(dp(10), dp(8), dp(10), dp(8))
            }.margine(8))
        }
        sezione(c, "Cosa significa", s.spiegazione)
        sezione(c, "Posso guidare?", s.guida)
        sezione(c, "Cause probabili (dalla più frequente)",
            s.cause.mapIndexed { i, x -> "${i + 1}. $x" }.joinToString("\n"))
        if (s.faiDaTe.isNotEmpty()) sezione(c, "Cosa puoi fare da solo", s.faiDaTe.joinToString("\n") { "• $it" })
        sezione(c, "Serve il meccanico?", s.meccanico)
        sezione(c, "Quanto costa (prezzi di mercato indicativi)", s.costo)
        sezione(c, "Se lo ignori", s.seIgnori)
        return c
    }

    private fun sezione(parent: LinearLayout, titolo: String, corpo: String) {
        parent.addView(testo(titolo.uppercase(), 12f, true, GRIGIO).margine(12))
        parent.addView(testo(corpo, 15f).margine(2))
    }

    private fun confermaCancellazione() {
        AlertDialog.Builder(this)
            .setTitle("Cancellare gli errori?")
            .setMessage(
                "Quadro acceso e motore SPENTO.\n\n" +
                    "• La spia si spegne, ma il guasto NON viene riparato: se c'è ancora, la spia si riaccenderà.\n" +
                    "• I test di autodiagnosi ripartono da zero: non farlo poco prima della revisione.\n\n" +
                    "Ha senso dopo una riparazione, per verificare che il problema non torni."
            )
            .setPositiveButton("Cancella") { _, _ ->
                val attesa = dialogoAttesa("Cancellazione in corso…")
                Thread {
                    val ok = try { obd.cancellaErrori() } catch (e: Exception) { false }
                    runOnUiThread {
                        attesa.dismiss()
                        if (ok) {
                            ultimoRapporto = null
                            info("Errori cancellati", "Fatto. Guida qualche giorno e rifai il controllo: se un errore torna, il problema non è risolto.")
                            mostraTab(T_CONTROLLO)
                        } else errore("Cancellazione non riuscita", "Assicurati che il quadro sia acceso e il motore spento, poi riprova.")
                    }
                }.start()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // ═════════════════════ TAB SPIE ═════════════════════

    private fun tabSpie() {
        aggiungi(testo("Tocca la spia che vedi accesa sul cruscotto: l'app ti spiega cosa significa, se puoi guidare, le cause, cosa controllare e quanto costa. Funziona anche senza adattatore.",
            14f, false, GRIGIO))
        for (sp in Spie.TUTTE) {
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = sfondo(Color.WHITE, sp.scheda.gravita.colore, 2)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            c.addView(testo("${sp.scheda.gravita.emoji}  ${sp.nome}", 16f, true))
            c.addView(testo(sp.aspetto, 13f, false, GRIGIO).margine(4))
            box.addView(c)
            var aperta = false
            c.setOnClickListener {
                aperta = !aperta
                if (aperta) box.addView(vistaScheda(sp.scheda).also {
                    it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) }
                }) else if (box.childCount > 1) box.removeViewAt(1)
            }
            aggiungi(box)
        }
        aggiungi(testo("Spie rosse = fermati e controlla subito. Spie gialle/arancioni = anomalia da far controllare. Spie verdi o blu = solo informative (luci, cruise control).",
            13f, false, GRIGIO), 16)
    }

    private fun selettoreSpie(): View {
        val contenitore = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val opzioni = listOf("motore", "motore_lamp", "esp", "abs", "freni", "airbag", "batteria", "olio", "temperatura", "servosterzo", "candelette")
        var riga: LinearLayout? = null
        opzioni.forEachIndexed { i, id ->
            if (i % 2 == 0) {
                riga = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                contenitore.addView(riga)
            }
            val sp = Spie.perId(id) ?: return@forEachIndexed
            val chip = TextView(this).apply {
                text = sp.nome
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(10), dp(8), dp(10))
            }
            fun aggiorna() {
                val on = id in spieAccese
                chip.background = sfondo(if (on) 0xFFFFF1E6.toInt() else Color.WHITE, if (on) ARANCIO else 0xFFE5E7EB.toInt(), if (on) 2 else 1)
                chip.setTextColor(if (on) TESTO else GRIGIO)
                chip.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
            }
            aggiorna()
            chip.setOnClickListener {
                if (id in spieAccese) spieAccese.remove(id) else spieAccese.add(id)
                aggiorna()
            }
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(if (i % 2 == 0) 0 else dp(4), dp(4), if (i % 2 == 0) dp(4) else 0, 0)
            }
            riga?.addView(chip, lp)
        }
        if (opzioni.size % 2 == 1) riga?.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        return contenitore
    }

    // ═════════════════════ TAB LIVE ═════════════════════

    private fun tabLive() {
        if (!obd.connesso) {
            val c = card()
            c.addView(testo("Collega prima l'adattatore per vedere i dati del motore in tempo reale.", 15f))
            c.addView(bottone("Vai al collegamento", BLU) { mostraTab(T_ADATTATORE) }.margine(12))
            aggiungi(c)
            return
        }
        aggiungi(testo("Dati del motore aggiornati in tempo reale, con la spiegazione di ogni valore. Non guardare il telefono mentre guidi: fallo usare a un passeggero o guarda da fermo.",
            14f, false, GRIGIO))

        val righe = HashMap<String, Triple<TextView, TextView, View>>()
        val c = card()
        for (p in Pids.LIVE) {
            val riga = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(10), 0, dp(10))
            }
            val alto = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            alto.addView(testo(p.nome, 15f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val valTv = testo("–", 18f, true)
            alto.addView(valTv)
            riga.addView(alto)
            val statoV = testo("in attesa…", 13f, false, GRIGIO)
            riga.addView(statoV)
            c.addView(riga)
            righe[p.chiave] = Triple(valTv, statoV, riga)
        }
        val pulsante = bottone("AVVIA LETTURA", ARANCIO) {}
        pulsante.setOnClickListener {
            if (liveAttivo) {
                liveAttivo = false
                pulsante.text = "AVVIA LETTURA"
            } else {
                liveAttivo = true
                pulsante.text = "FERMA LETTURA"
                avviaLive(righe, pulsante)
            }
        }
        aggiungi(pulsante)
        aggiungi(c)
    }

    private fun avviaLive(righe: Map<String, Triple<TextView, TextView, View>>, pulsante: TextView) {
        Thread {
            try {
                while (liveAttivo && obd.connesso) {
                    val valori = obd.leggiValori(Pids.LIVE)
                    runOnUiThread {
                        for (p in Pids.LIVE) {
                            val r = righe[p.chiave] ?: continue
                            val v = valori[p.chiave]
                            if (v == null) {
                                r.first.text = "n.d."
                                r.second.text = "Non disponibile su questa auto"
                                r.second.setTextColor(GRIGIO)
                            } else {
                                val (stato, g) = Interpreta.stato(p.chiave, v, valori)
                                r.first.text = Analizzatore.formatta(v, p)
                                r.second.text = stato
                                r.second.setTextColor(if (g == Gravita.VERDE) GRIGIO else g.colore)
                            }
                        }
                    }
                    Thread.sleep(250)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    liveAttivo = false
                    pulsante.text = "AVVIA LETTURA"
                    aggiornaStato()
                    errore("Lettura interrotta", e.message ?: "Connessione persa con l'adattatore.")
                }
            }
        }.start()
    }

    // ═════════════════════ TAB STORICO ═════════════════════

    private fun tabStorico() {
        val lista = storico.lista()
        if (lista.isEmpty()) {
            val c = card()
            c.addView(testo("Nessun controllo salvato", 17f, true))
            c.addView(testo("Ogni controllo viene salvato qui, solo nel tuo telefono. Serve per confrontare la situazione prima e dopo il meccanico.", 15f).margine(6))
            aggiungi(c)
            return
        }
        aggiungi(testo("${lista.size} controlli salvati nel telefono. Tocca un controllo per vedere il rapporto completo.", 14f, false, GRIGIO))
        for (ctrl in lista) {
            val g = Gravita.values().firstOrNull { it.livello == ctrl.livello }
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = sfondo(Color.WHITE, g?.colore ?: 0xFF2E7D32.toInt(), 2)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setOnClickListener { apriControllo(ctrl) }
            }
            val intest = fmtData.format(Date(ctrl.quando)) + (ctrl.km?.let { " · $it km" } ?: "")
            c.addView(testo(intest, 13f, true, GRIGIO))
            c.addView(testo("${g?.emoji ?: "✅"} ${ctrl.verdetto}", 15f, true).margine(4))
            c.addView(testo(if (ctrl.codici.isEmpty()) "Nessun codice errore" else "Codici: ${ctrl.codici.joinToString(", ")}", 14f).margine(2))
            aggiungi(c)
        }
        aggiungi(bottone("Cancella tutto lo storico", 0xFF9CA3AF.toInt()) {
            AlertDialog.Builder(this)
                .setTitle("Cancellare lo storico?")
                .setMessage("Tutti i controlli salvati verranno eliminati dal telefono. L'operazione non si può annullare.")
                .setPositiveButton("Elimina") { _, _ -> storico.cancellaTutto(); mostraTab(T_STORICO) }
                .setNegativeButton("Annulla", null)
                .show()
        }, 20)
    }

    private fun apriControllo(c: Controllo) {
        val tv = testo(c.testo, 14f).apply { setPadding(dp(20), dp(12), dp(20), dp(12)); setTextIsSelectable(true) }
        val sv = ScrollView(this).apply { addView(tv) }
        AlertDialog.Builder(this)
            .setView(sv)
            .setPositiveButton("Condividi") { _, _ -> condividi(c.testo) }
            .setNeutralButton("Elimina") { _, _ -> storico.elimina(c.quando); mostraTab(T_STORICO) }
            .setNegativeButton("Chiudi", null)
            .show()
    }

    // ═════════════════════ TAB ADATTATORE ═════════════════════

    private fun tabAdattatore() {
        val istr = card()
        istr.addView(testo("Come collegare l'adattatore", 17f, true))
        istr.addView(testo(
            "1. Inserisci l'ELM327 nella presa OBD: di solito è sotto il cruscotto, lato guida, vicino al volante.\n" +
                "2. Gira la chiave sul secondo scatto (quadro acceso, spie accese). Per i dati motore, avvia il motore.\n" +
                "3. Solo la prima volta: apri le impostazioni Bluetooth del telefono, cerca \"OBDII\" o \"OBD\" e abbinalo con il codice 1234 (oppure 0000).\n" +
                "4. Torna qui e tocca il nome dell'adattatore.", 15f).margine(6))
        istr.addView(bottone("Apri impostazioni Bluetooth", BLU) {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }.margine(12))
        aggiungi(istr)

        if (obd.connesso) {
            val c = card()
            c.addView(testo("✅ Collegato a ${obd.nomeDispositivo}", 16f, true, 0xFF2E7D32.toInt()))
            c.addView(testo("Protocollo dell'auto: ${obd.protocollo}", 14f, false, GRIGIO).margine(4))
            c.addView(bottone("Vai al controllo", ARANCIO) { mostraTab(T_CONTROLLO) }.margine(12))
            c.addView(bottone("Scollega", 0xFF9CA3AF.toInt()) {
                Thread { obd.chiudi(); runOnUiThread { aggiornaStato(); mostraTab(T_ADATTATORE) } }.start()
            }.margine(8))
            aggiungi(c)
            return
        }

        if (!haPermessoBluetooth()) {
            val c = card()
            c.addView(testo("L'app ha bisogno del permesso \"Dispositivi nelle vicinanze\" per usare il Bluetooth.", 15f))
            c.addView(bottone("Concedi il permesso", ARANCIO) { chiediPermesso() }.margine(12))
            aggiungi(c)
            return
        }

        val adapter = (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null) {
            aggiungi(card().apply { addView(testo("Questo telefono non ha il Bluetooth.", 15f)) })
            return
        }
        if (!adapter.isEnabled) {
            val c = card()
            c.addView(testo("Il Bluetooth è spento.", 15f))
            c.addView(bottone("Accendi Bluetooth", ARANCIO) {
                try { startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) } catch (e: Exception) { }
            }.margine(12))
            aggiungi(c)
            return
        }

        val dispositivi = adapter.bondedDevices.orEmpty().sortedByDescending { pareObd(it) }
        aggiungi(testo("DISPOSITIVI ABBINATI", 13f, true, GRIGIO), 20)
        if (dispositivi.isEmpty()) {
            aggiungi(card().apply {
                addView(testo("Nessun dispositivo abbinato. Segui il punto 3 qui sopra, poi torna su questa schermata.", 15f))
            })
        }
        val ultimo = prefs.getString("ultimo_adattatore", null)
        for (d in dispositivi) {
            val obdLike = pareObd(d)
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = sfondo(Color.WHITE, if (obdLike) ARANCIO else 0xFFE5E7EB.toInt(), if (obdLike) 2 else 1)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setOnClickListener { connetti(d) }
            }
            c.addView(testo((d.name ?: "Senza nome") + (if (d.address == ultimo) "  · usato l'ultima volta" else ""), 16f, true))
            c.addView(testo(if (obdLike) "Sembra un adattatore OBD · tocca per collegare" else d.address, 13f, false, GRIGIO))
            aggiungi(c)
        }
    }

    private fun pareObd(d: BluetoothDevice): Boolean {
        val n = (d.name ?: "").uppercase()
        return n.contains("OBD") || n.contains("ELM") || n.contains("V-LINK") || n.contains("VLINK") || n.contains("CAR")
    }

    private fun connetti(d: BluetoothDevice) {
        val attesa = dialogoAttesa("Collegamento a ${d.name ?: d.address}…\nLa prima volta può richiedere fino a 20 secondi.")
        Thread {
            try {
                obd.connetti(d)
                prefs.edit().putString("ultimo_adattatore", d.address).apply()
                runOnUiThread {
                    attesa.dismiss()
                    aggiornaStato()
                    Toast.makeText(this, "Collegato! Protocollo: ${obd.protocollo}", Toast.LENGTH_LONG).show()
                    mostraTab(T_CONTROLLO)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    attesa.dismiss()
                    aggiornaStato()
                    errore("Collegamento non riuscito",
                        (e.message ?: "Errore sconosciuto") +
                            "\n\nControlla che:\n• l'adattatore sia inserito bene (si accende una lucina)\n• il quadro sia acceso\n• nessun'altra app (es. Car Scanner) sia collegata all'adattatore")
                }
            }
        }.start()
    }

    private fun haPermessoBluetooth(): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun chiediPermesso() {
        if (Build.VERSION.SDK_INT >= 31) requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mostraTab(tabCorrente)
    }

    override fun onResume() {
        super.onResume()
        if (::contenuto.isInitialized && tabCorrente == T_ADATTATORE) mostraTab(T_ADATTATORE)
    }

    // ═════════════════════ UTILITÀ GRAFICHE ═════════════════════

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun testo(s: String, size: Float = 15f, bold: Boolean = false, colore: Int = TESTO): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(colore)
            setLineSpacing(0f, 1.15f)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun bottone(s: String, colore: Int, azione: () -> Unit): TextView =
        TextView(this).apply {
            text = s
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = sfondo(colore, colore)
            isClickable = true
            setOnClickListener { azione() }
        }

    private fun sfondo(riempimento: Int, bordo: Int, spessoreDp: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            setColor(riempimento)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(spessoreDp), bordo)
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = sfondo(Color.WHITE, 0xFFE5E7EB.toInt())
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }

    private fun rigaDato(nome: String, valore: String, stato: String, colore: Int): View {
        val riga = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val alto = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        alto.addView(testo(nome, 14f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        alto.addView(testo(valore, 15f, true))
        riga.addView(alto)
        if (stato.isNotEmpty()) riga.addView(testo(stato, 13f, false, if (colore == Gravita.VERDE.colore) GRIGIO else colore))
        return riga
    }

    private fun aggiungi(v: View, margineSopraDp: Int = 12) {
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(margineSopraDp)
        contenuto.addView(v, lp)
    }

    private fun <T : View> T.margine(sopraDp: Int): T {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(sopraDp) }
        return this
    }

    private fun dialogoAttesa(messaggio: String): AlertDialog =
        AlertDialog.Builder(this)
            .setMessage(messaggio)
            .setCancelable(false)
            .show()

    private fun errore(titolo: String, msg: String) {
        if (isFinishing) return
        AlertDialog.Builder(this).setTitle(titolo).setMessage(msg).setPositiveButton("OK", null).show()
    }

    private fun info(titolo: String, msg: String) = errore(titolo, msg)

    private fun condividi(t: String) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Rapporto AutoCheck")
            putExtra(Intent.EXTRA_TEXT, t)
        }
        startActivity(Intent.createChooser(i, "Condividi il rapporto"))
    }
}
