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

    /** [id] and every revision behind it. */
    fun ancestors(nodes: Collection<Node>, id: String): Set<String> {
        val byId = nodes.associateBy { it.revisionId }
        val found = mutableSetOf<String>()
        val pending = ArrayDeque(listOf(id))
        while (pending.isNotEmpty()) {
            val next = pending.removeLast()
            val node = byId[next] ?: continue
            if (found.add(next)) pending.addAll(node.parents)
        }
        return found
    }

    /**
     * The revisions every one of [ids] builds on, minus any that a later shared one supersedes:
     * the point where the branches split. Empty when they share no history.
     */
    fun splitPoints(nodes: Collection<Node>, ids: Collection<String>): Set<String> {
        val shared = ids.map { ancestors(nodes, it) }.reduceOrNull { a, b -> a intersect b }.orEmpty()
        val superseded = nodes.filter { it.revisionId in shared }.flatMap { it.parents }.toSet()
        return shared - superseded
    }

    /**
     * Every revision behind [headId], newest first. A revision always comes before its parents;
     * only revisions on separate branches, which no parent link orders, fall back to their times.
     */
    fun history(nodes: Collection<Node>, headId: String): List<Node> {
        val byId = nodes.associateBy { it.revisionId }
        val ancestors = linkedSetOf<String>()
        val pending = ArrayDeque(listOf(headId))
        while (pending.isNotEmpty()) {
            val id = pending.removeLast()
            val node = byId[id] ?: continue
            if (ancestors.add(id)) pending.addAll(node.parents)
        }
        // A revision is ready once every child that names it has been listed.
        val children = ancestors.associateWith { 0 }.toMutableMap()
        ancestors.forEach { id -> byId.getValue(id).parents.filter { it in children }.forEach { children[it] = children.getValue(it) + 1 } }
        val ordered = mutableListOf<Node>()
        val ready = ancestors.filter { children[it] == 0 }.map { byId.getValue(it) }.toMutableList()
        while (ready.isNotEmpty()) {
            val next = ready.maxWith(compareBy<Node> { it.createdAt }.thenByDescending { it.revisionId })
            ready.remove(next)
            ordered += next
            next.parents.filter { it in children }.forEach { parent ->
                children[parent] = children.getValue(parent) - 1
                if (children[parent] == 0) ready += byId.getValue(parent)
            }
        }
        return ordered
    }
}
