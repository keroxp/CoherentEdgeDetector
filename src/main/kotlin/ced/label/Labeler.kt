package ced.label

import ced.geometry.Direction
import ced.geometry.Point
import ced.geometry.Rect
import ced.iterate
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import java.util.*

class Labeler(val magnitude: Mat, val orientation: Mat) {
    private val LABEL_NOT_LABELED = -1.0
    public val CONNECTIVITY_4 = 4
    public val CONNECTIVITY_8 = 8
    var labelsIndex: Int = -1
    var labelsMap: Mat = Mat(magnitude.size(), CvType.CV_32S, Scalar(LABEL_NOT_LABELED))

    private fun getDirection(x: Int, y: Int): Direction {
        val o = orientation.get(y,x)[0]
        return if ((Math.PI*2-Math.PI/4 < o && o <= Math.PI*2) || (0 <= o && o <= Math.PI/4)) {
            Direction.Right
        } else if (Math.PI/4 < o && o <= Math.PI-Math.PI/4) {
            Direction.Top
        } else if (Math.PI-Math.PI/4 < o && o <= Math.PI+Math.PI/4) {
            Direction.Left
        } else if (Math.PI+Math.PI/4 < o && o <= Math.PI*2-Math.PI/4) {
            Direction.Bottom
        } else {
            throw Exception("unexpected orientation: $o")
        }
    }
    private fun labelIfPossible(x: Int, y: Int, label: Int, direction: Direction): Boolean {
        if (0 <= x && 0 <= y && x < magnitude.width() && y < magnitude.height() && labelsMap.get(y,x)[0] != LABEL_NOT_LABELED && magnitude.get(y,x)[0] != 0.0) {
            if (getDirection(x,y) == direction) {
                labelsMap.put(y, x, intArrayOf(label))
                return true
            }
        }
        return false
    }
    private fun pushIfPossible(p: Point, dx: Int, dy: Int, stack: Stack<PixelNeighbour>): Boolean {
        if (0 <= p.x && 0 <= p.y && p.x < magnitude.width() && p.y < magnitude.height() && magnitude.get(p.y,p.x)[0] != 0.0) {
            stack.push(PixelNeighbour(p, dx, dy))
            return true
        }
        return false
    }
    public fun doLabeling(connectivity: Int = CONNECTIVITY_4): Set<Label> {
        val stack = Stack<PixelNeighbour>()
        val labels = HashSet<Label>()
        // 1. ラベリング処理
        magnitude.iterate { y, x ->
            if (pushIfPossible(Point(x, y),0,0,stack)) {
                val bounds = Rect(x, y)
                val d = getDirection(x,y)
                val pixels = HashSet<Point>()
                val boundaries = HashSet<PixelNeighbour>()
                pixels.add(Point(x, y))
                ++labelsIndex
                while (!stack.isEmpty()) {
                    val n = stack.pop()
                    val p = n.pivot
                    if (labelIfPossible(n.x,n.y,labelsIndex,d)) {
                        bounds.extend(n.x,n.y)
                        pushIfPossible(p,0,-1,stack)
                        pushIfPossible(p,0,1,stack)
                        pushIfPossible(p,-1,0,stack)
                        pushIfPossible(p,1,0,stack)
                        if (connectivity == CONNECTIVITY_8) {
                            pushIfPossible(p,-1,-1,stack)
                            pushIfPossible(p,1,-1,stack)
                            pushIfPossible(p,-1,1,stack)
                            pushIfPossible(p,1,1,stack)
                        }
                    } else if (magnitude.get(n.y,n.x)[0] != 0.0) {
                        boundaries.add(n)
                    }
                }
                labels.add(Label(labelsIndex, pixels, boundaries, bounds, d))
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