package kodiForm;

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.System.out;

/**
 * Created by mike on 29.10.15.
 */
public class NetUtils {
    private static int TIMEOUT = 500;
    private static short MASQ_LIIM = 20;//limit prefix masq len for restrict lage network scan
    private static final String resPath = "./res";
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());
    ;

    private static Predicate<NetworkInterface> notLoopback = iface -> {
        boolean ret = false;
        try {
            ret = !iface.isLoopback();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ret;
    };
    private static Predicate<NetworkInterface> notFake = iface -> {
        boolean ret = false;
        try {
            ret = !iface.isLoopback() & !iface.isPointToPoint()
                    & (Collections.list(iface.getInetAddresses()).size() > 0)
                    & !iface.isVirtual();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ret;
    };
    private static Predicate<InterfaceAddress> masq = addr -> {
        boolean ret = false;
        ret = addr.getNetworkPrefixLength() >= MASQ_LIIM;
        return ret;
    };
    private static Predicate<InterfaceAddress> ipv4 = addr -> {
        LOG.info("filter IPV4 for:" + addr.getAddress().getHostAddress());
        return addr.getAddress() instanceof Inet4Address;
    };

    //http://commons.apache.org/proper/commons-net/apidocs/org/apache/commons/net/util/SubnetUtils.html
    //http://stackoverflow.com/questions/2942299/converting-cidr-address-to-subnet-mask-and-network-address

    /**
     * @param address tested address
     * @param subnet  CIDR notation "192.168.0.1/16"
     * @return true if included
     */
    public static boolean checkIpBySubNet(String address, String subnet) {
        SubnetUtils utils = new SubnetUtils(subnet);
        LOG.info("Network:{}", utils.getInfo().getNetworkAddress());
        return utils.getInfo().isInRange(address);
    }

    /**
     * @param address    tested address
     * @param subnetaddr address from subnet (any known)
     * @param mask       mask
     * @return true if included
     */
    public static boolean checkIpBySubNet(String address, String subnetaddr, String mask) {
        SubnetUtils utils = new SubnetUtils(subnetaddr, mask);
        return utils.getInfo().isInRange(address);
    }

    /**
     * @param anyIp
     * @return list local interface addresses accessible for anyIp (within subnet)
     * @throws SocketException
     */
    public static List<String> accessibleAddresses(String anyIp) throws SocketException {
        return getIfacesList().stream()
                .filter(iface -> {
                    String cidr = String.format("%s/%s", iface.getAddress().getHostAddress()
                            , String.valueOf(iface.getNetworkPrefixLength()));
                    return checkIpBySubNet(anyIp, cidr);
                })
                .map(iface -> iface.getAddress().getHostAddress())
                .collect(Collectors.toList());
    }

    public static List<String> accessibleRange(String anyIp) {
        ArrayList<String> res = new ArrayList<String>();
        InetAddress[] addrs = null;
        try {
            addrs = InetAddress.getAllByName(anyIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        for (InetAddress addr : addrs) {
            try {
                if (addr.isReachable(1000)) {
                    res.add(addr.getHostAddress());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    public static List<String> accessibleRange() {
        ArrayList<String> res = new ArrayList<String>();
        for (String saddr : getRangeFromLocal(0)) {
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(saddr);
                if (addr.isReachable(TIMEOUT)) {
                    res.add(addr.getHostAddress());
                } else {
                    System.out.println("Is Not reachiable: " + addr.getHostAddress());
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    public static <T> T[] concatAll(T[] first, T[]... rest) {
        //http://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static List<NetworkInterface> getIfaces() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream().filter(notFake).collect(Collectors.toList());
    }

    public static String[] getRangeFromLocal(int ifNum) {
        String[] res = {};
        NetworkInterface liface;
        try {
            if (ifNum >= 0) {
                //liface = Collections.pinglist(NetworkInterface.getNetworkInterfaces()).get(ifNum);
                //liface=NetworkInterface.getByIndex(ifNum); // by real num in interfaces pinglist
                liface = getIfaces().get(ifNum);
                //displayInterfaceInformation(liface);
                //res=new String[]{liface.getInterfaceAddresses().get(0).getAddress().getHostAddress()};
                //filter only IPV4
                res = liface.getInterfaceAddresses().stream()
                        .filter(ipv4)
                        .filter(masq)
                        .map(a -> {
                            return new SubnetUtils(a.getAddress().getHostAddress() + "/" + String.valueOf(a.getNetworkPrefixLength())).getInfo().getAllAddresses();
                        })
                        .reduce(new String[]{}, (s1, s2) -> {
                            return concatAll(s1, s2);
                        });
                /*for(InterfaceAddress addr:liface.getInterfaceAddresses()){
                    LOG.info(addr.getAddress().getHostAddress() + "/" +addr.getNetworkPrefixLength());
                    res = concatAll(res,new SubnetUtils(addr.getAddress().getHostAddress() + "/" + String.valueOf(addr.getNetworkPrefixLength())).getInfo().getAllAddresses());
                }*/
//              res = new SubnetUtils(liface.getInterfaceAddresses().get(0).getAddress().getHostAddress() + "/" + String.valueOf(liface.getInterfaceAddresses().get(0).getNetworkPrefixLength())).getInfo().getAllAddresses();
            } else {
                res = Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
//                networkInterface.getInterfaceAddresses().stream()
                        .filter(notFake)
                        //for each interface (excluding localhost, virtual, P2P)
                        //return String[]
                        .map(iface -> {
                            return iface.getInterfaceAddresses().stream()
                                    .filter(ipv4)
                                    .filter(masq)
                                    //for each ipv4 address on interface
                                    .map(a -> {
                                        return new SubnetUtils(a.getAddress().getHostAddress() + "/" + String.valueOf(a.getNetworkPrefixLength())).getInfo().getAllAddresses();
                                    })
                                    .reduce(new String[]{}, (s1, s2) -> {
                                        return concatAll(s1, s2);
                                    });
//                            return new SubnetUtils(iface.getInterfaceAddresses().get(0).getAddress().getHostAddress() +
//                                    "/" + String.valueOf(iface.getInterfaceAddresses().get(0).getNetworkPrefixLength())).getInfo().getAllAddresses();
                        })
                        //return combined String[] (for all interfaces)
                        .reduce(new String[]{}, (s1, s2) -> {
                            return concatAll(s1, s2);
                        });
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return res;
    }

    static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
        //https://docs.oracle.com/javase/tutorial/networking/nifs/listing.html
        out.printf("Display name: %s\n", netint.getDisplayName());
        out.printf("Name: %s\n", netint.getName());
        out.printf("Index: %s\n", netint.getIndex());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            out.printf("InetAddress: %s\n", inetAddress);
        }
        out.printf("\n");
    }

    public static List<InterfaceAddress> getIfacesList() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
//                networkInterface.getInterfaceAddresses().stream()
                .filter(notFake)
                //for each interface (excluding localhost, virtual, P2P)
                //return String[]
                .flatMap(iface -> {
                    return iface.getInterfaceAddresses().stream()
                            .filter(ipv4)
                            .filter(masq);
                })
                .collect(Collectors.toList())
                ;
    }

    public static void main(String args[]) throws SocketException {
        //createFtpUser("test","fortestonly");
        //startFtp("1680");
        //accessibleRange().forEach(addr->System.out.println(addr));
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
                if (!netint.isLoopback() & !netint.isPointToPoint()
                        & (Collections.list(netint.getInetAddresses()).size() > 0)
                        & !netint.isVirtual()) displayInterfaceInformation(netint);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (args.length < 1) {
            System.err.println("Usage: java Ping ifaceNum");
            return;
        }
        int firstArg = -1;
        if (Pattern.matches("[0-9]+", args[0])) {
            firstArg = Integer.parseInt(args[0]);
        }
//firstArg=-1;
        out.printf("Param: %d\n", firstArg);
        out.println(Arrays.toString(getRangeFromLocal(firstArg)));
        //return 0;
    }

}
