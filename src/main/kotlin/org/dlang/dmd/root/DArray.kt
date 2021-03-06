package org.dlang.dmd.root

class DArray<T : RootObject>(storage: Array<T?>, len: Int) {
    var data: Array<T?> = storage
    var length: Int = len

    constructor(arr: Array<T?>): this(arr, arr.size)

    fun toChars() = asString().ptr()

    fun asString(): ByteSlice {
        val accum = mutableListOf<Byte>()
        accum.add('['.toByte())
        for (i in 0 until length) {
            if (i != 0 ) {
                accum.add(','.toByte())
                accum.add(' '.toByte())
            }
            val src = if (data[i] != null)
                data[i]!!.asString().data
            else
                "null".toByteArray()
            for (v in src) accum.add(v)
        }
        accum.add(']'.toByte())
        return ByteSlice(accum.toByteArray())
    }

    override fun toString(): String {
        val slice = asString()
        return String(slice.data, slice.beg, slice.length)
    }

    operator fun get(i: Int): T? {
        assert(i < length)
        return data[i]
    }

    operator fun set(i: Int, v: T?) {
        assert(i < length)
        data[i] = v
    }

    val dim: Int
        get() = length

    fun setDim(size: Int) {
        if (size > data.size) reserve(size - data.size)
        length = size
    }

    fun pushSlice(a: Slice<T?>): DArray<T> {
        val oldLength = length
        setDim(oldLength + a.length)
        a.data.copyInto(data, oldLength, a.beg, a.length)
        return this
    }

    fun append(a: DArray<T>): DArray<T> {
        insert(length, a)
        return this
    }

    fun reserve(more: Int) {
        if (data.isEmpty()) data = data.copyOf(more)
        else if (data.size+more > data.size) {
            data = data.copyOf((data.size + more) * 3 / 2)
        }
    }

    fun push(item: T): DArray<T> {
        reserve(1)
        data[length++] = item
        return this
    }

    fun shift(item: T) {
        val before = length++
        data.copyOf(before+1)
        data.copyInto(data, 1, 0,  before)
        data[0] = item
    }

    fun remove(i: Int) {
        data.copyInto(data, i, i + 1, length)
        length--
    }

    fun insert(i: Int, arr: DArray<T>?) {
        if (arr != null) {
            val d = arr.length
            reserve(d)
            if (length != i)
                data.copyInto(data, i + d, i, length)
            arr.data.copyInto(data, i, 0, d + length)
            length += d
        }
    }

    fun insert(i: Int, ptr: T?) {
        reserve(1)
        data.copyInto(data, i + 1, i, length)
        data[i] = ptr
        length++
    }

    fun zero() = data.fill(null, 0, length)

    fun pop(): T? = data[--length]

    fun slice() =  Slice(data, 0, length)

    fun slice(a: Int, b: Int): Slice<T?> {
        assert(b in a..length)
        return Slice(data, a, b)
    }

    override fun equals(other: Any?): Boolean =
        when(other) {
            is DArray<*> -> this.slice() == other.slice()
            else -> false
        }

    override fun hashCode(): Int {
        return data.hashCode() + 31*length
    }
}

class BitArray {
    private var array: IntArray = IntArray(4)
    private var len = 0

    var length: Int
        get() = len
        set(n: Int) {
            array = array.copyOf(n / 32 + if (n % 32 > 0) 1 else 0)
            len = n
        }


    operator fun set(idx: Int, b: Boolean) {
        assert(idx < length)
        if (b)
            array[idx / 32] = array[idx / 32].or(1.shl(idx % 32))
        else
            array[idx / 32] = array[idx / 32].and(1.shl(idx % 32).inv())
    }

    operator fun get(idx: Int): Boolean =
        array[idx / 32].and(1.shl(idx % 32)) != 0
}


inline fun<reified T : RootObject> darray(size: Int = 16): DArray<T> {
    return DArray(Array<T?>(size) { null }, 0)
}

inline fun <reified T: RootObject> darrayOf(vararg elements: T?): DArray<T> {
    val array = Array<T?>(elements.size){ null }
    elements.copyInto(array)
    return DArray(array)
}

inline fun<reified T: RootObject> peekSlice(array: DArray<T>?): Slice<T?>? =
    array?.slice()

/**
 * Reverse an array in-place.
 * Params:
 *      a = array
 * Returns:
 *      reversed a[]
 */
fun<T> reverse(a: Slice<T>):Slice<T> {
    if (a.length > 1)
    {
        val mid = (a.length + 1).shr(1);
        for (i in 0 until mid)
        {
            val e = a[i]
            a[i] = a[a.length - 1 - i]
            a[a.length - 1 - i] = e
        }
    }
    return a
}

/**
 * Splits the array at $(D index) and expands it to make room for $(D length)
 * elements by shifting everything past $(D index) to the right.
 * Params:
 *  array = the array to split.
 *  index = the index to split the array from.
 *  length = the number of elements to make room for starting at $(D index).
 */
fun<T: RootObject> split(array: DArray<T>, index: Int, length: Int) {
    if (length > 0)
    {
        val previousDim = array.length
        array.setDim(array.length + length)
        array.data.copyInto(array.data, index+length, index, previousDim)
    }
}
