# AutoCheck

App Android che legge la centralina dell'auto tramite un adattatore **ELM327 Bluetooth** e spiega i problemi in italiano semplice, pensata per chi non è meccanico.

## Cosa fa
- Legge i codici errore (definitivi, in osservazione e permanenti) e li spiega con una scheda chiara:
  cosa significa, se puoi guidare (semaforo 🟢🟠🔴), cause probabili, cosa puoi fare da solo,
  se serve il meccanico, costo indicativo di mercato e cosa succede se lo ignori.
- Controlla i dati del motore (temperatura, batteria/alternatore, miscela aria-benzina) e segnala anomalie anche senza codici.
- **Anti-fregatura**: avvisa se gli errori sono stati cancellati da poco, se un errore "riparato" è tornato, se i test di autodiagnosi non sono completati.
- **Scansione estesa (sperimentale)**: prova a leggere anche le centraline ABS/ESP, airbag, cambio e quadro (UDS e KWP2000, su CAN e linea K), in sola lettura.
- **Guida alle spie del cruscotto**: per ogni spia gravità, cause, controlli e costi; le spie indicate entrano nella diagnosi (es. ESP acceso insieme a un errore motore).
- **Riconoscimento dell'auto** dal numero di telaio (VIN), oppure scelta manuale (c'è un profilo "Opel Agila B").
- **Indirizzi delle centraline per marca** (gruppo VW, PSA, Renault, Ford, Toyota, Hyundai/Kia, BMW…), con le coppie richiesta/risposta corrette.
- **Scansione profonda** (solo a motore spento): prova tutti gli indirizzi diagnostici CAN per trovare centraline non documentate. Prima ascolta il traffico dell'auto e non trasmette mai su ID usati dall'auto per funzionare.
- **Codici ABS/ESP Suzuki e Opel Agila B** spiegati in italiano (unità ATE MK60, compreso il difetto noto C1028).
- **Rapporto tecnico** condivisibile con le risposte grezze dell'adattatore, per adattare l'app a un'auto specifica.
- Dati del motore in tempo reale.
- Storico dei controlli salvato **solo nel telefono** (nessun server, nessun account).
- Rapporto condivisibile via WhatsApp/email, da mostrare al meccanico.
- Cancellazione errori (con avvertenze).

## Installazione
1. Dal telefono Android apri la pagina **Releases** di questo repository e scarica `AutoCheck.apk`.
2. Aprilo e consenti "Installa app sconosciute" quando Android lo chiede.
3. Abbina l'adattatore nelle impostazioni Bluetooth (PIN 1234 o 0000), poi aprilo dall'app.

## Come viene creato l'APK
A ogni modifica sul ramo `main`, GitHub Actions compila l'app (`.github/workflows/build-apk.yml`)
e pubblica `AutoCheck.apk` nella release **latest**. Il link resta sempre lo stesso.

## Note
- I costi sono stime indicative per un'officina indipendente in Italia (manodopera 40–60 €/h).
- La diagnosi è probabile, non certa.
- I codici OBD standard riguardano motore ed emissioni; ABS, airbag e carrozzeria usano protocolli del costruttore: la scansione estesa li prova, ma non è garantito che rispondano a un ELM327 generico.

## Fonti dei dati e licenze
- Indirizzi delle centraline per marca: [OBDb](https://github.com/OBDb), licenza **CC BY-SA 4.0**. Il file `TabellaIndirizzi.kt` è derivato da quei dati ed è distribuito con la stessa licenza.
- Descrizioni originali dei codici dei costruttori (in inglese, usate solo come riserva): [Wal33D/dtc-database](https://github.com/Wal33D/dtc-database), licenza **MIT** (vedi `app/src/main/assets/LICENZA_dtc_costruttori.txt`).
- Significato dei codici ABS/ESP Suzuki: manuali di servizio Suzuki (Swift 2004–2010, SX4); spiegazioni, cause e costi scritti per questa app.
- Unità ABS/ESP dell'Opel Agila B (ATE MK60) e difetto del sensore di pressione (C1028): documentazione di ditte di revisione (controlunits.com, sinspeed.co.uk).
