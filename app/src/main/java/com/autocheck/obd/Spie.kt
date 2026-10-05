package com.autocheck.obd

import com.autocheck.obd.Gravita.ARANCIONE
import com.autocheck.obd.Gravita.ROSSO
import com.autocheck.obd.Gravita.VERDE

/** Una spia del cruscotto, riconoscibile dal disegno. */
data class Spia(val id: String, val nome: String, val aspetto: String, val scheda: Scheda)

/**
 * Guida alle spie del cruscotto. Funziona anche quando nessuna centralina risponde:
 * è la persona a dire quale spia vede accesa.
 */
object Spie {

    private const val DIAGNOSI_TELAIO = "Diagnosi ABS/ESP con strumento professionale: 30–60 €."

    val ESP = Spia("esp", "ESP / controllo stabilità (fissa)",
        "Gialla: auto con due linee ondulate sotto, oppure la scritta ESP. Resta accesa fissa.",
        Scheda(null, "Spia ESP accesa fissa",
            "Il controllo di stabilità (ESP) si è disattivato perché ha rilevato un'anomalia, sua o di un sistema da cui dipende. L'auto frena e guida normalmente, ma l'ESP non ti aiuta a evitare sbandate.",
            ARANCIONE,
            "Puoi guidare, ma con più prudenza: su bagnato, in curva e nelle frenate brusche l'auto non corregge le sbandate. Se si accendono anche la spia ABS e quella rossa dei freni, vedi la scheda \"ABS + freni\".",
            listOf(
                "Sensore di velocità di una ruota sporco, danneggiato o con il cavo rovinato (la causa più frequente)",
                "Un guasto al motore: su molte auto, quando la centralina motore registra un errore, l'ESP si spegne per sicurezza",
                "Sensore dell'angolo di sterzo da ricalibrare (succede dopo lo stacco della batteria, una convergenza o lavori allo sterzo)",
                "Interruttore delle luci dei freni difettoso",
                "Tensione della batteria bassa",
                "Centralina o pompa ABS/ESP guasta (più raro e più costoso)"),
            listOf(
                "Controlla di non aver premuto il tasto ESP OFF (se c'è): premilo di nuovo",
                "Spegni il motore, aspetta un minuto e riaccendi: se la spia si spegne era un'anomalia momentanea",
                "Fai un controllo con l'app: se ci sono errori del motore, risolvi prima quelli",
                "Chiedi a qualcuno di guardare se le luci dei freni si accendono quando premi il pedale",
                "Se hai staccato la batteria da poco: a motore acceso gira il volante tutto a sinistra e tutto a destra, poi guida dritto per qualche centinaio di metri",
                "Guarda la tensione della batteria nella scheda \"Live\""),
            "Sì, se la spia resta accesa dopo i controlli. Chiedi una lettura della centralina ABS/ESP e la verifica dei sensori ruota prima di sostituire la centralina. $DIAGNOSI_TELAIO",
            "Sensore velocità ruota: 30–90 € + 40–80 € manodopera. Pulizia sensore/anello: 30–60 €. Calibrazione angolo sterzo: 30–60 €. Interruttore luci stop: 10–30 € + 20–30 €. Centralina ABS/ESP: riparazione 250–500 €, nuova 800–1.500 €.",
            "Nessun danno meccanico, ma in un'emergenza (sbandata, frenata su bagnato) l'auto è meno sicura. Con la spia accesa l'auto può non passare la revisione."))

    val ESP_LAMPEGGIA = Spia("esp_lamp", "ESP che lampeggia durante la guida",
        "La stessa spia gialla dell'ESP, ma lampeggia per qualche istante e poi si spegne.",
        Scheda(null, "Spia ESP che lampeggia in guida",
            "L'ESP sta intervenendo: ha rilevato che una ruota slitta o che l'auto inizia a sbandare e sta frenando le ruote per correggere. È il suo funzionamento normale.",
            VERDE,
            "Puoi guidare tranquillo, ma rallenta: la strada è scivolosa o stai affrontando la curva troppo veloce.",
            listOf("Strada bagnata, ghiacciata, sabbia o foglie", "Accelerazione brusca o curva veloce", "Gomme lisce o con pressione sbagliata"),
            listOf("Controlla la pressione e l'usura delle gomme"),
            "No, se la spia si spegne da sola.",
            "Nessun costo.",
            "Se lampeggia spesso anche su asciutto, fai controllare gomme e sensori ruota."))

