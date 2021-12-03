/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.utils.property


internal open class KotlinJsIrConfiguration(
    override val taskName: String,
    compilation: KotlinJsIrCompilation
) : Kotlin2JsCompileConfiguration(compilation) {

    val entryModule: DirectoryProperty = objectFactory.directoryProperty().fileProvider(
        compilation.output.classesDirs.elements.map { it.single().asFile }
    ).also {
        it.disallowChanges()
    }

    val mode: Property<KotlinJsBinaryMode> = objectFactory.property()

    fun configureTask(task: KotlinJsIrLink) {
        KotlinJsIrCompileConfigurator(this).configure(task)
    }
}

internal open class KotlinJsIrCompileConfigurator(config: KotlinJsIrConfiguration) :
    Kotlin2JsCompileConfigurator<KotlinJsIrLink, KotlinJsIrConfiguration>(config) {

    override fun configure(task: KotlinJsIrLink) {
        super.configure(task)

        task.entryModule.set(config.entryModule)
        task.compilation = config.kotlinCompilation
        task.modeProperty.value(config.mode).disallowChanges()
        task.destinationDirectory.fileProvider(task.outputFileProperty.map { it.parentFile }).disallowChanges()
    }
}
