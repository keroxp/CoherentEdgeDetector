package ced.label

import com.google.common.hash.Hashing
import java.nio.charset.Charset

data class Sketch (
        val hashFuncIndex: Int,
        val sketches: List<Int>) {
    val sketchString: String
    val sketchHash: Int
    init {
        sketchString = sketches.joinToString(",")
        sketchHash = Hashing.crc32c().hashString(sketchString, Charset.forName("UTF-8")).asInt()
    }
    fun toCSV(): String {
        return listOf(hashFuncIndex,sketchString,sketchHash).map { v -> "'$v'" }.joinToString(",")
    }
}