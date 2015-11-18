package geometry

import ced.geometry.Rect
import org.junit.Test
import kotlin.test.assertEquals

class RectTest {
    @Test fun test1 () {
        val r = Rect()
        r.width = 3
        r.height = 5
        assertEquals(3, r.right)
        assertEquals(5, r.bottom)
        r.left += 2
        r.top -= 4
        assertEquals(3+2,r.right)
        assertEquals(5-4,r.bottom)
    }
    @Test fun test2 () {
        val r = Rect(2,2)
        r.extend(4,5)
        assertEquals(2,r.width)
        assertEquals(3,r.height)
        r.extend(0,0)
        assertEquals(0,r.left)
        assertEquals(0,r.top)
        assertEquals(4,r.width)
        assertEquals(5,r.height)
    }
}