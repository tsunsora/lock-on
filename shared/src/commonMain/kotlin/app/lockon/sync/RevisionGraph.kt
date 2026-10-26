package app.lockon.sync

/**
 * Decides a deck's current state from its revision history. Ordering never uses clocks: a
 * revision supersedes another only by naming it as a parent, so devices with wrong clocks still
 * agree on the result.
 */
object RevisionGraph {
    data class Node(val revisionId: String, val parents: List<String>, val createdAt: Long, val deleted: Boolean)

    sealed interface DeckState {
        /** Exactly one head: no conflict. */
        data class Resolved(val head: Node) : DeckState
        /** Two or more devices edited independently. Every head is kept until someone chooses. */
        data class Conflicted(val heads: List<Node>) : DeckState
    }

    fun heads(nodes: Collection<Node>): List<Node> {
        val superseded = nodes.flatMap { it.parents }.toSet()
        // Newest first for display only; which head "wins" is left to the person resolving it.
        return nodes.filter { it.revisionId !in superseded }
            .sortedWith(compareByDescending<Node> { it.createdAt }.thenBy { it.revisionId })
    }

    fun state(nodes: Collection<Node>): DeckState? {
        val heads = heads(nodes)
        return when {
            heads.isEmpty() -> null
            heads.size == 1 -> DeckState.Resolved(heads.single())
            // Several devices deleting the same deck is agreement, not a conflict.
            heads.all { it.deleted } -> DeckState.Resolved(heads.first())
            else -> DeckState.Conflicted(heads)
        }
    }
}
