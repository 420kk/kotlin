/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.associateWithTransitiveClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinOptions
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_BUILD_DIR_NAME
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.project.model.LanguageSettings

/**
 * Configuration for the base compile task, [org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile].
 *
 * This contains all data necessary to configure the tasks, and should avoid exposing global state (project, extensions, other tasks)
 * as much as possible.
 */
internal abstract class AbstractKotlinCompileInfo(
    protected val compilation: KotlinCompilationData<*>
) {
    open val taskName: String = compilation.compileKotlinTaskName

    protected val objectFactory: ObjectFactory = compilation.project.objects
    protected val providers: ProviderFactory = compilation.project.providers

    protected val propertiesProvider: PropertiesProvider = PropertiesProvider(compilation.project)

    protected val ext: KotlinTopLevelExtensionConfig = compilation.project.topLevelExtension
    private val languageSettings: LanguageSettings = compilation.languageSettings

    //region Properties for task
    val friendPaths: ConfigurableFileCollection = objectFactory.fileCollection().from(compilation.friendPaths).also {
        it.disallowChanges()
    }

    val friendSourceSets: ListProperty<String> = objectFactory.listProperty(String::class.java).value(providers.provider {
        if (compilation is KotlinCompilation<*>) compilation.associateWithTransitiveClosure.map { it.name }
        else mutableListOf<String>()
    }).also {
        it.disallowChanges()
    }

    val pluginClasspath: ConfigurableFileCollection = objectFactory.fileCollection().also {
        if (compilation is KotlinCompilation<*>) it.from(compilation.project.configurations.getByName(compilation.pluginConfigurationName))
        it.disallowChanges()
    }

    val classpath: ConfigurableFileCollection = objectFactory.fileCollection()

    open val moduleName: Property<String> = objectFactory.property<String>().value(providers.provider { compilation.moduleName }).also {
        it.disallowChanges()
    }

    val sourceSetName: Property<String> =
        objectFactory.property<String>().value(providers.provider { compilation.compilationPurpose }).also {
            it.disallowChanges()
        }

    val multiPlatformEnabled: Property<Boolean> = objectFactory.property<Boolean>().value(providers.provider {
        compilation.project.plugins.any { it is KotlinPlatformPluginBase || it is KotlinMultiplatformPluginWrapper || it is KotlinPm20PluginWrapper }
    }).also {
        it.disallowChanges()
    }

    val taskBuildDirectory: DirectoryProperty
        get() = objectFactory.directoryProperty().value(compilation.project.layout.buildDirectory.dir("$KOTLIN_BUILD_DIR_NAME/${taskName}"))
            .also {
                it.disallowChanges()
            }

    val destinationDir = objectFactory.directoryProperty()
    val incremental: Property<Boolean> = objectFactory.property(false)

    val useModuleDetection: Property<Boolean> = objectFactory.property(false)
    val taskDescription: Property<String> = objectFactory.property()

    val coroutines: Property<Coroutines>
        get() = objectFactory.property(ext.experimental.coroutines ?: propertiesProvider.coroutines ?: Coroutines.DEFAULT).also {
            it.disallowChanges()
        }

    val kotlinDaemonJvmArguments: ListProperty<String>
        get() {
            val result = objectFactory.listProperty(String::class.java)
            propertiesProvider.kotlinDaemonJvmArgs?.let {
                result.value(it.split("\\s+".toRegex()))
            }
            return result
        }

    val compilerExecutionStrategy: Property<KotlinCompilerExecutionStrategy> =
        objectFactory.property<KotlinCompilerExecutionStrategy>().value(propertiesProvider.kotlinCompilerExecutionStrategy).also {
            it.disallowChanges()
        }
    //endregion

    open fun configureTask(task: AbstractKotlinCompile<*>) {
        Configurator<AbstractKotlinCompile<*>, AbstractKotlinCompileInfo>(this).configure(task)

        // TODO - migrate away from project lifecycle-based properties
        languageSettings.let {
            task.project.runOnceAfterEvaluated("apply properties and language settings to ${task.name}") {
                applyLanguageSettingsToKotlinOptions(
                    languageSettings, (task as org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>).kotlinOptions
                )
            }
        }
    }
}

internal open class Configurator<TASK : AbstractKotlinCompile<*>, CONFIG : AbstractKotlinCompileInfo>(val config: CONFIG) {
    open fun configure(task: TASK) {
        task.classpath = config.classpath
        task.friendPaths.from(config.friendPaths)
        task.friendSourceSets.set(config.friendSourceSets)
        task.pluginClasspath.from(config.pluginClasspath)
        task.moduleName.set(config.moduleName)
        task.sourceSetName.set(config.moduleName)
        task.multiPlatformEnabled.value(config.multiPlatformEnabled).disallowChanges()
        task.taskBuildDirectory.value(config.taskBuildDirectory).disallowChanges()
        task.destinationDirectory.set(config.destinationDir)
        task.coroutines.set(config.coroutines)
        task.useModuleDetection.set(config.useModuleDetection)
        task.description = config.taskDescription.get()
        task.incremental = config.incremental.get()
        task.kotlinDaemonJvmArguments.set(config.kotlinDaemonJvmArguments)
        task.compilerExecutionStrategy.set(config.compilerExecutionStrategy)

        task.localStateDirectories.from(task.taskBuildDirectory).disallowChanges()
    }
}