import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

object Filters {
    public fun angleToHSV(src: Mat, s: Double = 255.0, v: Double = 255.0): Mat {
        return Mat(src.rows(), src.cols(), CvType.CV_8UC3, Scalar.all(0.0)).mapDoubleArray { y, x ->
            var p = src.get(y,x)[0]*180.0/Math.PI
            p *= 255.0/360.0
            doubleArrayOf(p,s,v)
        }
    }
    public fun mapTo8UGray(src: Mat): Mat {
        val mm = Core.minMaxLoc(src)
        val ret = Mat()
        src.convertTo(ret,CvType.CV_8U, 255.0/(mm.maxVal-mm.minVal), -mm.minVal)
        return ret
    }
}