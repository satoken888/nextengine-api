package jp.co.kawakyo.nextengineapi.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.stream.Collectors;

public class LineConnectUtils {
    
    /**
     * LINE Notify経由でメッセージ送信の処理
     * @param message 送信メッセージ
     * @param token 送信先のLINE Notify トークン
     */
    public static void notifySales(String message,String token) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://notify-api.line.me/api/notify");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.addRequestProperty("Authorization", "Bearer " + token);
            try (OutputStream os = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(os)) {
                writer.append("message=").append(URLEncoder.encode(message, "UTF-8")).flush();
                try (InputStream is = connection.getInputStream();
                     BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                    String res = r.lines().collect(Collectors.joining());
                    if (!res.contains("\"message\":\"ok\"")) {
                        System.out.println(res);
                        System.out.println("なんか失敗したっぽい");
                    }
                }
            }
        } catch (Exception ignore) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
