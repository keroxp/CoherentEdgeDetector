package ced.label

import ced.geometry.Direction
import ced.geometry.Point
import ced.geometry.Rect
import ced.iterate
import ced.mapInt
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import java.util.*

class Labeler(val magnitude: Mat, val orientation: Mat) {
    private val LABEL_NOT_LABELED = -1.0
    var labelsIndex: Int = 0
    var labelsMap: Mat = Mat(magnitude.size(), CvType.CV_32S, Scalar(LABEL_NOT_LABELED))
    val direction: Mat
    val DIRECTION_RIGHT = 0
    val DIRECTION_TOP = 1
    val DIRECTION_LEFT = 2
    val DIRECTION_BOTTOM = 3
    init {
        direction = orientation.mapInt { y, x, mat ->
            val o = orientation.get(y,x)[0]
            if ((Math.PI*2-Math.PI/4 < o && o <= Math.PI*2) || (0 <= o && o <= Math.PI/4)) {
                DIRECTION_RIGHT
            } else if (Math.PI/4 < o && o <= Math.PI-Math.PI/4) {
                DIRECTION_TOP
            } else if (Math.PI-Math.PI/4 < o && o <= Math.PI+Math.PI/4) {
                DIRECTION_LEFT
            } else if (Math.PI+Math.PI/4 < o && o <= Math.PI*2-Math.PI/4) {
                DIRECTION_BOTTOM
            } else {
                throw Exception("unexpected orientation: $o")
            }
        }
    }
    private fun getDirection(x: Int, y: Int): Int {
        return direction.get(y,x)[0].toInt()
    }
    private fun isValidRange(x: Int, y: Int): Boolean {
        return 0 <= x && 0 <= y && x < magnitude.width() && y < magnitude.height()
    }
    private fun labelIfPossible(x: Int, y: Int, label: Int, direction: Int): Boolean {
        if (getDirection(x,y) == direction) {
            assert(isValidRange(x,y))
            assert(magnitude.get(y,x)[0] != 0.0)
            labelsMap.put(y, x, intArrayOf(label))
            return true
        }
        return false
    }
    private fun pushIfPossible(p: Point, dx: Int, dy: Int, stack: Stack<PixelNeighbour>): Boolean {
        val x = p.x+dx
        val y = p.y+dy
        if (isValidRange(x,y) && magnitude.get(y,x)[0] != 0.0 && labelsMap.get(y,x)[0] == LABEL_NOT_LABELED) {
            stack.push(PixelNeighbour(p, dx, dy))
            return true
        }
        return false
    }
    public fun doLabeling(minArea: Int = 0, minLength: Int = 0, minCoherency:Double = 0.0): Set<Label> {
        val stack = Stack<PixelNeighbour>()
        val labels = HashSet<Label>()
        // 1. ラベリング処理
        magnitude.iterate { y, x ->
            if (pushIfPossible(Point(x,y),0,0,stack)) {
                val bounds = Rect(x,y)
                val d = getDirection(x,y)
                val pixels = HashSet<Point>()
                val boundaries = HashSet<PixelNeighbour>()
                pixels.add(Point(x,y))
                var labeled = false
                while (!stack.isEmpty()) {
                    val n = stack.pop()
                    if (labelIfPossible(n.x,n.y,labelsIndex,d)) {
                        labeled = true
                        bounds.extend(n.x,n.y)
                        pixels.add(Point(n.x,n.y))
                        for (_y in -1..1) {
                            for (_x in -1..1) {
                                if (_y == 0 && _x == 0) {
                                    continue
                                }
                                pushIfPossible(Point(n.x,n.y),_x,_y,stack)
                            }
                        }
                    } else if (magnitude.get(n.y,n.x)[0] != 0.0) {
                        boundaries.add(n)
                    }
                }
                if (labeled) ++labelsIndex
                val squareness = Math.min(bounds.width,bounds.height)/Math.max(bounds.width,bounds.height).toDouble()
                val thinness = pixels.size/((bounds.width+1)*(bounds.height+1)).toDouble()
                val coherency = 1-squareness*thinness
                if (pixels.size >= minArea && Math.max(bounds.width,bounds.height) >= minLength && coherency >= minCoherency) {
                    assert(pixels.size >= minArea)
                    assert(Math.max(bounds.width,bounds.height) >= minLength)
                    labels.add(Label(labelsIndex, pixels, boundaries, bounds, d, coherency))
                }
            }
        }
        // 2. ラベルの関連性
        for (l in labels) {
            for (b in l.boundaries) {
                val i = labelsMap.get(b.y,b.x)[0].toInt()
                l.neighbours.add(i)
                l.labelRelations.add(LabelRelation(i, b))
            }
        }
        return labels.toSet()
    }
}