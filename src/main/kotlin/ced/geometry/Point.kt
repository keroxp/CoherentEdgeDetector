package ced.geometry

data class Point (
        public var x: Int = 0,
        public var y: Int = 0
) {
    public fun distanceTo(p: Point): Double {
        return Math.sqrt(powDistanceTo(p))
    }
    public fun powDistanceTo(p: Point): Double {
        val dx = (p.x-x).toDouble()
        val dy = (p.y-y).toDouble()
        return dx*dx + dy*dy
    }
}
