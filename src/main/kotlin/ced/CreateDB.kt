package ced

import ced.detector.CoherentEdgeDetector2
import ced.label.Labeler
import ced.label.Labels
import ced.util.Mats
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import java.io.FileWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
object CreateDB {
    fun getCount(connection: Connection, colName: String): Int {
        val rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM $colName;")
        rs.next()
        return rs.getInt("COUNT(*)")
    }
    fun exec(images: List<File>, category: String) {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        val connection = DriverManager.getConnection("jdbc:mysql://localhost/coherent_line_suggestion?user=root&password=password");
        // idを自前で振るためにはじめに数えておく
        val imageCount = getCount(connection, "Images")
        val lineCount = getCount(connection, "CoherentLines")
        var image_id = imageCount
        var line_id = lineCount
        val cohLinesFile = File("db/coherent_lines.csv")
        val imagesFile = File("db/images.csv")
        val minHashesFile = File("db/min_hashes.csv")
        val nearLinesFile = File("db/near_lines.csv")
        FileWriter("db/insert.sql").use { sql_writer ->
            sql_writer.write("load data infile '${imagesFile.absolutePath}' into table Images fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;\n")
            sql_writer.write("load data infile '${cohLinesFile.absolutePath}' into table CoherentLines fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;\n")
            sql_writer.write("load data infile '${minHashesFile.absolutePath}' into table MinHashes fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;\n")
            sql_writer.write("load data infile '${nearLinesFile.absolutePath}' into table NearLines fields terminated by ',' enclosed by '\\'' lines terminated by '\n' ignore 1 lines;\n")
        }
        FileWriter(cohLinesFile).use { lines_writer ->
            FileWriter(imagesFile).use { images_writer ->
                FileWriter(minHashesFile).use { min_hashes_writer ->
                    FileWriter(nearLinesFile).use { near_lines_write ->
                        val size = 256.0
                        images_writer.write("id,category,name,width,height\n")
                        lines_writer.write("id,image_id,label,sx,sy,width,height,area,length,coherency,direction,tiled_sx,tiled_sy\n")
                        min_hashes_writer.write("line_id, hash_index, value\n")
                        near_lines_write.write("line_id,target_line_id,distance\n")
                        for (file in images) {
                            print("category: $category, file: ${file.name} ..")
                            print("extraction..")
                            val src = Imgcodecs.imread(file.absolutePath).resize(size)
                            val ced2 = CoherentEdgeDetector2(src)
                            val labeler = Labeler(ced2.cohLine, ced2.orientation)
                            val labels = labeler.doLabeling(minLength = 10, minCoherency = 0.75)
                            val outdir = File("tmp/test")
                            !outdir.exists() && outdir.mkdirs()
                            val out = Labels.packOriginalImages(labels,1024)
                            Imgcodecs.imwrite("tmp/test/${file.nameWithoutExtension}.jpg", out)
                            print("done. ")
                            print("insert to db..")
                            // id, category, name, width, height
                            images_writer.write("$image_id,$category,${file.name},${src.width()},${src.height()}\n")
                            val label2line_id = HashMap<Int, Int>()
                            for (label in labels) {
                                // Insert Line
                                lines_writer.write(label.toCSV(line_id, image_id) + "\n")
                                // Insert MinHashes
                                for (mh in label.minHashes) {
                                    min_hashes_writer.write("'$line_id','${mh.key}','${mh.value}'\n")
                                }
                                label2line_id.put(label.index, line_id)
                                ++line_id
                            }
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
                                    near_lines_write.write("'$lid','$tgtid','$dist'\n")
                                }
                            }
                            print("done.\n")
                            ++image_id
                        }
                    }
                }
            }
        }
    }
}