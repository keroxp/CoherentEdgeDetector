package ced
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*

fun Mat.iterate(func: (y: Int, x: Int) -> Unit) {
    for (y in 0..height()-1) {
        for (x in 0..width() - 1) {
            func(y,x)
        }
    }
}

fun Mat.grayToBGR(): Mat {
    val ret = Mat(rows(), cols(), CvType.CV_8UC3)
    Imgproc.cvtColor(this,ret, Imgproc.COLOR_GRAY2BGR,3)
    return ret
}

fun Mat.to(code: Int): Mat {
    val ret = Mat()
    Imgproc.cvtColor(this,ret,code)
    return ret
}

fun Mat.resize(size: Double, dst: Mat? = null): Mat {
    val ret = dst ?: this
    Imgproc.resize(this,ret,if (this.width() > this.height()) {
        Size(size, size * this.height() / this.width())
    } else {
        Size(size * this.width() / this.height(), size)
    }, 1.0, 1.0, Imgproc.INTER_CUBIC)
    return ret
}

fun Mat.mergeTo(dst: Mat): Mat {
    if (rows() > dst.rows() || cols() > dst.cols()) {
        throw Exception("rows() > dst.rows() || cols > dst.cols())")
    }
    val _y = (dst.rows()*.5-rows()*.5).toInt()
    val _x = (dst.cols()*.5-cols()*.5).toInt()
    iterate { y, x ->
        dst.put(y+_y,x+_x,*get(y,x))
    }
    return dst
}

fun Mat.mapDouble(func: (y: Int, x: Int, self: Mat) -> Double): Mat {
    val ret = Mat(rows(), cols(), CvType.CV_32F)
    iterate { y, x ->
        ret.put(y,x,func(y,x,this))
    }
    return ret
}

fun Mat.mapByteArray(func: (y: Int, x: Int, self: Mat) -> ByteArray): Mat {
    val ret = Mat(rows(), cols(), CvType.CV_8UC3)
    iterate { y, x ->
        ret.put(y,x, func(y,x,this))
    }
    return ret
}

fun Mat.mapDoubleArray(func: (y: Int, x: Int, self: Mat) -> DoubleArray): Mat {
    val ret = Mat(rows(), cols(), type())
    iterate { y, x ->
        ret.put(y,x,*func(y,x,this))
    }
    return ret
}

fun <T> Mat.map(func: (y: Int, x: Int, self: Mat) -> T): List<T> {
    val ret = ArrayList<T>()
    iterate { y, x ->
        ret.add(func(y,x,this))
    }
    return ret
}

operator fun Mat.plus(tgt: Mat): Mat {
    val ret = Mat()
    Core.add(this,tgt,ret)
    return ret
}

operator fun Mat.minus(tgt: Mat): Mat {
    val ret = Mat()
    Core.subtract(this,tgt,ret)
    return ret
}

operator fun Mat.times(tgt: Mat): Mat {
    return this.mul(tgt)
}

operator fun Mat.timesAssign(tgt: Double): Unit {
    Core.multiply(this, Scalar(tgt), this)
}

operator fun Double.times(tgt: Mat): Mat {
    val ret = Mat()
    Core.multiply(tgt, Scalar(this),ret)
    return ret
}