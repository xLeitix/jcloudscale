/*
   Copyright 2013 Philipp Leitner

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package at.ac.tuwien.infosys.jcloudscale.server.aspects.eventing;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.jms.JMSException;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.IConfigurationChangedListener;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.CPUEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.NetworkEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.NetworkEvent.InterfaceStats;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.RAMEvent;
import at.ac.tuwien.infosys.jcloudscale.monitoring.MonitoringConfiguration;
import at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner;

/**
 * 
 * This is really just implemented as an aspect to keep consistency with all
 * other event sources - it is not intercepting anything, just creating a
 * separate scheduler which triggers state info periodically
 * 
 * @author philipp
 * 
 */
@Aspect
@SuppressWarnings("restriction")
public class StateEventAspect extends EventAspect implements IConfigurationChangedListener
{
	private Timer scheduler;
	private UUID hostId;
	private OperatingSystemMXBean osMX = null;
	private com.sun.management.OperatingSystemMXBean sunOsMX = null;
	private Sigar sigar;
	// private Semaphore lock = new Semaphore(1);
	private Object lock = new Object();

	// XXX AbstractJCloudScaleServerRunner
	@After("this(server) && execution(protected void at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner.run())")
	public void startStateEventScheduler(AbstractJCloudScaleServerRunner server) {

		log.fine("Initializing state events");

		this.hostId = server.getId();

		if (server.getConfiguration().common().monitoring().isEnabled()) {
			initializeScheduler(server.getConfiguration());
		}

		server.registerConfigurationChangeListner(this);
	}

	private void initializeScheduler(JCloudScaleConfiguration cfg)
	{
		synchronized (lock)
		{
			if (scheduler != null)
				scheduler.cancel();

			if (osMX == null) {
				osMX = ManagementFactory.getOperatingSystemMXBean();

				try {
					// com.sun.management.OperatingSystemMXBean is an inofficial
					// implementation java.lang.management.OperatingSystemMXBean
					// and (at least) included in Sun's, Oracle's and OpenJDK's
					// java distributions
					sunOsMX = (com.sun.management.OperatingSystemMXBean) osMX;
				} catch (ClassCastException ex) {
					log.severe("No Oracle/Sun/OpenJDK JVM implementation used. some system state properties won't be available.");
				}
			}

			try {
				Sigar.load();
				sigar = new Sigar(); // Test if native libs can be loaded
			} catch (SigarException sigex) {
				log.severe("Unable to load Sigar native libraries." + sigex.getMessage());
			}

			scheduler = new Timer();
			long shedulingInterval = cfg.common().monitoring().getSystemEventsInterval();
			scheduler.scheduleAtFixedRate(new StateEventTask(), 0, shedulingInterval);

			log.fine("Scheduling interval is " + shedulingInterval);

			log.info("Starting to periodically send state events");
		}
	}

	// XXX AbstractJCloudScaleServerRunner
	@Before("execution(public void at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner.shutdown())")
	public void stopStateEventScheduler()
	{
		synchronized (lock)
		{
			if (scheduler != null)
			{
				scheduler.cancel();
				if (sigar != null)
					sigar.close();

				log.info("Stopped sending state events");
			}
		}
	}

	@Override
	public void onConfigurationChange(JCloudScaleConfiguration newConfiguration)
	{
		MonitoringConfiguration oldMonitoring = JCloudScaleConfiguration.getConfiguration().common()
				.monitoring();
		MonitoringConfiguration newMonitoring = newConfiguration.common().monitoring();

		if (oldMonitoring.isEnabled() != newMonitoring.isEnabled() ||
				oldMonitoring.getSystemEventsInterval() != newMonitoring.getSystemEventsInterval())
			initializeScheduler(newConfiguration);
	}

	private class StateEventTask extends TimerTask {

		@Override
		public void run()
		{

			log.finest("Running StateEventTask");

			synchronized (lock)
			{
				MonitoringConfiguration cfg = JCloudScaleConfiguration.getConfiguration().common()
						.monitoring();
				if (cfg.triggerCpuEvents())
				{
					try {
						sendCPUEvent();
					} catch (Exception e) {
						e.printStackTrace();
						log.severe("Error while triggering CPUEvent: " + e.getMessage());
					}
				}

				if (cfg.triggerRamEvents())
				{
					try {
						sendRAMEvent();
					} catch (Exception e) {
						e.printStackTrace();
						log.severe("Error while triggering RAMEvent: " + e.getMessage());
					}
				}

				if (cfg.triggerNetworkEvents())
				{
					try {
						sendNetworkEvent();
					} catch (Exception e) {
						e.printStackTrace();
						log.severe("Error while triggering NetworkEvent: " + e.getMessage());
					}
				}
			}
		}

