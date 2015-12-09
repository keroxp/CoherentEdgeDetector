package ced
import ced.label.Labeler
import ced.detector.CoherentEdgeDetector
import ced.detector.CoherentEdgeDetector2
import ced.util.Mats
import ced.util.Filters
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Path
import java.sql.DriverManager
import java.util.*
import kotlin.text.Regex

fun main(args: Array<String>) {
    val lib = File("/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/java/lib"+Core.NATIVE_LIBRARY_NAME+".dylib")
    System.load(lib.absolutePath)
    val props = Properties()
    FileInputStream("args.properties").use { ins ->
        InputStreamReader(ins,"UTF-8").use { isr ->
            props.load(isr)
        }
    }
    val home = System.getProperty("user.home")
    val path = props.getProperty("path").replaceFirst(Regex("^~"),"$home")
    val reset = props.getProperty("reset").equals("1")
    CreateDB().use { cdb ->
        if (reset) cdb.resetDB()
        File(path).listFiles { f -> f.isDirectory }?.forEach { category ->
            cdb.insert(imageFiles(category), category.nameWithoutExtension)
        }
    }
}

fun doProc(i: Int, res: File, size: Double = 256.0) {
    val o = outdir("tmp/proc$i/${res.name}")
    imageFiles(res).forEach { f ->
        when (i) {
            0 -> proc0(o,f,size)
            1 -> proc1(o,f,size)
            2 -> proc2(o,f,size)
        }
    }
}

fun imageFiles(file: File): List<File> {
    return file.listFiles().filter { f -> f.extension.matches(Regex("jpe?g|png|gif")) }
}

fun outdir(path: String, clear: Boolean = true): File {
    val outdir = File(path)
    !outdir.exists() && outdir.mkdirs()
    if (clear) outdir.listFiles().forEach { f -> f.delete() }
    return outdir
}

fun proc0 (outdir: File, file: File, size: Double) {
    val src = Imgcodecs.imread(file.absolutePath)
    Mats.resize(src,size)
    val ced = CoherentEdgeDetector(src)
    val salience = ced.calc()
    val mm = Core.minMaxLoc(salience)
    salience.convertTo(salience,CvType.CV_8U, 255/(mm.maxVal-mm.minVal),-mm.minVal)
    val el = Filters.mapTo8UGray(ced.edgeLength)
    val out1 = Mat()
    Core.hconcat(listOf(el,salience),out1)
    Imgcodecs.imwrite("${outdir.path}/len_sal_${file.name}",out1)
    val angcolor = Mat()
    Filters.angleToHSV(ced.orientations).to(Imgproc.COLOR_HSV2BGR).copyTo(angcolor, salience)
    val out = Mats.concatMatrix(3, src,salience.grayToBGR(), angcolor)
    Imgcodecs.imwrite("${outdir.path}/steer_${file.name}",out)
    print("${file.name} has been done.\n")
}


fun proc1(outdir: File , file: File, size: Double) {
    val src = Imgcodecs.imread(file.absolutePath)
    Mats.resize(src,size)
    val ced2 = CoherentEdgeDetector2(src)
    val labeler = Labeler(ced2.cohLine,ced2.orientation)
    val res = labeler.doLabeling(minLength = 10, minCoherency = 0.75)
    val lines = res.sortedByDescending { r -> r.coherency }.map { r -> r.toMat() }.toTypedArray()
    val matrix = Mats.concatMatrix(10,*lines)
    Imgcodecs.imwrite("${outdir.path}/coh_${file.nameWithoutExtension}.jpg", matrix)
    print("${file.name} has been done.\n")
}

fun proc2 (outdir: File, file: File, size: Double) {
    val src = Imgcodecs.imread(file.absolutePath).resize(256.0)
    val gray = src.to(Imgproc.COLOR_BGR2GRAY)
    val edge = Mat()
    Imgproc.Canny(gray, edge, 50.0, 100.0, 3, true)
    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(edge,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE,Point())
    val out = Mat.zeros(edge.size(),CvType.CV_8UC3)
    val white = Scalar.all(255.0)
    for (i in 0..contours.size-1) {
        Imgproc.drawContours(out,contours,i, white, 1, Imgproc.LINE_AA, hierarchy, 0, Point())
    }
    Imgcodecs.imwrite("$outdir/${file.name}", Mats.concatMatrix(3,src,out))
    print("${file.name}, edges: ${contours.size}\n")
}

