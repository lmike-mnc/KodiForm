package kodiForm;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by mike on 12.11.15.
 */
// command to restart kodi: "systemctl restart kodi"
// command to pinglist external storage dir: "mount | grep /media/|head -1 | cut -d ' ' -f 3-| { read var; echo ${var%%' type'*}; }"
class SshUtils {
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());

    public static String launch(String host, String command, String user, String... auth) throws IOException {
        SSHClient ssh = new SSHClient();
        //http://stackoverflow.com/a/15800383
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        Session session = null;
        StringBuilder buf = new StringBuilder();
        String response = null;
        //ssh.loadKnownHosts();
        try {
            ssh.connect(host);
            if (auth.length == 0) {
                ssh.authPublickey(user);
            } else if (auth[0].contains(File.separator)) {
                ssh.authPublickey(user, auth);
            } else {
                ssh.authPassword(user, auth[0]);
            }
            //ssh.authPublickey("user","path2id");
            session = ssh.startSession();
            Session.Command cmd = session.exec(command);
/*
            BufferedReader in = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                buf.append(line);
                LOG.info("Result line: " + line);
            }
            response=buf.toString();
*/
            //cmd.join(timeout, TimeUnit.MILLISECONDS);
            response = IOUtils.readFully(cmd.getInputStream()).toString();
        } catch (UserAuthException | TransportException | ConnectionException e) {
            e.printStackTrace();
        } finally {
            if (session != null) session.close();
            ssh.disconnect();
        }
        return response;
    }

    public static void main(String[] args) {
        String result = null;
        try {
            if (args.length < 3) {
                System.err.println("too few Arguments:" + Arrays.toString(args));

                System.err.println("Usage: java -cp <all necessary jars> kodiForm.SshUtils host command user [path to key|password]");
                return;
            } else if (args.length > 3) {
                result = launch(args[0], args[1], args[2], Arrays.copyOfRange(args, 3, args.length));
            } else {
                result = launch(args[0], args[1], args[2]);
            }
            LOG.info("Result: " + result);
        } catch (IOException e) {
            LOG.error(Arrays.asList(e.getStackTrace()).stream().map(StackTraceElement::toString).collect(Collectors.joining("\n")));
        }
    }
}
