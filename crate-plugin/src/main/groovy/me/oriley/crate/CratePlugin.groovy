/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
        def variants = null;
        def plugin = project.plugins.findPlugin("android");
        if (plugin != null) {
            variants = "applicationVariants";
        } else {
            plugin = project.plugins.findPlugin("android-library");
            if (plugin != null) {
                variants = "libraryVariants";
            }
        }

        if (variants == null) {
            throw new ProjectConfigurationException("android or android-library plugin must be applied", null)
        }

        project.afterEvaluate {
            project.android[variants].all { variant ->
                //noinspection GroovyAssignabilityCheck
                String flavorString = capitalise(variant.flavorName) + capitalise(variant.buildType.name);

                String packageName = findPackageName(project)
                String variantBuildDir = "${project.buildDir}/generated/source/crate/${variant.dirName}"
                String variantAssetDir = "${project.buildDir}/intermediates/assets/${variant.dirName}"

                String crateOutputFile =  "${variantBuildDir}/" + packageName.replace('.', '/') + '/Crate.java';

                // Add source to main sourceset
                variant.sourceSets.each { sourceSet ->
                    if (sourceSet.name == 'main') {
                        sourceSet.java.srcDir "${variantBuildDir}"
                    }
                }

                Task mergeAssetsTask = project.tasks["merge${flavorString}Assets"];
                mergeAssetsTask.doLast {
                    CrateGenerator.writeJava(crateOutputFile, variantAssetDir, packageName);
                }

                variant.preBuild.doFirst {
                    if (!CrateGenerator.isCrateHashValid(crateOutputFile)) {
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

    private static String findPackageName(project) {
        File manifestFile = project.android.sourceSets.main.manifest.srcFile
        return (new XmlParser()).parse(manifestFile).@package
    }

    private static String capitalise(final String line) {
        if (line == null || line.isEmpty()) {
            return ""
        } else {
            return Character.toUpperCase(line.charAt(0)).toString() + line.substring(1);
        }
    }
}