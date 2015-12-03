package kodiForm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by mike on 27.11.15.
 */
//http://winterbe.com/posts/2015/04/07/java8-concurrency-tutorial-thread-executor-examples/
class Launcher {
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());
    public static final String JSONRPC = "/jsonrpc";
    public static final String HTTP_PROTO = "http";
    static final String FTP_URI = "ftp://";

    static Callable<String> callable(String result, long sleepSeconds) {
        return () -> {
            TimeUnit.SECONDS.sleep(sleepSeconds);
            return result;
        };
    }

    static Callable<String> callableRequest(String host, String protocol, String url, String data) {
        return () -> {
            String res = null;
            return JsonUtils.postRequest(host, protocol, url, data);
        };
    }

    static Callable<String> callableRequestList(String host, String protocol, String url, List<String> list) {
        return () -> {
            String res = null;
            return list.stream().map(d -> JsonUtils.postRequest(host, protocol, url, d)).collect(Collectors.joining("\n"));

        };
    }

    public static List<Callable<String>> callablesJson(String host, List<String> uriList, String... params) {
        return
                Arrays.asList(
                        callableJsonPL(host, uriList, params)
                );
    }

    public static Callable<String> callableJsonPL(String host, List<String> uriList, String... params) {
        String hostURI = host, defPort = ":80";
        String plid = "1";
        if (params.length > 0) {
            plid = params[0];
            if (params.length > 1) defPort = params[1];
        }
        hostURI += ":" + defPort;
        //List<Callable<String>> callables =
/*
        List<Callable<String>> callables=
                Arrays.asList(
                        callableRequestList(finalHostURI,"http","/jsonrpc",
                                Arrays.asList(
                                        JsonUtils.JSON_CLEAR_PIC,

                                    JsonUtils.JSON_PL_PIC
                                            .replace("$id","4")
                            )
                        )
                    );
                .flatMap(List::stream)

*/

        AtomicInteger cnt = new AtomicInteger(1);
        final String finalPlid = plid;
        List<String> list = uriList.stream()
                .map(e ->
                        JsonUtils.JSON_PL_ADD
                                .replace("$PLID", finalPlid)
                                .replace("$URI", e)
                                .replace("$id", String.valueOf(cnt.incrementAndGet())))
                .collect(Collectors.toList());
        list.add(0, JsonUtils.JSON_PL_CLEAR
                .replace("$PLID", plid)
        );
        list.add(JsonUtils.JSON_PL
                .replace("$PLID", plid)
                .replace("$id", String.valueOf(cnt.incrementAndGet())));
        list.add(JsonUtils.JSON_PL_STATUS
                .replace("$PLID", plid)
                .replace("$id", String.valueOf(cnt.incrementAndGet()))
        );

        return callableRequest(hostURI, HTTP_PROTO, JSONRPC, list.toString());
    }

    public static void main(String args[]) {
        ExecutorService executor = Executors.newCachedThreadPool();
        //newWorkStealingPool();
/*
        List<Callable<String>> callables = Arrays.asList(
                callable("task1", 2),
                callable("task2", 1),
                callable("task3", 3));
*/
        LOG.info("*push json");
/*
        List<Callable<String>> callables = Arrays.asList(
                callableRequestList("localhost:8090","http","/jsonrpc",
                        Arrays.asList(
                                JsonUtils.JSON_CLEAR_PIC,
                                JsonUtils.JSON_PL_PIC_ADD
                                        .replace("$URI","ftp://localhost:1680/DSC_1971.jpg")//ftp://localhost:1680/Картинки/карлсон.png")
                                        .replace("$id","2"),
                                JsonUtils.JSON_PL_PIC_ADD
                                        .replace("$URI","ftp://localhost:1680/DSC_1972.jpg")//ftp://localhost:1680/Картинки/карлсон.png")
                                        .replace("$id","3"),
                                JsonUtils.JSON_PL_PIC
                                        .replace("$id","4")
//                                JsonUtils.JSON_PIC.replace("$URI","ftp://localhost:1680/")
                        )
                )
        );
*/
        try {
            executor.invokeAll(//callables
                    callablesJson(
                            "localhost",
                            Arrays.asList(
                                    "ftp://localhost:1680/DSC_1971.jpg",
                                    "ftp://localhost:1680/DSC_1972.jpg"
                            )
                            , JsonUtils.PLID_PIC, "8090"), 1000, TimeUnit.MILLISECONDS
            )
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            future.cancel(true);
                            return null;
//                            throw new IllegalStateException(e);
//                            e.printStackTrace();
                        }
                    })
                    .forEach(System.out::println)
            ;
        } catch (InterruptedException e) {
            //e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
        //executor.shutdownNow();
    }

    private BigDecimal calculate() {
        return new BigDecimal(0);
    }

    void test() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        //final Future<BigDecimal> future = executor.submit(this::calculate);
        final CompletableFuture<BigDecimal> future = CompletableFuture.supplyAsync(this::calculate, executor);
    }
}
