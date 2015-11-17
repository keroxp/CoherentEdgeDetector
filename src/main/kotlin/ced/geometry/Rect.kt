package ced.geometry

data class Rect (
        public var left: Int = 0,
        public var top: Int = 0,
        public var width: Int = 0,
        public var height: Int = 0
) {
    public val right: Int
        get () {
            return left + width
        }
    public val bottom: Int
        get () {
            return top + height
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