# AutoCheck

App Android che legge la centralina dell'auto tramite un adattatore **ELM327 Bluetooth** e spiega i problemi in italiano semplice, pensata per chi non è meccanico.

## Cosa fa
- Legge i codici errore (definitivi, in osservazione e permanenti) e li spiega con una scheda chiara:
  cosa significa, se puoi guidare (semaforo 🟢🟠🔴), cause probabili, cosa puoi fare da solo,
  se serve il meccanico, costo indicativo di mercato e cosa succede se lo ignori.
- Controlla i dati del motore (temperatura, batteria/alternatore, miscela aria-benzina) e segnala anomalie anche senza codici.
- **Anti-fregatura**: avvisa se gli errori sono stati cancellati da poco, se un errore "riparato" è tornato, se i test di autodiagnosi non sono completati.
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
- I codici OBD standard riguardano motore ed emissioni; ABS, airbag e carrozzeria usano protocolli del costruttore e di solito non sono leggibili con un ELM327 generico.
