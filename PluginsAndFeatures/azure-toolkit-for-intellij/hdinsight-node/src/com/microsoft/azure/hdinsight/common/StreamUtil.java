package com.microsoft.azure.hdinsight.common;

import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class StreamUtil {

    public static String getResultFromInputStream(InputStream inputStream) throws IOException {
//      change string buffer to string builder for thread-safe
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }

    public static HttpResponse getResultFromHttpResponse(CloseableHttpResponse response) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        String reason = response.getStatusLine().getReasonPhrase();
        HttpEntity entity = response.getEntity();
        try (InputStream inputStream = entity.getContent()) {
            String response_content = getResultFromInputStream(inputStream);
            return new HttpResponse(code, response_content, new HashMap<String, List<String>>(), reason);
        }
    }

    public static File getResourceFile(String resource) throws IOException {
        File file = null;
        URL res = streamUtil.getClass().getResource(resource);

        if (res.toString().startsWith("jar:")) {
            InputStream input = null;
            OutputStream out = null;

            try {
                input = streamUtil.getClass().getResourceAsStream(resource);
                file = File.createTempFile(String.valueOf(new Date().getTime()), ".tmp");
                out = new FileOutputStream(file);

                int read;
                byte[] bytes = new byte[1024];

                while ((read = input.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
            } finally {
                if (input != null) {
                    input.close();
                }

                if (out != null) {
                    out.flush();
                    out.close();
                }

                if (file != null) {
                    file.deleteOnExit();
                }
            }

        } else {
            file = new File(res.getFile());
        }

        return file;
    }

    public static ImageIcon getImageResourceFile(String resourcePath) {
        URL url = classLoader.getResource(resourcePath);

        if(url != null) {
            return new ImageIcon(url);
        } else {
            return null;
        }
    }

    private static StreamUtil streamUtil = new StreamUtil();
    private static ClassLoader classLoader = streamUtil.getClass().getClassLoader();
}
