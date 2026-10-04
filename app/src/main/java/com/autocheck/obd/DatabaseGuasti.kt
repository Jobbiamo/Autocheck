package com.autocheck.obd

import com.autocheck.obd.Gravita.ARANCIONE
import com.autocheck.obd.Gravita.ROSSO
import com.autocheck.obd.Gravita.VERDE

/**
 * Database dei guasti, consultato offline.
 * I costi sono indicativi per un'utilitaria (es. Opel Agila) in un'officina
 * indipendente in Italia: manodopera circa 40–60 €/ora, pezzi di ricambio non originali di buona qualità.
 */
object DatabaseGuasti {

    private val esatti = HashMap<String, Scheda>()
    private val intervalli = ArrayList<Triple<String, String, (String) -> Scheda>>()

    private const val DIAGNOSI = "Una diagnosi professionale in officina costa di solito 30–60 €."

    fun trova(codice: String): Scheda {
        val c = codice.uppercase()
        esatti[c]?.let { return it.copy(codice = c) }
        for ((da, a, crea) in intervalli) {
            if (nelRange(c, da, a)) return crea(c)
        }
        return generica(c)
    }

    fun numeroCodiciConosciuti(): Int = esatti.size

    private fun nelRange(c: String, da: String, a: String): Boolean {
        if (c.length != 5 || c[0] != da[0]) return false
        val v = c.substring(1).toIntOrNull(16) ?: return false
        val min = da.substring(1).toInt(16)
        val max = a.substring(1).toInt(16)
        return v in min..max
    }

    private fun s(
        codici: String, titolo: String, spiegazione: String, g: Gravita, guida: String,
        cause: List<String>, faiDaTe: List<String>, meccanico: String, costo: String, seIgnori: String
    ) {
        for (c in codici.split(",")) {
            val k = c.trim()
            esatti[k] = Scheda(k, titolo, spiegazione, g, guida, cause, faiDaTe, meccanico, costo, seIgnori)
        }
    }

    private fun r(da: String, a: String, crea: (String) -> Scheda) {
        intervalli.add(Triple(da, a, crea))
    }