    val ABS = Spia("abs", "ABS",
        "Gialla: un cerchio con la scritta ABS all'interno.",
        Scheda(null, "Spia ABS accesa",
            "Il sistema antibloccaggio delle ruote (ABS) è disattivato. I freni funzionano, ma in una frenata forte le ruote possono bloccarsi e l'auto non sterza. Di solito si spegne anche l'ESP.",
            ARANCIONE,
            "Puoi guidare con prudenza: tieni più distanza e frena prima. Se si accende anche la spia ROSSA dei freni, fermati.",
            listOf("Sensore di velocità di una ruota o il suo cavo (la causa più frequente)", "Anello dentato del sensore (sul mozzo) sporco o crepato",
                "Fusibile o relè dell'ABS", "Tensione della batteria bassa", "Centralina o pompa ABS guasta"),
            listOf("Spegni e riaccendi il motore", "Controlla il fusibile dell'ABS (vedi il libretto)", "Guarda la tensione della batteria nella scheda \"Live\""),
            "Sì, entro pochi giorni. $DIAGNOSI_TELAIO",
            "Sensore ruota: 30–90 € + 40–80 €. Anello dentato/mozzo: 60–200 € + 60–120 €. Centralina ABS: riparazione 250–500 €, nuova 800–1.500 €.",
            "In frenata d'emergenza le ruote si bloccano: spazi di frenata più lunghi e auto che non sterza. Con questa spia non si passa la revisione."))

    val FRENI = Spia("freni", "Freni (rossa)",
        "Rossa: un cerchio con un punto esclamativo (!) dentro, tra due archi, o la scritta BRAKE.",
        Scheda(null, "Spia rossa dei freni accesa",
            "Indica il freno a mano tirato oppure un problema all'impianto frenante, quasi sempre il liquido dei freni basso.",
            ROSSO,
            "Se il freno a mano è abbassato e la spia resta accesa: NON guidare. Il pedale può diventare lungo o morbido e l'auto frenare molto meno.",
            listOf("Freno a mano non del tutto abbassato", "Liquido freni basso: pastiglie molto consumate o perdita nell'impianto",
                "Sensore del livello liquido difettoso", "Se accesa insieme ad ABS: guasto della ripartizione di frenata (EBD)"),
            listOf("Abbassa del tutto il freno a mano", "A motore spento controlla il livello nella vaschetta del liquido freni (tra MIN e MAX)",
                "Guarda se ci sono macchie di liquido vicino alle ruote"),
            "Sì, subito. Se il liquido è sotto il minimo, fai trainare l'auto.",
            "Rabbocco/sostituzione liquido: 30–70 €. Pastiglie anteriori: 30–60 € + 40–60 €. Tubo o pinza che perde: 80–250 €.",
            "Perdita della capacità di frenare: pericolo grave."))

    val AIRBAG = Spia("airbag", "Airbag",
        "Rossa: una persona seduta con un pallone davanti, oppure la scritta AIRBAG o SRS.",
        Scheda(null, "Spia airbag accesa",
            "La centralina degli airbag ha trovato un problema e, per sicurezza, gli airbag e i pretensionatori delle cinture potrebbero non aprirsi in un incidente.",
            ARANCIONE,
            "L'auto si guida normalmente, ma in caso di incidente gli airbag potrebbero non funzionare. Fai controllare il prima possibile.",
            listOf("Connettore sotto un sedile anteriore allentato (frequente dopo aver spostato o tolto i sedili)",
                "Contatto spiralato nel volante usurato (spesso anche il clacson o i comandi al volante non vanno)",
                "Pretensionatore di una cintura difettoso", "Tensione della batteria bassa all'avviamento", "Centralina airbag (dopo un incidente va sostituita o resettata)"),
            listOf("Controlla che sotto i sedili anteriori nessun connettore giallo sia staccato o tirato (a quadro SPENTO)"),
            "Sì. Non smontare nulla dell'airbag da solo.",
            "Connettore: 0–50 €. Contatto spiralato: 40–120 € + 50–100 €. Pretensionatore: 80–200 € + 60 €. Diagnosi airbag: 30–60 €.",
            "In un incidente gli airbag possono non aprirsi. Con questa spia non si passa la revisione."))

