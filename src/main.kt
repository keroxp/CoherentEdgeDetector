import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

fun main(args: Array<String>) {
    val lib = File("/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/java/lib"+Core.NATIVE_LIBRARY_NAME+".dylib")
    System.load(lib.absolutePath)
    val files = File("res").listFiles()
    File("tmp").listFiles().forEach { f -> f.delete() }
    proc0(files.asList())
    proc1(files.asList())
}

fun proc0 (files: List<File>) {
    for (file in files) {
        val src = Imgcodecs.imread(file.absolutePath)
        val size = 256.0
        Imgproc.resize(src,src,if (src.width() > src.height()) {
            Size(size,size*src.height()/src.width())
        } else {
            Size(size*src.width()/src.height(),size)
        }, 1.0, 1.0, Imgproc.INTER_CUBIC)
        val ced = CoherentEdgeDetector(src)
        val res = ced.calc()
        val mm = Core.minMaxLoc(res)
        val out = res.clone()
        res.convertTo(out,CvType.CV_8U, 255.0/(mm.maxVal-mm.minVal), -mm.minVal)
        Imgcodecs.imwrite("tmp/${file.name}_sal.${file.extension}", out)
        print("${file.name} has been done.\n")
    }
}

fun proc1(files: List<File>) {
    for (file in files) {
        val src = Imgcodecs.imread(file.absolutePath)
        val size = 256.0
        Imgproc.resize(src,src,if (src.width() > src.height()) {
            Size(size,size*src.height()/src.width())
        } else {
            Size(size*src.width()/src.height(),size)
        }, 1.0, 1.0, Imgproc.INTER_CUBIC)
        Imgcodecs.imwrite("tmp/${file.name}",src)
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
        Imgcodecs.imwrite("tmp/${file.name}.0_sob.${file.extension}", imgSobel)
        val cohLine = imgSobel.clone()
        Imgproc.threshold(imgSobel,cohLine,50.0,255.0, Imgproc.THRESH_TOZERO)
        Imgcodecs.imwrite("tmp/${file.name}.1_coh.${file.extension}", cohLine)
        // Angle
        val angcolor = convAngToColor(ang)
        Imgproc.cvtColor(angcolor,angcolor,Imgproc.COLOR_HSV2RGB_FULL)
        val out2 = Mat(angcolor.rows(),angcolor.cols(),CvType.CV_8UC3, Scalar.all(0.0))
        angcolor.copyTo(out2,cohLine)
        Imgcodecs.imwrite("tmp/${file.name}.2_ang.${file.extension}", out2)
        print("${file.name} has been done.\n")
    }
}

fun convAngToColor(src: Mat): Mat {
    val ret = Mat(src.rows(),src.cols(),CvType.CV_32FC3, Scalar.all(3.0))
    for (y in 0..src.rows()-1) {
        for (x in 0..src.cols()-1) {
            var p = src.get(y,x)[0]*180.0/Math.PI
            ret.put(y,x,p,255.0,255.0)
        }
    }
    return ret
}