    init {
        // ───────────── MISCELA ARIA / CARBURANTE ─────────────
        s("P0171,P0174", "Miscela troppo magra (troppa aria, poca benzina)",
            "La centralina deve aggiungere molta più benzina del normale: nel motore entra aria \"di troppo\" oppure arriva poco carburante.",
            ARANCIONE, "Puoi guidare normalmente, ma il motore può perdere potenza, avere un minimo irregolare e consumare di più. Non rimandare per mesi.",
            listOf("Presa d'aria: un tubo di gomma del vuoto crepato o staccato (la causa più frequente)",
                "Guarnizione del collettore di aspirazione rovinata",
                "Sensore pressione aspirazione (MAP) o debimetro sporco",
                "Pompa benzina debole o filtro benzina intasato",
                "Iniettori sporchi"),
            listOf("A motore acceso, ascolta se c'è un sibilo vicino ai tubi di gomma sopra il motore",
                "Guarda se ci sono tubi di gomma crepati o sfilati"),
            "Sì, se non trovi tu un tubo staccato. Chiedi la ricerca di prese d'aria (prova fumo) prima di cambiare pezzi.",
            "Tubo del vuoto: 10–30 € + 30 € manodopera. Guarnizione collettore: 20–40 € + 60–120 €. Filtro benzina: 15–30 € + 30–50 €. Pompa benzina: 80–200 € + 80–150 €.",
            "Il motore lavora \"magro\" e scalda di più: nel tempo rischi mancate accensioni, candele e catalizzatore rovinati (catalizzatore: 300–800 €).")

        s("P0172,P0175", "Miscela troppo grassa (troppa benzina)",
            "La centralina sta togliendo benzina perché nel motore ne arriva troppa rispetto all'aria.",
            ARANCIONE, "Puoi guidare, ma consumi di più e inquini. Fai controllare entro qualche settimana.",
            listOf("Filtro dell'aria molto sporco", "Iniettore che gocciola", "Sensore pressione aspirazione (MAP) o debimetro difettoso",
                "Sonda lambda che sbaglia la lettura", "Regolatore pressione benzina difettoso"),
            listOf("Controlla il filtro dell'aria: se è nero e intasato, cambialo (10–20 €, si fa in 5 minuti)"),
            "Sì, se il filtro dell'aria è pulito.",
            "Filtro aria: 10–20 €. Sensore MAP: 40–100 € + 30 €. Sonda lambda: 40–120 € + 30–60 €. Iniettore: 60–150 € l'uno + 60–100 €.",
            "Consumi alti, candele sporche e catalizzatore che si intasa e si surriscalda (300–800 €). Probabile bocciatura alla revisione.")

        s("P2187,P2189", "Miscela troppo magra al minimo",
            "Al minimo entra aria di troppo nel motore. Quasi sempre è una piccola perdita nei tubi di aspirazione.",
            VERDE, "Puoi guidare tranquillo. Il minimo può essere un po' irregolare.",
            listOf("Tubo del vuoto o del servofreno con una piccola crepa", "Guarnizione del corpo farfallato", "Valvola sfiato vapori olio (PCV) difettosa"),
            listOf("Ascolta a motore acceso se senti un sibilo nel vano motore"),
            "Sì, per la ricerca della presa d'aria (lavoro breve).",
            "Di solito 30–100 € in tutto (tubo o guarnizione + manodopera).",
            "Il problema tende a peggiorare e a diventare una miscela magra anche in marcia (vedi P0171).")

        s("P2188,P2190", "Miscela troppo grassa al minimo",
            "Al minimo arriva più benzina del necessario.",
            VERDE, "Puoi guidare tranquillo.",
            listOf("Iniettore che gocciola", "Valvola spurgo vapori benzina (canister) bloccata aperta", "Sensore MAP difettoso"),
            listOf("Controlla che il filtro dell'aria non sia intasato"),
            "Sì, alla prima occasione.",
            "Valvola spurgo: 30–70 € + 30 €. Iniettore: 60–150 € + 60–100 €.",
            "Consumi più alti e catalizzatore che si rovina nel tempo.")

        // ───────────── SENSORI ARIA ─────────────
        r("P0100", "P0104") { c -> Scheda(c, "Debimetro (sensore che misura l'aria) difettoso",
            "Il sensore che misura quanta aria entra nel motore dà valori sbagliati o non risponde.",
            ARANCIONE, "Puoi guidare, ma il motore può avere meno potenza o andare \"in protezione\" (recovery). Vai dal meccanico a breve.",
            listOf("Debimetro sporco", "Connettore ossidato o staccato", "Debimetro guasto", "Filtro aria non montato bene"),
            listOf("Controlla che il connettore del debimetro (dopo la scatola del filtro aria) sia ben inserito",
                "Si può pulire con lo spray specifico per debimetri (10 €), senza toccare il filo interno"),
            "Sì, se la pulizia non risolve.",
            "Pulizia: 10 € fai da te. Debimetro nuovo: 60–180 € + 20–30 € manodopera.",
            "Consumi alti, poca potenza e, se la miscela va molto fuori, danni a candele e catalizzatore.") }

        r("P0105", "P0109") { c -> Scheda(c, "Sensore pressione aspirazione (MAP) difettoso",
            "Il sensore che misura la pressione nell'aspirazione (serve a calcolare quanta benzina iniettare) dà valori sbagliati.",
            ARANCIONE, "Puoi guidare con prudenza: il motore può andare a strappi o consumare molto. Fai controllare a breve.",
            listOf("Sensore MAP sporco o guasto", "Tubetto del sensore crepato", "Connettore ossidato"),
            listOf("Controlla che il connettore del sensore sul collettore di aspirazione sia ben inserito"),
            "Sì.",
            "Sensore MAP: 40–100 € + 20–30 € manodopera.",
            "Miscela sbagliata: consumi alti, candele sporche e catalizzatore a rischio.") }

        r("P0110", "P0114") { c -> Scheda(c, "Sensore temperatura aria aspirata difettoso",
            "Il sensore che misura la temperatura dell'aria che entra nel motore non funziona bene. È un problema minore.",
            VERDE, "Puoi guidare tranquillo. Al massimo noterai avviamenti a freddo un po' più difficili.",
            listOf("Connettore staccato o ossidato", "Sensore guasto (spesso è integrato nel sensore MAP o nel debimetro)"),
            listOf("Controlla i connettori dei sensori sul tubo dell'aria e sul collettore"),
            "Alla prossima occasione, non è urgente.",
            "Sensore: 15–60 € + 20 € manodopera.",
            "Lievi aumenti di consumo, nessun danno grave.") }

        // ───────────── TEMPERATURA MOTORE ─────────────
        r("P0115", "P0119") { c -> Scheda(c, "Sensore temperatura motore difettoso",
            "La centralina non legge bene la temperatura del liquido di raffreddamento. È importante: da questo dato dipende anche l'accensione della ventola.",
            ARANCIONE, "Puoi guidare, ma tieni d'occhio la lancetta della temperatura. Se sale oltre metà, fermati. Vai dal meccanico entro pochi giorni.",
            listOf("Sensore temperatura guasto (pezzo economico)", "Connettore ossidato", "Livello liquido di raffreddamento basso"),
            listOf("A motore FREDDO controlla il livello del liquido nella vaschetta (tra MIN e MAX)"),
            "Sì, ma è un lavoro rapido.",
            "Sensore: 10–40 € + 30–50 € manodopera.",
            "Se la ventola non parte quando serve, il motore può surriscaldarsi: guarnizione della testata 600–1.200 €.") }

        s("P0125", "Il motore impiega troppo a scaldarsi",
            "Il motore ci mette troppo a raggiungere la temperatura giusta. Quasi sempre è il termostato rimasto aperto.",
            VERDE, "Puoi guidare tranquillo. D'inverno il riscaldamento sarà più debole.",
            listOf("Termostato bloccato aperto (molto frequente)", "Sensore temperatura difettoso", "Liquido di raffreddamento basso"),
            listOf("A motore freddo controlla il livello del liquido di raffreddamento"),
            "Sì, alla prima occasione (anche al prossimo tagliando).",
            "Termostato: 20–50 € + 50–120 € manodopera (con il liquido da rabboccare).",
            "Consumi più alti, motore che si usura di più perché lavora freddo, riscaldamento scarso.")

        s("P0128", "Termostato: il motore resta troppo freddo",
            "Il termostato (la valvola che fa scaldare il motore) resta aperto e il motore non arriva alla temperatura corretta.",
            VERDE, "Puoi guidare tranquillo, non è pericoloso.",
            listOf("Termostato bloccato aperto (causa quasi certa)", "Sensore temperatura che legge male"),
            listOf("Niente di specifico: è un pezzo da sostituire"),
            "Sì, quando ti è comodo (entro qualche settimana).",
            "Termostato: 20–50 € + 50–120 € manodopera. Totale tipico: 80–170 €.",
            "Consumi più alti del 5–10%, riscaldamento debole d'inverno, maggiore usura del motore.")

        s("P0217", "Motore surriscaldato",
            "La temperatura del motore ha superato il limite di sicurezza.",
            ROSSO, "FERMATI appena puoi in sicurezza e spegni il motore. Continuare a guidare può distruggere il motore.",
            listOf("Liquido di raffreddamento basso o perdita", "Ventola del radiatore che non parte", "Termostato bloccato chiuso",
                "Pompa dell'acqua guasta", "Guarnizione della testata bruciata"),
            listOf("Aspetta almeno 30 minuti a motore spento. NON aprire il tappo del radiatore a motore caldo: il liquido bollente schizza",
                "A motore freddo controlla il livello del liquido e cerca macchie sotto l'auto"),
            "Sì, assolutamente. Se il liquido è sparito, fai trainare l'auto.",
            "Rabbocco/perdita tubo: 20–80 €. Ventola: 80–200 € + 50 €. Termostato: 80–170 € totali. Pompa acqua: 150–350 € (spesso insieme alla cinghia). Guarnizione testata: 600–1.200 €.",
            "Testata deformata o motore grippato: riparazione da 1.000 € in su, spesso non conviene su un'auto datata.")

        // ───────────── ACCELERATORE / FARFALLA ─────────────
        r("P0120", "P0124") { c -> Scheda(c, "Sensore posizione acceleratore/farfalla difettoso",
            "La centralina non capisce bene quanto è aperta la farfalla (la valvola che regola l'aria quando acceleri).",
            ARANCIONE, "Guida con prudenza: l'auto può accelerare a scatti o entrare in modalità di emergenza con poca potenza. Vai dal meccanico entro pochi giorni.",
            listOf("Corpo farfallato sporco", "Sensore posizione farfalla usurato", "Connettore ossidato"),
            listOf("Controlla che il connettore sul corpo farfallato sia ben inserito"),
            "Sì.",
            "Pulizia corpo farfallato: 40–80 €. Sensore: 30–90 € + 30–50 €. Corpo farfallato completo: 150–350 € + 50 €.",
            "Accelerazioni imprevedibili e possibile modalità di emergenza nel momento meno adatto (es. in sorpasso).") }

        s("P2135", "Segnali del sensore acceleratore/farfalla non coincidono",
            "I due segnali che indicano la posizione della farfalla non corrispondono tra loro.",
            ARANCIONE, "Guida con prudenza: l'auto può entrare in modalità di emergenza con poca potenza.",
            listOf("Corpo farfallato sporco o usurato", "Connettore o cablaggio difettoso", "Pedale acceleratore elettronico difettoso"),
            listOf("Controlla il connettore sul corpo farfallato"),
            "Sì, entro pochi giorni.",
            "Pulizia: 40–80 €. Corpo farfallato: 150–350 € + 50 €. Pedale acceleratore: 60–150 € + 30 €.",
            "Perdita improvvisa di potenza durante la guida.")

        // ───────────── SONDE LAMBDA ─────────────
        r("P0130", "P0135") { c -> Scheda(c, "Sonda lambda anteriore (prima del catalizzatore) difettosa",
            "La sonda che misura l'ossigeno nei gas di scarico, e che aiuta il motore a dosare la benzina, non funziona bene.",
            ARANCIONE, "Puoi guidare normalmente, ma consumerai di più. Fai sostituire entro qualche settimana.",
            listOf("Sonda lambda esaurita (dopo 100.000–150.000 km è normale)", "Riscaldatore interno della sonda guasto", "Cavo o connettore danneggiato", "Perdita nello scarico prima della sonda"),
            listOf("Niente di pratico: serve una chiave apposita"),
            "Sì.",
            "Sonda lambda: 40–120 € + 30–60 € manodopera. Totale tipico: 80–170 €.",
            "Consumi +10–15%, catalizzatore che si rovina nel tempo (300–800 €), bocciatura alla revisione.") }

        r("P0136", "P0141") { c -> Scheda(c, "Sonda lambda posteriore (dopo il catalizzatore) difettosa",
            "La seconda sonda, che controlla se il catalizzatore funziona, non lavora bene. Incide poco sulla guida.",
            VERDE, "Puoi guidare tranquillo.",
            listOf("Sonda esaurita", "Riscaldatore della sonda guasto", "Connettore ossidato (è sotto l'auto, prende acqua e sporco)"),
            listOf("Niente di pratico"),
            "Sì, ma senza urgenza. Va risolto prima della revisione.",
            "Sonda lambda: 40–120 € + 30–60 €.",
            "La spia resta accesa (e non ti accorgeresti di un nuovo guasto) e rischi la bocciatura alla revisione.") }

        r("P0150", "P0167") { c -> Scheda(c, "Sonda lambda (secondo banco) difettosa",
            "Una sonda che misura l'ossigeno nei gas di scarico non funziona bene.",
            ARANCIONE, "Puoi guidare, ma fai controllare entro qualche settimana.",
            listOf("Sonda esaurita", "Riscaldatore interno guasto", "Cavo danneggiato"),
            listOf("Niente di pratico"),
            "Sì.",
            "Sonda lambda: 40–120 € + 30–60 €.",
            "Consumi più alti e catalizzatore a rischio.") }

        s("P2195,P2196,P2197,P2198", "Sonda lambda bloccata (segnale fermo)",
            "La sonda lambda manda sempre lo stesso valore: o è esaurita, o la miscela è davvero sbagliata.",
            ARANCIONE, "Puoi guidare, ma consumi di più. Fai controllare entro qualche settimana.",
            listOf("Sonda lambda esaurita", "Presa d'aria o perdita nello scarico", "Problema di pressione benzina"),
            listOf("Niente di pratico"),
            "Sì.",
            "Sonda lambda: 40–120 € + 30–60 €.",
            "Consumi alti e catalizzatore danneggiato nel tempo.")

        s("P2096,P2097", "Correzione miscela dopo il catalizzatore fuori limite",
            "La sonda dopo il catalizzatore vede una miscela troppo magra (P2096) o troppo grassa (P2097).",
            VERDE, "Puoi guidare tranquillo.",
            listOf("Perdita nello scarico", "Sonda lambda posteriore esaurita", "Catalizzatore esaurito"),
            listOf("A motore acceso ascolta se c'è un rumore di \"soffio\" sotto l'auto"),
            "Sì, senza urgenza.",
            "Sonda: 80–170 € totali. Riparazione scarico: 50–150 €.",
            "Il catalizzatore può peggiorare: bocciatura alla revisione.")

        // ───────────── INIETTORI / CARBURANTE ─────────────
        for (n in 1..4) {
            s("P020$n", "Iniettore del cilindro $n: problema elettrico",
                "La centralina rileva un problema al circuito elettrico dell'iniettore $n (il \"rubinetto\" che spruzza la benzina nel cilindro).",
                ARANCIONE, "Puoi guidare con prudenza: il motore può girare a 3 cilindri, tremare e avere poca potenza. Vai dal meccanico a breve.",
                listOf("Connettore dell'iniettore ossidato o staccato", "Cavo interrotto", "Iniettore guasto"),
                listOf("Controlla che il connettore dell'iniettore $n sia ben inserito"),
                "Sì.",
                "Connettore/cavo: 30–80 €. Iniettore: 60–150 € + 60–100 € manodopera.",
                "Benzina incombusta che arriva al catalizzatore e lo surriscalda (300–800 €).")
        }

        s("P0230,P0231,P0232", "Pompa benzina: problema elettrico",
            "La centralina rileva un problema al circuito che alimenta la pompa della benzina.",
            ARANCIONE, "Guida con prudenza: l'auto potrebbe spegnersi o non ripartire. Evita viaggi lunghi finché non è controllata.",
            listOf("Relè della pompa difettoso (economico)", "Fusibile", "Pompa benzina a fine vita", "Cablaggio"),
            listOf("Controlla il fusibile della pompa benzina (vedi il libretto d'uso)"),
            "Sì, entro pochi giorni.",
            "Relè: 10–30 €. Pompa benzina: 80–200 € + 80–150 € manodopera.",
            "L'auto può lasciarti a piedi all'improvviso.")

        s("P0087", "Pressione del carburante troppo bassa (diesel)",
            "Nel sistema di iniezione diesel non c'è abbastanza pressione.",
            ROSSO, "Evita di guidare: il motore può spegnersi all'improvviso o non partire. Se devi muoverti fallo solo per pochi km, piano.",
            listOf("Filtro gasolio intasato (molto frequente)", "Aria nel circuito del gasolio", "Pompa di alta pressione usurata", "Regolatore pressione difettoso"),
            listOf("Ricorda quando hai cambiato il filtro gasolio: se sono passati più di 30.000 km è il primo sospettato"),
            "Sì.",
            "Filtro gasolio: 20–40 € + 30–50 €. Regolatore: 80–200 € + 60 €. Pompa alta pressione: 600–1.500 €.",
            "Spegnimenti in marcia (pericolosi) e danni costosi alla pompa e agli iniettori.")

        s("P0088", "Pressione del carburante troppo alta (diesel)",
            "Nel sistema di iniezione diesel la pressione è più alta del previsto.",
            ARANCIONE, "Guida con prudenza: l'auto può entrare in modalità di emergenza.",
            listOf("Regolatore di pressione difettoso", "Sensore pressione rail difettoso"),
            listOf("Niente di pratico"),
            "Sì, entro pochi giorni.",
            "Sensore/regolatore: 80–250 € + 60–100 €.",
            "Usura degli iniettori e possibili perdite.")

        s("P0093", "Grossa perdita di carburante rilevata (diesel)",
            "La centralina rileva un calo di pressione tipico di una perdita di gasolio.",
            ROSSO, "FERMATI: una perdita di gasolio sul motore caldo è un rischio di incendio.",
            listOf("Tubo del gasolio rotto o raccordo allentato", "Iniettore che perde", "Pompa che perde"),
            listOf("Guarda se c'è gasolio sul motore o sotto l'auto e senti se c'è odore forte di gasolio"),
            "Sì, e fai trainare l'auto se vedi gasolio che cola.",
            "Raccordo/tubo: 30–100 €. Iniettore: 150–400 € + 100 €.",
            "Rischio di incendio e di restare fermi.")

        // ───────────── MANCATE ACCENSIONI ─────────────
        s("P0300", "Il motore perde colpi su più cilindri",
            "Il motore ha mancate accensioni (\"perde colpi\") su più cilindri: la benzina non brucia bene. Lo senti come vibrazioni, strappi o poca potenza.",
            ARANCIONE, "Guida solo con prudenza e senza sforzare il motore. Se la spia motore LAMPEGGIA o l'auto trema forte: fermati, il catalizzatore si sta danneggiando.",
            listOf("Candele consumate (se non le cambi da più di 30.000–40.000 km sono le prime sospettate)", "Bobina di accensione difettosa",
                "Presa d'aria / miscela magra", "Benzina di cattiva qualità o acqua nel serbatoio", "Compressione bassa (motore usurato, raro)"),
            listOf("Ricorda quando sono state cambiate le candele", "Se hai fatto benzina da poco in un distributore nuovo, prova a fare il pieno altrove"),
            "Sì, entro pochi giorni.",
            "Candele (4): 20–50 € + 30–50 €. Bobina: 40–90 € + 20–30 €. Totale tipico: 60–150 €.",
            "Catalizzatore rovinato dalla benzina incombusta (300–800 €) e possibili danni al motore.")

        for (n in 1..6) {
            s("P030$n", "Il motore perde colpi sul cilindro $n",
                "Nel cilindro $n la benzina non brucia correttamente. Lo senti come vibrazione, motore che \"zoppica\" o meno potenza.",
                ARANCIONE, "Guida con prudenza e senza sforzare il motore. Se la spia motore LAMPEGGIA o l'auto trema forte: fermati.",
                listOf("Candela del cilindro $n consumata o sporca (la più frequente)", "Bobina di accensione del cilindro $n guasta",
                    "Iniettore del cilindro $n sporco o guasto", "Compressione bassa nel cilindro (valvola o fasce, più raro)"),
                listOf("Ricorda quando hai cambiato le candele: se sono vecchie si cambiano tutte e 4",
                    "Trucco anti-fregatura: chiedi al meccanico di scambiare la bobina del cilindro $n con un altro cilindro. Se il codice \"si sposta\", è la bobina; se resta, è candela o iniettore"),
                "Sì, entro pochi giorni.",
                "Candele (4): 20–50 € + 30–50 €. Bobina: 40–90 € + 20–30 €. Iniettore: 60–150 € + 60–100 €. Nella maggior parte dei casi: 60–150 €.",
                "Benzina incombusta che rovina il catalizzatore (300–800 €), candela che può danneggiare il cilindro.")
        }

        s("P0335,P0336,P0337,P0338", "Sensore giri motore (albero motore) difettoso",
            "La centralina non legge bene il sensore che le dice a che velocità e in che posizione gira il motore. Senza questo segnale il motore non può funzionare.",
            ROSSO, "Evita di guidare: il motore può spegnersi di colpo in marcia (anche in autostrada) o non ripartire più.",
            listOf("Sensore albero motore guasto", "Connettore ossidato o cavo rovinato", "Ruota fonica sporca o danneggiata (raro)"),
            listOf("Niente di pratico"),
            "Sì, subito.",
            "Sensore: 20–70 € + 30–80 € manodopera. Totale tipico: 60–150 €.",
            "Spegnimento improvviso durante la guida, con perdita di servosterzo e servofreno: pericoloso.")

        s("P0340,P0341,P0342,P0343", "Sensore albero a camme (fase motore) difettoso",
            "La centralina non legge bene il sensore che indica la fase del motore.",
            ARANCIONE, "Puoi guidare con prudenza: l'avviamento può essere più lungo e il motore può andare in protezione. Vai dal meccanico entro pochi giorni.",
            listOf("Sensore guasto", "Connettore ossidato", "Catena o cinghia di distribuzione allungata o saltata di un dente (più grave)"),
            listOf("Niente di pratico"),
            "Sì, entro pochi giorni. Chiedi di verificare anche la distribuzione.",
            "Sensore: 20–70 € + 30–60 €. Distribuzione (se è quella): 250–600 €.",
            "Se la causa è la distribuzione, una rottura può distruggere il motore.")

        for (n in 1..4) {
            s("P035$n", "Bobina di accensione del cilindro $n difettosa",
                "La bobina che crea la scintilla per la candela del cilindro $n ha un problema elettrico.",
                ARANCIONE, "Guida con prudenza: il motore può perdere colpi. Se la spia lampeggia, fermati.",
                listOf("Bobina guasta", "Connettore della bobina ossidato o staccato", "Cavo danneggiato"),
                listOf("Controlla che il connettore della bobina sia ben inserito"),
                "Sì, entro pochi giorni.",
                "Bobina: 40–90 € + 20–30 €.",
                "Mancate accensioni e catalizzatore rovinato (300–800 €).")
        }

        r("P0325", "P0334") { c -> Scheda(c, "Sensore di detonazione difettoso",
            "Il sensore che \"ascolta\" se il motore batte in testa non funziona. La centralina, per sicurezza, riduce l'anticipo.",
            VERDE, "Puoi guidare. Il motore potrebbe avere un po' meno potenza e consumare leggermente di più.",
            listOf("Sensore guasto", "Connettore ossidato", "Cavo danneggiato"),
            listOf("Niente di pratico"),
            "Sì, senza urgenza.",
            "Sensore: 30–80 € + 40–100 € (a volte è in posizione scomoda).",
            "Meno potenza e consumi un po' più alti. Rischio basso.") }

        // ───────────── EMISSIONI ─────────────
        s("P0420,P0421,P0430", "Catalizzatore poco efficiente",
            "Il catalizzatore (il \"filtro\" che pulisce i gas di scarico) non lavora più bene come dovrebbe.",
            ARANCIONE, "Puoi guidare normalmente. Non è pericoloso, ma con questo errore NON passi la revisione.",
            listOf("Sonda lambda posteriore esaurita (controllare PRIMA di cambiare il catalizzatore!)", "Catalizzatore esaurito per età o chilometri",
                "Perdita nello scarico", "Motore che ha perso colpi a lungo e ha rovinato il catalizzatore"),
            listOf("Nessuno, ma attenzione: è il guasto su cui è più facile essere fregati"),
            "Sì. Chiedi di verificare prima la sonda lambda posteriore e lo scarico: un catalizzatore costa molto più di una sonda.",
            "Sonda lambda: 80–170 € totali. Catalizzatore non originale omologato: 250–600 € + 60–120 €. Originale: anche 800–1.200 €.",
            "Bocciatura alla revisione. Se il catalizzatore si intasa, il motore perde potenza.")

        s("P0440,P0441,P0446,P0449", "Impianto vapori benzina: anomalia",
            "Il sistema che raccoglie i vapori della benzina del serbatoio (per non farli uscire nell'aria) ha un problema.",
            VERDE, "Puoi guidare tranquillo: non incide sul motore.",
            listOf("Tappo del serbatoio chiuso male o guarnizione rovinata", "Valvola o elettrovalvola del canister difettosa", "Tubo dei vapori crepato"),
            listOf("Riapri e richiudi il tappo della benzina fino a sentire i \"clic\". Dopo qualche giorno di guida l'errore può sparire da solo"),
            "Solo se il tappo non risolve.",
            "Tappo: 10–30 €. Elettrovalvola: 30–80 € + 30 €. Ricerca perdita con fumo: 40–80 €.",
            "Odore di benzina e la spia accesa che nasconde eventuali guasti nuovi.")

        s("P0442,P0455,P0456,P0457", "Perdita nell'impianto vapori benzina (spesso il tappo!)",
            "Il sistema dei vapori della benzina ha una perdita. Nella maggior parte dei casi è il tappo del serbatoio.",
            VERDE, "Puoi guidare tranquillo.",
            listOf("Tappo del serbatoio non chiuso bene o guarnizione consumata (causa più frequente)", "Tubo dei vapori crepato", "Valvola del canister"),
            listOf("Controlla e richiudi il tappo della benzina fino ai \"clic\"", "Se la guarnizione del tappo è crepata, comprane uno nuovo (10–30 €)"),
            "Solo se, dopo aver sistemato il tappo e fatto qualche giorno di guida, l'errore torna.",
            "Tappo: 10–30 €. Ricerca perdita con fumo: 40–80 €. Tubo/valvola: 30–100 €.",
            "Odore di benzina e spia sempre accesa.")

        s("P0443,P0444,P0445,P0458,P0459", "Elettrovalvola vapori benzina (canister) difettosa",
            "La valvola che manda i vapori della benzina al motore ha un problema elettrico.",
            VERDE, "Puoi guidare tranquillo. A volte il minimo può essere un po' irregolare dopo il rifornimento.",
            listOf("Elettrovalvola guasta", "Connettore ossidato"),
            listOf("Niente di pratico"),
            "Sì, senza urgenza.",
            "Elettrovalvola: 30–80 € + 20–30 €.",
            "Minimo irregolare, odore di benzina, spia accesa.")

        r("P0400", "P0409") { c -> Scheda(c, "Valvola EGR (ricircolo gas di scarico) difettosa",
            "La valvola che rimanda una parte dei gas di scarico nel motore (per inquinare meno) è bloccata, sporca o non risponde.",
            ARANCIONE, "Puoi guidare, ma con meno potenza o strappi. Fai controllare entro qualche settimana.",
            listOf("Valvola EGR incrostata di carbone (molto frequente, soprattutto sui diesel e in città)", "Valvola guasta", "Tubetti o connettore danneggiati"),
            listOf("Ogni tanto fai un tratto di autostrada a regime medio-alto: aiuta a tenerla pulita"),
            "Sì.",
            "Pulizia EGR: 80–150 €. Valvola nuova: 120–350 € + 60–150 €.",
            "Fumo nero (diesel), poca potenza, consumi alti; sui diesel peggiora anche il filtro antiparticolato.") }

        s("P0491,P0492", "Aria secondaria: anomalia",
            "Il sistema che soffia aria nello scarico a freddo (per scaldare prima il catalizzatore) non funziona.",
            VERDE, "Puoi guidare tranquillo.",
            listOf("Pompa aria secondaria guasta", "Valvola bloccata", "Relè o fusibile"),
            listOf("Niente di pratico"),
            "Sì, senza urgenza (prima della revisione).",
            "Valvola: 50–150 €. Pompa: 150–350 € + 50 €.",
            "Emissioni a freddo più alte, possibile bocciatura alla revisione.")

        // ───────────── VENTOLA / VELOCITÀ / MINIMO ─────────────
        s("P0480,P0481,P0482,P0483", "Ventola del radiatore: problema elettrico",
            "La centralina rileva un problema al circuito della ventola che raffredda il radiatore.",
            ARANCIONE, "Puoi guidare, ma in coda o in città il motore può scaldare troppo. Tieni d'occhio la temperatura e vai dal meccanico entro pochi giorni.",
            listOf("Relè della ventola", "Fusibile", "Motorino della ventola guasto", "Connettore ossidato"),
            listOf("Controlla il fusibile della ventola", "Con motore caldo al minimo, la ventola dovrebbe partire da sola: verifica che giri"),
            "Sì, entro pochi giorni.",
            "Relè/fusibile: 5–30 €. Ventola: 80–200 € + 50 €.",
            "Surriscaldamento in coda o d'estate: guarnizione della testata 600–1.200 €.")

        s("P0500,P0501,P0502,P0503", "Sensore velocità del veicolo difettoso",
            "La centralina non legge bene la velocità dell'auto.",
            ARANCIONE, "Puoi guidare con prudenza. Il tachimetro potrebbe non funzionare; se c'è anche la spia ABS, la frenata d'emergenza è meno assistita.",
            listOf("Sensore velocità/ABS guasto o sporco", "Cavo rovinato vicino alla ruota", "Connettore ossidato"),
            listOf("Guarda se il tachimetro funziona e se è accesa anche la spia ABS"),
            "Sì, entro pochi giorni.",
            "Sensore: 20–80 € + 30–60 €.",
            "Tachimetro inaffidabile, ABS e controllo di trazione possono disattivarsi.")

        s("P0505,P0506,P0507", "Problema di regolazione del minimo",
            "Il motore non riesce a mantenere il minimo corretto: troppo basso (P0506), troppo alto (P0507) o regolazione difettosa (P0505).",
            VERDE, "Puoi guidare. L'auto può spegnersi al semaforo o avere il minimo alto.",
            listOf("Corpo farfallato sporco (molto frequente)", "Valvola del minimo sporca o guasta", "Presa d'aria (con minimo alto)"),
            listOf("Niente di pratico"),
            "Sì, quando ti è comodo.",
            "Pulizia corpo farfallato: 40–80 €. Valvola minimo: 60–150 € + 30 €.",
            "Spegnimenti al minimo (fastidiosi e a volte pericolosi in manovra).")

        // ───────────── ELETTRICO / CENTRALINA ─────────────
        s("P0560,P0562,P0563", "Tensione dell'impianto elettrico anomala",
            "La tensione della batteria/impianto è troppo bassa (P0562) o troppo alta (P0563).",
            ARANCIONE, "Puoi guidare, ma rischi di restare a piedi. Vai a far controllare batteria e alternatore a breve.",
            listOf("Batteria vecchia (dopo 4–6 anni è normale)", "Alternatore che non carica bene", "Morsetti della batteria ossidati o allentati", "Regolatore di tensione guasto (con tensione alta)"),
            listOf("Controlla che i morsetti della batteria siano stretti e puliti (niente polvere bianca/verde)",
                "Guarda la sezione \"Dati motore\" di questa app: la tensione a motore acceso deve essere tra 13,5 e 14,7 V"),
            "Sì, ma prima falla controllare gratis: molti ricambisti testano la batteria senza costi.",
            "Batteria: 70–140 € (montaggio spesso incluso). Alternatore revisionato: 150–300 € + 50–100 €. Pulizia morsetti: 0–10 €.",
            "Auto che non parte, centraline che si comportano in modo strano; con tensione troppo alta si rovinano la batteria e l'elettronica.")

        s("P0600,P0601,P0602,P0603,P0604,P0605,P0606", "Errore interno della centralina motore",
            "La centralina motore segnala un problema interno o di memoria. Può essere un difetto della centralina o, più spesso, un'alimentazione instabile.",
            ARANCIONE, "Guida con prudenza: l'auto può comportarsi in modo strano o entrare in emergenza.",
            listOf("Batteria debole o morsetti ossidati (controllare prima!)", "Massa del motore ossidata", "Centralina motore difettosa (più raro)"),
            listOf("Controlla i morsetti della batteria"),
            "Sì. Diffida di chi propone subito di cambiare la centralina: prima vanno controllate batteria e masse.",
            "Controllo masse/batteria: 30–60 €. Riparazione centralina: 200–500 €. Centralina nuova/ricondizionata: 400–1.000 € + programmazione.",
            "Spegnimenti o mancati avviamenti imprevedibili.")

        s("P0700", "Cambio automatico: errore",
            "La centralina del cambio automatico ha registrato un guasto (il dettaglio è nella centralina del cambio).",
            ARANCIONE, "Guida con prudenza: il cambio può bloccarsi in una marcia (modalità emergenza).",
            listOf("Olio del cambio basso o vecchio", "Sensore del cambio", "Elettrovalvole del cambio"),
            listOf("Controlla se le cambiate sono brusche o se il cambio resta in una sola marcia"),
            "Sì, da un'officina che legga anche la centralina del cambio.",
            "Cambio olio cambio automatico: 100–250 €. Elettrovalvola: 150–400 €.",
            "Usura del cambio: una revisione costa 1.000–2.500 €.")

        // ───────────── DIESEL ─────────────
        s("P0299", "Turbo: pressione troppo bassa",
            "Il turbo non soffia abbastanza: il motore ha poca potenza.",
            ARANCIONE, "Puoi guidare con prudenza, senza tirare il motore. L'auto può entrare in modalità di emergenza.",
            listOf("Tubo del turbo staccato o forato (frequente)", "Valvola EGR bloccata aperta", "Geometria variabile del turbo incrostata", "Turbo usurato"),
            listOf("Guarda se ci sono tubi grandi in gomma sfilati o unti d'olio attorno al motore"),
            "Sì.",
            "Manicotto turbo: 30–100 € + 30–60 €. Pulizia geometria: 150–300 €. Turbo revisionato: 400–900 € + 150–300 €.",
            "Se il turbo si rompe del tutto può mandare olio nel motore: danni gravi.")

        s("P0234", "Turbo: pressione troppo alta",
            "Il turbo soffia più del previsto.",
            ARANCIONE, "Guida con prudenza, senza accelerare a fondo.",
            listOf("Valvola di regolazione turbo bloccata", "Geometria variabile incrostata", "Sensore pressione difettoso"),
            listOf("Niente di pratico"),
            "Sì.",
            "Sensore: 40–100 € + 30 €. Pulizia/regolazione turbo: 150–300 €.",
            "Stress eccessivo su turbo e motore.")

        r("P0380", "P0384") { c -> Scheda(c, "Candelette di preriscaldamento: problema (diesel)",
            "Il circuito delle candelette, che scaldano il motore diesel per farlo partire a freddo, ha un problema.",
            VERDE, "Puoi guidare. D'inverno l'auto può faticare a partire e fumare bianco all'avvio.",
            listOf("Una o più candelette bruciate", "Centralina/relè candelette"),
            listOf("Niente di pratico"),
            "Sì, prima dell'inverno.",
            "Candelette: 15–30 € l'una + 50–150 € (se non sono grippate). Relè: 50–120 €.",
            "Avviamenti difficili a freddo e usura del motorino di avviamento.") }

        r("P0670", "P0684") { c -> Scheda(c, "Candelette di preriscaldamento: problema (diesel)",
            "Una candeletta o il suo circuito ha un problema.",
            VERDE, "Puoi guidare. D'inverno l'auto può faticare a partire.",
            listOf("Candeletta bruciata", "Relè/centralina candelette", "Cavo"),
            listOf("Niente di pratico"),
            "Sì, prima dell'inverno.",
            "Candeletta: 15–30 € + 50–150 €. Relè: 50–120 €.",
            "Avviamenti difficili a freddo.") }

        s("P2002,P2003", "Filtro antiparticolato (DPF) poco efficiente",
            "Il filtro antiparticolato del diesel non trattiene più bene la fuliggine.",
            ARANCIONE, "Puoi guidare, ma fai controllare a breve.",
            listOf("DPF danneggiato o crepato", "Sensore pressione differenziale difettoso"),
            listOf("Niente di pratico"),
            "Sì.",
            "Sensore: 40–100 € + 30 €. DPF non originale: 400–900 € + 100–200 €.",
            "Bocciatura alla revisione e possibili danni al turbo.")

        s("P242F,P2463,P2452,P2453", "Filtro antiparticolato (DPF) intasato",
            "Il filtro antiparticolato del diesel è pieno di fuliggine e non riesce a pulirsi da solo.",
            ARANCIONE, "Puoi guidare, ma fai subito un tratto di 20–30 minuti in autostrada a 2.500–3.000 giri per farlo rigenerare. Se non basta, officina.",
            listOf("Tanti tragitti brevi in città (il filtro non ha tempo di pulirsi)", "Sensore pressione differenziale difettoso", "Iniettori o EGR che producono troppa fuliggine"),
            listOf("Fai un tragitto di 20–30 minuti in autostrada a velocità costante, senza spegnere il motore"),
            "Sì, se la spia non si spegne dopo il tragitto in autostrada.",
            "Rigenerazione forzata in officina: 80–150 €. Pulizia DPF: 150–350 €. DPF nuovo: 400–900 € + 100–200 €.",
            "Poca potenza, consumi alti e, nei casi peggiori, olio diluito con gasolio che rovina il motore.")

        // ───────────── COMUNICAZIONE / ALTRI ─────────────
        s("U0100,U0101", "Perdita di comunicazione con la centralina motore/cambio",
            "Le centraline dell'auto non riescono a parlare con la centralina motore (o del cambio).",
            ROSSO, "Evita di guidare finché non è controllata: l'auto può spegnersi o non partire.",
            listOf("Batteria debole (causa frequente)", "Connettore della centralina ossidato", "Fusibile", "Cablaggio della rete CAN danneggiato"),
            listOf("Controlla i morsetti della batteria e lo stato di carica (vedi \"Dati motore\")"),
            "Sì.",
            "Batteria: 70–140 €. Ricerca guasto elettrico: 50–150 €.",
            "Auto ferma all'improvviso.")

        s("U0001,U0073", "Problema sulla rete di comunicazione dell'auto (CAN)",
            "La rete che collega le centraline dell'auto ha un problema.",
            ARANCIONE, "Guida con prudenza: possono comparire spie e malfunzionamenti strani.",
            listOf("Batteria debole", "Connettore ossidato", "Cavo danneggiato", "Una centralina guasta che disturba la rete"),
            listOf("Controlla i morsetti della batteria"),
            "Sì.",
            "Ricerca guasto: 50–150 €, poi dipende dal pezzo.",
            "Malfunzionamenti di più sistemi contemporaneamente.")

        s("U0121", "Perdita di comunicazione con l'ABS",
            "La centralina motore non riceve dati dalla centralina dell'ABS.",
            ARANCIONE, "Puoi guidare, ma l'ABS potrebbe non funzionare: in frenata forte le ruote possono bloccarsi. Mantieni più distanza.",
            listOf("Connettore della centralina ABS ossidato", "Fusibile ABS", "Centralina ABS guasta"),
            listOf("Controlla se è accesa la spia ABS"),
            "Sì, entro pochi giorni.",
            "Ricerca guasto: 50–150 €. Riparazione centralina ABS: 200–450 €.",
            "Frenate di emergenza meno sicure.")
    }

