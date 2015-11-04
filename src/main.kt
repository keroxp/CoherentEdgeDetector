import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.text.Regex

fun main(args: Array<String>) {
    val lib = File("/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/java/lib"+Core.NATIVE_LIBRARY_NAME+".dylib")
    System.load(lib.absolutePath)
    val files = File("res").listFiles()
    val tmp = File("tmp")
    !tmp.exists() && tmp.mkdir()
    tmp.listFiles().forEach { f -> f.delete() }
    files.filter { f -> f.extension.matches(Regex("jpe?g|png|gif")) } forEach { f -> proc1(f) }
}

fun proc0 (file: File) {
    val src = Imgcodecs.imread(file.absolutePath)
    val size = 512.0
    Imgproc.resize(src,src,if (src.width() > src.height()) {
        Size(size,size*src.height()/src.width())
    } else {
        Size(size*src.width()/src.height(),size)
    }, 1.0, 1.0, Imgproc.INTER_CUBIC)
    val ced = CoherentEdgeDetector(src)
    val salience = ced.calc()
    Imgcodecs.imwrite("tmp/${file.name}_sal.${file.extension}", Filters.mapTo8UGray(salience))
    Imgcodecs.imwrite("tmp/${file.name}_ang.${file.extension}", Filters.angleToHSV(ced.orientations).to(Imgproc.COLOR_HSV2RGB))
    print("${file.name} has been done.\n")
}

fun proc1(file: File) {
    val src = Imgcodecs.imread(file.absolutePath)
    val size = 256.0
    Imgproc.resize(src,src,if (src.width() > src.height()) {
        Size(size,size*src.height()/src.width())
    } else {
        Size(size*src.width()/src.height(),size)
    }, 1.0, 1.0, Imgproc.INTER_CUBIC)
    // Mean-Shift
    val ms = src.clone()
    Imgproc.pyrMeanShiftFiltering(src,ms,15.0,20.0)
    // Gray
    val gray = src.clone()
    Imgproc.cvtColor(ms,gray,Imgproc.COLOR_RGB2GRAY)
    // Sobel
    val sobelX = Mat()
    val sobelY = Mat()
    Imgproc.Sobel(gray,sobelX,CvType.CV_32F,1,0)
    Imgproc.Sobel(gray,sobelY,CvType.CV_32F,0,1)
    val mag = Mat()
    val ang = Mat()
    val imgSobel = src.clone()
    Core.cartToPolar(sobelX,sobelY,mag,ang)
    val minMax = Core.minMaxLoc(mag)
    mag.convertTo(imgSobel,CvType.CV_8U, 255.0/(minMax.maxVal-minMax.minVal), -minMax.minVal)
    val cohLine = imgSobel.clone()
    Imgproc.threshold(imgSobel,cohLine,50.0,255.0, Imgproc.THRESH_TOZERO)
    // Angle
    val angcolor = Mat()
    Filters.angleToHSV(ang).to(Imgproc.COLOR_HSV2BGR).copyTo(angcolor,cohLine)
    val out = Mats.concatMatrix(3,
            src, ms, gray.to(Imgproc.COLOR_GRAY2BGR),
            imgSobel.to(Imgproc.COLOR_GRAY2BGR), cohLine.to(Imgproc.COLOR_GRAY2BGR), angcolor
    )
    Imgcodecs.imwrite("tmp/${file.name}",out)
    print("${file.name} has been done.\n")
}

