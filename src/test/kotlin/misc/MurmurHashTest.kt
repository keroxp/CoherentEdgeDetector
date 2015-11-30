package misc

import com.google.common.hash.Hashing
import com.google.common.primitives.Ints
import org.junit.Test
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

class MurmurHashTest {
    @Test
    fun hoge() {
        println("endian order -> ${ByteOrder.nativeOrder()}")
        val hash = Hashing.murmur3_32(0).hashInt(0)
        println("hash as Int -> ${hash.asInt()}")
        val hashbb = hash.asBytes()
        print("hash as ByteArray -> ")
        for (i in 0..hashbb.size-1) {
            print("${hashbb[i]} ")
        }
        print("\n")
        val bb = ByteBuffer.allocate(hashbb.size).order(ByteOrder.LITTLE_ENDIAN).put(hashbb)
        print("hash as ByteBuffer -> ")
        for (i in 0..hashbb.size-1) {
            print("${bb[i]} ")
        }
        print("\n")
        print("hash as ByteBuffer(inverse) -> ")
        for (i in 0..hashbb.size-1) {
            val _i = hashbb.size-1-i
            print("${bb[_i]} ")
        }
        print("\n")
        bb.flip()
        print("hash as ByteBuffer(flip) -> ")
        for (i in 0..hashbb.size-1) {
            print("${bb[i]} ")
        }
        print("\n")
        val bbbe = ByteBuffer.allocate(hashbb.size).order(ByteOrder.BIG_ENDIAN).put(hashbb)
        print("hash as ByteBuffer(BIG_ENDIAN) -> ")
        for (i in 0..hashbb.size-1) {
            print("${bbbe[i]} ")
        }
        print("\n")
    }
    @Test
    fun test() {
        FileWriter("../BullBones/tmp/crc32c_java.csv").use { writer ->
            val func = Hashing.crc32c()
            for (i in 0..9) {
                var hash_int = func.hashInt(i).asInt()
                var hash_str = func.hashString("$i-$i", Charset.forName("UTF-8")).asInt()
                writer.write("$i,$hash_int\n")
                writer.write("$i-$i,$hash_str\n")
            }
        }
    }
}