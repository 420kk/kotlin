/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.isProduceUnzippedKlib
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.io.File

/**
 * Storing&computing properties that are need to configure [Kotlin2JsCompile] task. This data should be the only thing that's needed
 * to configure the task, and the API exposed should avoid exposing global objects such as project, extensions, other tasks etc.
 */
internal open class Kotlin2JsCompileConfiguration(
    compilation: KotlinJsCompilation
) : AbstractKotlinCompileInfo(compilation) {

    @Deprecated("Avoid using this, instead add new properties to this class.")
    val kotlinCompilation = compilation

    fun configureTask(task: Kotlin2JsCompile) {
        Kotlin2JsCompileConfigurator<Kotlin2JsCompile, Kotlin2JsCompileConfiguration>(this).configure(task)
    }
}

internal open class Kotlin2JsCompileConfigurator<TASK : Kotlin2JsCompile, CONFIG : Kotlin2JsCompileConfiguration>(config: CONFIG) :
    Configurator<TASK, CONFIG>(config) {

    override fun configure(task: TASK) {
        super.configure(task)

        task.outputFileProperty.value(task.project.provider {
            task.kotlinOptions.outputFile?.let(::File)
                ?: task.destinationDirectory.locationOnly.get().asFile.resolve("${config.kotlinCompilation.ownModuleName}.js")
        }).disallowChanges()

        task.optionalOutputFile.fileProvider(task.outputFileProperty.flatMap { outputFile ->
            task.project.provider {
                outputFile.takeUnless { task.kotlinOptions.isProduceUnzippedKlib() }
            }
        }).disallowChanges()

        val libraryCacheService = task.project.rootProject.gradle.sharedServices.registerIfAbsent(
            "${Kotlin2JsCompile.LibraryFilterCachingService::class.java.canonicalName}_${Kotlin2JsCompile.LibraryFilterCachingService::class.java.classLoader.hashCode()}",
            Kotlin2JsCompile.LibraryFilterCachingService::class.java
        ) {}
        task.libraryCache.set(libraryCacheService).also { task.libraryCache.disallowChanges() }
    }
}
