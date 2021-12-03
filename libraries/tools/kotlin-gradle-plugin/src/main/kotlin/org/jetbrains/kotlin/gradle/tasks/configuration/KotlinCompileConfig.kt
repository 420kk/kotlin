/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig
import org.jetbrains.kotlin.gradle.internal.transforms.ClasspathEntrySnapshotTransform
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_BUILD_DIR_NAME
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileApi
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompileApi
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.project.model.LanguageSettings

internal open class KotlinCompileConfig(
    compilation: KotlinCompilationData<*>
) : AbstractKotlinCompileInfo(compilation) {

    companion object {
        private const val TRANSFORMS_REGISTERED = "_kgp_internal_kotlin_compile_transforms_registered"

        val ARTIFACT_TYPE_ATTRIBUTE: Attribute<String> = Attribute.of("artifactType", String::class.java)
        private const val DIRECTORY_ARTIFACT_TYPE = "directory"
        private const val JAR_ARTIFACT_TYPE = "jar"
        const val CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE = "classpath-entry-snapshot"
    }

    private val classpathSnapshotConfiguration: Configuration?

    init {
        /**
         * Prepares for configuration of the task. This method must be called during build configuration, not during task configuration
         * (which typically happens after build configuration). The reason is that some actions must be performed early (e.g., creating
         * configurations should be done early to avoid issues with composite builds (https://issuetracker.google.com/183952598)).
         */
        classpathSnapshotConfiguration = if (propertiesProvider.useClasspathSnapshot) {
            registerTransformsOnce(compilation.project)
            compilation.project.configurations.create(classpathSnapshotConfigurationName(compilation.compileKotlinTaskName)).apply {
                compilation.project.dependencies.add(name, classpath)
            }
        } else null
    }

    private fun registerTransformsOnce(project: Project) {
        if (project.extensions.extraProperties.has(TRANSFORMS_REGISTERED)) {
            return
        }
        project.extensions.extraProperties[TRANSFORMS_REGISTERED] = true

        project.dependencies.registerTransform(ClasspathEntrySnapshotTransform::class.java) {
            it.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_ARTIFACT_TYPE)
            it.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
        }
        project.dependencies.registerTransform(ClasspathEntrySnapshotTransform::class.java) {
            it.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_ARTIFACT_TYPE)
            it.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
        }
    }

    private fun classpathSnapshotConfigurationName(taskName: String) = "_kgp_internal_${taskName}_classpath_snapshot"

    private val javaTaskProvider = when (compilation) {
        is KotlinJvmCompilation -> compilation.compileJavaTaskProvider
        is KotlinJvmAndroidCompilation -> compilation.compileJavaTaskProvider
        is KotlinWithJavaCompilation<*> -> compilation.compileJavaTaskProvider
        else -> null // can this happen???
    }

    val associatedJavaCompileTaskTargetCompatibility: Property<String> = objectFactory.property<String>().also {
        javaTaskProvider ?: return@also
        it.value(javaTaskProvider.map { it.targetCompatibility })
    }

    val associatedJavaCompileTaskSources: ConfigurableFileCollection = objectFactory.fileCollection().also {
        javaTaskProvider ?: return@also
        it.from(javaTaskProvider.map { it.source() })
    }

    val associatedJavaCompileTaskName: Property<String> = objectFactory.property<String>().also {
        javaTaskProvider ?: return@also
        it.value(javaTaskProvider.name)
    }

    internal val jvmTargetValidationMode: Property<PropertiesProvider.JvmTargetValidationMode> = objectFactory.property(propertiesProvider.jvmTargetValidationMode)

    val classpathSnapshotDir: DirectoryProperty
        get() = objectFactory.directoryProperty().value(
            compilation.project.layout.buildDirectory.dir("$KOTLIN_BUILD_DIR_NAME/classpath-snapshot/${taskName}")
        ).also {
            it.disallowChanges()
        }

    val classpathSnapshotFiles: FileCollection?
        get() {
            classpathSnapshotConfiguration ?: return null
            return classpathSnapshotConfiguration.incoming.artifactView {
                it.attributes.attribute(
                    KotlinCompile.Configurator.ARTIFACT_TYPE_ATTRIBUTE,
                    KotlinCompile.Configurator.CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE
                )
            }.files
        }

    val useClasspathSnapshot: Property<Boolean> = objectFactory.property(propertiesProvider.useClasspathSnapshot)

    override val moduleName: Property<String> = objectFactory.property<String>().value(
        providers.provider {
            (compilation.kotlinOptions as? KotlinJvmOptions)?.moduleName ?: parentKotlinOptions.orNull?.moduleName ?: compilation.moduleName
        }
    )

    val javaPackagePrefix: Property<String> = objectFactory.property()

    val usePreciseJavaTracking: Property<Boolean> = objectFactory.property(propertiesProvider.usePreciseJavaTracking ?: true)

    val useFir: Property<Boolean> = objectFactory.property(propertiesProvider.useFir)

    val parentKotlinOptions: Property<KotlinJvmOptions> = objectFactory.property()

    fun initFromExternal(obj: Any) {


        // get external data (say we make that a configurable.
        // we can have api with something like
        class KotlinCompileApiConfigurable {}
    }

    fun configureTask(task: KotlinCompile) {
        KotlinCompileConfigurator<KotlinCompile, KotlinCompileConfig>(this).configure(task)
    }
}

internal open class KotlinCompileConfigurator<TASK : KotlinCompile, CONFIG : KotlinCompileConfig>(config: CONFIG) :
    Configurator<TASK, CONFIG>(config) {
    override fun configure(task: TASK) {
        super.configure(task)

        task.associatedJavaCompileTaskTargetCompatibility.set(config.associatedJavaCompileTaskTargetCompatibility)
        task.associatedJavaCompileTaskSources.from(config.associatedJavaCompileTaskSources)
        task.associatedJavaCompileTaskName.set(config.associatedJavaCompileTaskName)
        task.jvmTargetValidationMode.set(config.jvmTargetValidationMode)

        if (config.useClasspathSnapshot.getOrElse(false)) {
            task.classpathSnapshotProperties.classpathSnapshot.from(config.classpathSnapshotFiles).disallowChanges()
            task.classpathSnapshotProperties.classpathSnapshotDir.value(config.classpathSnapshotDir).disallowChanges()
            task.classpathSnapshotProperties.classpathSnapshotDirFileCollection.from(config.classpathSnapshotDir)
        } else {
            task.classpathSnapshotProperties.classpath.from(task.project.provider { task.classpath })
        }
        task.javaPackagePrefixProperty.set(config.javaPackagePrefix)
        task.usePreciseJavaTrackingProperty.set(config.usePreciseJavaTracking)
        if (config.useFir.getOrElse(false)) {
            task.kotlinOptions.useFir = true
        }
    }
}

class ExteranlCOnfig(val kotlinJvmCompileApi: KotlinJvmCompileApi) {

}