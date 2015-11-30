package ced.label

import ced.util.Mats
import org.opencv.core.Mat
import org.opencv.core.Scalar

object Labels {
    public fun packOriginalImages(labels: Collection<Label>, maxWidth: Int): Mat {
        val _labels = labels.sortedByDescending { l -> l.boundsArea }
        var i = 0
        var ret: Mat? = null
        val fillValue = Scalar(Label.LABEL_NO)
        while (i < _labels.size) {
            var mat = _labels[i].original
            var w = mat.width()
            var buf: Mat? = null
            while (w < maxWidth) {
                if (++i == _labels.size) {
                    buf = Mats.extend(buf ?: Mat(), maxWidth = maxWidth, fillValue = fillValue)
                    break
                }
                _labels[i].tiledStartX = buf?.width() ?: 0
                _labels[i].tiledStartY = ret?.height() ?: 0
                if (buf == null) {
                    buf = _labels[i].original.clone()
                } else {
                    val ap = Mats.extend(_labels[i].original, maxHeight = buf.height(), fillValue = fillValue)
                    buf = Mats.appendRight(buf, ap)
                }
                w += mat.width()
            }
            ret = if (ret == null) buf else Mats.appendBottom(ret, Mats.extend(buf!!, maxWidth = ret.width(), fillValue = fillValue))
        }
        return ret!!
    }
}