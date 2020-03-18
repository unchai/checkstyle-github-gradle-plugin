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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.GradleException;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

class GithubHelper {
    private static final String CONTEXT = "coding-convention/checkstyle";
    private static final String PREFIX = "#### :rotating_light: checkstyle defects";

    private GHRepository repo;
    private GHPullRequest pr;
    private String username;

    GithubHelper(GitHub github, String repository, int pullRequest) throws IOException {
        this.username = github.getMyself().getLogin();
        this.repo = github.getRepository(repository);
        this.pr = this.repo.getPullRequest(pullRequest);
    }

    Map<Integer, Integer> parsePatch(String patch) {
        int lineNo = 0;
        int pathNo = 0;

        final Map<Integer, Integer> map = new HashMap<>();
        for (String line : patch.split("\\r?\\n")) {
            if (line.startsWith("@@")) {
                final Matcher matcher = Pattern.compile("^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@.*").matcher(line);

                if (matcher.matches()) {
                    lineNo = Integer.parseInt(matcher.group(3));
                }
            } else if (line.startsWith(" ")) {
                lineNo++;
            } else if (line.startsWith("+")) {
                map.put(lineNo++, pathNo);
            }

            pathNo++;
        }

        return map;
    }

    List<ChangedFile> listChangedFile() {
        final List<ChangedFile> linePositionMap = new ArrayList<>();

        for (GHPullRequestFileDetail fileDetail : this.pr.listFiles()) {
            if (fileDetail.getPatch() == null) {
                continue;
            }

            final Map<Integer, Integer> diffMap = parsePatch(fileDetail.getPatch());

            if (!diffMap.isEmpty()) {
                final ChangedFile changedFile = new ChangedFile();
                changedFile.setPath(fileDetail.getFilename());
                changedFile.setLinePositionMap(diffMap);

                linePositionMap.add(changedFile);
            }
        }

        return linePositionMap;
    }

    void changeStatus(GHCommitState state, String description) {
        try {
            this.repo.createCommitStatus(
                this.pr.getHead().getSha(),
                state,
                null,
                description,
                CONTEXT
            );
        } catch (IOException e) {
            throw new GradleException(e.getMessage(), e);
        }
    }

    void removeAllComment() {
        try {
            for (GHPullRequestReviewComment comment : this.pr.listReviewComments()) {
                if (comment.getUser().getLogin().equals(this.username)
                    && comment.getBody().startsWith(PREFIX)) {
                    comment.delete();
                }
            }
        } catch (IOException e) {
            throw new GradleException(e.getMessage(), e);
        }
    }

    void createComment(Comment comment) {
        try {
            this.pr.createReviewComment(
                buildCommentBody(comment),
                this.pr.getHead().getSha(),
                comment.getPath(),
                comment.getPosition()
            );
        } catch (IOException e) {
            throw new GradleException(e.getMessage(), e);
        }
    }

    private String buildCommentBody(Comment comment) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s%n", PREFIX));

        for (CheckstyleError error : comment.getCheckstyleErrors()) {
            builder.append(String.format("[%s] %s%n", error.getSeverityLevel().name(), error.getMessage()));
        }

        return builder.toString();
    }
}
