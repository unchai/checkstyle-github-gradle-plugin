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

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.Configuration;

public class CheckstyleExecutor {
    private final Checker checker = new Checker();
    private final String configLocation;

    public CheckstyleExecutor(String configLocation) {
        this.configLocation = configLocation;
    }

    public List<CheckstyleError> execute(String baseDir, List<String> files) {
        try {
            final Configuration configuration = ConfigurationLoader.loadConfiguration(
                configLocation,
                new PropertiesExpander(System.getProperties())
            );

            final CheckstyleAuditListener listener = new CheckstyleAuditListener();

            checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
            checker.configure(configuration);
            checker.addListener(listener);
            checker.process(files.stream().map(file -> new File(baseDir, file)).collect(Collectors.toList()));

            return listener.getErrors()
                .stream()
                .map(error -> stripBaseDir(baseDir, error))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new GradleException(e.getMessage(), e);
        }
    }

    private CheckstyleError stripBaseDir(String baseDir, CheckstyleError checkstyleError) {
        checkstyleError.setPath(checkstyleError.getPath().replace(baseDir, "").substring(1));
        return checkstyleError;
    }
}
