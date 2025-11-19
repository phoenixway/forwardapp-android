class UserManager(private val db: String) {
    fun getAll(): List<String> = emptyList()
}

// Стара версія, яку ми хочемо оновити
class UserManager {
    fun oldMethod() {}
}
