/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.unchai.gradle.checkstyle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;

public class CheckstyleGithubPlugin implements Plugin<Project> {
    public static final String TOOL_NAME = "checkstyleGithub";
    private static final String DEFAULT_CHECKSTYLE_VERSION = "8.29";
    private static final String DEFAULT_GITHUB_ENDPOINT = "https://api.github.com";

    @Override
    public void apply(Project project) {
        final Configuration configuration = project.getConfigurations().create(TOOL_NAME);

        final CheckstyleGithubPluginExtension extension = project.getExtensions().create(TOOL_NAME, CheckstyleGithubPluginExtension.class);
        extension.setToolVersion(DEFAULT_CHECKSTYLE_VERSION);
        extension.setGhEndpoint(DEFAULT_GITHUB_ENDPOINT);

        extension.validate();

        configuration.defaultDependencies(dependencies -> dependencies.add(
            project.getDependencies().create("com.puppycrawl.tools:checkstyle:" + extension.getToolVersion())
        ));

        final Task task = project.getTasks().create(TOOL_NAME, CheckstyleGithubTask.class);
        task.setGroup("Lint");
        task.setDescription("A gradle plugin that leaves comment of the result of a \"Checkstyle\" on github's pull request.");
    }
}
