package kodiForm;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.RemoteResourceFilter;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike on 16.11.15.
 */
public class SshJClient implements SshClient {
    static final String CMD_HOSTNAME = "hostname";
    static final String CMD_WAKEUP = "systemctl restart kodi";//'"'&"echo 'on 0' | cec-client -s -d 1"&'"';
    static final String CMD_REBOOT = "reboot";
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());

    private static final String BASH_JSON_TEMPL = "curl --header \"Content-Type: application/json\" --data $JSON \"http://localhost/jsonrpc\"";
    private static String BASH_FULLSCREEN = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_FULLSCREEN);

    private static String BASH_PL_CLEAR = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_PL_CLEAR);
/*
    private static String BASH_PL_MOV_CLEAR = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_CLEAR_MOV);
    private static String BASH_PL_PIC_CLEAR = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_CLEAR_PIC);
*/

    private static String BASH_PL_ADD = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_PL_ADD);
/*
    private static String BASH_PL_MOV_ADD = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_PL_MOV_ADD);
    private static String BASH_PL_PIC_ADD = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_PL_PIC_ADD);
*/

    private static String BASH_PL = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_PL);
    private static String BASH_PIC = BASH_JSON_TEMPL.replace("$JSON", JsonUtils.JSON_PIC);

    private static final int TIMEOUT_CMD = 5000;
    private static final int TIMEOUT_SSH = 5000;
    private String user;
    private String password;
    private Path privateKey;
    private Path knownHosts;
    private SSHClient sshClient;
    public static String STORAGE_SSH = "";
    public static String KODI_FILTER = "";

    @Override
    public void authUserPassword(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public void authUserPublicKey(String user, Path privateKey) {
        this.user = user;
        this.privateKey = privateKey;
    }

    private void authUserPublicKey(String user) {
        this.user = user;
    }

    @Override
    public void setKnownHosts(Path knownHosts) {
        this.knownHosts = knownHosts;
    }

    @Override
/*
    public void connect(String host) throws IOException {
        sshClient = new SSHClient();
        if (knownHosts == null) {
            sshClient.loadKnownHosts();
        } else {
            sshClient.loadKnownHosts(knownHosts.toFile());
        }

        sshClient.connect(host);

        if (privateKey != null) {
            sshClient.authPublickey(user, privateKey.toString());
        } else if (password != null) {
            sshClient.authPassword(user, password);
        } else {
            throw new RuntimeException("Either privateKey nor password is set. Please call one of the auth method.");
        }
    }

*/
    public void connect(String host, boolean... trusted) throws IOException {
        boolean trust = trusted.length > 0 && trusted[0];
        sshClient = new SSHClient();
        sshClient.setConnectTimeout(TIMEOUT_SSH);
        if (knownHosts == null) {
            if (trust) {
                sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            } else {
                sshClient.loadKnownHosts();
            }
        } else {
            sshClient.loadKnownHosts(knownHosts.toFile());
        }

        sshClient.connect(host);

        if (privateKey != null) {
            sshClient.authPublickey(user, privateKey.toString());
        } else if (password != null) {
            sshClient.authPassword(user, password);
        } else {
            sshClient.authPublickey(user);
            //throw new RuntimeException("Either privateKey nor password is set. Please call one of the auth method.");
        }
    }

    @Override
    public void disconnect() {
        try {
            close();
        } catch (Exception ex) {
            // Ignore because disconnection is quietly
        }
    }

    @Override
    public void download(String remotePath, Path local) throws IOException {
        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.get(remotePath, new FileSystemFile(local.toFile()));
        }
    }

    @Override
    public void upload(Path local, String remotePath) throws IOException {
        try (SFTPClient sFTPClient = sshClient.newSFTPClient()) {
            sFTPClient.put(new FileSystemFile(local.toFile()), remotePath);
        }
    }

    @Override
    public void move(String oldRemotePath, String newRemotePath) throws IOException {
        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.rename(oldRemotePath, newRemotePath);
        }
    }

    @Override
    public void copy(String oldRemotePath, String newRemotePath) throws IOException {
        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.put(oldRemotePath, newRemotePath);
        }
    }

    @Override
    public void delete(String remotePath) throws IOException {
        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.rm(remotePath);
        }
    }

    @Override
    public boolean fileExists(String remotePath) throws IOException {
        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            return sftpClient.statExistence(remotePath) != null;
        }
    }

    @Override
    public List<String> listChildrenNames(String remotePath) throws IOException {
        return listChildrenNamesByFilter(remotePath, null);
    }

    @Override
    public List<String> listChildrenFolderNames(String remotePath) throws IOException {
        return listChildrenNamesByFilter(remotePath, RemoteResourceInfo::isDirectory);
    }

    @Override
    public List<String> listChildrenFileNames(String remotePath) throws IOException {
        return listChildrenNamesByFilter(remotePath, RemoteResourceInfo::isRegularFile);
    }

    private List<String> listChildrenNamesByFilter(String remotePath, RemoteResourceFilter remoteFolderResourceFilter) throws IOException {
        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            List<String> children = new ArrayList<>();
            List<RemoteResourceInfo> childrenInfos = sftpClient.ls(remotePath, remoteFolderResourceFilter);
            childrenInfos.stream().forEach((childInfo) -> children.add(childInfo.getName()));
            return children;
        }
    }

    @Override
    public String execute(String command) throws IOException {
        String res = null;
        InputStream is;
        try (Session session = sshClient.startSession()) {
            Session.Command cmd = session.exec(command);
            cmd.join(TIMEOUT_CMD, TimeUnit.MILLISECONDS);
            Integer exitStatus = cmd.getExitStatus();
            is = cmd.getInputStream();
            res = IOUtils.readFully(is).toString();
        }
        return res;
    }

    @Override
    public void close() throws IOException {
        sshClient.close();
    }

    public String uploadAll(String host, String path2remote, String user, String... auth) throws IOException {
        SshJClient ssh = new SshJClient();
        try {
            if (auth.length == 0) {
                ssh.authUserPublicKey(user);
            } else if (auth[0].contains(File.separator)) {
                ssh.authUserPublicKey(user, Paths.get(auth[0]));
            } else {
                ssh.authUserPassword(user, auth[0]);
            }
            ssh.connect(host, true);
            String[] path = path2remote.split(":");

            ssh.upload(FileSystems.getDefault().getPath(path[0]), path[1]);
        } finally {
            ssh.close();
        }
        return "";
    }

    public String launch(String host, String command, String user, String... auth) throws IOException {
        SshJClient ssh = new SshJClient();
        if (auth.length == 0) {
            ssh.authUserPublicKey(user);
        } else if (auth[0].contains(File.separator)) {
            ssh.authUserPublicKey(user, Paths.get(auth[0]));
        } else {
            ssh.authUserPassword(user, auth[0]);
        }
        ssh.connect(host, true);
        try {
//            ssh.upload(Paths.get("/home/mike/tmp.org/log"),"./"); //put whole dir to remote user home
            return ssh.execute(command);
        } finally {
            ssh.close();
        }
    }

    public static void main(String[] args) {
        String result = null;
        SshJClient ssh = new SshJClient();
        try {
            if (args.length < 3) {
                LOG.error("too few Arguments:" + Arrays.toString(args));

                LOG.error("Usage: java -cp <all necessary jars> kodiForm.SshUtils host command user [path to key|password]");
                return;
            } else if (args.length > 3) {
                result = ssh.launch(args[0], args[1], args[2], Arrays.copyOfRange(args, 3, args.length));
            } else {
                ssh.uploadAll(args[0], "src:./", args[2]);
                //result = ssh.launch(args[0], args[1], args[2]);
            }
            LOG.info("Result: " + result);
        } catch (IOException e) {
            LOG.error("fail!", e);
        }
    }
}
