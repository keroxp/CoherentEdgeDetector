package ced.label

import ced.util.Mats
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import kotlin.test.assertTrue

object Labels {
    public fun packOriginalImages(labels: Collection<Label>): Mat {
        // できるだけ正方形のような形にする
        val mw = Math.sqrt(labels.sumBy { l -> l.area }.toDouble())
        return packOriginalImages(labels,mw.toInt())
    }
    public fun packOriginalImages(labels: Collection<Label>, maxWidth: Int): Mat {
        val _labels = labels.sortedByDescending { l -> l.length }
        val fillValue = Scalar(Label.LABEL_NO)
        var w = 0
        var h = 0
        var ret: Mat = Mat()
        var buf: Mat = Mat()
        for (l in _labels) {
            if (w >= maxWidth) {
                ret = if (ret.height() == 0) {
                    h += buf.height()
                    buf.clone()
                } else {
                    val ap = if (ret.width() < buf.width()) {
                        ret = Mats.extend(ret,maxWidth = buf.width(),fillValue = fillValue)
                        buf
                    } else {
                        Mats.extend(buf,maxWidth = ret.width(),fillValue = fillValue)
                    }
                    h += ap.height()
                    Mats.appendBottom(ret, ap)
                }

                w = 0
                buf = Mat()
            }
            buf = if (buf.width() == 0) {
                l.original.clone()
            } else {
                val ap = if (buf.height() < l.original.height()) {
                    buf = Mats.extend(buf,maxHeight = l.original.height(), fillValue = fillValue)
                    l.original
                } else {
                    Mats.extend(l.original,maxHeight = buf.height(), fillValue = fillValue)
                }
                Mats.appendRight(buf,ap)
            }
            l.tiledStartX = w
            l.tiledStartY = h
            w += l.original.width()
        }
        if (buf.width() > 0) {
            val ap = if (ret.width() < buf.width()) {
                ret = Mats.extend(ret,maxWidth = buf.width(),fillValue = fillValue)
                buf
            } else {
                Mats.extend(buf,maxWidth = ret.width(),fillValue = fillValue)
            }
            ret = Mats.appendBottom(ret,ap)
            h += ap.height()
        }
        assertTrue(ret.height() == h)
        assertTrue(ret.width() > 0)
        assertTrue(ret.height() > 0)
        for (l in labels) {
            assertTrue(l.tiledStartX > -1)
            assertTrue(l.tiledStartY > -1)
            assertTrue(l.tiledStartX+l.original.width() <= ret.width())
            assertTrue(l.tiledStartY+l.original.height() <= ret.height())
        }
        return ret
    }
}