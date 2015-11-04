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
    val kernG2a = calcKernel({x,y -> G2a(x,y)})
    val kernG2b = calcKernel({x,y -> G2b(x,y)})
    val kernG2c = calcKernel({x,y -> G2c(x,y)})
    val kernH2a = calcKernel({x,y -> H2a(x,y)})
    val kernH2b = calcKernel({x,y -> H2b(x,y)})
    val kernH2c = calcKernel({x,y -> H2c(x,y)})
    val kernH2d = calcKernel({x,y -> H2d(x,y)})
    var c1: Mat = Mat()
    var c2: Mat = Mat()
    var c3: Mat = Mat()
    init {
        // Calc Convolution products of basic filter of Gaussian
        Imgproc.filter2D(src, g2a, CvType.CV_32F, kernG2a)
        Imgproc.filter2D(src, g2b, CvType.CV_32F, kernG2b)
        Imgproc.filter2D(src, g2c, CvType.CV_32F, kernG2c)
        Imgproc.filter2D(src, h2a, CvType.CV_32F, kernH2a)
        Imgproc.filter2D(src, h2b, CvType.CV_32F, kernH2b)
        Imgproc.filter2D(src, h2c, CvType.CV_32F, kernH2c)
        Imgproc.filter2D(src, h2d, CvType.CV_32F, kernH2d)
    }
    fun calcMagnitudesAndDominantAngles(magnitudes: Mat, angles: Mat) {
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
        c1 = 0.5*(g2bb) + 0.25*(g2ac) + 0.375*(g2aa + g2cc) + 0.3125*(h2aa + h2dd) + 0.5625*(h2bb + h2cc) + 0.375*(h2ac + h2bd)
        c2 = 0.5*(g2aa - g2cc) + 0.46875*(h2aa - h2dd) + 0.28125*(h2bb - h2cc) + 0.1875*(h2ac - h2bd)
        c3 = (-1.0*g2ab) - g2bc - (0.9375*(h2cd + h2ab)) - (1.6875*(h2bc)) - (0.1875*(h2ad))
        Core.cartToPolar(c2,c3, magnitudes, angles)
        angles *= .5
    }
    fun calcKernel(func: (Double, Double) -> Double, width: Int = 2, spacing: Double = 0.67): Mat {
        return Mat(width*2+1,width*2+1,CvType.CV_32F, Scalar.all(0.0)).mapDouble { y, x ->
            func((x-width).toDouble(),(y-width).toDouble())*spacing
        }
    }
    // Basic Filter Functions for Second Derivative of Gaussian
    fun G2a (x: Double, y: Double): Double {
        return 0.9212*(2*x*x-1)*Math.exp(-(x*x+y*y))
    }
    fun G2b (x: Double, y: Double): Double {
        return 1.843*x*y*Math.exp(-(x*x+y*y))
    }
    fun G2c (x: Double, y: Double): Double  {
        return 0.9212*(2*y*y-1)*Math.exp(-(x*x+y*y))
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
    fun H2a (x: Double, y: Double): Double {
        return 0.9780*(-2.254*x + x*x*x)* Math.exp(-(x*x+y*y))
    }
    fun H2b (x: Double, y: Double): Double {
        return 0.9780*(-0.7515 + x*x)*Math.exp(-(x*x*+y*y))
    }
    fun H2c (x: Double, y: Double): Double {
        return 0.9780*(-0.7515 + y*y)*Math.exp(-(x*x*+y*y))
    }
    fun H2d (x: Double, y: Double): Double {
        return 0.9780*(-2.254*y + y*y*y)* Math.exp(-(x*x+y*y))
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