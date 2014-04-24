package org.jbpm.job.executor;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;

public class JobExecutor implements Serializable {

  private static final long serialVersionUID = 1L;

  JbpmConfiguration jbpmConfiguration;
  String name;
  int nbrOfThreads;
  int idleInterval;
  int maxIdleInterval;
  int historyMaxSize;

  int maxLockTime;
  int lockMonitorInterval;
  int lockBufferTime;

  Map threads = new HashMap();
  LockMonitorThread lockMonitorThread;
  Map monitoredJobIds = Collections.synchronizedMap(new HashMap());

  boolean isStarted = false;
  
  public synchronized void start() {
    if (! isStarted) {
      log.debug("starting thread group '"+name+"'...");
      for (int i=0; i<nbrOfThreads; i++) {
        startThread();
      }
      isStarted = true;
    } else {
      log.debug("ignoring start: thread group '"+name+"' is already started'");
    }
    
    lockMonitorThread = new LockMonitorThread(jbpmConfiguration, lockMonitorInterval, maxLockTime, lockBufferTime);
  }
  
  /**
   * signals to all threads in this job executor to stop.  It may be that 
   *   threads are in the middle of something and they will finish that firts.
   *   Use {@link #stopAndJoin()} in case you want a method that blocks until 
   *   all the threads are actually finished.
   * @return a list of all the stopped threads.  In case no threads were stopped
   *   an empty list will be returned. 
   */
  public synchronized List stop() {
    List stoppedThreads = new ArrayList(threads.size());
    if (isStarted) {
      log.debug("stopping thread group '"+name+"'...");
      for (int i=0; i<nbrOfThreads; i++) {
        stoppedThreads.add(stopThread());
      }
      isStarted = false;
    } else {
      log.debug("ignoring stop: thread group '"+name+"' not started");
    }
    return stoppedThreads;
  }

  public void stopAndJoin() throws InterruptedException {
    Iterator iter = stop().iterator();
    while (iter.hasNext()) {
      Thread thread = (Thread) iter.next();
      thread.join();
    }
  }

  protected synchronized void startThread() {
    String threadName = getNextThreadName();
    Thread thread = new JobExecutorThread(threadName, this, jbpmConfiguration, idleInterval, maxIdleInterval, maxLockTime, historyMaxSize);
    threads.put(threadName, thread);
    log.debug("starting new job executor thread '"+threadName+"'");
    thread.start();
  }

  protected String getNextThreadName() {
    return getThreadName(threads.size()+1);
  }
  protected String getLastThreadName() {
    return getThreadName(threads.size());
  }
  
  private String getThreadName(int index) {
    return name + ":" + getHostName() + ":" + index;
  }

  private String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      return "unknown";
    }
  }

  protected synchronized Thread stopThread() {
    String threadName = getLastThreadName();
    JobExecutorThread thread = (JobExecutorThread) threads.remove(threadName);
    log.debug("removing job executor thread '"+threadName+"'");
    thread.deactivate();
    return thread;
  }

  public void setMonitoredJobIds(Map monitoredJobIds) {
		this.monitoredJobIds = monitoredJobIds;
	}
  
  public Set getMonitoredJobIds() {
    return new HashSet(monitoredJobIds.values());
  }
  
  public void addMonitoredJobId(String threadName, long jobId) {
    monitoredJobIds.put(threadName, new Long(jobId));
  }
  
  public void removeMonitoredJobId(String threadName) {
    monitoredJobIds.remove(threadName);
  }

  public void setHistoryMaxSize(int historyMaxSize) {
		this.historyMaxSize = historyMaxSize;
	}
  
  public int getHistoryMaxSize() {
    return historyMaxSize;
  }
  
  public void setIdleInterval(int idleInterval) {
  	this.idleInterval = idleInterval;
  }
  
  public int getIdleInterval() {
    return idleInterval;
  }
  
  public void setStarted(boolean isStarted) {
  	this.isStarted = isStarted;
  }
  
  public boolean isStarted() {
    return isStarted;
  }
  
  public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}
  
  public JbpmConfiguration getJbpmConfiguration() {
    return jbpmConfiguration;
  }
  
  public void setMaxIdleInterval(int maxIdleInterval) {
		this.maxIdleInterval = maxIdleInterval;
	}
  
  public int getMaxIdleInterval() {
    return maxIdleInterval;
  }
  
  public void setName(String name) {
  	this.name = name;
  }
  
  public String getName() {
    return name;
  }
  
  public void setSize(int nbrOfThreads) {
  	this.nbrOfThreads = nbrOfThreads;
  }
  
  public int getSize() {
    return nbrOfThreads;
  }
  
  public void setThreads(Map threads) {
  	this.threads = threads;
  }
  
  public Map getThreads() {
    return threads;
  }
  
  public void setMaxLockTime(int maxLockTime) {
  	this.maxLockTime = maxLockTime;
  }
  
  public int getMaxLockTime() {
    return maxLockTime;
  }
  
  public void setLockBufferTime(int lockBufferTime) {
  	this.lockBufferTime = lockBufferTime;
  }
  
  public int getLockBufferTime() {
    return lockBufferTime;
  }
  
  public void setLockMonitorInterval(int lockMonitorInterval) {
  	this.lockMonitorInterval = lockMonitorInterval;
  }
  
  public int getLockMonitorInterval() {
    return lockMonitorInterval;
  }
  
  public void setNbrOfThreads(int nbrOfThreads) {
  	this.nbrOfThreads = nbrOfThreads;
  }
  
  public int getNbrOfThreads() {
    return nbrOfThreads;
  }
  
  private static Log log = LogFactory.getLog(JobExecutor.class);
}
