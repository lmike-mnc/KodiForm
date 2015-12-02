package kodiForm;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 12.11.15.
 */
public class FtpUtils {
    private static final String resPath = "./res";
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());
    ;
    public static final List<Authority> ADMIN_AUTHORITIES = new ArrayList();
    public static final List<Authority> ANON_AUTHORITIES = new ArrayList();
    private static final FtpServerFactory serverFactory = new FtpServerFactory();
    static String ANON_USER = "anonymous";

    static {
        ADMIN_AUTHORITIES.add(new WritePermission());
        //ANON_AUTHORITIES.add(new ConcurrentLoginPermission(20, 2));
        //ANON_AUTHORITIES.add(new TransferRatePermission(4800, 4800));
    }

    public static FtpServer startFtp(String port) {
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(Integer.parseInt(port));
        serverFactory.addListener("default", listenerFactory.createListener());
        serverFactory.setUserManager(createAnonymous());

        FtpServer server = null;
        try {
            server = serverFactory.createServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

// start the server
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return server;
    }

    public static UserManager createFtpUser(String userName, List<Authority> authority, String userPassword) {
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        File tmp = new File(resPath + "/ftpusers.properties");
        LOG.info("resource PATH:{}", tmp.getAbsolutePath());
        tmp.getParentFile().mkdirs();
        try {
            tmp.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        userManagerFactory.setFile(tmp);
        userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());
        UserManager userManager = userManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        try {
            if (!userManager.doesExist(userName)) {
                LOG.info("Creating user : " + userName);
                user = new BaseUser();
                user.setName(userName);
                user.setPassword(userName);
                user.setEnabled(true);
                user.setAuthorities(authority);
                tmp = new File(resPath + "/home/" + userName);
                tmp.mkdirs();
                user.setHomeDirectory(tmp.getPath());
                LOG.info("Home directory : " + tmp.getPath());
                user.setMaxIdleTime(0);
                userManager.save(user);
            }
        } catch (FtpException e) {
            e.printStackTrace();
        }
        return userManager;
    }

    public static UserManager changeUser(String userName, List<Authority> authority, String... userData) {
        UserManager userManager = serverFactory.getUserManager();
        try {
            if (userManager.doesExist(userName)) {
                BaseUser user = new BaseUser();
                userManager.delete(userName);
                user.setName(userName);
                user.setAuthorities(authority);
                if (userData.length > 1) {
                    user.setHomeDirectory(userData[1]);
                }
                if (userData.length > 0) {
                    user.setPassword(userData[0]);
                }
                userManager.save(user);
            }
        } catch (FtpException e) {
            e.printStackTrace();
        }
        return userManager;
    }

    public static UserManager createAnonymous() {
        return createFtpUser(ANON_USER, ANON_AUTHORITIES, "");
    }

}
