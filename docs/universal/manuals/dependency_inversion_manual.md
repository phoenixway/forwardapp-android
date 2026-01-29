# Як вирішити циклічні залежності за допомогою інверсії залежностей (Dependency Inversion)

## Проблема: Циклічна залежність

При розробці багатомодульного застосунку (наприклад, на Kotlin Multiplatform) ви можете зіткнутися з проблемою циклічних залежностей. Це трапляється, коли два або більше модулів посилаються один на одного прямо чи опосередковано.

**Класичний приклад:**

Уявімо, що ви виділяєте логіку синхронізації в окремий модуль `:sync`. Цей модуль містить `SyncRepository`, який відповідає за обмін даними з сервером.

Для збереження даних на пристрої `SyncRepository` потребує доступу до бази даних. База даних (`AppDatabase`) та її DAO (Data Access Objects) зазвичай визначені в головному модулі `:app`.

Виходить така картина:

1.  Модуль `:sync` містить `SyncRepository`.
2.  `SyncRepository` для своєї роботи хоче використовувати `AttachmentDao` з `AppDatabase`.
3.  `AppDatabase` знаходиться в модулі `:app`.
4.  Отже, `:sync` змушений залежати від `:app` (`:sync` -> `:app`).

Але, як правило, головний модуль `:app` сам хоче використовувати логіку з `:sync` для запуску синхронізації.

5.  Модуль `:app` хоче викликати методи з `SyncRepository`.
6.  Отже, `:app` змушений залежати від `:sync` (`:app` -> `:sync`).

Виникає **циклічна залежність**: `:app` -> `:sync` -> `:app`. Системи збірки (наприклад, Gradle) не дозволяють таких циклів, і ви отримаєте помилку.

## Рішення: Принцип інверсії залежностей (Dependency Inversion Principle)

Цей принцип з SOLID говорить:
> Модулі вищого рівня не повинні залежати від модулів нижчого рівня. Обидва повинні залежати від абстракцій.
> Абстракції не повинні залежати від деталей. Деталі повинні залежати від абстракцій.

Простими словами: замість того, щоб один конкретний клас залежав від іншого конкретного класу, обидва мають залежати від спільного "контракту" — **інтерфейсу**.

### План дій

Ми "інвертуємо" напрямок залежності. Замість того, щоб `:sync` знав про `:app`, ми змусимо обидва модулі залежати від нового, спільного модуля, який міститиме лише інтерфейси.

Назвемо його `:core-data-interfaces`.

**Крок 1: Створення "контракту" (інтерфейсу) у спільному модулі**

У модулі `:core-data-interfaces` ми створюємо інтерфейс, який описує **тільки ті методи**, які потрібні `SyncRepository` для роботи з локальними даними. `SyncRepository` не потрібно знати про `AppDatabase` або Room, йому потрібен лише спосіб зберегти та отримати дані.

`core-data-interfaces/src/main/java/com/forwardapp/core/data/datasource/AttachmentLocalDataSource.kt`
```kotlin
package com.forwardapp.core.data.datasource

import com.forwardapp.core.data.models.Attachment // Припустимо, модель також у спільному модулі

interface AttachmentLocalDataSource {
    suspend fun getAttachmentById(id: String): Attachment?
    suspend fun getAllAttachments(): List<Attachment>
    suspend fun insertAttachments(attachments: List<Attachment>)
    // ... інші необхідні методи
}
```

**Крок 2: Оновлення репозиторію (`:sync`)**

Тепер `SyncRepository` в модулі `:sync` буде залежати не від конкретної `AppDatabase`, а від нашого нового інтерфейсу.

`sync/src/main/java/com/forwardapp/sync/SyncRepository.kt`
```kotlin
import com.forwardapp.core.data.datasource.AttachmentLocalDataSource // Імпорт інтерфейсу

class SyncRepository(
    private val remoteApi: RemoteApi,
    private val attachmentLocalDataSource: AttachmentLocalDataSource // <-- Залежність від інтерфейсу!
) {
    suspend fun syncAttachments() {
        val localAttachments = attachmentLocalDataSource.getAllAttachments()
        // ... логіка синхронізації
    }
}
```

Тепер модуль `:sync` залежить тільки від `:core-data-interfaces`. Зв'язок з `:app` розірвано.

**Крок 3: Реалізація інтерфейсу в модулі `:app`**

Модуль `:app` знає про `AppDatabase`. Саме тут ми створюємо конкретну реалізацію нашого "контракту".

`app/src/main/java/com/forwardapp/data/AttachmentLocalDataSourceImpl.kt`
```kotlin
import com.forwardapp.core.data.datasource.AttachmentLocalDataSource
import com.forwardapp.data.database.AppDatabase // Конкретна БД з :app

class AttachmentLocalDataSourceImpl(
    private val appDatabase: AppDatabase
) : AttachmentLocalDataSource {

    private val attachmentDao = appDatabase.attachmentDao()

    override suspend fun getAttachmentById(id: String): Attachment? {
        return attachmentDao.getById(id) // Виклик конкретного DAO
    }

    override suspend fun getAllAttachments(): List<Attachment> {
        return attachmentDao.getAll()
    }

    override suspend fun insertAttachments(attachments: List<Attachment>) {
        attachmentDao.insertAll(attachments)
    }
}
```
**Крок 4: Налаштування Dependency Injection (Hilt/Koin)**

Залишилось останнє — пояснити вашому фреймворку для впровадження залежностей (наприклад, Hilt), що коли хтось (як `SyncRepository`) просить абстрактний `AttachmentLocalDataSource`, потрібно надати конкретну реалізацію `AttachmentLocalDataSourceImpl`.

Це налаштування робиться в модулі `:app`, оскільки тільки він знає про `AttachmentLocalDataSourceImpl`.

`app/src/main/java/com/forwardapp/di/DataModule.kt`
```kotlin
import com.forwardapp.core.data.datasource.AttachmentLocalDataSource
import com.forwardapp.data.AttachmentLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindAttachmentLocalDataSource(
        impl: AttachmentLocalDataSourceImpl
    ): AttachmentLocalDataSource
}
```
## Результат

Тепер ваші залежності виглядають так:

-   `:app` -> `:sync` (для виклику логіки синхронізації)
-   `:app` -> `:core-data-interfaces` (для реалізації інтерфейсу)
-   `:sync` -> `:core-data-interfaces` (для використання інтерфейсу)

**Циклічна залежність розірвана!**

`SyncRepository` більше не прив'язаний до деталей реалізації бази даних, а модуль `:app` надає цю реалізацію, задовольняючи "контракт". Це робить архітектуру чистою, гнучкою та легкою для тестування.
