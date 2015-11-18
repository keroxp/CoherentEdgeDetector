package ced.util
import ced.iterate
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.*

object Mats {
    fun concatMatrix(maxWidth: Int, vararg mats: Mat): Mat {
        val ret = Mat()
        val rows = ArrayList<Mat>()
        var i = 0
        while (i < mats.size) {
            val range = IntRange(i,i+maxWidth-1)
            val row = if (range.endInclusive < mats.size) {
                mats.slice(range)
            } else {
                val _row = ArrayList(mats.slice(IntRange(i,mats.size-1)))
                for (j in 0..maxWidth-_row.size-1) {
                    _row.add(Mat.zeros(mats[0].size(),mats[0].type()))
                }
                _row
            }
            var buf = Mat()
            Core.hconcat(row,buf)
            rows.add(buf)
            i += maxWidth
        }
        Core.vconcat(rows, ret)
        return ret
    }
    fun resize(src: Mat, size: Double, dst: Mat? = null): Mat {
        val ret = dst ?: src
        Imgproc.resize(src,ret,if (src.width() > src.height()) {
            Size(size, size * src.height() / src.width())
        } else {
            Size(size * src.width() / src.height(), size)
        }, 1.0, 1.0, Imgproc.INTER_CUBIC)
        return ret
    }
    fun copyTo(src: Mat, copied: Mat, sx: Int, sy: Int) {
        copied.iterate { y, x ->
            src.put(sy+y,sx+x,*copied.get(y,x))
        }
    }
}