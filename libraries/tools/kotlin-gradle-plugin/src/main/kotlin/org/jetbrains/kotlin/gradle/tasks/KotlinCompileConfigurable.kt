/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.utils.propertyWithNewInstance

class KotlinCompileConfigurable(val objects: ObjectFactory) {

    val moduleName: Property<String> = objects.propertyWithNewInstance()

    val sourceSetName: Property<String> = objects.propertyWithNewInstance()

    val useModuleDetection: Property<Boolean> = objects.propertyWithNewInstance()

    val multiPlatformEnabled: Property<Boolean> = objects.propertyWithNewInstance()

    val pluginClasspath: ConfigurableFileCollection = objects.fileCollection()

    val friendPaths: ConfigurableFileCollection = objects.fileCollection()

    val taskBuildDirectory: DirectoryProperty = objects.directoryProperty()

    val outputDirectory: DirectoryProperty = objects.directoryProperty()

    fun configureTask(task: KotlinCompile) {
        // configure all task properties using this configurable
        task.moduleName.set(moduleName)
        task.sourceSetName.set(sourceSetName)
        task.useModuleDetection.set(useModuleDetection)
        task.multiPlatformEnabled.set(multiPlatformEnabled)
        task.pluginClasspath.from(pluginClasspath)
        task.friendPaths.from(friendPaths)
        task.taskBuildDirectory.set(taskBuildDirectory)
        task.outputDirectory.set(outputDirectory)
    }
}