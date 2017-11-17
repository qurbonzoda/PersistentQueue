/**
 * Created by a.qurbonzoda on 14.11.17.
 */

private sealed class State<T>

private class Normal<T> : State<T>()
private data class Recopy<T>(val _r: PersistentStack<T>,
                             val s: PersistentStack<T>,
                             val toCopy: Int,
                             val isCopied: Boolean) : State<T>()

class PersistentQueue<T> private constructor(private val l: PersistentStack<T>,
                                             private val _l: PersistentStack<T>,
                                             private val r: PersistentStack<T>,
                                             private val state: State<T>) {
    private constructor(emptyStack: PersistentStack<T>) : this(emptyStack, emptyStack, emptyStack, Normal<T>())

    constructor() : this(PersistentStack<T>())

    val size: Int
        get() = when (state) {
            is Normal -> l.size + r.size
            is Recopy -> _l.size + l.size + state.toCopy + if (state.isCopied) r.size else 0
        }

    fun isEmpty(): Boolean = when (state) {
        is Normal -> r.isEmpty()
        is Recopy -> false
    }

    fun front(): T? = when (state) {
        is Normal -> r.peek()
        is Recopy -> state._r.peek()
    }

    fun enqueue(value: T): PersistentQueue<T> = when (state) {
        is Normal -> PersistentQueue(l.push(value), _l, r, state).checkRecopy()
        is Recopy -> PersistentQueue(l, _l.push(value), r, state).checkNormal(state)
    }

    fun dequeue(): DequeueResult<T> = when (state) {
        is Normal -> {
            val rn = r.pop()
            val newQueue = PersistentQueue(l, _l, rn.resultStack, state).checkRecopy()
            DequeueResult(newQueue, rn.value)
        }
        is Recopy -> {
            val _rn = state._r.pop()

            assert(state.toCopy > 0)

            val recopy = Recopy(_rn.resultStack, state.s, state.toCopy - 1, state.isCopied)
            val newQueue = PersistentQueue(l, _l, r, recopy).checkNormal(recopy)
            DequeueResult(newQueue, _rn.value)
        }
    }

    private fun checkRecopy(): PersistentQueue<T> = when {
        l.size <= r.size -> this
        else -> {
            assert(_l.isEmpty())

            val recopy = Recopy(r, _l, r.size, false)
            PersistentQueue(l, _l, r, recopy).checkNormal(recopy)
        }
    }

    private fun checkNormal(recopy: Recopy<T>): PersistentQueue<T> {
        val (q, didFinishCopying) = doRecopy(recopy)

        return if (didFinishCopying) {
            assert(q.l.isEmpty())

            PersistentQueue(q._l, q.l, q.r, Normal())
        } else {
            q
        }
    }

    private fun doRecopy(recopy: Recopy<T>): Pair<PersistentQueue<T>, Boolean> {
        var operationsToDo = 3
        var rn = r

        var (_rn, sn, toCopy, isCopied) = recopy

        while (operationsToDo > 0 && !isCopied && !rn.isEmpty()) {
            operationsToDo -= 1

            val (newRn, value) = rn.pop()
            rn = newRn
            sn = sn.push(value)
        }

        var ln = l
        while (operationsToDo > 0 && !ln.isEmpty()) {
            operationsToDo -= 1
            isCopied = true

            val (newLn, value) = ln.pop()
            ln = newLn
            rn = rn.push(value)
        }

        while (operationsToDo > 0 && toCopy > 0) {
            operationsToDo -= 1
            toCopy -= 1

            val (newSn, value) = sn.pop()
            sn = newSn
            rn = rn.push(value)
        }

        val newRecopy = Recopy(_rn, sn, toCopy, isCopied)
        return Pair(PersistentQueue(ln, _l, rn, newRecopy), toCopy == 0)
    }

    data class DequeueResult<T>(val resultQueue: PersistentQueue<T>, val value: T)
}