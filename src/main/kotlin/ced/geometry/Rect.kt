package ced.geometry

data class Rect (
        public var left: Int = 0,
        public var top: Int = 0,
        public var width: Int = 0,
        public var height: Int = 0
) {
    enum class Corner {
        LeftTop,
        RightTop,
        RightBottom,
        LeftBottom
    }
    constructor(p: Point) : this(p.x,p.y)
    public val right: Int
        get () {
            return left + width
        }
    public val bottom: Int
        get () {
            return top + height
        }
    public fun extend(p: Point) {
        extend(p.x,p.y)
    }
    public val center: Point get () = Point(left+width*.5.toInt(),top+height*.5.toInt())
    public val leftTop: Point get () = Point(left,top)
    public val leftBottom: Point get () = Point(left,bottom)
    public val rightTop: Point get () = Point(right,top)
    public val rightBottom: Point get () = Point(right,bottom)
    public fun corner(corner: Corner): Point {
        return when (corner) {
            Corner.LeftTop -> leftTop
            Corner.LeftBottom -> leftBottom
            Corner.RightBottom -> rightBottom
            Corner.RightTop -> rightTop
        }
    }
    public fun extend(x: Int, y: Int) {
        if (x < this.left) {
            this.width += this.left-x
            this.left = x
        }else if (this.right < x) {
            this.width += x-this.right
        }
        if (y < this.top) {
            this.height += this.top-y
            this.top = y
        } else if (this.bottom < y) {
            this.height += y-this.bottom
        }
    }
}