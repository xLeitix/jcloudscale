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
package at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NetworkEvent extends StateEvent {

	private static final long serialVersionUID = 87109;

	private Map<String, InterfaceStats> interfaces = new HashMap<String, NetworkEvent.InterfaceStats>();

	public int getInterfaceCount() {
		return interfaces.size();
	}

	public Set<String> getInterfaceNames() {
		return interfaces.keySet();
	}

	public Collection<InterfaceStats> getInterfaces() {
		return interfaces.values();
	}

	public InterfaceStats getInterfaceByName(String name) {
		return interfaces.get(name);
	}

	public void addInterface(InterfaceStats ifStats) {
		interfaces.put(ifStats.getIfname(), ifStats);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (NetworkEvent.InterfaceStats iface : interfaces.values())
			sb.append(iface.ifname).append(", ");
		if (sb.length() > 1)
			sb.delete(sb.length() - 2, sb.length());
		return super.toString() + " [" + sb + "]";
	}

	/**
	 * Network Interface Statistics
	 */
	public static class InterfaceStats implements Serializable {

		private static final long serialVersionUID = 871091;

		private final String ifname;
		private final long speed;

		private final long recv_bytes;
		private final long recv_packets;
		private final long recv_errs;
		private final long recv_drop;

		private final long trans_bytes;
		private final long trans_packets;
		private final long trans_errs;
		private final long trans_drop;

		public InterfaceStats(String ifname, long speed, long recv_bytes, long recv_packets, long recv_errs,
				long recv_drop, long trans_bytes, long trans_packets, long trans_errs, long trans_drop) {
			super();
			this.ifname = ifname;
			this.speed = speed;
			this.recv_bytes = recv_bytes;
			this.recv_packets = recv_packets;
			this.recv_errs = recv_errs;
			this.recv_drop = recv_drop;
			this.trans_bytes = trans_bytes;
			this.trans_packets = trans_packets;
			this.trans_errs = trans_errs;
			this.trans_drop = trans_drop;
		}

		public String getIfname() {
			return ifname;
		}

		public long getSpeed() {
			return speed;
		}

		public long getRecv_bytes() {
			return recv_bytes;
		}

		public long getRecv_packets() {
			return recv_packets;
		}

		public long getRecv_errs() {
			return recv_errs;
		}

		public long getRecv_drop() {
			return recv_drop;
		}

		public long getTrans_bytes() {
			return trans_bytes;
		}

		public long getTrans_packets() {
			return trans_packets;
		}

		public long getTrans_errs() {
			return trans_errs;
		}

		public long getTrans_drop() {
			return trans_drop;
		}
	}
}
