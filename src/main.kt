import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

fun main(args: Array<String>) {
    val lib = File("/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/java/lib"+Core.NATIVE_LIBRARY_NAME+".dylib")
    System.load(lib.absolutePath)
    val src = Imgcodecs.imread("res/lion.png")
    val dst = src.clone()
    val size = 256.0
    Imgproc.resize(src,dst,if (src.width() > src.height()) {
        Size(size,size*src.height()/src.width())
    } else {
        Size(size*src.width()/src.height(),size)
    }, 1.0, 1.0, Imgproc.INTER_CUBIC)
    Imgproc.pyrMeanShiftFiltering(dst,dst,30.0,20.0)
    Imgproc.cvtColor(dst,dst,Imgproc.COLOR_RGB2GRAY)
    val sobelX = dst.clone()
    val sobelY = dst.clone()
    Imgproc.Sobel(dst,sobelX,CvType.CV_32F,1,0)
    Imgproc.Sobel(dst,sobelY,CvType.CV_32F,0,1)
    val mag = Mat()
    val ang = Mat()
    val imgSobel = dst.clone()
    Core.cartToPolar(sobelX,sobelY,mag,ang)
    val minMax = Core.minMaxLoc(mag)
    mag.convertTo(imgSobel,CvType.CV_8U, -255/minMax.maxVal, 255.0)
    Imgcodecs.imwrite("tmp/tmp.png", imgSobel)
}