    val BATTERIA = Spia("batteria", "Batteria (rossa)",
        "Rossa: un rettangolo con i segni + e −.",
        Scheda(null, "Spia batteria accesa a motore acceso",
            "L'alternatore non sta ricaricando la batteria: l'auto va avanti usando la carica rimasta, finché non si spegne.",
            ARANCIONE,
            "Puoi guidare per poco, spegnendo radio, clima e luci se possibile. Vai direttamente in officina o a casa. Se sale la temperatura del motore o lo sterzo diventa duro, fermati: può essersi rotta la cinghia.",
            listOf("Cinghia dei servizi rotta o lenta", "Alternatore guasto", "Cavo o connettore dell'alternatore", "Morsetti della batteria ossidati"),
            listOf("Guarda la tensione nella scheda \"Live\": a motore acceso deve essere 13,5–14,7 V", "A motore spento guarda se la cinghia c'è ed è tesa"),
            "Sì, subito.",
            "Cinghia servizi: 20–40 € + 30–60 €. Alternatore revisionato: 150–300 € + 50–100 €.",
            "L'auto si spegne quando la batteria si scarica, anche in marcia."))

    val OLIO = Spia("olio", "Pressione olio (rossa)",
        "Rossa: un'oliera (lampada di Aladino) con una goccia.",
        Scheda(null, "Spia pressione olio accesa",
            "Nel motore non c'è abbastanza pressione dell'olio: le parti del motore stanno lavorando senza lubrificazione.",
            ROSSO,
            "FERMATI subito in sicurezza e spegni il motore. Pochi minuti così possono distruggere il motore.",
            listOf("Livello olio molto basso (perdita o consumo)", "Pompa dell'olio guasta", "Sensore della pressione olio difettoso", "Filtro olio intasato"),
            listOf("Dopo 5 minuti a motore spento controlla l'asta dell'olio", "Se il livello è basso rabbocca con l'olio giusto e riparti solo se la spia si spegne subito"),
            "Sì. Se la spia resta accesa con l'olio a livello, fai trainare l'auto.",
            "Rabbocco: 10–20 €. Sensore pressione: 15–40 € + 30 €. Pompa olio: 150–400 € + manodopera.",
            "Motore grippato: riparazione da 1.500 € in su, spesso non conviene."))

    val TEMPERATURA = Spia("temperatura", "Temperatura motore (rossa)",
        "Rossa: un termometro immerso nelle onde.",
        Scheda(null, "Spia temperatura motore accesa",
            "Il motore si sta surriscaldando.",
            ROSSO,
            "FERMATI appena puoi e spegni il motore. Non aprire il tappo del radiatore a motore caldo.",
            listOf("Liquido di raffreddamento basso o perdita", "Ventola del radiatore che non parte", "Termostato bloccato", "Pompa dell'acqua", "Guarnizione della testata"),
            listOf("Aspetta 30 minuti a motore spento", "A motore freddo controlla il livello del liquido"),
            "Sì, subito.",
            "Da 20 € (rabbocco/tubo) a 600–1.200 € (guarnizione testata).",
            "Motore danneggiato in modo grave."))

    val MOTORE = Spia("motore", "Motore (fissa)",
        "Gialla: il profilo di un motore, oppure la scritta CHECK ENGINE.",
        Scheda(null, "Spia motore accesa fissa",
            "La centralina motore ha registrato un guasto. Il controllo di AutoCheck legge proprio questi errori e li spiega uno per uno.",
            ARANCIONE,
            "Di solito puoi guidare con prudenza. Leggi le schede dei codici trovati per sapere quanto è urgente.",
            listOf("Vedi i codici errore trovati dal controllo"),
            listOf("Fai il controllo completo con l'app"),
            "Dipende dal codice: vedi le schede.",
            "Dipende dal codice: vedi le schede.",
            "Con la spia già accesa non ti accorgi di nuovi guasti."))

    val MOTORE_LAMPEGGIA = Spia("motore_lamp", "Motore che lampeggia",
        "La spia gialla del motore lampeggia invece di restare fissa.",
        Scheda(null, "Spia motore che lampeggia",
            "Il motore sta perdendo colpi in modo grave: benzina incombusta arriva al catalizzatore e lo surriscalda.",
            ROSSO,
            "Rallenta subito e fermati appena puoi. Continuare può danneggiare il catalizzatore in pochi chilometri.",
            listOf("Candela o bobina di accensione guasta", "Iniettore", "Presa d'aria grossa"),
            listOf("Fai il controllo con l'app: vedrai su quale cilindro"),
            "Sì, subito.",
            "Candele/bobina: 60–150 €. Catalizzatore se si rovina: 300–800 €.",
            "Catalizzatore distrutto (300–800 €) e possibili danni al motore."))

