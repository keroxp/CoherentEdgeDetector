package label

import ced.detector.CoherentEdgeDetector2
import ced.label.Labeler
import ced.resize
import ced.util.Mats
import ced.label.Labels
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
        val src = Imgcodecs.imread("res/test/bullbones.png").resize(256.0)
        val ced = CoherentEdgeDetector2(src)
        val labeler = Labeler(ced.magnitude,ced.orientation)
        val minLength = 10
        val minCoherency = 0.5
        val res = labeler.doLabeling(minLength = minLength, minCoherency = minCoherency)
        val out = File("tmp/test2")
        out.deleteRecursively()
        out.mkdirs()
        var i = 0
        val cohs = res.sortedByDescending { l -> l.coherency }
        for (l in cohs) {
            val m = l.toMat()
            assertTrue(l.area > 0)
            assertTrue(l.length >= minLength)
            assertTrue(l.coherency >= minCoherency)
            assertEquals(64,m.rows())
            assertEquals(64,m.cols())
            i++
        }
        val lines = cohs.map { c -> c.toMat() }.toTypedArray()
        val pack = Labels.packOriginalImages(res, 1024)
        Imgcodecs.imwrite("${out.path}/lines_packed.jpg", pack)
        Imgcodecs.imwrite("${out.path}/lines_of_${lines.size}.jpg",Mats.concatMatrix(10,*lines))
    }
}