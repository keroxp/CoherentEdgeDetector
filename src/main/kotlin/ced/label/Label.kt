package ced.label

import ced.geometry.Point
import ced.geometry.Rect
import ced.iterate
import ced.util.SQLs
import com.google.common.hash.Hashing
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import kotlin.test.assertTrue

class Label (
        val src: Mat,
        public val index: Int,
        public var pixels: MutableSet<Point>,
        public var boundaries: MutableSet<PixelNeighbour>, //  他のラベルと接しているピクセルの集合
        public val bounds: Rect,
        public val direction: Int
) {
    companion object {
        public val LABEL_YES = 0.0
        public val LABEL_NO = 255.0
    }
    public var neighbours: MutableSet<Label> = HashSet()
    public val minHashes: MutableSet<Sketch> = HashSet()
    public val original: Mat
    public val bitmap: BigInteger
    public var coherency: Double
    public val area: Int get () = pixels.size
    public val length: Int get () = Math.max(bounds.width+1,bounds.height+1)
    public val boundsArea: Int get () = (bounds.width+1)*(bounds.height+1)
    public var tiledStartX = -1
    public var tiledStartY = -1
    init {
        assert(pixels.size > 0)
        // 線画をビットマップに
        val line = Mat(bounds.height+1,bounds.width+1, CvType.CV_8U, Scalar.all(LABEL_NO))
        for (p in pixels) {
            line.put(p.y-bounds.top, p.x-bounds.left, LABEL_YES)
        }
        original = line
        // 中央揃えして64x64へリサイズ
        val norm = Mat(64,64, CvType.CV_8U,Scalar(LABEL_NO))
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
            bit.append(if (norm.get(y,x)[0] == LABEL_NO) 0 else 1)
        }
        bitmap = BigInteger(bit.toString(), 2)
        // Coherency
        coherency = calcCoherency()
        // MinHash
        val tmp = ArrayList<Int>()
        norm.iterate { y, x ->
            if (norm.get(y,x)[0] == LABEL_YES) {
                tmp.add(y*norm.width()+x)
            }
        }
        val hashFunc = Hashing.crc32c()
        for (n in 0..19) {
            val hashes = ArrayList<Int>(3)
            for (k in 0..2) {
                var min = Int.MAX_VALUE
                for (b in tmp) {
                    var h = hashFunc.hashString("$n-$k-$b",Charset.forName("UTF-8")).asInt()
                    if (h < min) {
                        min = h
                    }
                }
                hashes.add(min)
            }
            minHashes.add(Sketch(n,hashes))
        }
    }
    public fun calcCoherency(): Double {
//        val ln = length/Math.max(src.width()+1,src.height()+1).toDouble()
        val sq = Math.min(bounds.width+1,bounds.height+1)/Math.max(bounds.width+1,bounds.height+1).toDouble()
        val tn = pixels.size/boundsArea.toDouble()
        return  1 - sq*tn
    }
    public fun merge(child: Label) {
        if (index == child.index) {
            assert(index != child.index)
        }
        pixels.plusAssign(child.pixels)
        boundaries = boundaries.union(child.boundaries).filter { b -> b.label != index && b.label != child.index }.toMutableSet()
        child.pixels.forEach { p -> bounds.extend(p) }
        neighbours = neighbours.union(child.neighbours).minus(listOf(child,this)).toHashSet()
        coherency = calcCoherency()
        assertTrue(!neighbours.contains(child))
        assertTrue(!neighbours.contains(this))
    }
    public fun toMat(): Mat {
        val ret = Mat.zeros(64,64,CvType.CV_8U)
        val bits = bitmap.toString(2)
        val s = 64*64-bits.length
        for (i in s..4095) {
            val c = bits[i-s]
            if (c == '1') {
                val y = (i/64).toInt()
                val x = i%64
                ret.put(y,x,255.0)
            }
        }
        return ret
    }

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Label

        if (index != other.index) return false

        return true
    }

    override fun hashCode(): Int{
        return index
    }

    public fun distTo(tgt: Label): Double {
        val tmp = ArrayList<Double>()
        for (a in Rect.Corner.values) {
            for (b in Rect.Corner.values) {
                tmp.add(bounds.corner(a).powDistanceTo(tgt.bounds.corner(b)))
            }
        }
        tmp.sort()
        return Math.sqrt(tmp.first())
    }

    /*
    id INTEGER PRIMARY KEY AUTOINCREMENT,
     image_id INTEGER NOT NULL,
     label INTEGER NOT NULL,
     sx INTEGER NOT NULL,
     sy INTEGER NOT NULL,
     width INTEGER NOT NULL,
     height INTEGER NOT NULL,
     area REAL NOT NULL,
     length REAL NOT NULL,
     coherency REAL NOT NULL,
     direction INTEGER NOT NULL
     */
    public fun toInsertQuery(line_id: Int, image_id: Int, table_name: String): String {
        val hash = HashMap<String, Any>()
        hash.put("id", line_id)
        hash.put("image_id", image_id)
        hash.put("label", index)
        hash.put("sx", bounds.left)
        hash.put("sy", bounds.top)
        hash.put("width", bounds.width+1)
        hash.put("height", bounds.height+1)
        hash.put("area", area)
        hash.put("length", length)
        hash.put("coherency", coherency)
        hash.put("direction", direction)
        hash.put("tiled_sx", tiledStartX)
        hash.put("tiled_sy", tiledStartY)
        return SQLs.hashToInsertQuery(hash, table_name)
    }
    public fun toCSV(line_id: Int, image_id: Int): String {
        return listOf(line_id,image_id,index,bounds.left,bounds.top,bounds.width+1,bounds.height+1,area,length,coherency,direction,tiledStartX,tiledStartY).map { i ->
            "'$i'"
        }.joinToString(",")
    }

}