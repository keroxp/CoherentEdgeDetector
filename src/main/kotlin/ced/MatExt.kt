package ced
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

fun Mat.iterate(func: (y: Int, x: Int) -> Unit) {
    for (y in 0..height()-1) {
        for (x in 0..width() - 1) {
            func(y,x)
        }
    }
}

fun Mat.grayToBGR(): Mat {
    val ret = Mat(rows(),cols(),CvType.CV_8UC3)
    Imgproc.cvtColor(this,ret,Imgproc.COLOR_GRAY2BGR,3)
    return ret
}

fun Mat.to(code: Int): Mat {
    val ret = Mat()
    Imgproc.cvtColor(this,ret,code)
    return ret
}

fun Mat.mapDouble(func: (y: Int, x: Int) -> Double): Mat {
    val ret = Mat(rows(),cols(),CvType.CV_32F)
    iterate { y, x ->
        ret.put(y,x,func(y,x))
    }
    return ret
}

fun Mat.mapByteArray(func: (y: Int, x: Int) -> ByteArray): Mat {
    val ret = Mat(rows(),cols(),type())
    iterate { y, x ->
        ret.put(y,x, func(y,x))
    }
    return ret
}

fun Mat.mapDoubleArray(func: (y: Int, x: Int) -> DoubleArray): Mat {
    val ret = Mat(rows(),cols(),type())
    iterate { y, x ->
        ret.put(y,x,*func(y,x))
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