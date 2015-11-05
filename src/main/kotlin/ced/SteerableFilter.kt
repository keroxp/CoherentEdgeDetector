package ced
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class SteerableFilter (val src: Mat) {
    val g2a = Mat()
    val g2b = Mat()
    val g2c = Mat()
    val h2a = Mat()
    val h2b = Mat()
    val h2c = Mat()
    val h2d = Mat()
    val kernG2a = calcKernel({x -> G2a(x)})
    val kernG2b = calcKernel({x -> G2b(x)})
    val kernG2c = calcKernel({x -> G2c(x)})
    val kernH2a = calcKernel({x -> H2a(x)})
    val kernH2b = calcKernel({x -> H2b(x)})
    val kernH2c = calcKernel({x -> H2c(x)})
    val kernH2d = calcKernel({x -> H2d(x)})
    var c1: Mat = Mat()
    var c2: Mat = Mat()
    var c3: Mat = Mat()
    init {
        // Calc Convolution products of basic filter of Gaussian
        Imgproc.sepFilter2D(src, g2a, CvType.CV_32F, kernG2a, kernG2b.t())
        Imgproc.sepFilter2D(src, g2b, CvType.CV_32F, kernG2c, kernG2c.t())
        Imgproc.sepFilter2D(src, g2c, CvType.CV_32F, kernG2b, kernG2a.t())
        Imgproc.sepFilter2D(src, h2a, CvType.CV_32F, kernH2a, kernH2b.t())
        Imgproc.sepFilter2D(src, h2b, CvType.CV_32F, kernH2d, kernH2c.t())
        Imgproc.sepFilter2D(src, h2c, CvType.CV_32F, kernH2c, kernH2d.t())
        Imgproc.sepFilter2D(src, h2d, CvType.CV_32F, kernH2b, kernH2a.t())
    }
    fun calcDominantOrientation(strength: Mat, orientation: Mat) {
        val g2aa = g2a*g2a // g2a*
        val g2ab = g2a*g2b
        val g2ac = g2a*g2c
        val g2bb = g2b*g2b // g2b*
        val g2bc = g2b*g2c
        val g2cc = g2c*g2c // g2c*
        val h2aa = h2a*h2a // h2a*
        val h2ab = h2a*h2b
        val h2ac = h2a*h2c
        val h2ad = h2a*h2d
        val h2bb = h2b*h2b // h2b*
        val h2bc = h2b*h2c
        val h2bd = h2b*h2d
        val h2cc = h2c*h2c // h2c*
        val h2cd = h2c*h2d
        val h2dd = h2d*h2d // h2d*
//        c1 = 0.5*(g2bb) + 0.25*(g2ac) + 0.375*(g2aa + g2cc) + 0.3125*(h2aa + h2dd) + 0.5625*(h2bb + h2cc) + 0.375*(h2ac + h2bd)
        c2 = 0.5*(g2aa - g2cc) + 0.46875*(h2aa - h2dd) + 0.28125*(h2bb - h2cc) + 0.1875*(h2ac - h2bd)
        c3 = (-1.0*g2ab) - g2bc - (0.9375*(h2cd + h2ab)) - (1.6875*(h2bc)) - (0.1875*(h2ad))
        Core.cartToPolar(c2,c3, strength, orientation)
        orientation *= .5
    }
    fun calcKernel(func: (Double) -> Double, width: Int = 2, spacing: Double = 0.67): Mat {
        val ret = Mat(1,width*2+1,CvType.CV_32F, Scalar.all(0.0))
        for (i in -width..width) {
            ret.put(0,i+width,func(i.toDouble())*spacing)
        }
        return ret
    }
    // Basic Filter Functions for Second Derivative of Gaussian
    fun G2a (x: Double): Double {
        return 0.9212*(2*x*x-1)*Math.exp(-x*x)
    }
    fun G2b (x: Double): Double {
        return Math.exp(-x*x)
    }
    fun G2c (x: Double): Double  {
        return Math.sqrt(1.843)*x*Math.exp(-x*x)
    }
    // Coefficient Functions for Gaussian Filter
    fun kG2a (theta: Double): Double {
        val a = Math.cos(theta)
        return a*a
    }
    fun kG2b (theta: Double): Double {
        return -2*Math.cos(theta)*Math.sin(theta)
    }
    fun kG2c (theta: Double): Double {
        val a = Math.sin(theta)
        return a*a
    }
    // Basic Filter Functions for Herbert Function
    fun H2a (x: Double): Double {
        return 0.9780*(-2.254*x + x*x*x)* Math.exp(-(x*x))
    }
    fun H2b (x: Double): Double {
        return Math.exp(-x*x)
    }
    fun H2c (x: Double): Double {
        return x * Math.exp(-x*x)
    }
    fun H2d (x: Double): Double {
        return 0.9780 * (-0.7515 + x*x) * Math.exp(-x*x)
    }
    // Coefficient Functionss for Basic Herbert Filter
    fun kH2a (theta: Double): Double {
        return Math.pow(Math.cos(theta), 3.0)
    }
    fun kH2b (theta: Double): Double {
        return -3*Math.pow(Math.cos(theta), 2.0)*Math.sin(theta)
    }
    fun kH2c (theta: Double): Double {
        return 3 *Math.cos(theta)*Math.pow(Math.sin(theta),2.0)
    }
    fun kH2d (theta: Double): Double {
        return -Math.pow(Math.sin(theta),3.0)
    }
}