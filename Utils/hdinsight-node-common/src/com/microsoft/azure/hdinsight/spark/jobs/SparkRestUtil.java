/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.rest.AttemptWithAppId;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.RestUtil;
import com.microsoft.azure.hdinsight.sdk.rest.spark.Application;
import com.microsoft.azure.hdinsight.sdk.rest.spark.executor.Executor;
import com.microsoft.azure.hdinsight.sdk.rest.spark.job.Job;
import com.microsoft.azure.hdinsight.sdk.rest.spark.stage.Stage;
import com.microsoft.azure.hdinsight.sdk.rest.spark.task.Task;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpEntity;


import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SparkRestUtil {
    public static final String SPARK_REST_API_ENDPOINT = "%s/sparkhistory/api/v1/applications/%s";

    @NotNull
    public static List<Application> getSparkApplications(@NotNull IClusterDetail clusterDetail) throws HDIException, IOException {
        HttpEntity entity = getSparkRestEntity(clusterDetail, "");
        Optional<List<Application>> apps = ObjectConvertUtils.convertEntityToList(entity, Application.class);

        // spark job has at least one attempt
        return apps.orElse(RestUtil.getEmptyList(Application.class))
                .stream()
                .filter(app -> app.getAttempts().size() != 0 && app.getAttempts().get(0).getAttemptId() != null)
                .collect(Collectors.toList());
    }

    public static List<Executor> getAllExecutorFromApp(@NotNull ApplicationKey key) throws IOException, HDIException, ExecutionException {
        final AttemptWithAppId attemptWithAppId = getLastAttemptFromLocalCache(key);
        final HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), String.format("/%s/%s/executors", attemptWithAppId.getAppId(), attemptWithAppId.getAttemptId()));
        Optional<List<Executor>> executors = ObjectConvertUtils.convertEntityToList(entity, Executor.class);
        return executors.orElse(RestUtil.getEmptyList(Executor.class));
    }

    public static List<Stage> getAllStageFromApp(@NotNull ApplicationKey key) throws IOException, HDIException, ExecutionException {
        final AttemptWithAppId attemptWithAppId = getLastAttemptFromLocalCache(key);
        final HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), String.format("/%s/%s/stages", attemptWithAppId.getAppId(), attemptWithAppId.getAttemptId()));
        final Optional<List<Stage>> stages = ObjectConvertUtils.convertEntityToList(entity, Stage.class);
        return stages.orElse(RestUtil.getEmptyList(Stage.class));
    }

    public static List<Job> getLastAttemptJobsFromApp(@NotNull ApplicationKey key) throws IOException, HDIException, ExecutionException {
        AttemptWithAppId attemptWithAppId = getLastAttemptFromLocalCache(key);
        return getSparkJobsFromApp(key.getClusterDetails(), key.getAppId(), attemptWithAppId.getAttemptId());
    }

    public static List<Job> getSparkJobsFromApp(@NotNull IClusterDetail clusterDetail, @NotNull String appId, @NotNull String attemptId) throws IOException, HDIException {
        HttpEntity entity = getSparkRestEntity(clusterDetail, String.format("/%s/%s/jobs", appId, attemptId));
        Optional<List<Job>> apps = ObjectConvertUtils.convertEntityToList(entity, Job.class);
        return apps.orElse(RestUtil.getEmptyList(Job.class));
    }

    public static List<Task> getSparkTasks(@NotNull ApplicationKey key, @NotNull int stage, int attemptId) throws IOException, ExecutionException, HDIException {
        AttemptWithAppId attemptWithAppId = getLastAttemptFromLocalCache(key);
        String url = String.format("/%s/%s/stages/%s/%s/taskList", attemptWithAppId.getAppId(), attemptWithAppId.getAttemptId(),stage, attemptId);
        HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), url);

        Optional<List<Task>> tasks = ObjectConvertUtils.convertEntityToList(entity, Task.class);
        return tasks.orElse(RestUtil.getEmptyList(Task.class));
    }

    private static AttemptWithAppId getLastAttemptFromLocalCache(@NotNull ApplicationKey key) throws ExecutionException, HDIException {
        List<Application> sparkApplications = JobViewCacheManager.getSparkApplications(key.getClusterDetails());
        Optional<Application> selectedApplication = sparkApplications.stream().filter(application -> application.getId().equalsIgnoreCase(key.getAppId())
        ).findFirst();
        return selectedApplication.orElseThrow(()-> new HDIException("application can't find")).getLastAttemptWithAppId(key.getClusterDetails().getName());
    }

    private static HttpEntity getSparkRestEntity(@NotNull IClusterDetail clusterDetail, @NotNull String restUrl) throws HDIException, IOException {
        final String url = String.format(SPARK_REST_API_ENDPOINT, clusterDetail.getConnectionUrl(), restUrl);
        return JobUtils.getEntity(clusterDetail, url);
    }
}
