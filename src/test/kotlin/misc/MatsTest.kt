package misc

import ced.util.Mats
import org.junit.BeforeClass
import org.junit.Test
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.File
import kotlin.test.assertEquals

class MatsTest {
    companion object {
        @BeforeClass @JvmStatic fun setup(): Unit {
            val lib = File("/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/java/lib" + Core.NATIVE_LIBRARY_NAME + ".dylib")
            System.load(lib.absolutePath)
        }
    }
    @Test fun testExtend1() {
        val mat = Mat.zeros(100,100,CvType.CV_8U)
        val ext = Mats.extend(mat,maxWidth = 200)
        assertEquals(200,ext.width())
        assertEquals(100,ext.height())
    }
    @Test fun testExtend2() {
        val mat = Mat.zeros(100,100,CvType.CV_8U)
        val ext = Mats.extend(mat,maxHeight = 200)
        assertEquals(100,ext.width())
        assertEquals(200,ext.height())
    }
    @Test fun testExtend3() {
        val mat = Mat.zeros(100,100,CvType.CV_8U)
        val ext = Mats.extend(mat,maxWidth = 300, maxHeight = 200)
        assertEquals(300,ext.width())
        assertEquals(200,ext.height())
    }
    @Test fun testAppendRight() {
        val mat1 = Mat.zeros(100,200,CvType.CV_8U)
        val mat2 = Mat.zeros(100,300,CvType.CV_8U)
        val ret = Mats.appendRight(mat1,mat2)
        assertEquals(100,ret.height())
        assertEquals(500,ret.width())
    }
    @Test fun testAppendBottom() {
        val mat1 = Mat.zeros(200,100,CvType.CV_8U)
        val mat2 = Mat.zeros(300,100,CvType.CV_8U)
        val ret = Mats.appendBottom(mat1,mat2)
        assertEquals(500,ret.height())
        assertEquals(100,ret.width())
    }
}