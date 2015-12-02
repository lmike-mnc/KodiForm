package kodiForm;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import org.fourthline.cling.UpnpService;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());

    private static final long TIMEOUT_SSH = 5000;//timeout for ssh checking threads (service.submit())
    private static final String SEP_PATH = ";";
    private static final FileChooser.ExtensionFilter imgFilter = new FileChooser.ExtensionFilter("Images", "*.PNG", "*.png", "*.JPG", "*.jpg");
    private static final FileChooser.ExtensionFilter movFilter = new FileChooser.ExtensionFilter("Movies", "*.AVI", "*.avi", "*.M4V", "*.m4v");
    private static final String MOV_MATCHES = "(?i).+\\.(avi|m4v)(;.+)?";
    private static final String IMG_MATCHES = "(?i).+\\.(jpg|png)(;.+)?";
    private static final String PL_MATCHES = "(?i).+\\.(avi|m4v|jpg|png)(;.+)?";

    @FXML
    private Button btnAdd;
    @FXML
    private Button btnDel;
    @FXML
    private Button btnAssign;
    @FXML
    private Button btnPlayAll;
    @FXML
    private Button btnPlay;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnWakeUp;
    @FXML
    private Button btnReboot;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnCheck;
    @FXML
    private CheckBox chkPlayList;
    @FXML
    private CheckBox chkMovies;
    @FXML
    private TableView<Device> ctlDevices;
    @FXML
    private TableColumn<Device, String> colName;
    @FXML
    private TableColumn<Device, String> colURI;
    @FXML
    private TableColumn<Device, String> colResource;
    @FXML
    private TableView ctlResources;
    @FXML
    private TableColumn<Resource, String> colResourceOrg;
    @FXML
    private TextArea ctlMsg;

    private Thread thPing; //search hosts on network by ping an UPnPScan
    private Thread thCheck; //check host availability (create "List" for thLaunch)
    private Thread thLaunch; //launch "task" for "List" elements
    Ping ping = new Ping();
    PingListener listener = new PingListener();
    //http://habrahabr.ru/post/116363/
    ScheduledExecutorService serviceSch = Executors.newSingleThreadScheduledExecutor();
    //    ExecutorService service = Executors.newCachedThreadPool();//Executors.newSingleThreadExecutor(); //.newFixedThreadPool(100);
    /*
            new ThreadPoolExecutor(5, Integer.MAX_VALUE,
            1L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());//Executors.newCachedThreadPool();
*/
    private static final long TIMEOUT_PING = 500;
    private static final long TIMEOUT_SCAN = 60000;//60000;

    private Main mainApp;
    private int ifaceNum = -1;
    private UpnpService upnpService;
    private Runnable scanTask;
    private ExecutorService executor = Executors.newCachedThreadPool();
