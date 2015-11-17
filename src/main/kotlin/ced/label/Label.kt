package ced.label

import ced.geometry.Direction
import ced.geometry.Point
import ced.geometry.Rect
import ced.iterate
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.math.BigInteger
import java.util.*

class Label (
        public val index: Int,
        public val pixels: Set<Point>,
        public val boundaries: Set<PixelNeighbour>, //  他のラベルと接しているピクセルの集合
        public val bounds: Rect,
        public val direction: Direction
) {
    public val neighbours: HashSet<Int> = HashSet()
    public val labelRelations: HashSet<LabelRelation> = HashSet()
    public val bitmap: BigInteger
    init {
        // 線画をビットマップに
        val line = Mat.zeros(bounds.height,bounds.width, CvType.CV_8U)
        for (p in pixels) {
            line.put(p.y-bounds.top, p.x-bounds.left, 255.0)
        }
        // 中央揃えして64x64へリサイズ
        val norm = Mat.zeros(64,64, CvType.CV_8U)
        val dst = Mat()
        val size = if (line.width() > line.height()) {
            Size(64.0, line.height() * 64.0 / line.width())
        } else {
            Size(line.width() * 64.0 / line.height(), 64.0)
        }
        Imgproc.resize(line, dst, size, 1.0,1.0, Imgproc.INTER_NEAREST)
        val left = (32-size.width*.5).toInt()
        val top = (32-size.height*.5).toInt()
        dst.iterate { y, x ->
            norm.put(top+y,left+x,dst.get(y,x)[0])
        }
        // BigIntegerのビット列へ変換
        val bit = StringBuilder()
        norm.iterate { y, x ->
            bit.append(if (norm.get(y,x)[0] == 0.0) 0 else 1)
        }
        bitmap = BigInteger(bit.toString(), 2)
    }
    public fun toMat(): Mat {
        val ret = Mat.zeros(64,64,CvType.CV_8U)
        bitmap.toString(2).forEachIndexed { i, c ->
            if (c == '1') {
                val y = (i/64).toInt()
                val x = i%64
                ret.put(y,x,255.0)
            }
        }
        return ret
    }
}