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
package com.microsoft.azure.hdinsight.jobs;

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.common.task.LivyTask;
import com.microsoft.azure.hdinsight.common.task.RestTask;
import com.microsoft.azure.hdinsight.common.task.TaskExecutor;
import com.microsoft.azure.hdinsight.common.task.YarnHistoryTask;
import com.microsoft.azure.hdinsight.jobs.framework.RequestDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobViewDummyHttpServer {

    private static final String sparkPreRestUrl = "https://%s.azurehdinsight.net/sparkhistory/api/v1";
    private static final String yarnPreRestUrl = "https://%s.azurehdinsight.net/yarnui/ws/v1";
    private static final String yarnHistoryUrl = "https://%s.azurehdinsight.net/yarnui";
    private static final String LivyBatchesRestUrl = "https://%s.azurehdinsight.net/livy/batches";

    private static Logger LOGGER = Logger.getLogger(JobViewDummyHttpServer.class);
    private static Pattern clusterPattern = Pattern.compile("^/clusters/([^/]*)(/.*)");

    private static RequestDetail requestDetail;
    public  static final int PORT = 39129;
    private static HttpServer server;
    private static final int NO_OF_THREADS = 10;
   private static  ExecutorService executorService;

    public static RequestDetail getCurrentRequestDetail() {
        return requestDetail;
    }

    public static void close() {
        if (server != null) {
            server.stop(0);
        }
        if(executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    @Nullable
    private static RequestDetail getRequestDetail(@NotNull URI myUrl) {
        String[] queries = myUrl.getQuery() == null ? null : myUrl.getQuery().split("&");
        String path = myUrl.getPath();
        Matcher matcher = clusterPattern.matcher(path);
        if (matcher.find()) {
            requestDetail = new RequestDetail(matcher.group(1), matcher.group(2), queries);
            return requestDetail;
        }
        return null;
    }

    public static void initlize() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 10);
            server.createContext("/clusters/", new HttpHandler() {
                @Override
                public void handle(final HttpExchange httpExchange) throws IOException {
                    final RequestDetail requestDetail = getRequestDetail(httpExchange.getRequestURI());

                    if (requestDetail == null) {
                        return;
                    }

                    IClusterDetail clusterDetail = requestDetail.getClusterDetail();

                    if (clusterDetail == null) {
                        return;
                    }

                    String preUrl = "";
                    switch (requestDetail.getApiType()) {
                        case YarnHistory:
                            preUrl = yarnHistoryUrl;
                            break;
                        case YarnRest:
                            preUrl = yarnPreRestUrl;
                            break;
                        case LivyBatchesRest:
                            preUrl = LivyBatchesRestUrl;
                            break;
                        default:
                            preUrl = sparkPreRestUrl;
                    }

                    String queryUrl = String.format(preUrl, clusterDetail.getName()) + requestDetail.getRestUrl();

                    if (requestDetail.getApiType() == RequestDetail.APIType.YarnHistory) {
                        if(queryUrl.endsWith("stderr")) {
                            queryUrl = queryUrl + "?start=0";
                        }

                        TaskExecutor.submit(new YarnHistoryTask(clusterDetail, queryUrl, new FutureCallback<String>() {
                            @Override
                            public void onSuccess(String str) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    // work around of get job result
                                    //TODO: get job result by REST API
                                    Document doc = Jsoup.parse(str);
                                    Elements contentElements = doc.getElementsByClass("content");
                                    if (contentElements.size() == 1) {
                                        Elements elements = contentElements.get(0).getElementsByTag("pre");
                                        if (elements.size() == 1) {
                                            str = elements.get(0).html();
                                        }
                                    }

                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                } catch (IOException e) {
                                    LOGGER.error("Get job history error", e);
                                }
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    String str = throwable.getMessage();
                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                }catch (Exception e) {
                                    LOGGER.error("Get job history error", e);
                                }
                            }
                        }));
                    } else if(requestDetail.getApiType() == RequestDetail.APIType.LivyBatchesRest) {
                        TaskExecutor.submit(new LivyTask(clusterDetail, queryUrl, new FutureCallback<String>() {
                            @Override
                            public void onSuccess(String str) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    String applicationId = requestDetail.getProperty("applicationId");
                                    if(applicationId != null) {
                                       str = JobUtils.getJobInformation(str, applicationId);
                                    }

                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                } catch (IOException e) {
                                    LOGGER.error("Get job history error", e);
                                }
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    String str = throwable.getMessage();
                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                }catch (Exception e) {
                                    LOGGER.error("Get job history error", e);
                                }
                            }
                        }));

                    }  else {
                        TaskExecutor.submit(new RestTask(clusterDetail, queryUrl, new FutureCallback<String>() {
                            @Override
                            public void onSuccess(String str) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                } catch (IOException e) {
                                    LOGGER.error("Get job history error", e);
                                }
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    String str = throwable.getMessage();
                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                }catch (Exception e) {
                                    LOGGER.error("Get job history error", e);
                                }
                            }
                        }));
                    }

                }
            });

            executorService = Executors.newFixedThreadPool(NO_OF_THREADS);
            server.setExecutor(executorService);
            server.start();
        } catch (IOException e) {
            LOGGER.error("Get job history error", e);
            DefaultLoader.getUIHelper().showError(e.getClass().getName(), e.getMessage());
        }
    }
}
