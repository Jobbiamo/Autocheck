package com.autocheck.obd

import com.autocheck.obd.Gravita.ARANCIONE
import com.autocheck.obd.Gravita.ROSSO
import com.autocheck.obd.Gravita.VERDE

/**
 * Codici specifici di una marca, spiegati in italiano.
 *
 * Suzuki (anche Opel Agila A/B, gemella della Suzuki Splash): codici della centralina ABS/ESP
 * come elencati nei manuali di servizio Suzuki (Swift 2004–2010, SX4, Splash; unità ATE MK60).
 * I costi sono stime per un'officina indipendente in Italia.
 */
object CodiciMarca {

    private val suzuki = HashMap<String, Scheda>()

    private const val NOTA_SUZUKI = "Codice Suzuki (valido anche per Opel Agila B / Suzuki Splash, ABS/ESP ATE MK60)."
    private const val DIAG = "Diagnosi ABS/ESP con strumento professionale: 30–60 €."

    fun trova(codice: String, marca: String?): Scheda? {
        val c = codice.uppercase()
        val sc = suzuki[c] ?: return null
        return when (marca) {
            "Suzuki", "Opel Agila B" -> sc.copy(codice = c)
            "Opel" -> sc.copy(codice = c, nota = "Spiegazione Suzuki: è quella giusta se la tua Opel è un'Agila (costruita da Suzuki). " +
                "Su altri modelli Opel lo stesso codice può avere un significato diverso.")
            else -> null
        }
    }

    private fun su(codici: String, titolo: String, spiegazione: String, g: Gravita, guida: String, cause: List<String>,
                   faiDaTe: List<String>, meccanico: String, costo: String, seIgnori: String) {
        for (c in codici.split(",")) {
            val k = c.trim()
            suzuki[k] = Scheda(k, titolo, spiegazione, g, guida, cause, faiDaTe, meccanico, costo, seIgnori, NOTA_SUZUKI)
        }
    }

    private const val SENZA_ESP = "Puoi guidare con prudenza: l'ESP (e spesso anche l'ABS) è disattivato. Tieni più distanza e frena prima, soprattutto sul bagnato."

