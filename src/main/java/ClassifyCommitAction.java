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
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.Nullable;
import settings.CommitClassifyConfig;
import utils.Notification;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * @author darekkay
 */
public class ClassifyCommitAction extends AnAction implements DumbAware {

    private static final String urlSuffix = "changes/msg";
    private static final int TIMEOUT_SECONDS = 5;
    private Project project;
    private ProjectLevelVcsManager vcsManager;
    private CheckinProjectPanel checkinPanel;
    private Stream<Change> changeStream;
    private List<ChangePair> changedContent;

    private CommitClassifyConfig config;

    public ClassifyCommitAction() {

    }

    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        this.vcsManager = ProjectLevelVcsManager.getInstance(project); //TODO maybe someday use this?
        this.changedContent = new ArrayList<>();
        this.config = CommitClassifyConfig.getInstance(project);

        checkinPanel = (CheckinProjectPanel) getCheckinPanel(e);
        if (checkinPanel == null)
            return;

        getChanges();
        changeStream = checkinPanel.getSelectedChanges().stream();

        changeStream.forEach(change -> {
            try {
                String src = change.getBeforeRevision() != null ? change.getBeforeRevision().getContent() : "";
                String dst = change.getAfterRevision() != null ? change.getAfterRevision().getContent() : "";

                ChangePair pair = new ChangePair(change.getVirtualFile().getName(), src, dst);
                pair.convertToBase64();
                changedContent.add(pair);
            } catch (VcsException e1) {
                e1.printStackTrace();
            }
        });

        String commitMessage = loadCommitMessage();
        if (!commitMessage.isEmpty()) {
            checkinPanel.setCommitMessage(commitMessage);
            Notification.notify("Success", "Generated commit message summary.");
        }
    }

    public String loadCommitMessage() {
        final FutureTask<String> downloadTask = new FutureTask<>(() -> {
            ChangesRequestBody body = new ChangesRequestBody();
            body.setMatcher(this.config.getDifferID());
            body.setData(changedContent);
            body.setDepth(this.config.getDepth());

            Gson gson = new Gson();
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(new URL(new URL(this.config.getEndpointURL()), urlSuffix).toString());
            StringEntity postingString = null;
            try {
                postingString = new StringEntity(gson.toJson(body));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            post.setEntity(postingString);
            post.setHeader("Content-type", "application/json");
            try {
                HttpResponse response = httpClient.execute(post);
                return IOUtils.toString(response.getEntity().getContent());
            } catch (Exception e) {
                Notification.notifyError("Network error", "Could not fetch commit message, are you online?");
                e.printStackTrace();
            }
            return "";
        });

        ApplicationManager.getApplication().executeOnPooledThread(downloadTask);

        try {
            return downloadTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // ignore
        } catch (Exception e) {
            Notification.notifyError("Network error", "Could not fetch commit message, are you online?");
            throw new RuntimeException(e.getMessage(), e);
        }

        if (!downloadTask.isDone()) {
            downloadTask.cancel(true);
            Notification.notifyError("Network error", "Connection timed out.");
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
