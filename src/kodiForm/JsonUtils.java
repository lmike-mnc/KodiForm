package kodiForm;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.FutureRequestExecutionService;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpRequestFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike on 12.11.15.
 */
class JsonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());
    public static String HTTP_PORT = "";
    public static final String PLID_MOV = "1";
    public static final String PLID_PIC = "2";
    public static final String JSON_FULLSCREEN = "{\"id\":$id,\"jsonrpc\":2.0,\"method\":\"GUI.SetFullscreen\",\"params\":{\"fullscreen\":true}}";
    public static final String JSON_PL_STATUS = "{\"id\":$id,\"jsonrpc\":\"2.0\",\"method\":\"Player.GetItem\",\"params\":{\"playerid\":$PLID}}";
    public static final String JSON_STOP = "{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"Player.Stop\",\"params\":{\"playerid\":$PLID}}";
    public static final String JSON_PL_CLEAR = "{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"Playlist.Clear\",\"params\":{\"playlistid\":$PLID}}";
    public static final String JSON_PL_ADD = "{\"id\":$id,\"jsonrpc\":\"2.0\",\"method\":\"Playlist.Add\",\"params\":{\"playlistid\":$PLID,\"item\": {\"file\":\"$URI\"}}}";
    public static final String JSON_PL = "{\"id\":$id,\"jsonrpc\":\"2.0\",\"method\":\"Player.Open\",\"params\":{\"item\": {\"playlistid\":$PLID},\"options\":{\"repeat\":\"all\"}}}";
    /*
        public static final String JSON_CLEAR_MOV = JSON_PL_CLEAR.replace("$PLID", PLID_MOV);
        public static final String JSON_CLEAR_PIC = JSON_PL_CLEAR.replace("$PLID", PLID_PIC);
        public static final String JSON_PL_MOV_ADD = JSON_PL_ADD.replace("$PLID", PLID_MOV);
        public static final String JSON_PL_PIC_ADD = JSON_PL_ADD.replace("$PLID", PLID_PIC);
    */
/*
    public static String JSON_PL_MOV = JSON_PL.replace("$PLID", PLID_MOV);
    public static String JSON_PL_PIC = JSON_PL.replace("$PLID", PLID_PIC);
    public static String JSON_STOP_MOV = JSON_STOP.replace("$PLID", PLID_MOV);
    public static String JSON_STOP_PIC = JSON_STOP.replace("$PLID", PLID_PIC);
*/
    public static final String JSON_PIC = "{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"Player.Open\",\"params\":{\"item\":{\"path\":\"$URI\",\"random\":false}}}";

    public static String postRequest(String host, String protocol, String url, String data) {
        HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead
        String res = null;
        try {
            HttpPost request = new HttpPost(protocol + "://" + host + url);
//            LOG.info("request:{}",request.toString());
            LOG.info("data:{}", data);
            StringEntity params = new StringEntity(data);
            request.addHeader("content-type", "application/json");//"application/x-www-form-urlencoded");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            // handle response here...
            if (response.getStatusLine().getStatusCode() == 200) {
                res = org.apache.http.util.EntityUtils.toString(response.getEntity());
            } else {
                res = response.getStatusLine().getReasonPhrase();
            }
        } catch (Exception ex) {
            // handle exception here
            LOG.error("fail!", ex);
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        // the simplest way to create a HttpAsyncClientWithFuture
        HttpClient httpclient = HttpClientBuilder.create()
                .setMaxConnPerRoute(5)
                .setMaxConnTotal(5).build();
        ExecutorService execService = Executors.newFixedThreadPool(5);
        try (FutureRequestExecutionService requestExecService = new FutureRequestExecutionService(
                httpclient, execService)) {
            // Because things are asynchronous, you must provide a ResponseHandler
            ResponseHandler<Boolean> handler = response -> {
                // simply return true if the status was OK
                LOG.info(org.apache.http.util.EntityUtils.toString(response.getEntity()));
                return response.getStatusLine().getStatusCode() == 200;
            };

            // Simple request ...
            HttpGet request1 = new HttpGet("http://httpbin.org/get");
            HttpRequestFutureTask<Boolean> futureTask1 = requestExecService.execute(request1,
                    HttpClientContext.create(), handler);
            Boolean wasItOk1 = futureTask1.get();
            System.out.println("It was ok? " + wasItOk1);

            // Cancel a request
            try {
                HttpGet request2 = new HttpGet("http://httpbin.org/get");
                HttpRequestFutureTask<Boolean> futureTask2 = requestExecService.execute(request2,
                        HttpClientContext.create(), handler);
                futureTask2.cancel(true);
                Boolean wasItOk2 = futureTask2.get();
                System.out.println("It was cancelled so it should never print this: " + wasItOk2);
            } catch (CancellationException e) {
                System.out.println("We cancelled it, so this is expected");
            }

            // Request with a timeout
            HttpGet request3 = new HttpGet("http://httpbin.org/get");
            HttpRequestFutureTask<Boolean> futureTask3 = requestExecService.execute(request3,
                    HttpClientContext.create(), handler);
            Boolean wasItOk3 = futureTask3.get(10, TimeUnit.SECONDS);
            System.out.println("It was ok? " + wasItOk3);

            FutureCallback<Boolean> callback = new FutureCallback<Boolean>() {
                @Override
                public void completed(Boolean result) {
                    System.out.println("completed with " + result);
                }

                @Override
                public void failed(Exception ex) {
                    System.out.println("failed with " + ex.getMessage());
                }

                @Override
                public void cancelled() {
                    System.out.println("cancelled");
                }
            };

            // Simple request with a callback
            HttpGet request4 = new HttpGet("http://httpbin.org/get");
            // using a null HttpContext here since it is optional
            // the callback will be called when the task completes, fails, or is cancelled
            HttpRequestFutureTask<Boolean> futureTask4 = requestExecService.execute(request4,
                    HttpClientContext.create(), handler, callback);
            Boolean wasItOk4 = futureTask4.get(10, TimeUnit.SECONDS);
            System.out.println("It was ok? " + wasItOk4);
        }
    }

}