    init {
        val ruote = listOf(
            "C1021,C1022" to "anteriore destra", "C1025,C1026" to "anteriore sinistra",
            "C1031,C1032" to "posteriore destra", "C1035,C1036" to "posteriore sinistra")
        for ((cod, ruota) in ruote) {
            su(cod, "Sensore velocità ruota $ruota",
                "Il sensore che misura quanto gira la ruota $ruota (o il suo anello dentato) non manda un segnale corretto. Il primo codice della coppia indica un problema elettrico (cavo, connettore), il secondo un segnale sbagliato (sensore sporco o anello rovinato). Senza questo dato ABS ed ESP si spengono.",
                ARANCIONE, SENZA_ESP,
                listOf("Sensore sporco di fango o polvere dei freni", "Cavo del sensore rovinato o connettore ossidato vicino alla ruota",
                    "Anello dentato (sul mozzo o sul giunto) crepato, arrugginito o con denti mancanti", "Gioco eccessivo del cuscinetto ruota", "Sensore guasto"),
                listOf("Guarda se il cavo che arriva alla ruota $ruota è rotto, schiacciato o staccato"),
                "Sì, entro pochi giorni. È uno dei lavori più comuni ed economici su ABS/ESP.",
                "Pulizia sensore: 30–60 €. Sensore nuovo: 30–90 € + 40–80 € manodopera. Cuscinetto/mozzo con anello: 60–150 € + 60–120 €.",
                "ABS ed ESP restano spenti: frenate d'emergenza meno sicure e bocciatura alla revisione.")
        }
        su("C1033", "Differenza di velocità tra le ruote",
            "La centralina vede una ruota girare a velocità diversa dalle altre in modo continuo.",
            ARANCIONE, SENZA_ESP,
            listOf("Gomme di misura diversa o ruotino di scorta montato", "Pressione molto diversa tra le gomme", "Gomma molto più consumata delle altre",
                "Sensore ruota sporco o anello dentato rovinato"),
            listOf("Controlla che le 4 gomme siano della stessa misura e alla pressione giusta", "Se hai il ruotino montato, la spia è normale finché non rimetti la ruota"),
            "Solo se le gomme sono a posto.",
            "Gomme: dipende. Sensore: 70–170 € in totale.",
            "ESP disattivato finché la causa resta.")
        su("C1013", "Centralina ABS/ESP non corrispondente",
            "La centralina ABS/ESP non riconosce la configurazione dell'auto: succede quasi sempre dopo la sostituzione della centralina con una di un'altra versione.",
            ARANCIONE, SENZA_ESP,
            listOf("Centralina sostituita con una non compatibile o non configurata", "Configurazione persa"),
            listOf("Se la centralina è stata cambiata da poco, torna da chi l'ha montata"),
            "Sì: serve lo strumento di diagnosi per la configurazione.",
            "Configurazione: 50–100 €. Centralina compatibile: 150–400 € usata, 800–1.500 € nuova.",
            "ABS/ESP restano disattivati.")
        su("C1015", "Sensore di accelerazione (versioni 4x4)",
            "Il sensore che misura l'accelerazione dell'auto, montato sulle versioni a trazione integrale, dà un segnale non valido.",
            ARANCIONE, SENZA_ESP,
            listOf("Connettore del sensore ossidato", "Sensore guasto"), listOf("Niente di pratico"),
            "Sì.", "Sensore: 80–200 € + 40–60 €.", "ABS/ESP disattivati.")
        su("C1016", "Interruttore delle luci dei freni",
            "L'interruttore sul pedale del freno, che accende le luci stop e avvisa l'ESP quando freni, manda un segnale non valido.",
            ARANCIONE, SENZA_ESP + " Controlla anche che le luci dei freni si accendano: chi ti segue deve vederle.",
            listOf("Interruttore sul pedale del freno guasto o mal regolato (causa più frequente)", "Lampadine delle luci stop bruciate", "Fusibile delle luci stop", "Connettore dell'interruttore"),
            listOf("Chiedi a qualcuno di guardare se le luci dei freni si accendono quando premi il pedale", "Controlla il fusibile delle luci stop"),
            "Sì, ma è un lavoro piccolo.",
            "Interruttore: 10–30 € + 20–40 € manodopera. Lampadina: 2–5 €.",
            "Se le luci stop non si accendono rischi un tamponamento; l'ESP resta spento.")
        su("C1017,C1023", "Sensore di imbardata / accelerazione laterale",
            "Il sensore che misura quanto l'auto ruota su sé stessa e quanto è spinta di lato (serve all'ESP per capire se stai sbandando) è guasto.",
            ARANCIONE, SENZA_ESP,
            listOf("Sensore guasto", "Connettore ossidato (il sensore è di solito sotto il sedile o la console centrale)", "Sensore non calibrato dopo una sostituzione"),
            listOf("Controlla che sotto la console centrale non ci sia acqua o umidità"),
            "Sì.", "Sensore: 80–250 € + 40–80 €. Calibrazione: 30–60 €.", "ESP disattivato.")
        su("C1018", "Livello del liquido freni",
            "L'interruttore del livello del liquido freni segnala un livello basso o un problema al circuito.",
            ROSSO, "Controlla subito il livello del liquido freni. Se è sotto il minimo, NON guidare: il pedale può diventare lungo e l'auto frenare meno.",
            listOf("Liquido freni basso: pastiglie molto consumate o perdita", "Interruttore del livello o connettore difettoso"),
            listOf("A motore spento guarda il livello nella vaschetta del liquido freni (tra MIN e MAX)", "Cerca macchie di liquido vicino alle ruote"),
            "Sì, subito se il liquido è basso.",
            "Liquido: 30–70 €. Pastiglie: 70–120 € in totale. Riparazione perdita: 80–250 €.",
            "Perdita di capacità di frenata: pericolo grave.")
        su("C1020", "Alimentazione del sensore di pressione freni",
            "Il sensore che misura quanto premi il freno (dentro l'unità ABS/ESP) non riceve la tensione corretta.",
            ARANCIONE, SENZA_ESP,
            listOf("Problema interno all'unità ABS/ESP", "Connettore dell'unità ossidato", "Tensione della batteria bassa"),
            listOf("Guarda la tensione della batteria nella scheda \"Live\""),
            "Sì. $DIAG",
            "Revisione dell'unità ABS/ESP da ditta specializzata: 200–350 € + 100–200 € per smontaggio, rimontaggio e spurgo freni.",
            "ESP disattivato.")
        su("C1024", "Sensore angolo di sterzo",
            "Il sensore che dice all'ESP quanto stai girando il volante ha un problema al circuito.",
            ARANCIONE, SENZA_ESP,
            listOf("Connettore del sensore sotto il volante", "Sensore guasto", "Lavori recenti a volante, sterzo o airbag"),
            listOf("Spegni e riaccendi il motore; gira il volante tutto a sinistra e tutto a destra"),
            "Sì.", "Sensore: 80–200 € + 50–80 €. Calibrazione: 30–60 €.", "ESP disattivato.")
        su("C1028", "Sensore di pressione freni nell'unità ABS/ESP (difetto noto)",
            "Il sensore che misura la pressione del freno, che si trova DENTRO l'unità ABS/ESP, manda un segnale fuori dai limiti. È un difetto noto dell'unità ATE MK60 montata su Suzuki Splash, Swift e Opel Agila B: con il tempo il circuito del sensore si degrada. La spia ESP resta accesa e il tasto ESP OFF non la spegne.",
            ARANCIONE, SENZA_ESP + " I freni normali funzionano.",
            listOf("Sensore di pressione interno all'unità ABS/ESP guasto (causa più frequente su questa auto)", "Connettore dell'unità ossidato", "Tensione della batteria bassa (meno probabile)"),
            listOf("Controlla la tensione della batteria nella scheda \"Live\" per escludere quella causa", "Cancella l'errore e guarda se torna subito: se torna, è quasi certamente il sensore"),
            "Sì. Attenzione al preventivo: di solito non serve una centralina nuova. Esistono ditte che revisionano l'unità originale (sostituendo il sensore con uno migliorato) e la restituiscono già funzionante, senza bisogno di programmazione.",
            "Revisione dell'unità originale: circa 200–350 € + 100–200 € per smontaggio, rimontaggio e spurgo freni. Unità nuova: 800–1.500 € + manodopera. Unità usata: 100–250 €, ma può avere lo stesso difetto.",
            "Resti senza ESP (e a volte senza ABS) finché non è riparato. Con la spia accesa l'auto non passa la revisione.")
        su("C1034,C1039", "Sensore di imbardata: alimentazione o guasto interno",
            "Il sensore che misura la rotazione dell'auto (fondamentale per l'ESP) non è alimentato correttamente o è guasto all'interno.",
            ARANCIONE, SENZA_ESP,
            listOf("Sensore guasto", "Connettore o cavo di alimentazione", "Tensione batteria bassa"), listOf("Guarda la tensione della batteria nella scheda \"Live\""),
            "Sì.", "Sensore: 80–250 € + 40–80 €. Calibrazione: 30–60 €.", "ESP disattivato.")
        su("C1037,C1038", "Sensore angolo di sterzo: alimentazione o comunicazione",
            "Il sensore dell'angolo di sterzo non è alimentato correttamente (C1037) oppure l'ESP non riceve i suoi dati in modo regolare (C1038).",
            ARANCIONE, SENZA_ESP,
            listOf("Connettore sotto il volante", "Cablaggio", "Sensore guasto", "Tensione batteria bassa"), listOf("Spegni e riaccendi il motore"),
            "Sì.", "Ricerca guasto: 50–100 €. Sensore: 80–200 € + 50–80 €.", "ESP disattivato.")
        su("C1040", "ESP intervenuto troppo a lungo",
            "L'ESP è rimasto in funzione per un tempo anomalo (per esempio guidando a lungo su neve o fango, o con le ruote che slittano su un rullo). Può essere anche un segnale sbagliato di un sensore.",
            VERDE, "Puoi guidare tranquillo. Se la spia si spegne dopo aver riacceso il motore, era solo la situazione di guida.",
            listOf("Lunga guida su fondo scivoloso o ruote che slittano", "Gomme molto diverse tra loro", "Sensore ruota o di imbardata che sbaglia"),
            listOf("Spegni e riaccendi il motore", "Controlla gomme e pressioni"),
            "Solo se l'errore torna su strada normale.", "Nessun costo se era la situazione di guida.", "Nessuno, se non torna.")
        su("C1041,C1042,C1043,C1044,C1045,C1046,C1051,C1052,C1053,C1054,C1055,C1056", "Elettrovalvola dell'unità ABS/ESP",
            "Una delle valvole elettriche dentro l'unità ABS/ESP (quelle che regolano la pressione di ogni ruota) ha un problema elettrico.",
            ARANCIONE, SENZA_ESP,
            listOf("Guasto interno all'unità ABS/ESP", "Connettore dell'unità ossidato", "Tensione batteria bassa"),
            listOf("Controlla che il connettore dell'unità ABS (vano motore) sia ben inserito e asciutto"),
            "Sì. $DIAG",
            "Revisione unità: 200–350 € + 100–200 €. Unità nuova: 800–1.500 €.",
            "ABS/ESP disattivati.")
        su("C1057", "Alimentazione della centralina ABS/ESP",
            "La centralina ABS/ESP riceve una tensione troppo bassa o troppo alta.",
            ARANCIONE, SENZA_ESP,
            listOf("Batteria debole o morsetti ossidati (controllare per prima)", "Alternatore che non carica bene", "Fusibile o cavo di alimentazione dell'unità ABS", "Massa ossidata"),
            listOf("Guarda la tensione nella scheda \"Live\": a motore acceso deve essere 13,5–14,7 V", "Controlla i morsetti della batteria"),
            "Sì, se la batteria è a posto.", "Batteria: 70–140 €. Ricerca guasto alimentazione: 50–100 €.",
            "ABS/ESP che si spengono a intermittenza.")
        su("C1061", "Pompa dell'unità ABS/ESP",
            "Il motorino della pompa ABS/ESP o il suo comando non funzionano correttamente.",
            ARANCIONE, SENZA_ESP,
            listOf("Motorino della pompa guasto o bloccato", "Fusibile della pompa ABS", "Connettore ossidato", "Guasto della scheda dell'unità"),
            listOf("Controlla il fusibile della pompa ABS (di solito quello di amperaggio più alto, nel vano motore)"),
            "Sì. $DIAG", "Revisione unità: 200–350 € + 100–200 €. Unità nuova: 800–1.500 €.", "ABS/ESP disattivati.")
        su("C1063", "Alimentazione delle elettrovalvole ABS/ESP",
            "Il circuito che alimenta le valvole dell'unità ABS/ESP (relè interno) non funziona.",
            ARANCIONE, SENZA_ESP,
            listOf("Fusibile delle valvole ABS", "Relè interno all'unità guasto", "Connettore ossidato"),
            listOf("Controlla i fusibili dell'ABS nel vano motore"),
            "Sì. $DIAG", "Fusibile: 2–10 €. Revisione unità: 200–350 € + 100–200 €.", "ABS/ESP disattivati.")
        su("C1071", "Guasto interno della centralina ABS/ESP",
            "La centralina ABS/ESP ha rilevato un proprio guasto interno.",
            ARANCIONE, SENZA_ESP,
            listOf("Guasto della scheda elettronica dell'unità", "Tensione instabile (controllare la batteria prima)"),
            listOf("Controlla batteria e morsetti", "Cancella l'errore: se torna subito, il guasto è reale"),
            "Sì. Chiedi la revisione dell'unità prima della sostituzione.", "Revisione unità: 200–350 € + 100–200 €. Unità nuova: 800–1.500 €.",
            "ABS/ESP disattivati.")
        su("C1073", "Persa la comunicazione con il sensore di imbardata",
            "L'ESP non riceve più i dati del sensore che misura la rotazione dell'auto.",
            ARANCIONE, SENZA_ESP,
            listOf("Connettore o cavo del sensore", "Sensore guasto"), listOf("Niente di pratico"),
            "Sì.", "Ricerca guasto: 50–100 €. Sensore: 80–250 € + 40–80 €.", "ESP disattivato.")
        su("C1075,C1076,C1078", "Calibrazione di un sensore ESP non eseguita",
            "Un sensore dell'ESP (angolo di sterzo C1075, pressione freni C1076, accelerazione laterale C1078) non è stato calibrato. Succede dopo la sostituzione di un sensore o dell'unità, o dopo lavori allo sterzo.",
            ARANCIONE, SENZA_ESP,
            listOf("Calibrazione non fatta dopo una riparazione"), listOf("Se hai fatto lavori di recente, torna dall'officina"),
            "Sì: la calibrazione si fa con lo strumento di diagnosi.", "Calibrazione: 30–60 €.", "ESP disattivato.")
        su("C1090,C1091,C1094", "L'ESP non riceve dati validi dal motore",
            "La centralina ESP non riceve correttamente i dati dalla centralina motore (per esempio la coppia del motore, che serve all'ESP per intervenire). Spesso dipende da un guasto del motore o della rete di comunicazione, non dall'ESP.",
            ARANCIONE, SENZA_ESP,
            listOf("Un errore registrato nella centralina motore (guarda i codici del motore)", "Problema sulla rete CAN tra motore ed ESP", "Batteria debole"),
            listOf("Risolvi prima gli errori del motore, poi cancella e ricontrolla"),
            "Sì, partendo dai codici del motore.", "Dipende dal guasto del motore. Ricerca guasto rete: 50–150 €.", "ESP disattivato.")
        su("U1100", "Persa la comunicazione con la centralina motore",
            "L'ESP non riceve messaggi dalla centralina motore.",
            ARANCIONE, SENZA_ESP,
            listOf("Batteria debole", "Connettore della centralina motore o dell'ESP", "Rete CAN danneggiata"),
            listOf("Controlla i morsetti della batteria"), "Sì.", "Ricerca guasto: 50–150 €.", "ESP disattivato.")
        su("U1126", "Persa la comunicazione con il sensore angolo di sterzo",
            "L'ESP non riceve messaggi dal sensore dell'angolo di sterzo.",
            ARANCIONE, SENZA_ESP,
            listOf("Connettore sotto il volante", "Cablaggio", "Sensore guasto"), listOf("Niente di pratico"),
            "Sì.", "Ricerca guasto: 50–100 €. Sensore: 80–200 € + 50–80 €.", "ESP disattivato.")
        su("U1140", "Persa la comunicazione con la centralina carrozzeria",
            "L'ESP non riceve messaggi dalla centralina carrozzeria (BCM).",
            ARANCIONE, SENZA_ESP,
            listOf("Batteria debole", "Fusibile o connettore della centralina carrozzeria", "Rete CAN"),
            listOf("Controlla i morsetti della batteria"), "Sì.", "Ricerca guasto: 50–150 €.", "ESP disattivato.")
    }
}

