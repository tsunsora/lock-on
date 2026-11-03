package app.lockon

import app.lockon.model.Card
import app.lockon.model.CardQuery
import app.lockon.model.CardSort
import app.lockon.model.CardType
import kotlin.test.Test
import kotlin.test.assertEquals

class CardQueryStatsTest {
    private val wall = Card("GD01-001", "Wall", CardType.UNIT, ap = 2, hp = 5, cost = 3, setCode = "GD01")
    private val striker = Card("GD01-002", "Striker", CardType.UNIT, ap = 5, hp = 2, cost = 3, setCode = "GD01")
    private val command = Card("GD01-003", "Order", CardType.COMMAND, cost = 1, setCode = "GD01")
    private val cards = listOf(wall, striker, command)

    @Test
    fun apAndHpFilterOnTheirOwnStatAndSkipCardsWithoutOne() {
        assertEquals(listOf(striker), cards.filter(CardQuery(aps = setOf(4, 5, 6))::matches))
        assertEquals(listOf(wall), cards.filter(CardQuery(hps = setOf(5))::matches))
        assertEquals(emptyList(), cards.filter(CardQuery(aps = setOf(5), hps = setOf(5))::matches))
    }

    @Test
    fun statSortsPutTheStrongestFirstAndCardsWithoutTheStatLast() {
        assertEquals(listOf(striker, wall, command), cards.sortedWith(CardSort.AP.comparator))
        assertEquals(listOf(wall, striker, command), cards.sortedWith(CardSort.HP.comparator))
    }
}
