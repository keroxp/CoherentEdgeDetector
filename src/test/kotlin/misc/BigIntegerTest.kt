package misc

import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class BigIntegerTest {
    @Test fun testBI () {
        val s = "01010111"
        val bi = BigInteger(s,2)
        assertEquals(s.length, bi.bitLength())
        assertEquals(s,bi.toString(2))
    }
    @Test fun testBI2 () {
        val s = "001111"
        val bi1 = BigInteger(s,2)
        val bi2 = BigInteger(bi1.toString(2),2)
        assertEquals(4,bi1.bitCount())
        assertEquals(4,bi2.bitCount())
        assertEquals(0,bi1.xor(bi2).bitCount())
    }
}