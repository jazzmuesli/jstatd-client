import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.remote.RemoteHost;
import sun.jvmstat.monitor.remote.RemoteVm;
import sun.jvmstat.perfdata.monitor.protocol.rmi.RemoteMonitoredVm;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import java.util.stream.Collectors;

public class JstatdClient {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 1099;
        if (args.length == 2) {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }
        Registry reg = LocateRegistry.getRegistry(host, port);
        RemoteHost remote = (RemoteHost) reg.lookup("JStatRemoteHost");
        final int[] activeVms = remote.activeVms();
        Timer timer = new Timer(true);
        int interval = 10;
        for (int pid : activeVms) {
            RemoteVm remoteVm = remote.attachVm(pid, null);
            VmIdentifier nvmid = new VmIdentifier("remote://" + pid + "@" + host);
            RemoteMonitoredVm rmvm = new RemoteMonitoredVm(remoteVm, nvmid, timer, interval);
            List<Monitor> monitors = rmvm.findByPattern(".*");
            Map<String, Object> monitorVals = monitors.stream().collect(Collectors.toMap(Monitor::getName, Monitor::getValue));
            String cmdLine = String.valueOf(monitorVals.get("java.rt.vmArgs"));
            //old gen
            long usedMemory = ((Long) monitorVals.get("sun.gc.generation.1.space.0.used")) / 1048576;
            long maxMemory = ((Long) monitorVals.get("sun.gc.generation.1.space.0.maxCapacity")) / 1048576;
            System.out.println(pid + ":" + usedMemory + ":" + maxMemory + ":" + cmdLine);
        }

    }
}
