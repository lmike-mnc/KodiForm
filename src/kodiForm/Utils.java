package kodiForm;

import java.util.Scanner;
import java.util.StringJoiner;

/**
 * Created by mike on 29.10.15.
 */
public class Utils {
    public static String runSystemCommand(String command) {
        StringJoiner joiner = new StringJoiner("\n");
        try {
            Process p = Runtime.getRuntime().exec(command);
            Scanner scan = new Scanner(p.getInputStream());
            String s = "";
            while (scan.hasNextLine()) {
                s = scan.nextLine();
                System.out.println(s);
                joiner.add(s);
            }

/*            BufferedReader inputStream = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            // reading output stream of the command
            while ((s = inputStream.readLine()) != null) {
                System.out.println(s);joiner.add(s);
            }
*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        return joiner.toString();
    }

    public static void main(String args[]) {
        Utils p = new Utils();
        p.ping("127.0.0.1");
    }

    public String ping(String arg) {
        System.out.println(System.getProperty("os.name"));
        if (System.getProperty("os.name").contains("Windows")) {
            return runSystemCommand("ping -t 1 " + arg);
        } else {
            return runSystemCommand("ping -c 1 " + arg);
        }
    }
}
