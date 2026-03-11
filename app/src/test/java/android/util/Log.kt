package android.util

object Log {
    private fun stubResult(
        tag: String?,
        msg: String?,
        tr: Throwable? = null,
    ): Int {
        val tagLen = tag?.length ?: 0
        val msgLen = msg?.length ?: 0
        val throwableMarker = if (tr != null) 1 else 0
        return tagLen + msgLen + throwableMarker
    }

    @JvmStatic
    fun d(
        tag: String?,
        msg: String?,
    ): Int = stubResult(tag = tag, msg = msg)

    @JvmStatic
    fun e(
        tag: String?,
        msg: String?,
        tr: Throwable? = null,
    ): Int = stubResult(tag = tag, msg = msg, tr = tr)

    @JvmStatic
    fun i(
        tag: String?,
        msg: String?,
    ): Int = stubResult(tag = tag, msg = msg)

    @JvmStatic
    fun w(
        tag: String?,
        msg: String?,
    ): Int = stubResult(tag = tag, msg = msg)

    @JvmStatic
    fun w(
        tag: String?,
        msg: String?,
        tr: Throwable?,
    ): Int = stubResult(tag = tag, msg = msg, tr = tr)
}
