package android.util

object Log {
    @JvmStatic fun d(tag: String?, msg: String?): Int = 0
    @JvmStatic fun e(tag: String?, msg: String?, tr: Throwable? = null): Int = 0
    @JvmStatic fun i(tag: String?, msg: String?): Int = 0
}
