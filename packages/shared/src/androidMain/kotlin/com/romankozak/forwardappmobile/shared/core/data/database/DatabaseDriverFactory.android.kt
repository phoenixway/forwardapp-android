package com.romankozak.forwardappmobile.shared.core.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

private const val DB_NAME = "forwardapp.db"

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        // ✅ Завантажуємо SQLCipher
        SQLiteDatabase.loadLibs(context)
        
        // ✅ Порожній пароль = без шифрування
        val passphrase = SQLiteDatabase.getBytes(CharArray(0))
        val factory = SupportFactory(passphrase)
        
        return AndroidSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = context,
            name = DB_NAME,
            factory = factory,
            callback = object : AndroidSqliteDriver.Callback(ForwardAppDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    
                    // ✅ Перевірка FTS5 БЕЗ типу колонки
                    try {
                        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS fts_test USING fts5(content)")
                        db.execSQL("DROP TABLE IF EXISTS fts_test")
                        android.util.Log.i("SQLDELIGHT_FTS5", "✅ FTS5 is supported!")
                    } catch (t: Throwable) {
                        android.util.Log.e("SQLDELIGHT_FTS5", "❌ FTS5 NOT supported", t)
                    }
                }
            }
        )
    }
}
