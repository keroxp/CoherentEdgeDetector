package ced.filter
import ced.*
import ced.timesAssign
import ced.util.Mats
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
        c1 = 0.5*(g2bb) + 0.25*(g2ac) + 0.375*(g2aa + g2cc) + 0.3125*(h2aa + h2dd) + 0.5625*(h2bb + h2cc) + 0.375*(h2ac + h2bd)
        c2 = 0.5*(g2aa - g2cc) + 0.46875*(h2aa - h2dd) + 0.28125*(h2bb - h2cc) + 0.1875*(h2ac - h2bd)
        c3 = (-1.0*g2ab) - g2bc - (0.9375*(h2cd + h2ab)) - (1.6875*(h2bc)) - (0.1875*(h2ad))
        Core.cartToPolar(c2,c3, strength, orientation)
        orientation *= .5
    }
    fun calcKernel(func: (Double) -> Double, width: Int = 2, spacing: Double = 0.67): Mat {
        val ret = Mat(1, width * 2 + 1, CvType.CV_32F, Scalar.all(0.0))
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
    fun steer(theta: Double, g2: Mat, h2: Mat) {
        // Create the steering coefficients, then compute G2 and H2 at orientation theta:
        val ct = Math.cos(theta)
        val ct2 = ct*ct
        val ct3 = ct2*ct
        val st = Math.sin(theta)
        val st2 = st*st
        val st3 = st2*st
        val ga = ct2
        val gb = (-2.0 * ct * st)
        val gc = st2
        val ha = ct3
        val hb = (-3.0 * ct2 * st)
        val hc = (3.0 * ct * st2)
        val hd = (-st3)
        (ga * g2a + gb * g2b + gc * g2c).copyTo(g2)
        (ha * h2a + hb * h2b + hc * h2c + hd * h2d).copyTo(h2);
    }

    // Steer filters at a single pixel:
    fun steer(x: Int, y: Int, theta: Double): Pair<Double,Double>
    {
        // Create the steering coefficients, then compute G2 and H2 at orientation theta:
        val ct = Math.cos(theta)
        val ct2 = ct*ct
        val ct3 = ct2*ct
        val st = Math.sin(theta)
        val st2 = st*st
        val st3 = st2*st
        val ga = ct2
        val gb = (-2.0 * ct * st)
        val gc = st2
        val ha = ct3
        val hb = (-3.0 * ct2 * st)
        val hc = 3.0 * ct * st2
        val hd = -st3
        val g2 = ga * g2a.get(y,x)[0] + gb * g2b.get(y,x)[0] + gc * g2c.get(y,x)[0]
        val h2 = ha * h2a.get(y,x)[0] + hb * g2b.get(y,x)[0] + hc * g2c.get(y,x)[0] + hd * h2d.get(y,x)[0]
        return Pair(g2,h2)
    }

    fun phaseWeights(phase: Mat, phi: Double, signum: Boolean, k: Double): Mat
    {
        val ct = Mat()
        val st = Mat()
        var error = if (signum) {
            Mats.abs(phase, Scalar(phi))
        } else {
            Mats.abs(Mats.abs(phase, Scalar(0.0)), Scalar(phi))
        }
        error = Mats.min(error, -(error-2.0*Math.PI))
        Core.polarToCart(Mat(), error, ct, st);
        val mask = Mat()
        Core.compare(Mats.abs(error, Scalar(0.0)), Scalar(Math.PI*.5),mask,Core.CMP_GT)
        val lambda = ct.mul(ct);
        lambda.setTo(Scalar(0.0), mask)
        return lambda
    }

    //  phase = arg(G2, H2) where arg(x + iy) = atan(y,x), (opencv return angles in [0..2pi])
    //  0      = dark line
    //  pi     = bright line
    // +pi/2   = edge
    // -pi/2   = edge
    fun phaseEdge(e: Mat, phase: Mat, phi: Double, signum: Boolean, k : Double): Mat
    {
        val lambda = phaseWeights(phase, phi, signum, k);
        return e.mul(lambda)
    }

    fun findEdges(e: Mat, phase: Mat, k: Double): Mat
    {
        return phaseEdge(e, phase, Math.PI*.5, false, k);
    }

    fun findDarkLines(e: Mat, phase: Mat, k: Double): Mat
    {
        return phaseEdge(e, phase, 0.0, true, k);
    }

    fun findBrightLines(e: Mat, phase: Mat, k: Double): Mat
    {
        return phaseEdge(e, phase, Math.PI*.5, true, k);
    }
}