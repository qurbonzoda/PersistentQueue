/**
 * Created by a.qurbonzoda on 14.11.17.
 */

class PersistentStack<T> private constructor(private val top: Node<T>?, val size: Int) {
    constructor() : this(null, 0)

    fun isEmpty() = top == null

    fun peek() = top?.value

    fun pop(): PopResult<T> {
        if (top == null) {
            throw NoSuchElementException()
        }
        return PopResult(PersistentStack(top.previous, size = size - 1), top.value)
    }

    fun push(value: T) = PersistentStack(Node(value, top), size = size + 1)

    private data class Node<T>(val value: T, val previous: Node<T>?)

    data class PopResult<T>(val resultStack: PersistentStack<T>, val value: T)
}