		private void sendCPUEvent() throws JMSException, SigarException {

			log.finer("Starting to collect CPU event");

			final CPUEvent event = new CPUEvent();
			initializeBaseEventProperties(event);
			event.setHostId(hostId);
			event.setSystemLoadAverage(osMX.getSystemLoadAverage());
			event.setArch(osMX.getArch());
			event.setNrCPUs(osMX.getAvailableProcessors());

			if (sunOsMX != null) {
				// event.setProcessCpuLoad(sunOsMX.getProcessCpuLoad());
				// event.setProcessCpuTime(sunOsMX.getProcessCpuTime());
				// event.setSystemCpuLoad(sunOsMX.getSystemCpuLoad());
				event.setSystemLoadAverage(sunOsMX.getSystemCpuLoad());
				event.setOsVersion(sunOsMX.getVersion());
			}

			// Sigar
			if (sigar != null) {
				final CpuInfo[] cpuInfo = sigar.getCpuInfoList();
				final CpuPerc cpuPerc = sigar.getCpuPerc();

				event.setVendor(cpuInfo[0].getVendor());
				event.setModel(cpuInfo[0].getModel());
				event.setNrCPUs(cpuInfo[0].getTotalCores());
				event.setMaxMHz(cpuInfo[0].getMhz());

				event.setSystemLoadAverage(sigar.getLoadAverage()[0]);
				event.setCombinedCpuLoad(cpuPerc.getCombined());
				event.setIdleCpuLoad(cpuPerc.getIdle());
				event.setNiceCpuLoad(cpuPerc.getNice());
				event.setSystemCpuLoad(cpuPerc.getSys());
				event.setUserCpuLoad(cpuPerc.getUser());
			}

			log.finer("Currently system's load average is " + event.getSystemLoadAverage());
			getMqHelper().sendEvent(event);
			log.finer("Successfully sent CPU event");

		}

		private void sendRAMEvent() throws JMSException, SigarException {

			log.finer("Starting to collect RAM event");

			final RAMEvent event = new RAMEvent();
			initializeBaseEventProperties(event);
			event.setHostId(hostId);

			if (sunOsMX == null) {
				final Runtime runtime = Runtime.getRuntime();
				event.setMaxMemory(runtime.maxMemory());
				event.setFreeMemory(runtime.freeMemory());
				event.setUsedMemory(event.getMaxMemory() - event.getFreeMemory());
			} else {
				event.setFreeMemory(sunOsMX.getFreePhysicalMemorySize());
				event.setMaxMemory(sunOsMX.getTotalPhysicalMemorySize());
				event.setUsedMemory(event.getMaxMemory() - event.getFreeMemory());
				event.setCommitedMemory(sunOsMX.getCommittedVirtualMemorySize());
				event.setFreeSwapMemory(sunOsMX.getFreeSwapSpaceSize());
				event.setMaxSwapMemory(sunOsMX.getTotalSwapSpaceSize());
				event.setUsedSwapMemory(event.getMaxSwapMemory() - event.getFreeSwapMemory());
			}

			// Sigar
			if (sigar != null) {
				final Mem memory = sigar.getMem();
				final Swap swap = sigar.getSwap();

				// getActual* doesn't take buffers/caches into account, but
				// doesn' match the behavior of runtime/sunOsMX
				// event.setFreeMemory(memory.getActualFree());
				// event.setUsedMemory(memory.getActualUsed());
				event.setFreeMemory(memory.getFree());
				event.setMaxMemory(memory.getTotal());
				event.setUsedMemory(memory.getUsed());

				event.setFreeSwapMemory(swap.getFree());
				event.setMaxSwapMemory(swap.getTotal());
				event.setUsedSwapMemory(swap.getUsed());
			}

			log.finer("Current RAM usage is " + event.getUsedMemory());
			getMqHelper().sendEvent(event);
			log.finer("Successfully sent RAM event");

		}

		private void sendNetworkEvent() throws JMSException, SigarException {
			if (sigar == null)
				return;

			log.finer("Starting to collect Network event");

			final NetworkEvent event = new NetworkEvent();
			initializeBaseEventProperties(event);
			event.setHostId(hostId);

			final String[] nics = sigar.getNetInterfaceList();
			for (final String nic : nics) {
				final NetInterfaceStat nicstat = sigar.getNetInterfaceStat(nic);

				event.addInterface(new InterfaceStats(nic, nicstat.getSpeed(), nicstat.getRxBytes(),
						nicstat.getRxPackets(),
						nicstat.getRxErrors(),
						nicstat.getRxDropped(),
						nicstat.getTxBytes(),
						nicstat.getTxPackets(),
						nicstat.getTxErrors(),
						nicstat.getTxDropped()));
			}

			log.finer("Currently systems network stats are " + event);
			getMqHelper().sendEvent(event);
			log.finer("Successfully sent network event");

		}
	}
}
