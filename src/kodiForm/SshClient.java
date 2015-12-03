package kodiForm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
//https://github.com/sparsick/comparison-java-ssh-libs/blob/master/src/main/java/com/github/sparsick/ssh4j/SshClient.java

interface SshClient extends AutoCloseable {

    void authUserPassword(String user, String password);

    void authUserPublicKey(String user, Path privateKey);

    void setKnownHosts(Path knownHosts);

    void connect(String host, boolean... trusted) throws IOException;

    void disconnect();

    void download(String remotePath, Path local) throws IOException;

    void upload(Path local, String remotePath) throws IOException;

    void move(String oldRemotePath, String newRemotePath) throws IOException;

    void copy(String oldRemotePath, String newRemotePath) throws IOException;

    void delete(String remotePath) throws IOException;

    boolean fileExists(String remotePath) throws IOException;

    List<String> listChildrenNames(String remotePath) throws IOException;

    List<String> listChildrenFolderNames(String remotePath) throws IOException;

    List<String> listChildrenFileNames(String remotePath) throws IOException;

    String execute(String command) throws IOException;

}