//https://docs.oracle.com/javase/tutorial/collections/implementations/wrapper.html

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        /*btnAdd.setOnAction(event->add());
        btnDel.setOnAction(event->del());
        btnCheck.setOnAction(event->check());
        */
        ctlDevices.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        colName.setCellValueFactory(cellData -> cellData.getValue().devNameProperty());
        colURI.setCellValueFactory(cellData -> cellData.getValue().devURIProperty());
        colResource.setCellValueFactory(cellData -> cellData.getValue().devResourceProperty());

        ctlResources.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        colResourceOrg.setCellValueFactory(cellData -> cellData.getValue().resourceProperty());
        chkPlayList.setSelected(true);

        ctlMsg.appendText(mainApp.getWoringkdir() + "\n");
    }

    private void openFile(File file) {
    }

    public ObservableList<Device> getSelectedDev() {
        return ctlDevices.getSelectionModel().getSelectedItems();
    }

    public ObservableList<Device> getSelectedRes() {
        return ctlResources.getSelectionModel().getSelectedItems();
    }

    public ArrayList<String> getSelectedDevString() {
        ArrayList<String> arr = new ArrayList<>();
        //ctlDevices.requestFocus();
        //ctlDevices.focusModelProperty().get().focusBelowCell();
        //ctlDevices.getSelectionModel().getSelectedIndices().forEach(idx->arr.add(String.valueOf(mainApp.getDeviceData().get(idx))));
        ObservableList<Device> list = ctlDevices.getSelectionModel().getSelectedItems();
        int idx = ctlDevices.getSelectionModel().getSelectedIndex();
        //LOG.info("Selected Index:" +idx);
        if (list.size() >= 0 && idx >= 0) {
            if (list.size() < 2) {
                arr.add(String.valueOf(ctlDevices.getSelectionModel().getSelectedItem()));
            } else {
                list.forEach(dev -> arr.add(String.valueOf(dev)));
            }
        }
        return arr;
    }

    public ArrayList<String> getSelectedResString() {
        ArrayList<String> arr = new ArrayList<>();
        //ctlDevices.requestFocus();
        //ctlDevices.focusModelProperty().get().focusBelowCell();
        //ctlDevices.getSelectionModel().getSelectedIndices().forEach(idx->arr.add(String.valueOf(mainApp.getDeviceData().get(idx))));
        ObservableList<Resource> list = ctlResources.getSelectionModel().getSelectedItems();
        int idx = ctlResources.getSelectionModel().getSelectedIndex();
        //LOG.info("Selected Index:" +idx);
        if (list.size() >= 0 && idx >= 0) {
            if (list.size() < 2) {
                String s = String.valueOf(ctlResources.getSelectionModel().getSelectedItem());
                arr.add(s);
            } else {
                list.forEach(r -> {
                    String s = String.valueOf(r);
                    arr.add(s);
                });
            }
        }
        return arr;
    }

    public void add() {
        List<String> res = null;
        if (chkPlayList.isSelected()) {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(mainApp.getWoringkdir()));
            if (chkMovies.isSelected()) {
                //fileChooser.setSelectedExtensionFilter(movFilter);
                fileChooser.getExtensionFilters().add(movFilter);
            } else {
                //fileChooser.setSelectedExtensionFilter(imgFilter);
                fileChooser.getExtensionFilters().add(imgFilter);
            }
            List<File> list =
                    fileChooser.showOpenMultipleDialog(mainApp.getPrimaryStage());
            if (list != null) {
                res = list.stream()
                        .filter(f -> f.getAbsolutePath().startsWith(mainApp.getWoringkdir()))
                        .map(f -> {
                            String path = f.getAbsolutePath();
                            return path.substring(path.indexOf(mainApp.getWoringkdir()) + mainApp.getWoringkdir().length());
                        })
                        .collect(Collectors.toList());
                //toArray(String[]::new);
/*
                for (File file : pinglist) {
                    openFile(file);
                }
*/
            }
        } else {
            final DirectoryChooser directoryChooser =
                    new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(mainApp.getWoringkdir()));
            final File selectedDirectory =
                    directoryChooser.showDialog(mainApp.getPrimaryStage());
            if (selectedDirectory != null) {
                res = new ArrayList<String>();
                if (selectedDirectory.getAbsolutePath().startsWith(mainApp.getWoringkdir())) {
                    String path = selectedDirectory.getAbsolutePath() + "/";
                    res.add(path.substring(path.indexOf(mainApp.getWoringkdir()) + mainApp.getWoringkdir().length()));
                }
            }
        }
        if (res != null && res.size() > 0) {
            res.forEach(r -> mainApp.getResourceData().addAll(new Resource(r)));
        } else {
            alertInfo("Wrong choice", "You haven't selected resource\nor resource out of working directory scope");
        }
        //mainApp.getDeviceData().add(new Device("Castrol-002","http://Castrol-002","/Public/Picturies"));
    }

    public void alertInfo(String header, String msg) {
        //stackoverflow.com/questions/26341152/controlsfx-dialogs-deprecated-for-what
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle("Information");
        alert.setHeaderText(header);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void del() {
        mainApp.getResourceData().removeAll(getSelectedRes());
    }

    public void assign() {
        LOG.info("Assigning...");
        ObservableList<Device> devices = getSelectedDev();
        ArrayList<String> list = getSelectedResString();
        if (devices.size() == 0 || list.size() == 0) {
            alertInfo("Wrong selection", "You have to select devices\nand corresponding resources");
        }
        devices
                .stream()
                .forEach(dev -> {
                    LOG.info("Assign to Device:{}", dev.getDevName());
                    ctlMsg.appendText("Device:" + dev.getDevName() + "\n");
                    dev.setDevResource(
                            list.stream()
                                    .map(r -> {
                                        LOG.info("Assign:{}", r);
                                        ctlMsg.appendText("resource:" + r + "\n");
                                        return r;
                                    })
                                    .collect(Collectors.joining(SEP_PATH)));
                });
    }

    public void playAll() {
        ctlDevices.getSelectionModel().selectAll();
        play();
        ctlDevices.getSelectionModel().clearSelection();
    }

    public void play() {
        ctlMsg.setText("Trying 1 to playing...\n");
        List<Callable<String>> callables = getSelectedDev().stream()
                .filter(d -> !d.getDevResource().isEmpty())
                .map(d -> {
                    String sResources = d.getDevResource();
                    String saddr = null;
                    Callable<String> ret = null;
                    try {
                        saddr = NetUtils.accessibleAddresses(d.getDevURI()).get(0);
                        LOG.info("Resources:{}", d.getDevResource());

                        String resURI = Launcher.FTP_URI + saddr + ":" + mainApp.FTP_PORT;

                        LOG.info("accessible address:{}", saddr);
                        String plid = sResources.matches(MOV_MATCHES) ? JsonUtils.PLID_MOV : JsonUtils.PLID_PIC;
                        //play directory or playlist
                        if (!sResources.matches(PL_MATCHES)) {
                            ret = Launcher.
                                    callableRequest(
                                            d.getDevURI() + ((JsonUtils.HTTP_PORT.length() > 0) ? (":" + JsonUtils.HTTP_PORT) : "")
                                            , Launcher.HTTP_PROTO
                                            , Launcher.JSONRPC
                                            , Arrays.asList(
                                                    JsonUtils.JSON_PIC.replace("$URI", resURI + sResources)
                                                    , JsonUtils.JSON_PL_STATUS
                                                            .replace("$PLID", plid)
                                                            .replace("$id", "2")
                                            ).toString()
                                    );
                        } else {
                            final String finalSaddr = resURI + "$URI";//saddr;
                            ret = Launcher
                                    .callableJsonPL(
                                            d.getDevURI(),
                                            Arrays.asList(sResources.split(";")).stream()
                                                    .map(s -> {
                                                        //add to playlist all files
                                                        return finalSaddr.replace("$URI", s);
                                                    })
                                                    .collect(Collectors.toList()
                                                    )
                                            , plid
                                            , JsonUtils.HTTP_PORT
                                    );
                        }
                    } catch (SocketException e) {
                        LOG.error("get accessible fail!", e);//.printStackTrace();
                    }
                    return ret;
                }).collect(Collectors.toList());

/*
        for (String s : getSelectedDevString()) {
            ctlMsg.appendText(s + "\n");
        }
*/
        try {
            executor.invokeAll(callables
                    //, 1000, TimeUnit.MILLISECONDS
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
                    .forEach(ctlMsg::appendText
                            //System.out::println
                    )
            ;
        } catch (InterruptedException e) {
            //e.printStackTrace();
        } finally {
        }

    }

    public void stop() {
    }

    public void wakeUp() {
        getSelectedDev().forEach(d -> {
                    try {
                        String res = new SshJClient().launch(d.getDevURI(), SshJClient.CMD_WAKEUP, mainApp.DEF_USER);
                        LOG.info("Result:{}", res);
                    } catch (IOException e) {
                        LOG.error("IO error!", e);//.printStackTrace();
                    }
                }
        );
    }

    public void reboot() {
        getSelectedDev().forEach(d -> {
                    try {
                        String res = new SshJClient().launch(d.getDevURI(), SshJClient.CMD_REBOOT, mainApp.DEF_USER);
                        LOG.info("Result:{}", res);
                    } catch (IOException e) {
                        LOG.error("IO error!", e);//.printStackTrace();
                    }
                }
        );
    }

    public void save() {
    }

    public void check() {
        LOG.info("Checking...");
    }

    @FXML
    void setChkMovies() {
        if (chkMovies.isSelected()) {
            chkPlayList.setSelected(true);
        }
    }

    @FXML
    void setChkPlayList() {
        if (!chkPlayList.isSelected()) {
            chkMovies.setSelected(false);
        }

    }

    class PingListener extends Component {
        private final Map<String, String> chkDevMap = Collections.synchronizedMap(new HashMap<String, String>());
        private final List<InetSocketAddress> pinglist = Collections.synchronizedList(new ArrayList<InetSocketAddress>());

        @Subscription
        public void onString(String s, boolean avail) {
            LOG.info("My String: " + s);
            //System.out.println("String - " + s);
        }

        @Subscription
        public void onInetSocketAddress(InetSocketAddress addr, boolean avail) {
            String saddr = addr.getAddress().getHostAddress();
            //LOG.info("*test Address:" +saddr);
            if (avail) {
                if (!pinglist.contains(addr)) {
                    pinglist.add(addr);
                }
            } else {
                pinglist.remove(addr);
            }
/*
                try {
                    String ret=SshJClient.launch(saddr,CMD_HOSTNAME,DEF_USER).split("\n")[0]; //define hostname, if it's possible
                    if (ret.length()>0) {
                        LOG.info(String.format("*Address: %s; HostName: %s", saddr, ret));
                        chkDevMap.put(ret,saddr);
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                }
*/
        }

        synchronized String checkSshHost(InetSocketAddress addr) {
            String ret = "";
            try {
                // snip... piece of code
                String saddr = addr.getAddress().getHostAddress();
                SshJClient ssh = new SshJClient();
                LOG.info("*Checking {} host...", saddr);
                ret = ssh.launch(saddr, SshJClient.CMD_HOSTNAME, mainApp.DEF_USER).split("\n")[0]; //define hostname, if it's possible
                if (ret.length() > 0) {
                    LOG.info("*Address: {}; HostName: {}", saddr, ret);
                    //put in list all hosts has been connected with user and ID by ssh (see:SshJClient.launch)
                    if (ret.matches(SshJClient.KODI_FILTER)) chkDevMap.put(ret, saddr);
                }
            } catch (IOException e) {
                //e.printStackTrace();
            } finally {
                return ret;
            }
        }

/*
        void checkSshPool() {
            LOG.info("Iterating pinglist... , size:" + pinglist.size());
            Collection<Future<?>> futures = Collections.synchronizedList(new LinkedList<Future<?>>());

            pinglist.forEach(addr -> {
                futures.add(
                        service.submit(
                                new Callable<String>() {
                                    @Override
                                    public String call() {
                                        String res="";
                                        try {
                                            res=checkSshHost(addr);
                                            return res;
                                        }catch (Exception e){
                                            return res;
                                        }
                                    }

                                    public void run() {
                                        checkSshHost(addr);
                                    }
                                })
                );
            });
            LOG.info("*Awating for ssh check...");
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.info("service pool interrupting...");
                }finally {
                    f.cancel(true);
                }
            });
            LOG.info("*Exit checking");
        }
*/

        void checkSsh() {
/*
                pinglist.parallelStream().forEach(addr -> {
                    checkSshHost(addr);
                });
*/
            chkDevMap.clear();
            pinglist.forEach(addr -> {
                checkSshHost(addr);
            });
            LOG.info("*Exit checking");
            LOG.info("*Assign to DeviceData");
            ObservableList<Device> devdata = mainApp.getDeviceData();
            List<Device> list = new ArrayList<Device>();
            devdata
                    .forEach(d -> {
                        String devname = d.getDevName();
                        if (chkDevMap.get(devname) != null) {
                            d.setDevURI(chkDevMap.get(devname));
                            chkDevMap.remove(devname);
                            list.add(d);
                        }
                    });
            devdata.retainAll(list);
            LOG.info("*Add \"new\" DeviceData");
            chkDevMap.entrySet().forEach(d -> devdata.add(new Device(d.getKey(), d.getValue(), "")));
        }
    }

    /**
     * Set main application class
     *
     * @param mainApp
     */
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
        // Add observable pinglist data to the table
        ctlDevices.setItems(mainApp.getDeviceData());
        ctlResources.setItems(mainApp.getResourceData());


/*
        upnpService = new UpnpServiceImpl(new UPnPListener());
        STAllHeader stAllHeader=new STAllHeader();
*/
/*
        Runnable task= () -> {
            Ping ping = new Ping();
            PingListener listener = new PingListener();
            ping.add2broker(listener);
        };
*/
        ping.add2broker(listener);
/*
        scanTask=new Runnable() {
            @Override
            public void run() {
                //Thread.currentThread().setName("Ping");
                try {
//                            upnpService.getControlPoint().search(stAllHeader);//broadcast UPnP
                    ping.pingIface(ifaceNum, TIMEOUT_PING);
                    LOG.info("Waiting for processing...");
                    listener.checkSsh();
                    LOG.info("Timeout Scan {} ms...",TIMEOUT_SCAN);
                } catch (InterruptedException e) {
                    LOG.info("interrupted");
                    return;
                } catch (IOException e) {
                    LOG.error("IO fail!", e);
                    return;
                }
            }
        };
        serviceSch.scheduleWithFixedDelay(scanTask, 0, TIMEOUT_SCAN, TimeUnit.MILLISECONDS);
*/

        thPing = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("Ping");
/*
                        Ping ping=new Ping();
                        PingListener listener=new PingListener();
                        ping.add2broker(listener);
*/
                try {
                    while (true) {
                        // my code goes here
//                                    upnpService.getControlPoint().search(stAllHeader);//broadcast UPnP
                        ping.pingIface(ifaceNum, TIMEOUT_PING);
                        //launch result processing
                        LOG.info("Waiting for processing...");
                        listener.checkSsh();
                        LOG.info("Timeout Scan {} ms...", TIMEOUT_SCAN);
                        Thread.currentThread().sleep(TIMEOUT_SCAN);
//                                    {Launcher.main(new String[]{""});}
//                                Thread.currentThread().sleep(5000);
                    }
                } catch (InterruptedException e) {
                    LOG.info("Interrupted");
//                                e.printStackTrace();
                } catch (IOException e) {
                    LOG.error("IO fail!", e);
                    return;
                }
            }
        }
        );
        thPing.setDaemon(true);
        thPing.start();
    }

    /**
     * close all threads have been created
     */
    public void closeUtilThreads() throws InterruptedException {
        LOG.info("*Closing threads...");
//        service.shutdownNow();
        if (scanTask != null) {
            scanTask = null;
        }
        ping.removeFromBroker(listener);
        executor.shutdownNow();
        serviceSch.shutdownNow();
        if (thPing != null) thPing.interrupt();
        if (upnpService != null) upnpService.shutdown();
    }
}
