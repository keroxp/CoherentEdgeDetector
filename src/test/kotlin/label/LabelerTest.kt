package label

import ced.detector.CoherentEdgeDetector2
import ced.label.Labeler
import ced.resize
import org.junit.BeforeClass
import org.opencv.core.Core
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import kotlin.test.assertEquals
import org.junit.Test
import org.opencv.core.Mat
import kotlin.test.assertTrue

class LabelerTest {
    companion object {
        @BeforeClass @JvmStatic fun setup(): Unit {
            val lib = File("/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/java/lib" + Core.NATIVE_LIBRARY_NAME + ".dylib")
            System.load(lib.absolutePath)
        }
    }
//    @Test fun test(): Unit {
//        val src = Imgcodecs.imread("res/test/penguin.jpg").resize(256.0)
//        val ced = CoherentEdgeDetector2(src)
//        val labeler = Labeler(ced.magnitude,ced.orientation)
//        val res = labeler.doLabeling()
//        assertEquals(labeler.labelsIndex,res.size)
//        for (l in res) {
//            val m = l.toMat()
//            assertEquals(64,m.rows())
//            assertEquals(64,m.cols())
//        }
//    }
    @Test fun test2(): Unit {
        val src = Imgcodecs.imread("res/test/forest.jpg").resize(256.0)
        val ced = CoherentEdgeDetector2(src)
        val labeler = Labeler(ced.magnitude,ced.orientation)
        val minLength = 30
        val minCoherency = 0.8
        val res = labeler.doLabeling(minLength = minLength, minCoherency = minCoherency)
        val out = File("tmp/test2")
        out.deleteRecursively()
        out.mkdirs()
        var i = 0
        val cohs = res.sortedByDescending { l -> l.coherency }
        for (l in cohs) {
            val m = l.toMat()
            assertTrue(l.area > 0)
            assertTrue(Math.max(l.bounds.width,l.bounds.height) >= minLength)
            assertTrue(l.coherency >= minCoherency)
            assertEquals(64,m.rows())
            assertEquals(64,m.cols())
            Imgcodecs.imwrite("${out.path}/line_${i}_org.bmp",l.original)
            Imgcodecs.imwrite("${out.path}/line_${i}_${l.coherency}_.bmp",m)
            i++
        }
    }
}