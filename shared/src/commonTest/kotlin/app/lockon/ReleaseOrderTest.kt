package app.lockon

import app.lockon.model.ReleaseOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseOrderTest {
    @Test
    fun setsRunInReleaseOrderThenByNumber() {
        val numbers = listOf("T-014", "EB01-003", "GD02-001", "ST06-001", "GD09-001", "RP-002", "ST01-010", "GD01-002", "ST01-002", "SC01-001")
        assertEquals(
            listOf("ST01-002", "ST01-010", "GD01-002", "GD02-001", "ST06-001", "EB01-003", "SC01-001", "GD09-001", "RP-002", "T-014"),
            numbers.sortedWith(ReleaseOrder.comparator),
        )
    }
}
