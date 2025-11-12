# DatabaseDriverFactory Debug Information

This document consolidates the content of all `DatabaseDriverFactory` files and the latest compilation errors for debugging the `expect`/`actual` mechanism.

--- FILE: shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.common.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

// 🔹 оголошення "порожнього" типу, який кожна платформа реалізує по-своєму
expect class PlatformContext

// 🔹 дефолтний аргумент вказується тільки тут
expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}
```

--- FILE: shared/src/jvmMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.jvm.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

// 🔹 JVM реалізація: контекст не потрібен
actual class PlatformContext

actual class DatabaseDriverFactory actual constructor(
    platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        return driver
    }
}
```

--- FILE: shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.android.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

// 🔹 Android реалізація: просто alias на Context
actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val ctx = platformContext ?: error("Android Context required")
        return AndroidSqliteDriver(ForwardAppDatabase.Schema, ctx, "ForwardAppDatabase.db")
    }
}
```

--- FILE: shared/src/jvmTest/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.jvmTest.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual interface PlatformContext

actual class DatabaseDriverFactory actual constructor(
    platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        return driver
    }
}
```

--- FILE: shared/src/androidUnitTest/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.androidUnitTest.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: Context
) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ForwardAppDatabase.Schema, platformContext, "test.db")
}
```

--- LATEST COMPILATION ERRORS ---
```
> Task :shared:compileKotlinJvm                                                                                                     
w: ⚠️ Deprecated Legacy Compilation Outputs Backup                                                                                   
Backups of compilation outputs using the non-precise method are deprecated and will be phased out soon in favor of a more precise an
d efficient approach (https://kotl.in/3v7v7).                                                                                       
Please remove 'kotlin.compiler.preciseCompilationResultsBackup=false' and/or 'kotlin.compiler.keepIncrementalCompilationCachesInMemo
ry=false' from your 'gradle.properties' file.                                                                                       
                                                                                                                                    
                                                                                                                                    
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmo
bile/shared/database/DatabaseDriverFactory.common.kt:5:1 Modifier 'expect' is not applicable to 'typealias'.                        
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/jvmMain/kotlin/com/romankozak/forwardappmobil
e/shared/database/DatabaseDriverFactory.jvm.kt:7:18 'actual typealias PlatformContext = Any' has no corresponding expected declarati
on                                                                                                                                  
                                                                                                                                    
> Task :shared:compileKotlinJvm FAILED                                                                                              
                                                                                                                                    
FAILURE: Build failed with an exception.                                                                                            
                                                                                                                                    
* What went wrong:                                                                                                                  
Execution failed for task ':shared:compileKotlinJvm'.                                                                               
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAct
ion                                                                                                                                 
   > Compilation error. See log for more details                                                                                    
                                                                                                                                    
* Try:                                                                                                                              
> Run with --stacktrace option to get the stack trace.                                                                              
> Run with --info or --debug option to get more log output.                                                                         
> Get more help at https://help.gradle.org.                                                                                         
                                                                                                                                    
BUILD FAILED in 15s                                                                                                                 
55 actionable tasks: 22 executed, 24 from cache, 9 up-to-date
```