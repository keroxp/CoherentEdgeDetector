package ced

import ced.detector.CoherentEdgeDetector2
import ced.label.Labeler
import ced.label.Labels
import ced.util.Mats
import ced.util.Performance
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
class  CreateDB {
    fun getCount(connection: Connection, colName: String): Int {
        val rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM $colName;")
        rs.next()
        return rs.getInt("COUNT(*)")
    }
    val connection: Connection
    init {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        connection = DriverManager.getConnection("jdbc:mysql://localhost/bullghost?user=root&password=password");
    }
    fun resetDB () {
        connection.createStatement().executeUpdate("DELETE FROM Images;")
        connection.createStatement().executeUpdate("DELETE FROM CoherentLines;")
        connection.createStatement().executeUpdate("DELETE FROM MinHashes;")
        connection.createStatement().executeUpdate("DELETE FROM NearLines;")
    }
    fun insert(images: List<File>, category: String) {
        // idを自前で振るためにはじめに数えておく
        val imageCount = getCount(connection, "Images")
        val lineCount = getCount(connection, "CoherentLines")
        var image_id = imageCount
        var line_id = lineCount
        val cohLinesFile = File("db/coherent_lines.csv")
        val imagesFile = File("db/images.csv")
        val minHashesFile = File("db/min_hashes.csv")
        val nearLinesFile = File("db/near_lines.csv")
        /*
        out/
          category/
            name/
             lines.ext
             edge.png
             original.png
             thumbnail.png
         */
        val size = 256.0
        val lines_writer = FileWriter(cohLinesFile)
        val images_writer = FileWriter(imagesFile)
        val min_hashes_writer = FileWriter(minHashesFile)
        val near_lines_writer = FileWriter(nearLinesFile)
        Performance.init()
        try {
            images_writer.write("id,category,name,width,height,thumb_width,thumb_height\n")
            lines_writer.write("id,image_id,label,sx,sy,width,height,area,length,coherency,direction,tiled_sx,tiled_sy\n")
            min_hashes_writer.write("line_id, hash_func_index, sketches, sketch_hash\n")
            near_lines_writer.write("line_id,target_line_id,distance\n")
            for (file in images) {
                print("category: $category, name: ${file.name} ..")
                val org = Imgcodecs.imread(file.absolutePath)
                val src = org.resize(size)
                val ced2 = Performance.calc("CoherentEdgeDetector2#init") {
                    CoherentEdgeDetector2(src)
                }
                val labeler = Performance.calc("Labeler#init") {
                    Labeler(ced2.cohLine, ced2.orientation)
                }
                val labels = Performance.calc("Labeler#doLabeling") {
                    labeler.doLabeling(minLength = 10, minCoherency = 0.75)
                }
                val name = file.nameWithoutExtension
                val outdir = File("out/createdb/$category/$name")
                outdir.exists() && outdir.deleteRecursively()
                outdir.mkdirs()
                val lines = Performance.calc("Labels#packOriginalImage") {
                    Labels.packOriginalImages(labels)
                }
                Performance.calc("write images") {
                    Files.copy(file.toPath(), File(outdir, "original.${file.extension}").toPath())
                    Imgcodecs.imwrite("$outdir/lines.png", lines)
                    Imgcodecs.imwrite("$outdir/edge.png", labeler.magnitude)
                    Imgcodecs.imwrite("$outdir/thumbnail.png", src)
                }
                // id, category, name, width, height, thumb_width, thumb_height
                images_writer.write("$image_id,$category,${file.name},${org.width()},${org.height()},${src.width()},${src.height()}\n")
                val label2line_id = HashMap<Int, Int>()
                Performance.calc("write cohl and minhash") {
                    for (label in labels) {
                        // Insert Line
                        lines_writer.write(label.toCSV(line_id, image_id) + "\n")
                        // Insert MinHashes
                        for (mh in label.minHashes) {
                            min_hashes_writer.write("'$line_id',${mh.toCSV()}\n")
                        }
                        label2line_id.put(label.index, line_id)
                        ++line_id
                    }
                }
                Performance.calc("clac & write nearlines") {
                    // 近いラベル
                    for (i in labels) {
                        val dists = ArrayList<Pair<Int, Double>>()
                        for (j in labels) {
                            if (i == j) continue
                            dists.add(Pair(j.index, i.distTo(j)))
                        }
                        dists.sortBy { d -> d.second }
                        for (di in 0..9) {
                            if (dists.size <= di) break
                            val lid = label2line_id[i.index]
                            val tgtid = label2line_id[dists[di].first]
                            val dist = dists[di].second
                            near_lines_writer.write("'$lid','$tgtid','$dist'\n")
                        }
                    }
                }
                println("done.")
                ++image_id
            }
        } finally {
            lines_writer.close()
            images_writer.close()
            min_hashes_writer.close()
            near_lines_writer.close()
        }
        print("start insert to db...")
        FileWriter("db/insert.sql").use { sql_writer ->
            sql_writer.write("load data infile '${imagesFile.absolutePath}' into table Images fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;\n")
            sql_writer.write("load data infile '${cohLinesFile.absolutePath}' into table CoherentLines fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;\n")
            sql_writer.write("load data infile '${minHashesFile.absolutePath}' into table MinHashes fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;\n")
            sql_writer.write("load data infile '${nearLinesFile.absolutePath}' into table NearLines fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;\n")
        }
        Performance.calc("insert to DB") {
            connection.createStatement().executeUpdate("load data infile '${imagesFile.absolutePath}' into table Images fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;")
            connection.createStatement().executeUpdate("load data infile '${cohLinesFile.absolutePath}' into table CoherentLines fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;")
            connection.createStatement().executeUpdate("load data infile '${minHashesFile.absolutePath}' into table MinHashes fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;")
            connection.createStatement().executeUpdate("load data infile '${nearLinesFile.absolutePath}' into table NearLines fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;")
        }
        println("done.")
        Performance.flush(Performance.TimeUnit.Seconds)
    }
}