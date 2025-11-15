import android.content.Context
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.androidx.AndroidXSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase

actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val ctx = platformContext ?: error("Android Context required")

        return AndroidXSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = ctx,
            name = "ForwardAppDatabase.db",
            factory = FrameworkSQLiteOpenHelperFactory()
        )
    }
}
