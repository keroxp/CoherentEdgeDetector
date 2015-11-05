package ced
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.text.Regex

fun main(args: Array<String>) {
    val lib = File("/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/java/lib"+Core.NATIVE_LIBRARY_NAME+".dylib")
    System.load(lib.absolutePath)
    val o0 = outdir("tmp/proc0")
    val o1 = outdir("tmp/proc1")
    imageFiles("res").forEach { f ->
        proc0(o0,f)
        proc1(o1,f)
    }
}

fun imageFiles(path: String): List<File> {
    return File(path).listFiles().filter { f -> f.extension.matches(Regex("jpe?g|png|gif")) }
}

fun outdir(path: String, clear: Boolean = true): File {
    val outdir = File(path)
    !outdir.exists() && outdir.mkdirs()
    if (clear) outdir.listFiles().forEach { f -> f.delete() }
    return outdir
}

fun proc0 (outdir: File, file: File) {
    val src = Imgcodecs.imread(file.absolutePath)
    val size = 256.0
    Mats.resize(src,src,size)
    val ced = CoherentEdgeDetector(src)
//    val salience = Filters.mapTo8UGray(ced.calc())
    val salience = ced.calc()
    val mm = Core.minMaxLoc(salience)
    salience.convertTo(salience,CvType.CV_8UC3, 255/(mm.maxVal-mm.minVal),-mm.minVal)
    val angcolor = Mat()
    Filters.angleToHSV(ced.orientations).to(Imgproc.COLOR_HSV2BGR).copyTo(angcolor, salience)
    Imgcodecs.imwrite("${outdir.path}/sal_${file.name}",salience)
    val out = Mats.concatMatrix(3, src,salience.grayToBGR(), angcolor)
    Imgcodecs.imwrite("${outdir.path}/steer_${file.name}",out)
    print("${file.name} has been done.\n")
}

fun proc1(outdir: File , file: File) {
    val src = Imgcodecs.imread(file.absolutePath)
    val size = 256.0
    Mats.resize(src,src,size)
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
    Imgcodecs.imwrite("${outdir.path}/${file.name}",out)
    print("${file.name} has been done.\n")
}

