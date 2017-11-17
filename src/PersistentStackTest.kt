import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.NoSuchElementException

class PersistentStackTests {
    @Test
    fun newStackShouldBeEmpty() {
        assertTrue(PersistentStack<Int>().isEmpty())
    }

    @Test
    fun peekForNewStackShouldBeNull() {
        assertNull(PersistentStack<Int>().peek())
    }

    @Test
    fun sizeOfNewStackShouldBeZero() {
        assertEquals(0, PersistentStack<Int>().size)
    }

    @Test
    fun popForNewStackShouldThrowEmptyStackException() {
        assertThrows(NoSuchElementException::class.java) { PersistentStack<Int>().pop() }
    }

    @Test
    fun pushShouldNotMutateStackButShouldReturnNewStack() {
        val stack = PersistentStack<Int>()
        val newStack = stack.push(0)

        assertTrue(stack.isEmpty())

        assertFalse(newStack.isEmpty())
        assertEquals(0, newStack.peek())
    }

    @Test
    fun popShouldNotMutateStackButShouldReturnNewStack() {
        val stack = PersistentStack<Int>().push(0)
        val (newStack, value) = stack.pop()

        assertFalse(stack.isEmpty())
        assertTrue(newStack.isEmpty())

        assertEquals(0, stack.peek())
        assertEquals(0, value)
    }

    @Test
    fun popShouldReturnPeek() {
        var stack = PersistentStack<Int>()
        repeat(times = 100000) { index ->
            stack = stack.push(index)
        }
        repeat(times = 100000) {
            val (popped, value) = stack.pop()
            assertEquals(stack.peek(), value)
            stack = popped
        }
    }

    @Test
    fun isEmptyTests() {
        val pushCount = 5

        var stack = PersistentStack<Int>()
        assertTrue(stack.isEmpty())

        repeat(times = pushCount) {
            stack = stack.push(0)
            assertFalse(stack.isEmpty())
        }

        repeat(times = pushCount - 1) {
            stack = stack.pop().resultStack
            assertFalse(stack.isEmpty())
        }

        stack = stack.pop().resultStack
        assertTrue(stack.isEmpty())
    }

    @Test
    fun sizeTests() {
        val pushCount = 5

        var stack = PersistentStack<Int>()
        assertEquals(0, stack.size)

        repeat(times = pushCount) { index ->
            stack = stack.push(0)
            assertEquals(index + 1, stack.size)
        }

        repeat(times = pushCount - 1) { index ->
            stack = stack.pop().resultStack
            assertEquals(pushCount - index - 1, stack.size)
        }

        stack = stack.pop().resultStack
        assertEquals(0, stack.size)
    }

    @Test
    fun smallStackTests() {
        fun <T> pushAndTestPopResult(stack: PersistentStack<T>, value: T): PersistentStack<T> {
            val pushResult = stack.push(value)
            val popResult = pushResult.pop()

            assertEquals(stack.peek(), popResult.resultStack.peek())
            assertEquals(pushResult.peek(), popResult.value)

            return pushResult
        }

        val empty = PersistentStack<String>()

        val a = pushAndTestPopResult(empty, "a")
        val ab = pushAndTestPopResult(a, a.peek() + "b")
        val abc = pushAndTestPopResult(ab, ab.peek() + "c")

        pushAndTestPopResult(abc, abc.peek() + "d")
        pushAndTestPopResult(abc, abc.peek() + "e")

        val abcf = pushAndTestPopResult(abc, abc.peek() + "f")

        pushAndTestPopResult(abcf, abcf.peek() + "g")
        pushAndTestPopResult(abcf, abcf.peek() + "h")

        val x = pushAndTestPopResult(empty, "x")

        pushAndTestPopResult(x, x.peek() + "y")
        pushAndTestPopResult(x, x.peek() + "z")
    }

    @Test
    fun bigStackTests() {
        /*
                                      - - - - - - - d
                                    /
                        - - - - - - - - - - - - - - - - - - - - - - - - c
                      /             \
        0 - - - - - a                 - - - - - - - e
                      \
                        - - - - - - - - - - b
        */

        val random = Random()

        fun pushRandomIntTo(stack: PersistentStack<Int>, times: Int): PersistentStack<Int> {
            var pushed = stack
            repeat(times) {
                pushed = pushed.push(random.nextInt())
            }
            return pushed
        }

        var a = pushRandomIntTo(PersistentStack(), times = 100000)
        var b = pushRandomIntTo(a, times = 200000)
        var c = pushRandomIntTo(a, times = 150000)
        var d = pushRandomIntTo(c, times = 100000)
        var e = pushRandomIntTo(c, times = 100000)
        c = pushRandomIntTo(c, times = 250000)

        fun popFrom(stack: PersistentStack<Int>, times: Int): PersistentStack<Int> {
            var popped = stack
            repeat(times) {
                popped = popped.pop().resultStack
            }
            return popped
        }

        c = popFrom(c, times = 250000)
        d = popFrom(d, times = 100000)
        e = popFrom(e, times = 100000)

        repeat(times = 150000) {
            assertEquals(1, listOf(c, d, e).map { it.peek() }.distinct().size)

            c = c.pop().resultStack
            d = d.pop().resultStack
            e = e.pop().resultStack
        }

        b = popFrom(b, times = 200000)

        repeat(times = 100000) {
            assertEquals(1, listOf(a, b, c, d, e).map { it.peek() }.distinct().size)

            a = a.pop().resultStack
            b = b.pop().resultStack
            c = c.pop().resultStack
            d = d.pop().resultStack
            e = e.pop().resultStack
        }
    }

    @Test
    fun randomOperationsTests() {
        val random = Random()
        val lists = mutableListOf<List<Int>>(emptyList())
        val stacks = mutableListOf<PersistentStack<Int>>(PersistentStack())

        repeat(times = 100000) {
            val index = random.nextInt(lists.size)
            val list = lists[index]
            val stack = stacks[index]

            assertEquals(list.size, stack.size)
            assertEquals(list.lastOrNull(), stack.peek())

            val shouldPop = random.nextDouble() < 0.1
            if (!list.isEmpty() && shouldPop) {
                stacks.add(stack.pop().resultStack)
                lists.add(list.dropLast(1))
            }

            val value = random.nextInt()
            stacks.add(stack.push(value))
            lists.add(list + listOf(value))
        }
    }
}