package misc

import ced.geometry.Point
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class HashSetTest {
    @Test fun test() {
        val hs = HashSet<Point>()
        hs.add(Point(1,1))
        hs.add(Point(2,2))
        assertEquals(2, hs.size)
        hs.remove(Point(2,2))
        assertEquals(1, hs.size)
        hs.add(Point(1,1))
        assertEquals(1, hs.size)
    }
}