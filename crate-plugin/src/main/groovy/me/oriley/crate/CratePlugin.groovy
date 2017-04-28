/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package me.oriley.crate

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task

class CratePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def variants = null
        def plugin = project.plugins.findPlugin("android")
        if (plugin != null) {
            variants = "applicationVariants"
        } else {
            plugin = project.plugins.findPlugin("android-library")
            if (plugin != null) {
                variants = "libraryVariants"
            }
        }

        if (variants == null) {
            throw new ProjectConfigurationException("android or android-library plugin must be applied", null)
        }

        project.extensions.create('crate', CrateExtension)

        project.afterEvaluate {
            project.android[variants].all { variant ->
                //noinspection GroovyAssignabilityCheck
                String flavorString = capitalise(variant.flavorName) + capitalise(variant.buildType.name)

                boolean debugLogging = project.crate.debugLogging

                String variantBuildDir = "${project.buildDir}/generated/source/crate/${variant.dirName}"
                String variantAssetDir = "${project.buildDir}/intermediates/assets/${variant.dirName}"

                // Add source to variant source set
                variant.sourceSets.each { sourceSet ->
                    if (sourceSet.name == variant.dirName) {
                        sourceSet.java.srcDir "${variantBuildDir}"
                    }
                }

                //noinspection GrUnresolvedAccess,GroovyAssignabilityCheck
                CrateGenerator generator = new CrateGenerator(variantBuildDir, variantAssetDir, debugLogging)
                Task mergeAssetsTask = project.tasks["merge${flavorString}Assets"]
                mergeAssetsTask.doLast {
                    generator.buildCrate()
                }

                variant.preBuild.doFirst {
                    if (!generator.isCrateHashValid()) {
                        mergeAssetsTask.outputs.upToDateWhen {
                            false
                        }
                    }
                }

                variant.javaCompile.dependsOn mergeAssetsTask
                variant.javaCompile.mustRunAfter mergeAssetsTask
                variant.registerJavaGeneratingTask(mergeAssetsTask, project.file(variantBuildDir))
            }
        }
    }

    private static String capitalise(final String line) {
        if (line == null || line.isEmpty()) {
            return ""
        } else {
            return Character.toUpperCase(line.charAt(0)).toString() + line.substring(1)
        }
    }
}