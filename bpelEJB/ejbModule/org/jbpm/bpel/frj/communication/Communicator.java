package org.jbpm.bpel.frj.communication;

import org.jbpm.bpel.frj.VersionControlManager;

public interface Communicator {
	public void send(VersionControlManager vm);
	public void receive();
}
