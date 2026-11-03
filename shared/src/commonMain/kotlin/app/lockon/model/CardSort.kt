package app.lockon.model

/** Orders for a card list. Settings picks the one the Cards screen opens with. */
enum class CardSort(val label: String, val comparator: Comparator<Card>) {
    NUMBER("Number", compareBy(ReleaseOrder.comparator) { it.cardNumber }),
    NEWEST("Newest", compareBy(ReleaseOrder.newestFirst) { it.cardNumber }),
    LEVEL("Level", compareBy<Card>({ it.level ?: 99 }, { it.cost ?: 99 }, { it.cardNumber })),
    COST("Cost", compareBy<Card>({ it.cost ?: 99 }, { it.level ?: 99 }, { it.cardNumber })),
    // Strongest first, the way people look for a finisher or a wall; cards without the stat go last.
    AP("AP", compareBy<Card>({ -(it.ap ?: -1) }, { it.cost ?: 99 }, { it.cardNumber })),
    HP("HP", compareBy<Card>({ -(it.hp ?: -1) }, { it.cost ?: 99 }, { it.cardNumber })),
    NAME("Name", compareBy { it.name.lowercase() }),
}
