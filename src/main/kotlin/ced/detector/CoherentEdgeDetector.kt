package ced.detector
import ced.iterate
import ced.mapDouble
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*

/*
    Edge Detector Class based on Algorithm by GradientShop
 */
class CoherentEdgeDetector(val src: Mat) {
    public val magnitudes: Mat = Mat()
    public val orientations: Mat = Mat()
    val gradientX: Mat = Mat()
    val gradientY: Mat = Mat()
    val normalizedMagnitudes: Mat
    val messages0: Mat
    val messages1: Mat
    val edgeLength: Mat
    init {
        messages0 = Mat(src.rows(), src.cols(), CvType.CV_32F, Scalar.all(Double.NaN))
        messages1 = Mat(src.rows(), src.cols(), CvType.CV_32F, Scalar.all(Double.NaN))
        edgeLength = Mat(src.rows(), src.cols(), CvType.CV_32F, Scalar.all(Double.NaN))
        // Gray
        val gray = Mat()
        Imgproc.cvtColor(src,gray, Imgproc.COLOR_BGR2GRAY)
        // Sobel
        Imgproc.Sobel(gray,gradientX, CvType.CV_32F,1,0)
        Imgproc.Sobel(gray,gradientY, CvType.CV_32F,0,1)
        Core.cartToPolar(gradientX, gradientY, magnitudes, orientations)
        // Steerable Filter
//        val stf = SteerableFilter(gray)
//        stf.calcDominantOrientation(Mat(), orientations)
        // normalize magnitudes
        val roi = Rect()
        normalizedMagnitudes = magnitudes.mapDouble { y, x, self ->
            if (x < 2 || y < 2 || magnitudes.width()-2 <= x || magnitudes.height()-2 <= y) {
                0.0
            } else {
                roi.x = x - 2
                roi.y = y - 2
                roi.width = 5
                roi.height = 5
                val neibs = Mat(magnitudes, roi)
                val mag = magnitudes.get(y, x)[0]
                val avr = calcAverage(neibs)
                val mother = Math.sqrt(calcVariance(neibs, avr))
                if (mother == 0.0) {
                    0.0
                } else {
                    (mag-avr)/mother
                }
            }
        }
    }

    public fun calc(): Mat {
        val ret = Mat(src.rows(), src.cols(), CvType.CV_32F)
        ret.iterate { y, x ->
            val s = calcSalience(x,y)
            ret.put(y,x,s)
        }
        return ret
    }

    public fun calcSalience(px: Int, py: Int): Double {
        val sx = calcSalienceX(px,py)
        val sy = calcSalienceY(px,py)
        return Math.sqrt(sx*sx + sy*sy)
    }

    fun calcSalienceX(px: Int, py: Int): Double {
        val a =  Math.cos(calcEdgeOrientation(px,py))
        return a*a*calcEdgeLength(px,py)*gradientX.get(py,px)[0]
    }

    fun calcSalienceY(px: Int, py: Int): Double {
        val a = Math.sin(calcEdgeOrientation(px,py))
        return a*a*calcEdgeLength(px,py)*gradientY.get(py,px)[0]
    }

    fun calcAverage(mat: Mat) : Double {
        var ret = 0.0
        var w = mat.width()
        var h = mat.height()
        for (yy in 0..h-1) {
            for (xx in 0..w-1) {
                ret += mat.get(yy,xx)[0]
            }
        }
        return ret/(w*h)
    }

    fun calcVariance(mat: Mat, average: Double) : Double {
        var ret = 0.0
        val w = mat.width()
        val h = mat.height()
        for (y in 0..h-1) {
            for (x in 0..w-1) {
                val diff = average - mat.get(y,x)[0]
                ret += diff*diff
            }
        }
        return ret/(w*h)
    }

    fun calcEdgeLength(px: Int, py: Int): Double {
        var ret = edgeLength.get(py,px)[0]
        if (!ret.isNaN()) {
            return ret
        }
        ret = messagePassing0(px,py,60) + messagePassing1(px,py,60) + normalizedMagnitudes.get(py,px)[0]
        edgeLength.put(py,px,ret)
        return ret
    }

    fun calcEdgeOrientation(px: Int, py: Int): Double {
        return orientations.get(py,px)[0]
    }

    inner class BiLinearPixel(
            px: Int,
            py: Int,
            rx: Double,
            ry: Double,
            floorX: Boolean,
            floorY: Boolean)
    {
        public val x: Int
        public val y: Int
        public val alpha: Double
        public val theta: Double
        val PI_2_DIV_5 = 2*Math.PI/5
        init {
            this.x = (if (floorX) Math.floor(rx) else Math.ceil(rx)).toInt()
            this.y = (if (floorY) Math.floor(ry) else Math.ceil(ry)).toInt()
            this.alpha = Math.abs((rx-x)*(ry-y))
            this.theta = if (x < 0 || y < 0 || src.width() <= x || src.height() <= y) {
                0.0
            } else {
                val p = orientations.get(py,px)[0]
                val q = orientations.get(y,x)[0]
                val d = p-q
                Math.exp(-(d*d)/PI_2_DIV_5)
            }
        }
    }
    val r2 = 1.41421356237
    fun getQs(px: Int, py: Int, angle: Double): List<BiLinearPixel> {
        val ret = ArrayList<BiLinearPixel>(4)
        val qx = px + r2*Math.cos(angle)
        val qy = py + r2*Math.sin(angle)
        try {
            ret.add(BiLinearPixel(px, py, qx, qy, true, true))
            ret.add(BiLinearPixel(px, py, qx, qy, true, false))
            ret.add(BiLinearPixel(px, py, qx, qy, false, true))
            ret.add(BiLinearPixel(px, py, qx, qy, false, false))
            return ret
        } catch (e: NullPointerException) {
            return emptyList()
        }

    }

    fun messagePassing0(px: Int, py:Int, itrCnt: Int): Double {
        val v = messagePassingInternal(px,py,false,itrCnt)
        return v;
    }

    fun messagePassing1(px: Int, py: Int, itrCnt: Int): Double {
        return messagePassingInternal(px,py,true,itrCnt)
    }

    fun messagePassingInternal(x: Int, y:Int, inverse: Boolean, itrCnt: Int): Double {
        // 計算済みの場合はそれを返す
        val messages = if (inverse) messages1 else messages0
        var ret = messages.get(y,x)[0]
        if (!ret.isNaN()) {
            return ret
        }
        if (itrCnt <= 0) {
            return 0.0
        }
        var sum = 0.0
        val ang = (if (inverse) orientations.get(y,x)[0] + Math.PI else orientations.get(y,x)[0])
        for (q in getQs(x, y, ang)) {
            if (q.x < 0 || q.y < 0 || src.width() <= q.x || src.height() <= q.y) {
                continue
            } else {
                val add = normalizedMagnitudes.get(q.y, q.x)[0] + messagePassingInternal(q.x, q.y, inverse, itrCnt - 1)
                sum += q.alpha * q.theta * add
            }
        }
        messages.put(y,x,sum)
        return sum
    }
}