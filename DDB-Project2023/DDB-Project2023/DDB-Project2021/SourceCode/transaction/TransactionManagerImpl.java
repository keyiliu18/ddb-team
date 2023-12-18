package transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jcp.xml.dsig.internal.dom.Utils;

/**
 * * Transaction Manager for the Distributed Travel Reservation System.
 *
 * * Description: toy implementation of the TM
 */

public class TransactionManagerImpl
		extends java.rmi.server.UnicastRemoteObject
		implements TransactionManager {

	// xid and status log data path
	private final static String DATA_ROOT = "data/";
	private final static String TM_COUNTER_LOG = DATA_ROOT + "tm_xidCounter.log";
	private final static String TM_LOG = DATA_ROOT + "tm_xids.log";
	private final static String TM_RMs_LOG = DATA_ROOT + "tm_RMs.log";
	private final static String TM_RECOVER_LOG = DATA_ROOT + "tm_xidRecover.log";

	private Integer xidCounter;// transaction id
	private String dieTime;

	// RMs(HashSet<ResourceManager>) of transaction(Integer xid)
	private HashMap<Integer, HashSet<ResourceManager>> RMs;

	// status(String) of transactions(Integer xid)
	private HashMap<Integer, String> xids;

	// transactions to be recovered (Integer xid)) of transactions(Integer xid)
	private HashMap<Integer, Integer> xids_recover = new HashMap<>();

	public static void main(String args[]) {
		System.setSecurityManager(new RMISecurityManager());

		String rmiPort = System.getProperty("rmiPort");
		if (rmiPort == null) {
			rmiPort = "";
		} else if (!rmiPort.equals("")) {
			rmiPort = "//:" + rmiPort + "/";
		}

		try {
			TransactionManagerImpl obj = new TransactionManagerImpl();
			// Naming.rebind(rmiPort + TransactionManager.RMIName, obj);
			 Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345,Socket::new);
            registry.rebind(rmiPort + TransactionManager.RMIName, obj);
			System.out.println("TM bound");
		} catch (Exception e) {
			System.err.println("TM not bound:" + e);
			System.exit(1);
		}
	}

	public void ping() throws RemoteException {
	}

	public void enlist(int xid, ResourceManager rm) throws RemoteException,InvalidTransactionException {
		if (!this.xids.containsKey(xid)) {
			rm.abort(xid);
			return;
		}

		synchronized (this.xids) {
			String xidStatus = this.xids.get(xid);
			/**
			 * if transaction's status is ABORTED, the realted RM will abort
			 * after abort，deletes the current transaction information
			 */
			if (xidStatus.equals(TransactionManager.ABORTED)) {
				rm.abort(xid);
				synchronized (this.RMs) {
					Set<ResourceManager> temp = this.RMs.get(xid);
					ResourceManager randomRemove = temp.iterator().next();
					temp.remove(randomRemove);

					if (temp.size() == 0) {
						this.RMs.remove(xid);
						this.storeLogData(TM_RMs_LOG, this.RMs);

						this.xids.remove(xid);
						this.storeLogData(TM_LOG, this.xids);
					} else {
						this.RMs.put(xid, temp);
						this.storeLogData(TM_RMs_LOG, this.RMs);
					}
				}

				return;
			}

			/**
			 * if transaction's status is COMMITTED, the realted RM will commit
			 * after commit, deletes the current transaction information
			 */
			if (xidStatus.equals(TransactionManager.COMMITTED)) {
				rm.commit(xid);

				synchronized (this.RMs) {
					Set<ResourceManager> temp = this.RMs.get(xid);
					System.out.println(temp.toString());
					ResourceManager randomRemove = temp.iterator().next();
					System.out.println(randomRemove);

					temp.remove(randomRemove);
					if (temp.size() == 0) {
						this.RMs.remove(xid);
						this.storeLogData(TM_RMs_LOG, this.RMs);

						this.xids.remove(xid);
						this.storeLogData(TM_LOG, this.xids);
					  } else {
						this.RMs.put(xid, temp);
						this.storeLogData(TM_RMs_LOG, this.RMs);
					}
				}

				return;
			}

			synchronized (this.RMs) {
				Set<ResourceManager> temp = this.RMs.get(xid);
				ResourceManager findSameRMId = null;
				boolean abort = false;
				for (ResourceManager curRm : temp) {
					try {
						if (curRm.getID().equals(rm.getID())) {
							findSameRMId = curRm;
						}
					} catch (Exception e) {
						/**
						 * if some RM die(dieRM, dieRMAfterEnlist)
						 * then RMs will abort and TM abort
						 */
						abort = true;
						break;
					}
				}
				/*
				 * If any RM is unabled,
				 * TM issues a rollback request to all RMs
				 */
				if (abort) {
					rm.abort(xid);

					ResourceManager randomRemove = temp.iterator().next();
					temp.remove(randomRemove);
					if (temp.size() == 0) {
						this.RMs.remove(xid);
						this.storeLogData(TM_RMs_LOG, this.RMs);

						this.xids.remove(xid);
						this.storeLogData(TM_LOG, this.xids);
					} else {
						this.RMs.put(xid, temp);
						this.storeLogData(TM_RMs_LOG, this.RMs);

						this.xids.put(xid, TransactionManager.ABORTED);
						this.storeLogData(TM_LOG, this.xids);
					}

					return;
				}

				/**don't find currnet rm in RMs
				 * add it into RMs
				*/
				if (findSameRMId == null) {
					temp.add(rm);
					this.RMs.put(xid, temp);
					this.storeLogData(TM_RMs_LOG, this.RMs);
					return;
				}
			}
		}
	}

	public TransactionManagerImpl() throws RemoteException {
		this.xidCounter = 1;
		this.dieTime = "noDie";
		this.xids = new HashMap<>();
		this.RMs = new HashMap<>();
		this.xids_recover = new HashMap<>();

		this.recover();
	}

	/**
	 * recover if TM die
	 * load data from log file, and abort transactions not in COMMITTED state
	 */
	private void recover() {
		// load data from log file
		Object xidCounterTmp = this.loadLogData(TM_COUNTER_LOG);
		if (xidCounterTmp != null)
			this.xidCounter = (Integer) xidCounterTmp;

		Object xidsTmp = this.loadLogData(TM_LOG);
		if (xidsTmp != null) {
			this.xids = (HashMap<Integer, String>) xidsTmp;
		}

		Object RMsTmp = this.loadLogData(TM_RMs_LOG);
		if (RMsTmp != null) {
			this.RMs = (HashMap<Integer, HashSet<ResourceManager>>) RMsTmp;
		}

		if (xidsTmp != null) {
			Set<Integer> xidsKeys = this.xids.keySet();
			for (Integer key : xidsKeys) {
				/**if a xid not in COMMITTED state 
				 * it should be aborted*/
				if (!this.xids.get(key).equals(TransactionManager.COMMITTED)) {
					this.xids.put(key, TransactionManager.ABORTED);
				}
			}
		}
	}

	@Override
	public int start() throws RemoteException {
		// store log data of new xid
		synchronized (this.xidCounter) {
			Integer newXid = this.xidCounter++;
			this.storeLogData(TM_COUNTER_LOG, this.xidCounter);

			synchronized (this.xids) {
				this.xids.put(newXid, TransactionManager.INITIATED);
				this.storeLogData(TM_LOG, this.xids);
			}

			synchronized (this.RMs) {
				this.RMs.put(newXid, new HashSet<>());
				this.storeLogData(TM_RMs_LOG, this.RMs);
			}

			return newXid;
		}
	}

	private Object loadLogData(String filePath) {
		File file = new File(filePath);
		ObjectInputStream oin = null;
		try {
			oin = new ObjectInputStream(new FileInputStream(file));
			return oin.readObject();
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (oin != null)
					oin.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public boolean commit(int xid)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {

		if (!this.xids.containsKey(xid)) {
			throw new TransactionAbortedException(xid, "TM");
		}
		/**
		 * Two phase commit
		 * prepare phase
		 * log xid PREPARING
		 */
		synchronized (this.xids) {
			xids.put(xid, TransactionManager.PREPARING);
			this.storeLogData(TM_LOG, this.xids);
		}
		HashSet<ResourceManager> curRMs = this.RMs.get(xid);
		for (ResourceManager resourceManager : curRMs) {
			try {
				System.out.println("call RM prepare: " + xid + ": " + resourceManager.getID());
				boolean prepared = resourceManager.prepare(xid);
				if (!prepared) {
					this.abort(xid);
					throw new TransactionAbortedException(xid, "RM is not prepared,aborted");
				}
			} catch (Exception e) {
				/** if RM die AfterPrepare or BeforePrepare */
				// e.printStackTrace();
				this.abort(xid);
				throw new TransactionAbortedException(xid, "RM aborted");
			}
		}
		/**
		 * all RMs prepared
		 * BeforeCommit -> TM die before COMMITTED is logged
		 * all rm will call enlist() later when TM restart, and enlist() will mark xid
		 * ABORTED
		 */

		if (this.dieTime.equals("BeforeCommit")) {
			this.dieNow();
		}
		/**
		 * all RMs COMMITTED
		 * log xid COMMITTED
		 */
		synchronized (this.xids) {
			xids.put(xid, TransactionManager.COMMITTED);
			this.storeLogData(TM_LOG, this.xids);
		}
		/**
		 * 
		 * AfterCommit -> TM die after COMMITTED is logged
		 * call recover() and recover the xid
		 * call enlist() and commit RM
		 */

		if (this.dieTime.equals("AfterCommit")) {
			this.dieNow();
		}

		/**
		 * Two phase commit
		 * commit phase
		 * log all data
		 */
		Set<ResourceManager> committedRMs = new HashSet<>();
		for (ResourceManager resourceManager : curRMs) {
			try {
				System.out.println("call rm commit " + xid + ": " + resourceManager.getID());
				resourceManager.commit(xid);
				committedRMs.add(resourceManager);
			} catch (Exception e) {
				/** FdieRMBeforeCommit -> rm dies before/during commit */
				System.out.println("rm die when commit" );
				e.printStackTrace();
				// FdieRMBeforeCommit
			}
		}

		if (committedRMs.size() == curRMs.size()) {
			synchronized (this.RMs) {
				this.RMs.remove(xid);
				this.storeLogData(TM_RMs_LOG, this.RMs);
			}

			synchronized (this.xids) {
				this.xids.remove(xid);
				this.storeLogData(TM_LOG, this.xids);
			}
		} else {
			/**
			 * dieRMBeforeCommit
			 * store unfinished transactions(RMs that not commited)
			 */
			synchronized (this.RMs) {
				curRMs.removeAll(committedRMs);
				this.RMs.put(xid, curRMs);
				this.storeLogData(TM_RMs_LOG, this.RMs);
			}
		}
		return true;
	}


    @Override
    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        if (!this.xids.containsKey(xid)) {
            throw new InvalidTransactionException(xid, "TM abort");
        }

        HashSet<ResourceManager> resourceManagers = this.RMs.get(xid);
        HashSet<ResourceManager> abortedRMs = new HashSet<>();
        for (ResourceManager resourceManager : resourceManagers) {
            try {
                resourceManager.abort(xid);
                abortedRMs.add(resourceManager);
            } catch (Exception e) {
                // e.printStackTrace();

                // FdieRMBeforeAbort
            }
        }

        synchronized (this.RMs) {
            this.RMs.remove(xid);
            this.storeLogData(TM_RMs_LOG, this.RMs);
        }

        synchronized (this.xids) {
            this.xids.remove(xid);
            this.storeLogData(TM_LOG, this.xids);
        }

    }

	private void storeLogData(String filePath, Object obj) {
		File file = new File(filePath);
		file.getParentFile().mkdirs();
		ObjectOutputStream oout = null;
		try {
			oout = new ObjectOutputStream(new FileOutputStream(file));
			oout.writeObject(obj);
			oout.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (oout != null)
					oout.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public boolean dieNow()
			throws RemoteException {
		System.exit(1);
		return true; // We won't ever get here since we exited above;
						// but we still need it to please the compiler.
	}

}
