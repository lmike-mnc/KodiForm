package kodiForm;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.ftpserver.FtpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import static kodiForm.FtpUtils.*;

public class Main extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());
    public static String DEF_USER = "root";
    public static String FTP_PORT = "1680";//CarbonCopy port - often it's free and not blocked (on Windows)
    private ObservableList<Device> deviceData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private ObservableList<Resource> resourceData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private Stage primaryStage = null;
    private static final String workingdir = Paths.get(".").toAbsolutePath().normalize().toString();
    private static Controller controller;

    public static String getWorkingkdir() {
        return workingdir;
    }
/*
    private static FtpThread ftpctl = null;
    private static Thread thFtp = null;
*/

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage primaryStage) {
        //Parent root = FXMLLoader.load(getClass().getResource("kodiForm.fxml"));
        initDevices();

        FXMLLoader loader = new FXMLLoader();
//        Parent root = loader.load(getClass().getResource("kodiForm.fxml"));
        loader.setLocation(Main.class.getResource("kodiForm.fxml"));

        Preferences userPrefs = Preferences.userNodeForPackage(getClass());
        double x = userPrefs.getDouble("stage.x", 100);
        double y = userPrefs.getDouble("stage.y", 100);
        double w = userPrefs.getDouble("stage.width", 850);
        double h = userPrefs.getDouble("stage.height", 602);
        SshJClient.STORAGE_SSH = userPrefs.get("ssh.storage", "");
        SshJClient.KODI_FILTER = userPrefs.get("ssh.kodifilter", "(?i)ub1-[0-9]+");//"(?i)castrol-[0-9]+");//"(?i)ubsrv[0-9]+");
        JsonUtils.HTTP_PORT = userPrefs.get("http.port", "8090");
        FTP_PORT = userPrefs.get("ftp.port", "1680");

        LOG.info(String.format("Window Width: %s, Height %s", String.valueOf(w), String.valueOf(h)));

        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            LOG.error("failed!", e);//.printStackTrace();
        }
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Kodi Form");
        primaryStage.setScene(new Scene(root));
        primaryStage.setX(x);
        primaryStage.setY(y);
        primaryStage.setWidth(w);
        primaryStage.setHeight(h);

        primaryStage.show();

        controller = loader.getController();
        controller.setMainApp(this);
    }

    @Override
    public void stop() throws InterruptedException {
        LOG.info("Stoping application...");
        Preferences userPrefs = Preferences.userNodeForPackage(getClass());
        userPrefs.putDouble("stage.x", primaryStage.getX());
        userPrefs.putDouble("stage.y", primaryStage.getY());
        userPrefs.putDouble("stage.width", primaryStage.getWidth());
        userPrefs.putDouble("stage.height", primaryStage.getHeight());
        userPrefs.put("ssh.storage", SshJClient.STORAGE_SSH);
        userPrefs.put("ssh.kodifilter", SshJClient.KODI_FILTER);
        userPrefs.put("http.port", JsonUtils.HTTP_PORT);
        userPrefs.put("ftp.port", FTP_PORT);

        LOG.info(String.format("Width: %s, Height %s", String.valueOf(primaryStage.getWidth()), String.valueOf(primaryStage.getHeight())));
    }

/*
    private static class FtpThread implements Runnable{
        private static final Logger LOG = LoggerFactory.getLogger(new Throwable() .getStackTrace()[0].getClassName());
        FtpServer srv=null;

        @Override
        public void run() {
            srv=FtpUtils.startFtp(FTP_PORT);
            changeUser(ANON_USER,ANON_AUTHORITIES,"",workingdir);
        }

        public void terminate(){
            LOG.info("stoping FTP...");
            srv.stop();
            LOG.info(" FTP has stopped");
        }
    }
*/

    public static void main(String[] args) throws InterruptedException {
        FtpServer srv = null;
        try {
            LOG.info("Working Directory = " +
                    Paths.get(".").toAbsolutePath().normalize().toString());
            //http://stackoverflow.com/questions/10961714/how-to-properly-stop-the-thread-in-java
            if (args.length > 0) {
                DEF_USER = args[0];
            }
            srv = FtpUtils.startFtp(FTP_PORT);
            changeUser(ANON_USER, ANON_AUTHORITIES, "", workingdir);
            launch(args);
        } finally {
            LOG.info("stopping FTP...");
            srv.stop();
            srv = null;//garbage
            LOG.info(" FTP has stopped");
/*
            if (ftpctl != null) ftpctl.terminate();
            if (thFtp != null) thFtp.interrupt();
*/
            if (controller != null) controller.closeUtilThreads();
            controller = null;
        }
    }

    public ObservableList<Device> getDeviceData() {
        return deviceData;
    }

    public ObservableList<Resource> getResourceData() {
        return resourceData;
    }

    private void initDevices() {
//        deviceData.add(new Device("ub1-1","http://Castrol-000","/Public/Picturies"));
//        deviceData.add(new Device("Castrol-001","http://Castrol-001","/Public/Picturies"));
    }

}
