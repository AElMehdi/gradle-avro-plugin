/*
 * Copyright © 2018 Commerce Technologies, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.commercehub.gradle.plugin.avro

import org.gradle.util.GradleVersion
import spock.lang.IgnoreIf
import spock.lang.PendingFeature

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE

/**
 * Testing for <a href="https://docs.gradle.org/current/userguide/build_cache.html">Build Cache</a> feature support.
 *
 * Only run if on a version of Gradle after Build Cache support was introduced.
 */
@IgnoreIf({ gradleVersion < GradleVersion.version("3.5") })
class BuildCacheSupportFunctionalSpec extends FunctionalSpec {
    def "setup"() {
        applyAvroPlugin()
        addAvroDependency()
    }

    @PendingFeature
    def "supports build cache for schema/protocol java source generation"() {
        given: "a project is built once with build cache enabled"
        copyResource("user.avsc", avroDir)
        copyResource("mail.avpr", avroDir)
        addAvroIpcDependency()
        run("build", "--build-cache")

        and: "the project is cleaned"
        run("clean")

        when: "the project is built again with build cache enabled"
        def result = run("build", "--build-cache")

        then: "the expected outputs were produced from the build cache"
        taskInfoAbsent || result.task(":generateAvroJava").outcome == FROM_CACHE
        taskInfoAbsent || result.task(":compileJava").outcome == FROM_CACHE
        projectFile("build/generated-main-avro-java/example/avro/User.java").file
        projectFile("build/generated-main-avro-java/org/apache/avro/test/Mail.java").file
        projectFile(buildOutputClassPath("example/avro/User.class")).file
        projectFile(buildOutputClassPath("org/apache/avro/test/Mail.class")).file
    }

    @PendingFeature
    def "supports build cache for IDL to protocol conversion"() {
        given: "a project is built once with build cache enabled"
        copyResource("interop.avdl", avroDir)
        run("build", "--build-cache")

        and: "the project is cleaned"
        run("clean")

        when: "the project is built again with build cache enabled"
        def result = run("build", "--build-cache")

        then: "the expected outputs were produced from the build cache"
        taskInfoAbsent || result.task(":generateAvroProtocol").outcome == FROM_CACHE
        taskInfoAbsent || result.task(":generateAvroJava").outcome == FROM_CACHE
        taskInfoAbsent || result.task(":compileJava").outcome == FROM_CACHE
        projectFile("build/generated-main-avro-avpr/interop.avpr").file
        projectFile("build/generated-main-avro-java/org/apache/avro/Interop.java").file
        projectFile(buildOutputClassPath("org/apache/avro/Interop.class")).file
    }

    @PendingFeature
    def "supports build cache for protocol to schema conversion"() {
        given: "a project is built once with build cache enabled"
        copyResource("mail.avpr", avroDir)
        buildFile << """
            task("generateSchema", type: com.commercehub.gradle.plugin.avro.GenerateAvroSchemaTask) {
                source file("src/main/avro")
                include("**/*.avpr")
                outputDir = file("build/generated-main-avro-avsc")
            }
        """
        run("generateSchema", "--build-cache")

        and: "the project is cleaned"
        run("clean")

        when: "the project is built again with build cache enabled"
        def result = run("generateSchema", "--build-cache")

        then: "the expected outputs were produced from the build cache"
        taskInfoAbsent || result.task(":generateSchema").outcome == FROM_CACHE
        projectFile("build/generated-main-avro-avsc/org/apache/avro/test/Message.avsc").file
    }
}
