package settings;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import data.Matcher;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.Notification;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@State(
        name = "SingleFileExecutionConfig",
        storages = {@Storage("SingleFileExecutionConfig.xml")}
)
public class CommitClassifyConfig implements PersistentStateComponent<CommitClassifyConfig> {

    private static final String DEFAULT_ENDPOINT = "http://localhost:8080/v1/changes/msg";
    private static final Matcher DEFAULT_DIFFER = new Matcher();
    private static final Integer DEFAULT_DEPTH = 0;

    String endpointURL = DEFAULT_ENDPOINT;
    private Matcher differ = DEFAULT_DIFFER;
    private Integer depth = DEFAULT_DEPTH;

    public CommitClassifyConfig() {
    }

    @Nullable
    public static CommitClassifyConfig getInstance(Project project) {
        return ServiceManager.getService(project, CommitClassifyConfig.class);
    }

    public String getEndpointURL() {
        return endpointURL;
    }

    public void setEndpointURL(String endpointURL) {
        this.endpointURL = endpointURL;
    }

    public Matcher getDiffer() {
        return differ;
    }

    public void setDiffer(Matcher differ) {
        this.differ = differ;
    }

    public Integer getDifferID() {
        return differ.getId();
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    @Nullable
    @Override
    public CommitClassifyConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CommitClassifyConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public List<Matcher> loadDiffers(String url) {
        final FutureTask<List<Matcher>> downloadTask = new FutureTask<>(() -> {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            URL endpointNew = new URL(url);
            HttpGet get = new HttpGet(new URL(endpointNew, "matchers").toString());
            try {
                HttpResponse response = httpClient.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    Notification.notify("Network Error", response.getStatusLine().getReasonPhrase());
                }
                String rawJson = IOUtils.toString(response.getEntity().getContent());
                JsonObject jsonObject = (JsonObject) new JsonParser().parse(rawJson);

                Type listType = new TypeToken<ArrayList<Matcher>>() {
                }.getType();
                return new Gson().fromJson(jsonObject.getAsJsonArray("matchers").toString(), listType);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });

        ApplicationManager.getApplication().executeOnPooledThread(downloadTask);

        try {
            return downloadTask.get(15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // ignore
        } catch (Exception e) {
            Notification.notify("Fatal error", "Could not fetch commit message :(");
            throw new RuntimeException(e.getMessage(), e);
        }

        if (!downloadTask.isDone()) {
            downloadTask.cancel(true);
            throw new RuntimeException("Connection timed out");
        }

        return null;
    }


}
