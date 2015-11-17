package ced.util
import ced.mapDoubleArray
import org.opencv.core.*

object Filters {
    public fun angleToHSV(src: Mat, s: Double = 255.0, v: Double = 255.0): Mat {
        return Mat(src.rows(), src.cols(), CvType.CV_8UC3, Scalar.all(0.0)).mapDoubleArray { y, x, self ->
            var p = src.get(y,x)[0]*90.0/Math.PI
            doubleArrayOf(p,s,v)
        }
    }
    public fun mapTo8UGray(src: Mat): Mat {
        val mm = Core.minMaxLoc(src)
        val ret = Mat()
        src.convertTo(ret, CvType.CV_8U, 255.0/(mm.maxVal-mm.minVal), -mm.minVal)
        return ret
    }
}