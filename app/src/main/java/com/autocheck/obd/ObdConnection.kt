package com.autocheck.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Collegamento Bluetooth classico (profilo seriale SPP) con un adattatore ELM327.
 * Tutti i metodi sono bloccanti: vanno chiamati da un thread in background.
 */
@SuppressLint("MissingPermission")
class ObdConnection {

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    var protocollo: String = ""
        private set
    var nomeDispositivo: String = ""
        private set
    /** Numero di protocollo ELM327 (3 = ISO 9141, 4/5 = KWP, 6–9 = CAN). */
    var numeroProtocollo: Int = 0
        private set

    val connesso: Boolean
        get() = socket?.isConnected == true

    @Throws(IOException::class)
    fun connetti(device: BluetoothDevice) {
        chiudi()
        val sock = apriSocket(device)
        socket = sock
        input = sock.inputStream
        output = sock.outputStream
        nomeDispositivo = device.name ?: device.address
        try {
            inizializza()
        } catch (e: IOException) {
            chiudi()
            throw e
        }
    }

    private fun apriSocket(device: BluetoothDevice): BluetoothSocket {
        var primo: BluetoothSocket? = null
        try {
            primo = device.createRfcommSocketToServiceRecord(SPP_UUID)
            primo.connect()
            return primo
        } catch (e: IOException) {
            try { primo?.close() } catch (_: IOException) { }
        }
        // Molti cloni ELM327 rispondono solo sul canale RFCOMM 1
        try {
            val metodo = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            val s = metodo.invoke(device, 1) as BluetoothSocket
            s.connect()
            return s
        } catch (e: Exception) {
            throw IOException("Impossibile collegarsi all'adattatore. Controlla che sia inserito e che il quadro sia acceso.")
        }
    }

    private fun inizializza() {
        comando("ATZ", 4000)          // reset
        Thread.sleep(800)
        comando("ATE0")               // niente eco
        comando("ATL0")               // niente a capo extra
        comando("ATS0")               // niente spazi
        comando("ATH0")               // niente intestazioni
        comando("ATAT1")              // tempi adattivi
        comando("ATSP0")              // protocollo automatico
        // La prima richiesta fa partire la ricerca del protocollo: può richiedere vari secondi
        val r = comando("0100", 20000)
        if (ObdParser.estraiBytes(r, "0100") == null) {
            throw IOException(
                "L'adattatore è collegato, ma la centralina dell'auto non risponde.\n" +
                    "Gira la chiave sul secondo scatto (quadro acceso) e riprova."
            )
        }
        val dpn = comando("ATDPN")
        protocollo = ObdParser.nomeProtocollo(dpn)
        numeroProtocollo = dpn.trim().uppercase().removePrefix("A").take(1).toIntOrNull(16) ?: 0
    }

    /** Invia un comando e restituisce la risposta senza il prompt '>'. */
    @Synchronized
    @Throws(IOException::class)
    fun comando(cmd: String, timeoutMs: Long = 4000): String {
        val inp = input ?: throw IOException("Adattatore non collegato")
        val out = output ?: throw IOException("Adattatore non collegato")
        while (inp.available() > 0) inp.read() // svuota eventuali residui
        out.write((cmd + "\r").toByteArray(Charsets.US_ASCII))
        out.flush()

        val sb = StringBuilder()
        val scadenza = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < scadenza) {
            if (inp.available() > 0) {
                val c = inp.read()
                if (c == -1) throw IOException("Connessione con l'adattatore persa")
                if (c.toChar() == '>') return pulisci(sb.toString(), cmd)
                sb.append(c.toChar())
            } else {
                Thread.sleep(10)
            }
        }
        if (!connesso) throw IOException("Connessione con l'adattatore persa")
        return "TIMEOUT"
    }

    private fun pulisci(r: String, cmd: String): String =
        r.split('\r', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals(cmd, ignoreCase = true) }
            .joinToString("\n")

    fun richiediPid(cmd: String, timeoutMs: Long = 4000): IntArray? =
        ObdParser.estraiBytes(comando(cmd, timeoutMs), cmd)

    fun leggiValori(pids: List<Pid>): Map<String, Double> {
        val m = LinkedHashMap<String, Double>()
        for (p in pids) {
            if (p.comando == "ATRV") {
                ObdParser.tensione(comando("ATRV"))?.let { m[p.chiave] = it }
            } else {
                val b = richiediPid(p.comando)
                if (b != null && b.size >= p.byteMinimi) m[p.chiave] = p.formula(b)
            }
        }
        return m
    }

    /** Lettura completa per la diagnosi. */
    fun leggiTutto(): Letture {
        val stato = richiediPid("0101")
        val memorizzati = ObdParser.estraiDtc(comando("03", 8000), "43")
        val pendenti = ObdParser.estraiDtc(comando("07", 8000), "47")
        val permanenti = ObdParser.estraiDtc(comando("0A", 8000), "4A")
        val valori = leggiValori(Pids.CONTROLLO)
        return Letture(memorizzati, pendenti, permanenti, stato, valori, protocollo)
    }

    /** Prova a leggere le altre centraline (ABS/ESP, airbag...). Solo lettura. */
    fun scansioneEstesa(progresso: (String) -> Unit): EsitoScansione =
        ScansioneEstesa({ c, t -> comando(c, t) }, numeroProtocollo, protocollo, progresso).esegui()

    /** Cancella i codici errore e spegne la spia motore. */
    fun cancellaErrori(): Boolean {
        val r = comando("04", 8000)
        return r.uppercase().replace(" ", "").contains("44")
    }

    fun chiudi() {
        try { input?.close() } catch (_: IOException) { }
        try { output?.close() } catch (_: IOException) { }
        try { socket?.close() } catch (_: IOException) { }
        input = null
        output = null
        socket = null
        protocollo = ""
        numeroProtocollo = 0
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
