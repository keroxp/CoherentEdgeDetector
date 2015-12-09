package ced.util

import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object Performance {
    val results = TreeMap<String, Long>()
    val total = AtomicLong()
    var start = 0L
    val lock = ReentrantLock()
    fun <T> calc(tag: String, func: () -> T): T {
        val s = lock.withLock {System.nanoTime()}
        val ret = func()
        val e = lock.withLock{System.nanoTime()}
        val diff = e - s
        lock.withLock {
            results[tag] = if (results.containsKey(tag)) {
                results[tag]!! + diff
            } else {
                diff
            }
        }
        total.addAndGet(diff)
        return ret
    }
    enum class TimeUnit (val power: Long, val unitstr: String) {
        NanoSeconds(0,"ns"),
        MicroSeconds(1,"mics"),
        MilliSeconds(2,"mils"),
        Seconds(3,"s");
        val p: Double
        init  {
            p = 1/Math.pow(1000.0,power.toDouble())
        }
        fun format(nanotime: Long): String {
            val t = "${(nanotime*p)}".substring(0,4)
            return "$t $unitstr"
        }
    }
    fun init() {
        lock.withLock {
            start = System.nanoTime()
            total.set(0)
            results.clear()
        }
    }
    fun flush(unit: TimeUnit = TimeUnit.MilliSeconds) {
        lock.withLock {
            println("====================")
            println("actual time:\t${unit.format(System.nanoTime()-start)}")
            println("total time:\t${unit.format(total.get())}")
            results.entries.sortedByDescending { e ->
                e.value
            }.forEach { e ->
                val perc = "${100.0 * e.value / total.get()}".substring(0, 4)
                println("${e.key}:\t${unit.format(e.value)}\t($perc%)")
            }
            println("====================")
        }
    }
}