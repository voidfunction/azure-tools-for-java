package com.microsoft.azure.hdinsight.sdk.rest.spark.event;

import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ltian on 6/12/2017.
 */
public class Test {
    public static void main(String[] args) {
        String str = "{\n" +
                "  \"Event\": \"SparkListenerJobStart\",\n" +
                "  \"Job ID\": 0,\n" +
                "  \"Submission Time\": 1497236981665,\n" +
                "  \"Stage Infos\": [\n" +
                "    {\n" +
                "      \"Stage ID\": 0,\n" +
                "      \"Stage Attempt ID\": 0,\n" +
                "      \"Stage Name\": \"collect at SparkSQL_RDDRelation.scala:44\",\n" +
                "      \"Number of Tasks\": 4,\n" +
                "      \"RDD Info\": [\n" +
                "        {\n" +
                "          \"RDD ID\": 4,\n" +
                "          \"Name\": \"MapPartitionsRDD\",\n" +
                "          \"Scope\": \"{\\\"id\\\":\\\"4\\\",\\\"name\\\":\\\"mapPartitionsInternal\\\"}\",\n" +
                "          \"Callsite\": \"collect at SparkSQL_RDDRelation.scala:44\",\n" +
                "          \"Parent IDs\": [\n" +
                "            3\n" +
                "          ],\n" +
                "          \"Storage Level\": {\n" +
                "            \"Use Disk\": false,\n" +
                "            \"Use Memory\": false,\n" +
                "            \"Deserialized\": false,\n" +
                "            \"Replication\": 1\n" +
                "          },\n" +
                "          \"Number of Partitions\": 4,\n" +
                "          \"Number of Cached Partitions\": 0,\n" +
                "          \"Memory Size\": 0,\n" +
                "          \"Disk Size\": 0\n" +
                "        },\n" +
                "        {\n" +
                "          \"RDD ID\": 3,\n" +
                "          \"Name\": \"MapPartitionsRDD\",\n" +
                "          \"Scope\": \"{\\\"id\\\":\\\"3\\\",\\\"name\\\":\\\"ExistingRDD\\\"}\",\n" +
                "          \"Callsite\": \"collect at SparkSQL_RDDRelation.scala:44\",\n" +
                "          \"Parent IDs\": [\n" +
                "            1\n" +
                "          ],\n" +
                "          \"Storage Level\": {\n" +
                "            \"Use Disk\": false,\n" +
                "            \"Use Memory\": false,\n" +
                "            \"Deserialized\": false,\n" +
                "            \"Replication\": 1\n" +
                "          },\n" +
                "          \"Number of Partitions\": 4,\n" +
                "          \"Number of Cached Partitions\": 0,\n" +
                "          \"Memory Size\": 0,\n" +
                "          \"Disk Size\": 0\n" +
                "        },\n" +
                "        {\n" +
                "          \"RDD ID\": 0,\n" +
                "          \"Name\": \"ParallelCollectionRDD\",\n" +
                "          \"Scope\": \"{\\\"id\\\":\\\"0\\\",\\\"name\\\":\\\"parallelize\\\"}\",\n" +
                "          \"Callsite\": \"parallelize at SparkSQL_RDDRelation.scala:37\",\n" +
                "          \"Parent IDs\": [],\n" +
                "          \"Storage Level\": {\n" +
                "            \"Use Disk\": false,\n" +
                "            \"Use Memory\": false,\n" +
                "            \"Deserialized\": false,\n" +
                "            \"Replication\": 1\n" +
                "          },\n" +
                "          \"Number of Partitions\": 4,\n" +
                "          \"Number of Cached Partitions\": 0,\n" +
                "          \"Memory Size\": 0,\n" +
                "          \"Disk Size\": 0\n" +
                "        },\n" +
                "        {\n" +
                "          \"RDD ID\": 1,\n" +
                "          \"Name\": \"MapPartitionsRDD\",\n" +
                "          \"Scope\": \"{\\\"id\\\":\\\"1\\\",\\\"name\\\":\\\"map\\\"}\",\n" +
                "          \"Callsite\": \"rddToDatasetHolder at SparkSQL_RDDRelation.scala:37\",\n" +
                "          \"Parent IDs\": [\n" +
                "            0\n" +
                "          ],\n" +
                "          \"Storage Level\": {\n" +
                "            \"Use Disk\": false,\n" +
                "            \"Use Memory\": false,\n" +
                "            \"Deserialized\": false,\n" +
                "            \"Replication\": 1\n" +
                "          },\n" +
                "          \"Number of Partitions\": 4,\n" +
                "          \"Number of Cached Partitions\": 0,\n" +
                "          \"Memory Size\": 0,\n" +
                "          \"Disk Size\": 0\n" +
                "        }\n" +
                "      ],\n" +
                "      \"Parent IDs\": [],\n" +
                "      \"Details\": \"org.apache.spark.sql.Dataset.collect(Dataset.scala:2173)\\norg.apache.spark.examples.sql.RDDRelation$.main(SparkSQL_RDDRelation.scala:44)\\norg.apache.spark.examples.sql.RDDRelation.main(SparkSQL_RDDRelation.scala)\\nsun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\\nsun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\\nsun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\\njava.lang.reflect.Method.invoke(Method.java:498)\\norg.apache.spark.deploy.yarn.ApplicationMaster$$anon$2.run(ApplicationMaster.scala:627)\",\n" +
                "      \"Accumulables\": []\n" +
                "    }\n" +
                "  ],\n" +
                "  \"Stage IDs\": [\n" +
                "    0\n" +
                "  ],\n" +
                "  \"Properties\": {\n" +
                "    \"spark.rdd.scope.noOverride\": \"true\",\n" +
                "    \"spark.rdd.scope\": \"{\\\"id\\\":\\\"5\\\",\\\"name\\\":\\\"collect\\\"}\",\n" +
                "    \"spark.sql.execution.id\": \"0\"\n" +
                "  }\n" +
                "}";
        try {
            JobStartEventLog log = ObjectConvertUtils.convertJsonToObject(str, JobStartEventLog.class).get();
            int a= 1;
            String [] strs = new String[2];
            strs[0] = "abc";
            strs[1] = "def";
            List list = Arrays.stream(strs).map(s -> null).collect(Collectors.toList());
            int b = 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
