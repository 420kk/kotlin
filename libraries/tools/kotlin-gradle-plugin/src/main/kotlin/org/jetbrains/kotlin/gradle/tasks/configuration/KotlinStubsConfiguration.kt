/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.utils.property

internal class KotlinStubsConfiguration(
    kotlinCompilation: KotlinCompilationData<*>,
) : KotlinCompileConfig(kotlinCompilation) {

    val sources: ConfigurableFileCollection = objectFactory.fileCollection()
    val javaSourceRoots: ConfigurableFileCollection = objectFactory.fileCollection()
    val verbose: Property<Boolean> = objectFactory.property()

}

internal open class KotlinStubsConfigurator(config: KotlinStubsConfiguration) :
    KotlinCompileConfigurator<KaptGenerateStubsTask, KotlinStubsConfiguration>(config) {
    override fun configure(task: KaptGenerateStubsTask) {
        super.configure(task)

        task.source(config.sources)
        task.javaSourceRoots.from(config.javaSourceRoots)
        task.verbose.set(config.verbose)



//            val kotlinCompileTask = kotlinCompileTaskProvider.get()
//        val providerFactory = kotlinCompileTask.project.providers
//
//        task.useModuleDetection.value(kotlinCompileTask.useModuleDetection).disallowChanges()
//        task.moduleName.value(kotlinCompileTask.moduleName).disallowChanges()
//
//
////            task.compileKotlinArgumentsContributor.set(
////                providerFactory.provider {
////                    kotlinCompileTask.compilerArgumentsContributor
////                }
////            )
//        task.source(providerFactory.provider {
//            kotlinCompileTask.getSourceRoots().kotlinSourceFiles.filter { task.isSourceRootAllowed(it) }
//        })
//        task.javaSourceRoots.from(
//            providerFactory.provider {
//                kotlinCompileTask.getSourceRoots().let { compileTaskSourceRoots ->
//                    compileTaskSourceRoots.javaSourceRoots.filter { task.isSourceRootAllowed(it) }
//                }
//            }
//        )
//        task.verbose.set(KaptTask.queryKaptVerboseProperty(task.project))
    }
}