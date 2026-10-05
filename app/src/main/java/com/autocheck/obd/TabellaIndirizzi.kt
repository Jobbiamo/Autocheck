package com.autocheck.obd

/**
 * Indirizzi diagnostici (richiesta -> risposta) osservati per marca.
 * Dati ricavati da OBDb (https://github.com/OBDb), licenza CC BY-SA 4.0.
 * Questo file è distribuito con la stessa licenza (CC BY-SA 4.0).
 */
object TabellaIndirizzi {
    class C(val richiesta: String, val risposta: String, val tipo: TipoCentralina?)

    val PER_MARCA: Map<String, List<C>> = mapOf(
        "Audi" to listOf(C("70E", "778", null), C("70F", "779", null), C("710", "77A", null), C("712", "77C", null), C("713", "77D", TipoCentralina.ABS_ESP), C("714", "77E", null), C("71E", "788", null), C("742", "7AC", null), C("744", "7AE", null), C("746", "7B0", null), C("752", "7BC", null), C("767", "7D1", null), C("7E0", "7E8", null), C("7E1", "7E9", null), C("7E5", "7ED", null)),
        "Cupra" to listOf(C("70F", "779", null), C("710", "77A", null), C("712", "77C", null), C("713", "77D", null), C("714", "77E", null), C("715", "77F", null), C("746", "7B0", null), C("767", "7D1", null), C("7E1", "7E9", null), C("7E5", "7ED", null)),
        "Fiat" to listOf(C("18DA40F1", "18DAF140", null), C("18DA42F1", "18DAF142", null), C("18DA44F1", "18DAF144", null)),
        "Ford" to listOf(C("6F5", "6FD", null), C("720", "728", null), C("726", "72E", null), C("746", "74E", null), C("760", "768", null), C("764", "76C", null), C("7D0", "7D8", null), C("7E0", "7E8", null), C("7E1", "7E9", TipoCentralina.CAMBIO), C("7E2", "7EA", null), C("7E4", "7EC", null), C("7E6", "7EE", null), C("7E7", "7EF", null)),
        "Honda" to listOf(C("18DA01F1", "18DAF101", null), C("18DA0EF1", "18DAF10E", null), C("18DA10F1", "18DAF110", null), C("18DA11F1", "18DAF111", null), C("18DA1DF1", "18DAF11D", null), C("18DA1EF1", "18DAF11E", TipoCentralina.CAMBIO), C("18DA26F1", "18DAF126", null), C("18DA28F1", "18DAF128", null), C("18DA2AF1", "18DAF12A", null), C("18DA60F1", "18DAF160", null)),
        "Hyundai" to listOf(C("730", "738", null), C("740", "748", null), C("744", "74C", null), C("770", "778", TipoCentralina.AIRBAG), C("7A0", "7A8", null), C("7B3", "7BB", null), C("7C6", "7CE", null), C("7D1", "7D9", null), C("7D4", "7DC", null), C("7D6", "7DE", null), C("7E0", "7D8", null), C("7E0", "7E8", null), C("7E1", "7E9", null), C("7E2", "7EA", null), C("7E3", "7EB", null), C("7E4", "7EC", null), C("7E5", "7ED", null), C("7E6", "7EE", null), C("7E7", "7EF", null)),
        "Jeep" to listOf(C("18DA10F1", "18DAF110", null), C("18DA18F1", "18DAF118", null), C("18DA40F1", "18DAF140", null), C("18DAC7F1", "18DAF1C7", null), C("7E0", "7E8", null), C("7E1", "7E9", TipoCentralina.CAMBIO)),
        "Kia" to listOf(C("730", "738", null), C("744", "74C", null), C("770", "778", TipoCentralina.AIRBAG), C("7A0", "7A8", null), C("7B3", "7BB", null), C("7C6", "7CE", null), C("7D1", "7D9", null), C("7D4", "7DC", null), C("7E0", "7E8", null), C("7E1", "7E9", null), C("7E2", "7EA", null), C("7E3", "7EB", null), C("7E4", "7EC", null), C("7E5", "7ED", null), C("7E6", "7EE", null), C("7E7", "7EF", null)),
        "Land-Rover" to listOf(C("726", "72E", null), C("751", "759", null), C("7E0", "7E8", null), C("7E4", "7EC", null)),
        "Mazda" to listOf(C("700", "708", null), C("720", "728", null), C("726", "72E", null), C("761", "769", null), C("793", "79B", null), C("7B0", "7B8", TipoCentralina.ABS_ESP), C("7B3", "7BB", null), C("7C0", "7C8", null), C("7D0", "7DA", null), C("7E0", "7E8", null), C("7E1", "7E9", TipoCentralina.CAMBIO)),
        "Mitsubishi" to listOf(C("18DA15F1", "18DAF115", null), C("18DA18F1", "18DAF118", null), C("18DA2DF1", "18DAF12D", null), C("600", "500", null), C("688", "511", null), C("6A0", "514", null), C("743", "763", null), C("773", "774", null), C("784", "785", null), C("7B6", "7B7", null)),
        "Nissan" to listOf(C("740", "760", null), C("743", "763", null), C("744", "764", null), C("797", "79A", null), C("79B", "7BB", null)),
        "Opel" to listOf(C("6A2", "682", null), C("6B4", "694", null)),
        "Peugeot" to listOf(C("6A2", "682", null), C("6A6", "686", null), C("6A8", "688", null), C("6A9", "689", null), C("6B4", "694", null)),
        "Renault" to listOf(C("18DADAF1", "18DAF1DA", null), C("18DADBF1", "18DAF1DB", null), C("18DADFF1", "18DAF1DF", null), C("740", "760", null), C("744", "764", null), C("745", "765", null), C("74D", "76D", null), C("79B", "7BB", null), C("7E0", "7E8", null), C("7E4", "7EC", null)),
        "Seat" to listOf(C("70E", "778", null), C("710", "77A", null), C("712", "77C", null), C("713", "77D", null), C("714", "77E", null), C("715", "77F", null), C("746", "7B0", null), C("74F", "7B9", null), C("7E0", "7E8", null), C("7E1", "7E9", null)),
        "Smart" to listOf(C("61A", "483", null), C("624", "5A4", null), C("743", "763", null), C("745", "765", null), C("792", "793", null), C("79B", "7BB", null), C("7E4", "7EC", null), C("7E5", "7ED", TipoCentralina.ABS_ESP), C("7E7", "7EF", null)),
        "Subaru" to listOf(C("753", "75B", null), C("7A2", "7AA", null), C("7A3", "7AB", null), C("7B0", "7B8", null), C("7E0", "7E8", null), C("7E6", "7EE", null), C("7E7", "7EF", null)),
        "Toyota" to listOf(C("700", "708", null), C("701", "709", null), C("745", "74D", null), C("747", "74F", null), C("780", "788", TipoCentralina.AIRBAG), C("7B0", "7B8", TipoCentralina.ABS_ESP), C("7B1", "7B9", null), C("7B3", "7BB", null), C("7C0", "7C8", TipoCentralina.AIRBAG), C("7C4", "7CC", null), C("7D0", "7D8", null), C("7D2", "7DA", null), C("7E0", "7E8", null), C("7E1", "7E9", null), C("7E2", "7EA", null), C("7E3", "7EB", null)),
        "Volkswagen" to listOf(C("70E", "778", null), C("70F", "779", null), C("710", "77A", null), C("712", "77C", null), C("713", "77D", null), C("714", "77E", null), C("715", "77F", null), C("744", "7AE", null), C("746", "7B0", null), C("74F", "7B9", null), C("767", "7D1", null), C("7E0", "7E8", null), C("7E1", "7E9", null), C("7E5", "7ED", null)),
        "Volvo" to listOf(C("7E0", "7E8", null)),
    )
}
