/*
 * Copyright 2014 Darek Kay <darekkay@eclectide.com>
 *
 * MIT license
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.ui.Refreshable;
import data.ChangePair;
import data.ChangesRequestBody;
import javafx.util.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.Nullable;
import utils.Notification;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * @author darekkay
 */
public class WhatTheCommitAction extends AnAction implements DumbAware {

    private static final String URL = "http://whatthecommit.com/index.txt";
    private static final int TIMEOUT_SECONDS = 5;
    private Project project;
    private ProjectLevelVcsManager vcsManager;
    private CheckinProjectPanel checkinPanel;
    private Stream<Change> changeStream;
    private List<ChangePair> changedContent;

    public WhatTheCommitAction() {
        this.changedContent = new ArrayList<>();
    }

    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        this.vcsManager = ProjectLevelVcsManager.getInstance(project);


        checkinPanel = (CheckinProjectPanel) getCheckinPanel(e);
        if (checkinPanel == null)
            return;

        getChanges();
        changeStream = checkinPanel.getSelectedChanges().stream();
        changeStream.forEach(change -> {
            try {
                changedContent.add(new ChangePair(change.getVirtualFile().getName(), change.getBeforeRevision().getContent(), change.getAfterRevision().getContent()));
            } catch (VcsException e1) {
                e1.printStackTrace();
            }
        });


        String commitMessage = loadCommitMessage();
        if (!commitMessage.isEmpty()) {
            checkinPanel.setCommitMessage(commitMessage);
        }
    }

    public String loadCommitMessage() {
        final FutureTask<String> downloadTask = new FutureTask<String>(new Callable<String>() {
            public String call() {
//                final HttpClient client = new HttpClient();
////                final PostMethod postMethod = new PostMethod(url);
////
////                try {
////                    final int statusCode = client.executeMethod(postMethod);
////                    if (statusCode != HttpStatus.SC_OK)
////                        throw new RuntimeException("Connection error (HTTP status = " + statusCode + ")");
////                    return getMethod.getResponseBodyAsString();
////                } catch (IOException e) {
////                    throw new RuntimeException(e.getMessage(), e);
////                }
                ChangesRequestBody body = new ChangesRequestBody();
                body.setMatcher(5);
                body.setData(changedContent);

                String postUrl = "https://localhost/changes/msg";// put in your url
                Gson gson = new Gson();
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost post = new HttpPost(postUrl);
                StringEntity postingString = null;//gson.tojson() converts your pojo to json
                try {
                    postingString = new StringEntity(gson.toJson(body));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                post.setEntity(postingString);
                post.setHeader("Content-type", "application/json");
                try {
                    HttpResponse response = httpClient.execute(post);
                    return response.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "";
            }
        });

        ApplicationManager.getApplication().executeOnPooledThread(downloadTask);

        VcsRoot[] roots = vcsManager.getAllVcsRoots();
        for (VcsRoot root : roots) {
            Notification.notify("Found root", root.toString());
        }



        try {
            return downloadTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // ignore
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (!downloadTask.isDone()) {
            downloadTask.cancel(true);
            throw new RuntimeException("Connection timed out");
        }

        return "";
    }

    @Nullable
    private static CommitMessageI getCheckinPanel(@Nullable AnActionEvent e) {
        if (e == null) {
            return null;
        }
        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (data instanceof CommitMessageI) {
            return (CommitMessageI) data;
        }
        CommitMessageI commitMessageI = VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext());
        if (commitMessageI != null) {
            return commitMessageI;
        }
        return null;
    }

    private String[] getChanges() {
        return checkinPanel.getSelectedChanges()
                .stream()
                .flatMap(this::getAllRevisionPaths)
                .distinct()
                .toArray(String[]::new);
    }

    private Stream<String> getAllRevisionPaths(Change change) {
        ContentRevision[] revisions = new ContentRevision[2];
        revisions[0] = change.getBeforeRevision();
        revisions[1] = change.getAfterRevision();

        return Arrays.stream(revisions)
                .filter(Objects::nonNull)
                .map(ContentRevision::getFile)
                .map(FilePath::getPath)
                .distinct();
    }
}