/**
 * Descrizioni originali (in inglese) di codici dei costruttori da un database aperto
 * (Wal33D/dtc-database, licenza MIT). Usate solo come riserva e segnalate come da verificare.
 */
object CodiciCostruttore {
    /** Impostata dall'app all'avvio (legge il file in assets). Restituisce la descrizione o null. */
    @Volatile var cerca: ((marca: String, codice: String) -> String?)? = null

    fun descrizione(marca: String?, codice: String): String? {
        if (marca == null) return null
        val f = cerca ?: return null
        return try { f(marca, codice.uppercase()) } catch (e: Exception) { null }
    }

    /** Marca dell'auto -> nomi usati nel database aperto. */
    fun alias(marca: String): List<String> = when (marca) {
        "Volkswagen", "Skoda", "Seat", "Cupra" -> listOf("volkswagen", "audi")
        "Audi" -> listOf("audi", "volkswagen")
        "BMW", "Mini" -> listOf("bmw")
        "Mercedes-Benz", "Smart" -> listOf("mercedes")
        "Ford" -> listOf("ford", "lincoln", "mercury")
        "Toyota" -> listOf("toyota", "lexus")
        "Nissan" -> listOf("nissan", "infiniti")
        "Honda" -> listOf("honda", "acura")
        "Hyundai", "Kia" -> listOf("kia")
        "Mazda" -> listOf("mazda")
        "Mitsubishi" -> listOf("mitsubishi")
        "Subaru" -> listOf("subaru")
        "Suzuki" -> listOf("suzuki")
        "Jeep" -> listOf("jeep", "chrysler", "dodge")
        "Land Rover", "Jaguar" -> listOf("jaguar")
        else -> emptyList()
    }
}