    val SERVOSTERZO = Spia("servosterzo", "Servosterzo (EPS)",
        "Gialla o rossa: un volante con un punto esclamativo, oppure la scritta EPS.",
        Scheda(null, "Spia servosterzo accesa",
            "Il servosterzo elettrico si è disattivato o funziona a potenza ridotta: il volante diventa duro, soprattutto da fermo.",
            ARANCIONE,
            "Puoi guidare con prudenza: lo sterzo funziona, ma serve più forza. Attento nelle manovre e in curva.",
            listOf("Tensione della batteria bassa", "Sensore di coppia o di angolo dello sterzo", "Motorino o centralina del servosterzo", "Connettore ossidato"),
            listOf("Spegni il motore, aspetta un minuto e riaccendi", "Guarda la tensione della batteria nella scheda \"Live\""),
            "Sì, entro pochi giorni.",
            "Calibrazione sensore: 30–60 €. Centralina/piantone revisionato: 250–600 € + 80–150 €.",
            "Sterzo duro all'improvviso: rischio in manovra o in curva."))

    val CANDELETTE = Spia("candelette", "Candelette (diesel)",
        "Gialla: una spirale (molla). Normale che si accenda per pochi secondi all'avvio.",
        Scheda(null, "Spia candelette che resta accesa o lampeggia",
            "Sui diesel la spia delle candelette che lampeggia in marcia indica spesso un guasto al motore.",
            ARANCIONE,
            "Puoi guidare con prudenza: l'auto può avere poca potenza (modalità di emergenza).",
            listOf("Candelette o loro centralina", "Guasto al motore registrato dalla centralina", "Filtro antiparticolato intasato"),
            listOf("Fai il controllo con l'app: leggerai il codice del motore"),
            "Sì, entro pochi giorni.",
            "Candelette: 15–30 € l'una + 50–150 €.",
            "Avviamenti difficili e possibile modalità di emergenza."))

    val TUTTE = listOf(MOTORE, MOTORE_LAMPEGGIA, ESP, ESP_LAMPEGGIA, ABS, FRENI, AIRBAG, BATTERIA, OLIO, TEMPERATURA, SERVOSTERZO, CANDELETTE)

    fun perId(id: String) = TUTTE.firstOrNull { it.id == id }

    /** Combinazioni di spie che cambiano la diagnosi. */
    fun combinazioni(accese: Set<String>, codiciMotore: List<String>): List<Scheda> {
        val out = ArrayList<Scheda>()
        if ("abs" in accese && "freni" in accese) out.add(Scheda(null, "ABS e freni accesi insieme",
            "Quando si accendono insieme la spia ABS e quella rossa dei freni, può essere disattivata anche la ripartizione della frenata tra anteriore e posteriore (EBD).",
            ROSSO, "Non guidare: in frenata il posteriore può bloccarsi e far girare l'auto. Se devi muoverti, pochissimi km, piano.",
            listOf("Guasto alla centralina ABS o alla sua alimentazione", "Liquido freni basso insieme a un guasto ABS", "Più sensori ruota guasti"),
            listOf("Controlla il livello del liquido freni", "Controlla i fusibili dell'ABS"),
            "Sì, subito.", "Da 60 € (sensore) a 800–1.500 € (centralina ABS nuova).",
            "Frenate instabili: pericolo grave."))
        if (("esp" in accese || "abs" in accese) && "batteria" in accese) out.add(Scheda(null, "Più spie accese insieme alla batteria",
            "Quando la batteria non viene ricaricata, la tensione scende e varie centraline (ESP, ABS, servosterzo) si spengono e accendono la propria spia. Il problema vero è la ricarica.",
            ARANCIONE, "Vedi la scheda della spia batteria: vai direttamente in officina.",
            listOf("Alternatore o cinghia dei servizi"), listOf("Guarda la tensione nella scheda \"Live\""),
            "Sì, subito: chiedi di controllare la ricarica prima dei singoli sistemi.", "Alternatore: 200–400 € in totale. Cinghia: 50–100 €.",
            "Auto che si spegne in marcia."))
        if ("esp" in accese && codiciMotore.isNotEmpty()) out.add(Scheda(null, "ESP acceso insieme a un errore del motore",
            "La centralina motore ha registrato degli errori (${codiciMotore.joinToString(", ")}). Su molte auto, quando c'è un guasto al motore l'ESP si disattiva da solo per sicurezza e accende la sua spia: la causa potrebbe essere il motore, non l'ESP.",
            ARANCIONE, "Puoi guidare con prudenza.",
            listOf("Guasto al motore che spegne l'ESP (verifica prima questo)", "Guasto proprio dell'ESP (vedi la scheda della spia ESP)"),
            listOf("Ripara prima il guasto del motore, cancella gli errori e guida qualche km: se la spia ESP si spegne, era quello"),
            "Sì, per il guasto del motore.", "Vedi le schede dei codici del motore.",
            "Resti senza ESP finché il guasto del motore non è risolto."))
        return out
    }
}
