import org.opencv.core.Core
import org.opencv.core.Mat
import java.util.*

object Mats {
    fun concatMatrix(maxWidth: Int, vararg mats: Mat): Mat {
        val ret = Mat()
        val rows = ArrayList<Mat>()
        var i = 0
        while (i < mats.size) {
            val range = IntRange(i,i+maxWidth-1)
            val row = if (range.end < mats.size) {
                mats.slice(range)
            } else {
                mats.slice(IntRange(i,mats.size-1))
            }
            var buf = Mat()
            Core.hconcat(row,buf)
            rows.add(buf)
            i += maxWidth
        }
        Core.vconcat(rows, ret)
        return ret
    }
}