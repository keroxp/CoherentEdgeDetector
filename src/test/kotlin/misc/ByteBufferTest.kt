package misc

import org.junit.Test
import java.nio.ByteBuffer
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ByteBufferTest {
    @Test fun test0() {
        val seta = HashSet<Int>(listOf(1,2,3))
        val setb = HashSet<Int>(listOf(2,3,3,3,1))
        println(seta.hashCode())
        assertEquals(seta.hashCode(),setb.hashCode())
        val setc = HashSet<Int>(listOf(0,6))
        assertFalse(seta.equals(setc))
    }
    @Test fun test() {
        val b = ByteBuffer.allocate(4).putInt(0xffeecc)
    }
}