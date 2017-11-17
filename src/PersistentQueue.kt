/**
 * Created by a.qurbonzoda on 14.11.17.
 */

class PersistentQueue<T> private constructor(
        private val l: PersistentStack<T>,
        private val _l: PersistentStack<T>,
        private val r: PersistentStack<T>,
        private val _r: PersistentStack<T>,
        private val s: PersistentStack<T>,
        private val toCopyFromSToR: Int,
        private val isLCopiedToR: Boolean,
        private val isRecopyState: Boolean
) {
    private constructor(emptyStack: PersistentStack<T>) : this(
            l = emptyStack,
            _l = emptyStack,
            r = emptyStack,
            _r = emptyStack,
            s = emptyStack,
            toCopyFromSToR = 0,
            isLCopiedToR = false,
            isRecopyState = false
    )

    constructor() : this(emptyStack = PersistentStack<T>())

    val size: Int
        get() {
            if (!isRecopyState) return l.size + r.size
            return _l.size + l.size + toCopyFromSToR + if (isLCopiedToR) r.size else 0
        }

    fun isEmpty(): Boolean {
        if (isRecopyState) return false
        return r.isEmpty()
    }

    fun front(): T? {
        if (isRecopyState) return _r.peek()
        return r.peek()
    }

    fun enqueue(value: T): PersistentQueue<T> {
        if (isRecopyState) return PersistentQueue(
                l = l,
                _l = _l.push(value),
                r = r,
                _r = _r,
                s = s,
                toCopyFromSToR = toCopyFromSToR,
                isLCopiedToR = isLCopiedToR,
                isRecopyState = isRecopyState
        ).checkNormal()

        return PersistentQueue(
                l = l.push(value),
                _l = _l,
                r = r,
                _r = _r,
                s = s,
                toCopyFromSToR = toCopyFromSToR,
                isLCopiedToR = isLCopiedToR,
                isRecopyState = isRecopyState
        ).checkRecopy()
    }

    fun dequeue(): DequeueResult<T> {
        if (isRecopyState) {
            assert(toCopyFromSToR > 0)

            val _rn = _r.pop()
            val newQueue = PersistentQueue(
                    l = l,
                    _l = _l,
                    r = r,
                    _r = _rn.resultStack,
                    s = s,
                    toCopyFromSToR = toCopyFromSToR - 1,
                    isLCopiedToR = isLCopiedToR,
                    isRecopyState = isRecopyState
            ).checkNormal()
            return DequeueResult(newQueue, _rn.value)
        }
        val rn = r.pop()
        val newQueue = PersistentQueue(
                l = l,
                _l = _l,
                r = rn.resultStack,
                _r = _r,
                s = s,
                toCopyFromSToR = toCopyFromSToR,
                isLCopiedToR = isLCopiedToR,
                isRecopyState = isRecopyState
        ).checkRecopy()
        return DequeueResult(newQueue, rn.value)
    }

    private fun checkRecopy(): PersistentQueue<T> {
        if (l.size > r.size) {
            assert(_l.isEmpty())
            return PersistentQueue(
                    l = l,
                    _l = _l,
                    r = r,
                    _r = r,
                    s = _l,
                    toCopyFromSToR = r.size,
                    isLCopiedToR = false,
                    isRecopyState = true
            ).checkNormal()
        }
        return this
    }

    private fun checkNormal(): PersistentQueue<T> {
        val q = doRecopy()

        if (q.toCopyFromSToR == 0) {
            assert(q.l.isEmpty())
            return PersistentQueue(
                    l = q._l,
                    _l = q.l,
                    r = q.r,
                    _r = q.l,
                    s = q.l,
                    toCopyFromSToR = 0,
                    isLCopiedToR = false,
                    isRecopyState = false
            )
        }
        return q
    }

    private fun doRecopy(): PersistentQueue<T> {
        var operationsToDo = 3

        var rn = r
        var sn = s
        while (operationsToDo > 0 && !isLCopiedToR && !rn.isEmpty()) {
            operationsToDo -= 1

            val (newRn, value) = rn.pop()
            rn = newRn
            sn = sn.push(value)
        }

        var ln = l
        var isCopied = isLCopiedToR
        while (operationsToDo > 0 && !ln.isEmpty()) {
            operationsToDo -= 1
            isCopied = true

            val (newLn, value) = ln.pop()
            ln = newLn
            rn = rn.push(value)
        }

        var toCopy = toCopyFromSToR
        while (operationsToDo > 0 && toCopy > 0) {
            operationsToDo -= 1
            toCopy -= 1

            val (newSn, value) = sn.pop()
            sn = newSn
            rn = rn.push(value)
        }

        return PersistentQueue(
                l = ln,
                _l = _l,
                r = rn,
                _r = _r,
                s = sn,
                toCopyFromSToR = toCopy,
                isLCopiedToR = isCopied,
                isRecopyState = isRecopyState
        )
    }

    data class DequeueResult<T>(val resultQueue: PersistentQueue<T>, val value: T)
}