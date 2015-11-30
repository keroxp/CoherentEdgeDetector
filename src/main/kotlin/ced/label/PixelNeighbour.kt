package ced.label

import ced.geometry.Point

public data class PixelNeighbour(
        val pivot: Point,
        val dx: Int = 0,
        val dy: Int = 0
) {
    public var label: Int = -1
    val x: Int
        get () {
            return pivot.x+dx
        }
    val y: Int
        get ()  {
            return pivot.y+dy
        }
}