package kodiForm;
/*
 * @(#)Ping.java	1.2 01/12/13
 * Connect to each of a pinglist of hosts and measure the time required to complete
 * the connection.  This example uses a selector and two additional threads in
 * order to demonstrate non-blocking connects and the multithreaded use of a
 * selector.
 *
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * -Redistributions of source code must retain the above copyright
 *
 * -Redistributions of source code must retain the above copyright
 * notice, this  pinglist of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduct the above copyright
 * notice, this pinglist of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 *
 * -Redistribution in binary form must reproduct the above copyright
 * notice, this pinglist of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Oracle nor the names of
 * contributors may be used to endorse or promote products derived
 *
 * Neither the name of Oracle nor the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 * You acknowledge that Software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Ping {
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());
    // The default daytime port
    static int DAYTIME_PORT = 13;
    // The default http port
    static int HTTP_PORT = 80;
    // the default ssh port
    static int SSH_PORT = 22;
    // The port we'll actually use
    static int port = SSH_PORT;
    static short TIMEOUT_SELECT = 10;//10ms timeout for nio select
    static boolean logConnect = true;

    static final Broker broker = new Broker();
    private static final List<InetSocketAddress> list = Collections.synchronizedList(new ArrayList<InetSocketAddress>());

    static void add2broker(Object o) {
        broker.add(o);
        logConnect = false;
    }

    static void removeFromBroker(Object o) {
        broker.remove(o);
    }

    // Representation of a ping target
    //
    //
    class Target {

        private InetSocketAddress address;
        private SocketChannel channel;
        private Exception failure;
        private long connectStart;
        private long connectFinish = 0;
        private boolean shown = false;
        private boolean connected = false;

        Target(String host) {
            try {
                address = new InetSocketAddress(InetAddress.getByName(host),
                        port);
            } catch (IOException x) {
                failure = x;
            }
        }

        void show() throws IOException {
            String result;
            if (connectFinish != 0) {
                result = Long.toString(connectFinish - connectStart) + "ms";
                //broker.publish(address.getAddress().getHostAddress());
                broker.publish(address, true); //add to Component addresses list
            } else if (failure != null) {
                result = failure.toString();
                broker.publish(address, false);//remove from Component addresses list
            } else {
                result = "Timed out";
                broker.publish(address, false);
            }
            //channel.close();
            //System.out.println(address + " : " + result);
            if (logConnect) LOG.info("{} : {}", address, result);
            shown = true;
        }

        public InetSocketAddress getAddress() {
            InetSocketAddress res = null;
            if (connectFinish != 0) {
                res = address;
                connected = true;
            }
            return res;
        }
    }

    // Thread for printing targets as they're heard from
    //
    static class Printer
            extends Thread {
        LinkedList pending = new LinkedList();

        Printer() {
            setName("Printer");
            setDaemon(true);
        }

        void add(Target t) {
            synchronized (pending) {
                pending.add(t);
                pending.notify();
            }
        }

        public void run() {
            try {
                for (; ; ) {
                    Target t = null;
                    synchronized (pending) {
                        while (pending.size() == 0)
                            pending.wait();
                        t = (Target) pending.removeFirst();
                    }
                    t.show();
                }
            } catch (InterruptedException | IOException x) {
                return;
            }
        }

    }


    // Thread for connecting to all targets in parallel via a single selector
    //
    //
    class Connector
            extends Thread {
        Selector sel;
        Printer printer;

        // List of pending targets.  We use this pinglist because if we try to
        // register a channel with the selector while the connector thread is
        // blocked in the selector then we will block.
        //
        LinkedList pending = new LinkedList();

        Connector(Printer pr) throws IOException, InterruptedException {
            printer = pr;
            sel = Selector.open();
            setName("Connector");
        }

        // Initiate a connection sequence to the given target and add the
        // target to the pending-target pinglist
        //
        void add(Target t) {
            SocketChannel sc = null;
            try {

                // Open the channel, set it to non-blocking, initiate connect
                sc = SocketChannel.open();
                sc.configureBlocking(false);
/* <p> If this channel is in non-blocking mode then an invocation of this
* method initiates a non-blocking connection operation.  If the connection
* is established immediately, as can happen with a local connection, then
* this method returns <tt>true</tt>.  Otherwise this method returns
* <tt>false</tt> and the connection operation must later be completed by
* invoking the {@link #finishConnect finishConnect} method.
*/
                boolean connected = sc.connect(t.address);

                // Record the time we started
                t.channel = sc;
                t.connectStart = System.currentTimeMillis();

                if (connected) {
                    t.connectFinish = t.connectStart;
                    sc.close();
                    printer.add(t);
                } else {
                    // Add the new channel to the pending pinglist
                    synchronized (pending) {
                        pending.add(t);
                    }

                    // Nudge the selector so that it will process the pending pinglist
                    sel.wakeup();
                }
            } catch (IOException x) {
                if (sc != null) {
                    try {
                        sc.close();
                    } catch (IOException xx) {
                    }
                }
                t.failure = x;
                printer.add(t);
            }
        }

        // Process any targets in the pending pinglist
        //
        void processPendingTargets() throws IOException {
            synchronized (pending) {
                while (pending.size() > 0) {
                    Target t = (Target) pending.removeFirst();
                    try {

                        // Register the channel with the selector, indicating
                        // interest in connection completion and attaching the
                        // target object so that we can get the target back
                        // after the key is added to the selector's
                        // selected-key set
                        t.channel.register(sel, SelectionKey.OP_CONNECT, t);

                    } catch (IOException x) {
                        // Something went wrong, so close the channel and
                        // record the failure
                        t.channel.close();
                        t.failure = x;
                        printer.add(t);
                    }
                }

            }
        }

        // Process keys that have become selected
        //
        void processSelectedKeys() throws IOException {
/*
            for (Iterator i = sel.selectedKeys().iterator(); i.hasNext(); ) {

                // Retrieve the next key and remove it from the set
                SelectionKey sk = (SelectionKey) i.next();
                i.remove();

                // Retrieve the target and the channel
                Target t = (Target) sk.attachment();
                SocketChannel sc = (SocketChannel) sk.channel();

                // Attempt to complete the connection sequence
                try {
                    if (sc.finishConnect()) {
                        sk.cancel();
                        t.connectFinish = System.currentTimeMillis();
                        sc.close();
                        printer.add(t);
                    }
                } catch (IOException x) {
                    sc.close();
                    t.failure = x;
                    printer.add(t);
                }
            }
*/
            //http://tutorials.jenkov.com/java-nio/selectors.html
            Set<SelectionKey> selectedKeys = sel.selectedKeys();

            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {

                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isAcceptable()) {
                    // a connection was accepted by a ServerSocketChannel.

                } else if (key.isConnectable()) {
                    // a connection was established with a remote server.
                    Target t = (Target) key.attachment();
                    SocketChannel sc = (SocketChannel) key.channel();
                    try {//test connection for reject
                        if (sc.finishConnect()) {
                            key.cancel();
                            t.connectFinish = System.currentTimeMillis();
                            sc.close();
                            printer.add(t);
                        }
                    } catch (IOException x) {
                        sc.close();
                        t.failure = x;
                        printer.add(t);
                    }

                } else if (key.isReadable()) {
                    // a channel is ready for reading

                } else if (key.isWritable()) {
                    // a channel is ready for writing
                }

            }
        }


        volatile boolean shutdown = false;

        // Invoked by the main thread when it's time to shut down
        //
        void shutdown() {
            shutdown = true;
            sel.wakeup();
        }

        // Connector loop
        //
        public void run() {
            for (; ; ) {
                try {
                    int n = sel.select(TIMEOUT_SELECT);
                    if (n > 0)
                        processSelectedKeys();
                    processPendingTargets();
                    if (shutdown) {
                        sel.close();
                        //sel=null;
                        return;
                    }
                } catch (InterruptedIOException e) {
                    try {
                        sel.close();
                    } catch (IOException e1) {
                        LOG.error("failed within Interruption!", e1);
                        //e1.printStackTrace();
                    }
                    return;
                } catch (IOException x) {
                    LOG.error("failed!", x);
                    //x.printStackTrace();
                }
            }
        }

    }

    public List<Target> pingIface(int iface, long timeout) throws IOException, InterruptedException {
        Printer printer = new Printer();
        printer.start();
        Connector connector = new Connector(printer);
        connector.start();
        List<Target> filtered = null;
        LinkedList<Target> targets = new LinkedList();
        try {

            // Create the targets and add them to the connector
/*        for (int i = firstArg; i < args.length; i++) {
        Target t = new Target(args[i]);
        targets.add(t);
        connector.add(t);
    }
*/
            Arrays.stream(NetUtils.getRangeFromLocal(iface)).forEach(saddr -> {
                Target t = new Target(saddr);
                targets.add(t);
                connector.add(t);
            });
            // Wait for everything to finish
            if (timeout > 0) Thread.sleep(timeout);

            filtered = targets.stream()
                    .filter(t -> t.getAddress() != null)
                    .collect(Collectors.toList());

            if (logConnect) {
                LOG.info("*************");
                filtered.forEach(t -> LOG.info(String.valueOf(t.address)));
            }
        } finally {
            connector.shutdown();
            connector.interrupt();
            printer.interrupt();
            //clear all opened Sockets
            targets.forEach(t -> {
                try {
                    if (t.channel != null && t.channel.isOpen()) {
                        if (logConnect)
                            LOG.info("*channel Address: " + String.valueOf(t.channel.getRemoteAddress()));
                        broker.publish(t.channel.getRemoteAddress(), false);
                        t.channel.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            LOG.info("************* Finally exit");
        }
        // Print status of targets that have not yet been shown
/*        for (Iterator i = targets.iterator(); i.hasNext(); ) {
            Target t = (Target) i.next();
            if (!t.shown)
                t.show();
        }
*/
        return filtered;
    }

    public static void main(String[] args)
            throws InterruptedException, IOException {
/*
        if (args.length < 1) {
            System.err.println("Usage: java Ping [port] host...");
            return;
        }
        int firstArg = 0;

        // If the first argument is a string of digits then we take that
        // to be the port number to use
        if (Pattern.matches("[0-9]+", args[0])) {
            port = Integer.parseInt(args[0]);
            firstArg = 1;
        }
*/

        if (args.length < 1) {
            System.err.println("Usage: java Ping ifaceNum");
            return;
        }
        class My extends Component {
            @Subscription
            public void onString(String s) {
                LOG.info("My String: " + s);
                //System.out.println("String - " + s);
            }

            @Subscription
            public void onInetSocketAddress(InetSocketAddress addr, boolean avail) {
                String saddr = addr.getAddress().getHostAddress();
                //LOG.info("*test Address:" +saddr);
                if (avail) {
                    if (!list.contains(addr)) {
                        LOG.info("*add Address if it's not in list:{}", saddr);
                        list.add(addr);
                    }
                } else {
                    LOG.info("*remove Address if present:{}", saddr);
                    list.remove(addr);
                }
            }
        }
        add2broker(new My());
        int firstArg = -1;
        if (Pattern.matches("[0-9]+", args[0])) {
            firstArg = Integer.parseInt(args[0]);
        }
        Ping ping = new Ping();
        ping.pingIface(firstArg, 500);
    }

}
