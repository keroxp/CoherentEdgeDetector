package ced.detector

import ced.util.Mats
import ced.geometry.Direction
import ced.mapByteArray
import ced.to
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.imgproc.Imgproc

class CoherentEdgeDetector2 (val src: Mat) {
    val magnitude: Mat = Mat()
    val orientation: Mat = Mat()
    val cohLine: Mat = Mat()
    val grayed: Mat = Mat()
    val meanShifted: Mat = Mat()
    val gradientX: Mat = Mat()
    val gradientY: Mat = Mat()
    val sobel: Mat = Mat()
    val leftEdges = Mat.zeros(src.rows(),src.cols(), CvType.CV_8UC3)
    val topEdges = Mat.zeros(src.rows(),src.cols(), CvType.CV_8UC3)
    val rightEdges = Mat.zeros(src.rows(),src.cols(), CvType.CV_8UC3)
    val bottomEdges = Mat.zeros(src.rows(),src.cols(), CvType.CV_8UC3)
    val orientationMap: Mat = Mat()
    init {
        // Mean-Shift
        Imgproc.pyrMeanShiftFiltering(src,meanShifted,15.0,20.0)
        // Gray
        Imgproc.cvtColor(meanShifted,grayed, Imgproc.COLOR_BGR2GRAY)
        // Sobel
        Imgproc.Sobel(grayed,gradientX, CvType.CV_32F,1,0)
        Imgproc.Sobel(grayed,gradientY, CvType.CV_32F,0,1)
        Core.cartToPolar(gradientX,gradientY,magnitude,orientation)
        val minMax = Core.minMaxLoc(magnitude)
        magnitude.convertTo(sobel, CvType.CV_8U, 255.0/(minMax.maxVal-minMax.minVal), -minMax.minVal)
        Imgproc.threshold(sobel,cohLine,50.0,255.0, Imgproc.THRESH_TOZERO)
    }
    public fun detect(): List<MatOfPoint> {
        // Angle
        // ４方向に分解する
        val _left = leftEdges.clone()
        val _top = topEdges.clone()
        val _right = rightEdges.clone()
        val _bottom = bottomEdges.clone()
        orientation.mapByteArray { y, x, self ->
            val v = self.get(y,x)[0]
            val r = if ((Math.PI*2-Math.PI/4 < v && v <= Math.PI*2) || (0 <= v && v <= Math.PI/4)) {
                Direction.Right
            } else if (Math.PI/4 < v && v <= Math.PI-Math.PI/4) {
                Direction.Top
            } else if (Math.PI-Math.PI/4 < v && v <= Math.PI+Math.PI/4) {
                Direction.Left
            } else if (Math.PI+Math.PI/4 < v && v <= Math.PI*2-Math.PI/4) {
                Direction.Bottom
            } else {
                throw Exception("unexpected orientation: $v")
            }
            val b = 255.toByte()//sobel.get(y,x)[0].toByte()
            when (r) {
                Direction.Right ->  {
                    val ret = byteArrayOf(0.toByte(),255.toByte(),b)
                    _right.put(y,x,ret)
                    ret
                }
                Direction.Top -> {
                    val ret = byteArrayOf(45.toByte(),255.toByte(),b)
                    _top.put(y,x,ret)
                    ret
                }
                Direction.Left -> {
                    val ret = byteArrayOf(90.toByte(),255.toByte(),b)
                    _left.put(y,x,ret)
                    ret
                }
                Direction.Bottom -> {
                    val ret = byteArrayOf(135.toByte(),255.toByte(),b)
                    _bottom.put(y,x,ret)
                    ret
                }
            }
        }.to(Imgproc.COLOR_HSV2BGR).copyTo(orientationMap,cohLine)
        _left.to(Imgproc.COLOR_HSV2BGR).copyTo(leftEdges,cohLine)
        _top.to(Imgproc.COLOR_HSV2BGR).copyTo(topEdges,cohLine)
        _right.to(Imgproc.COLOR_HSV2BGR).copyTo(rightEdges,cohLine)
        _bottom.to(Imgproc.COLOR_HSV2BGR).copyTo(bottomEdges,cohLine)
        return emptyList<MatOfPoint>()
    }
    public fun createResultInfo(): Mat {
        return Mats.concatMatrix(2,createEdgeInfo(),createOrientationInfo())
    }
    private fun createOrientationInfo(): Mat {
        return Mats.concatMatrix(2,topEdges,leftEdges,bottomEdges,rightEdges)
    }
    private fun createEdgeInfo(): Mat {
        return Mats.concatMatrix(3,
                src,meanShifted,grayed.to(Imgproc.COLOR_GRAY2BGR),
                sobel.to(Imgproc.COLOR_GRAY2BGR),cohLine.to(Imgproc.COLOR_GRAY2BGR),orientationMap)
    }
}