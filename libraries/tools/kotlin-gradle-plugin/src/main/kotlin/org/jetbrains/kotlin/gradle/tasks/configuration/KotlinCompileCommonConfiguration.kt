/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.AbstractKotlinFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.refinesClosure
import org.jetbrains.kotlin.gradle.plugin.sources.resolveAllDependsOnSourceSets
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

internal class KotlinCompileCommonConfiguration(
    compilation: KotlinCompilationData<*>,
) : KotlinCompileConfig(compilation) {

    val refinesMetadataPaths: ConfigurableFileCollection = objectFactory.fileCollection().from(
        getRefinesMetadataPaths(compilation.project)
    ).also {
        it.disallowChanges()
    }

    val expectActualLinker: Property<Boolean> = objectFactory.property<Boolean>().value(
        providers.provider { (compilation as? KotlinCommonCompilation)?.isKlibCompilation == true || compilation is KotlinMetadataCompilationData }
    ).also {
        it.disallowChanges()
    }

    private fun getRefinesMetadataPaths(project: Project): Provider<Iterable<File>> {
        return project.provider {
            when (compilation) {
                is KotlinCompilation<*> -> {
                    val defaultKotlinSourceSet: KotlinSourceSet = compilation.defaultSourceSet
                    val metadataTarget = compilation.owner as KotlinTarget
                    defaultKotlinSourceSet.resolveAllDependsOnSourceSets()
                        .mapNotNull { sourceSet -> metadataTarget.compilations.findByName(sourceSet.name)?.output?.classesDirs }
                        .flatten()
                }
                is AbstractKotlinFragmentMetadataCompilationData -> {
                    val fragment = compilation.fragment
                    project.files(
                        fragment.refinesClosure.minus(fragment).map {
                            compilation.metadataCompilationRegistry.byFragment(it).output.classesDirs
                        }
                    )
                }
                else -> error("unexpected compilation type")
            }
        }
    }

    fun configureTask(task: KotlinCompileCommon) {
        KotlinCompileCommonConfigurator(this).configure(task)
    }
}

internal open class KotlinCompileCommonConfigurator(config: KotlinCompileCommonConfiguration) :
    Configurator<KotlinCompileCommon, KotlinCompileCommonConfiguration>(config) {
    override fun configure(task: KotlinCompileCommon) {
        super.configure(task)

        task.expectActualLinker.value(config.expectActualLinker).disallowChanges()
        task.refinesMetadataPaths.from(config.refinesMetadataPaths).disallowChanges()
    }
}