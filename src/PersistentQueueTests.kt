import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.NoSuchElementException

class PersistentQueueTests {
    @Test
    fun newQueueShouldBeEmpty() {
        assertTrue(PersistentQueue<Int>().isEmpty())
    }

    @Test
    fun frontForNewQueueShouldBeNull() {
        assertNull(PersistentQueue<Int>().front())
    }

    @Test
    fun sizeOfNewQueueShouldBeZero() {
        assertEquals(0, PersistentQueue<Int>().size)
    }

    @Test
    fun dequeueForNewQueueShouldThrow() {
        assertThrows(NoSuchElementException::class.java) { PersistentQueue<Int>().dequeue() }
    }

    @Test
    fun enqueueShouldNotMutateQueueButShouldReturnNewQueue() {
        val queue = PersistentQueue<Int>()
        val newQueue = queue.enqueue(0)

        assertTrue(queue.isEmpty())

        assertFalse(newQueue.isEmpty())
        assertEquals(0, newQueue.front())
    }

    @Test
    fun dequeueShouldNotMutateQueueButShouldReturnNewQueue() {
        val queue = PersistentQueue<Int>().enqueue(0)
        val (newQueue, value) = queue.dequeue()

        assertFalse(queue.isEmpty())
        assertTrue(newQueue.isEmpty())

        assertEquals(0, queue.front())
        assertEquals(0, value)
    }

    @Test
    fun dequeueShouldReturnFront() {
        var queue = PersistentQueue<Int>()
        repeat(times = 100000) { index ->
            queue = queue.enqueue(index)
        }
        repeat(times = 100000) {
            val (dequeued, value) = queue.dequeue()
            assertEquals(queue.front(), value)
            queue = dequeued
        }
    }

    @Test
    fun isEmptyTests() {
        val enqueueCount = 5

        var queue = PersistentQueue<Int>()
        assertTrue(queue.isEmpty())

        repeat(times = enqueueCount) {
            queue = queue.enqueue(0)
            assertFalse(queue.isEmpty())
        }

        repeat(times = enqueueCount - 1) {
            queue = queue.dequeue().resultQueue
            assertFalse(queue.isEmpty())
        }

        queue = queue.dequeue().resultQueue
        assertTrue(queue.isEmpty())
    }

    @Test
    fun sizeTests() {
        val enqueueCount = 5

        var queue = PersistentQueue<Int>()
        assertEquals(0, queue.size)

        repeat(times = enqueueCount) { index ->
            queue = queue.enqueue(0)
            assertEquals(index + 1, queue.size)
        }

        repeat(times = enqueueCount - 1) { index ->
            queue = queue.dequeue().resultQueue
            assertEquals(enqueueCount - index - 1, queue.size)
        }

        queue = queue.dequeue().resultQueue
        assertEquals(0, queue.size)
    }

    @Test
    fun smallQueueTests() {
        fun <T> enqueueAndTestDequeueResult(queue: PersistentQueue<T>, value: T): PersistentQueue<T> {
            var inputQueue = queue
            var enqueued = queue.enqueue(value)

            while (!inputQueue.isEmpty()) {
                val (inputQueueResultQueue, inputQueueResultValue) = inputQueue.dequeue()
                inputQueue = inputQueueResultQueue

                val (enqueuedResultQueue, enqueuedResultValue) = enqueued.dequeue()
                enqueued = enqueuedResultQueue

                assertEquals(inputQueueResultValue, enqueuedResultValue)
            }
            val (enqueuedResultQueue, enqueuedResultValue) = enqueued.dequeue()
            enqueued = enqueuedResultQueue

            assertEquals(value, enqueuedResultValue)
            assertTrue(enqueued.isEmpty())

            return queue.enqueue(value)
        }

        val empty = PersistentQueue<String>()

        val a = enqueueAndTestDequeueResult(empty, "a")
        val ab = enqueueAndTestDequeueResult(a, "ab")
        val abc = enqueueAndTestDequeueResult(ab, "abc")

        enqueueAndTestDequeueResult(abc, "abcd")
        enqueueAndTestDequeueResult(abc, "abce")

        val abcf = enqueueAndTestDequeueResult(abc, "abcf")

        enqueueAndTestDequeueResult(abcf, "abcfg")
        enqueueAndTestDequeueResult(abcf, "abcfh")

        val x = enqueueAndTestDequeueResult(empty, "x")

        enqueueAndTestDequeueResult(x, "xy")
        enqueueAndTestDequeueResult(x, "xz")
    }

    @Test
    fun bigQueueTests() {
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

        fun enqueueRandomIntTo(queue: PersistentQueue<Int>, times: Int): PersistentQueue<Int> {
            var enqueued = queue
            repeat(times) {
                enqueued = enqueued.enqueue(random.nextInt())
            }
            return enqueued
        }

        var a = enqueueRandomIntTo(PersistentQueue(), times = 100000)
        var b = enqueueRandomIntTo(a, times = 200000)
        var c = enqueueRandomIntTo(a, times = 150000)
        var d = enqueueRandomIntTo(c, times = 100000)
        var e = enqueueRandomIntTo(c, times = 100000)
        c = enqueueRandomIntTo(c, times = 250000)

        repeat(times = 100000) {
            assertEquals(1, listOf(a, b, c, d, e).map { it.front() }.distinct().size)

            a = a.dequeue().resultQueue
            b = b.dequeue().resultQueue
            c = c.dequeue().resultQueue
            d = d.dequeue().resultQueue
            e = e.dequeue().resultQueue
        }

        repeat(times = 150000) {
            assertEquals(1, listOf(c, d, e).map { it.front() }.distinct().size)

            c = c.dequeue().resultQueue
            d = d.dequeue().resultQueue
            e = e.dequeue().resultQueue
        }
    }

    @Test
    fun randomOperationsTests() {
        val random = Random()
        val lists = mutableListOf<List<Int>>(emptyList())
        val queues = mutableListOf<PersistentQueue<Int>>(PersistentQueue())

        repeat(times = 100000) {
            val index = random.nextInt(lists.size)
            val list = lists[index]
            val queue = queues[index]

            assertEquals(list.size, queue.size)
            assertEquals(list.firstOrNull(), queue.front())

            val shouldDequeue = random.nextDouble() < 0.1
            if (!list.isEmpty() && shouldDequeue) {
                queues.add(queue.dequeue().resultQueue)
                lists.add(list.drop(1))
            }

            val value = random.nextInt()
            queues.add(queue.enqueue(value))
            lists.add(list + listOf(value))
        }
    }
}