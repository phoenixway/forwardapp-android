package com.romankozak.forwardappmobile.di

import android.app.Application
import com.romankozak.forwardappmobile.features.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.features.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.features.projectscreen.di.ProjectScreenModule
import kotlin.String
import kotlin.reflect.KClass
import me.tatarka.inject.`internal`.LazyMap
import me.tatarka.inject.`internal`.ScopedComponent

public fun KClass<AppComponent>.create(application: Application): AppComponent = InjectAppComponent(application)

public class InjectAppComponent(
  application: Application,
) : AppComponent(application),
    ScopedComponent {
  override val _scoped: LazyMap = LazyMap()

  override val mainScreenViewModel: MainScreenViewModel
    get() = provideMainScreenViewModel(
      projectRepository = _scoped.get("com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository") {
        provideProjectRepository(
          database = _scoped.get("com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase") {
            provideForwardAppDatabase(
              driverFactory = _scoped.get("com.romankozak.forwardappmobile.shared.core.`data`.database.DatabaseDriverFactory") {
                provideDatabaseDriverFactory(
                  application = application
                )
              }
            )
          },
          ioDispatcher = provideIoDispatcher()
        )
      },
      ioDispatcher = provideIoDispatcher()
    )

  override val backlogViewModelFactory: ProjectScreenModule.BacklogViewModelFactory
    get() = object : ProjectScreenModule.BacklogViewModelFactory {
      override fun create(projectId: String?): BacklogViewModel = BacklogViewModel(
        projectRepository = _scoped.get("com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository") {
          this@InjectAppComponent.provideProjectRepository(
            database = _scoped.get("com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase") {
              this@InjectAppComponent.provideForwardAppDatabase(
                driverFactory = _scoped.get("com.romankozak.forwardappmobile.shared.core.`data`.database.DatabaseDriverFactory") {
                  this@InjectAppComponent.provideDatabaseDriverFactory(
                    application = this@InjectAppComponent.application
                  )
                }
              )
            },
            ioDispatcher = this@InjectAppComponent.provideIoDispatcher()
          )
        },
        ioDispatcher = this@InjectAppComponent.provideIoDispatcher(),
        projectId = projectId
      )
    }
}
