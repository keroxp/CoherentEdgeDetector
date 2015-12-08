package ced.util

import java.util.*

object Performance {
    val results = TreeMap<String, Long>()
    var total: Long = 0
    fun <T> calc(tag: String, func: () -> T): T {
        val start = System.nanoTime()
        val ret = func()
        val end = System.nanoTime()
        val diff = end - start
        results[tag] = if (results.containsKey(tag)) {
            results[tag]!! + diff
        } else {
            diff
        }
        total += diff
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
        total = 0
        results.clear()
    }
    fun flush(unit: TimeUnit = TimeUnit.MilliSeconds) {
        println("total time:\t${unit.format(total)}")
        results.entries.sortedByDescending { e ->
            e.value
        }.forEach { e ->
            val perc = "${100.0*e.value/total}".substring(0,4)
            println("${e.key}:\t${unit.format(e.value)}\t($perc%)")
        }
    }
}