    /** Spiegazione per codici non presenti nel database, in base alla famiglia. */
    private fun generica(c: String): Scheda {
        val famiglia = when {
            c.startsWith("P0") || c.startsWith("P2") -> when (c.getOrNull(2)) {
                '0', '1' -> "dosaggio di aria e carburante" to ARANCIONE
                '2' -> "iniezione del carburante" to ARANCIONE
                '3' -> "accensione / mancate accensioni" to ARANCIONE
                '4' -> "controllo delle emissioni" to VERDE
                '5' -> "velocità, minimo o segnali dei sensori" to ARANCIONE
                '6' -> "centralina motore o suoi circuiti" to ARANCIONE
                '7', '8', '9' -> "cambio" to ARANCIONE
                'A', 'B', 'C', 'D', 'E', 'F' -> "componenti del motore e delle emissioni" to ARANCIONE
                else -> "motore" to ARANCIONE
            }
            c.startsWith("P1") || c.startsWith("P3") -> "specifico del costruttore (Opel/Suzuki), relativo al motore" to ARANCIONE
            c.startsWith("C") -> "telaio: freni/ABS, sterzo o sospensioni" to ARANCIONE
            c.startsWith("B") -> "carrozzeria: airbag, luci, climatizzatore o comfort" to VERDE
            c.startsWith("U") -> "comunicazione tra le centraline" to ARANCIONE
            else -> "sistema non identificato" to ARANCIONE
        }
        val sicurezzaFreni = c.startsWith("C")
        return Scheda(
            c, "Codice $c: problema di ${famiglia.first}",
            "Questo codice non è nel database dell'app. Appartiene alla famiglia \"${famiglia.first}\". La centralina ha rilevato un'anomalia in quel sistema.",
            famiglia.second,
            if (sicurezzaFreni) "Guida con prudenza: se è accesa la spia ABS o dei freni, mantieni più distanza e fai controllare a breve."
            else "Se l'auto si comporta normalmente puoi guidare con prudenza. Se noti strappi, rumori, fumo o spie rosse, fermati.",
            listOf("Cerca online \"$c Opel Agila\" per la descrizione esatta", "Sensore o connettore del sistema indicato"),
            listOf("Annota quando compare il problema (a freddo, in accelerazione, sempre...): aiuta il meccanico a trovarlo prima"),
            "Sì, per una diagnosi precisa. $DIAGNOSI",
            "Dipende dal guasto. $DIAGNOSI",
            "Se ignorato, un guasto minore può peggiorare e la spia accesa nasconde eventuali guasti nuovi."
        )
    }
}
