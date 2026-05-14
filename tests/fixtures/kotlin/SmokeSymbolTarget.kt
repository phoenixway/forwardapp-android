package smoke

class SmokeSymbolTarget {
    fun marker(): String = "replaced"
}

object SmokeObjectTarget {
    const val label: String = "before"
}

interface SmokeInterfaceTarget {
    val label: String
}
