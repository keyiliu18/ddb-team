package transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	private final static String TM_RMs_LOG = DATA_ROOT + "tm_xidRMs.log";
	private final static String TM_RECOVER_LOG = DATA_ROOT + "tm_xidRecover.log";

	private Integer xidCounter;// transaction id
	private String dieTime;

	// RMs(HashSet<ResourceManager>) of transaction(Integer xid)
	private HashMap<Integer, HashSet<ResourceManager>> RMs;

	// status(String) of transactions(Integer xid)
	private HashMap<Integer, String> xids;

	// transactions  to be recovered (Integer xid)) of transactions(Integer xid)
	private HashMap<Integer, Integer> xids_to_be_recovered = new HashMap<>();

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
			Naming.rebind(rmiPort + TransactionManager.RMIName, obj);
			System.out.println("TM bound");
		} catch (Exception e) {
			System.err.println("TM not bound:" + e);
			System.exit(1);
		}
	}

	public void ping() throws RemoteException {
	}

	public void enlist(int xid, ResourceManager rm) throws RemoteException {
	}

	public TransactionManagerImpl() throws RemoteException {
		this.xidCounter = 1;
		this.dieTime = "noDie";
		this.xids = new HashMap<>();
		this.RMs = new HashMap<>();
		this.xids_to_be_recovered = new HashMap<>();

		
		this.recover();
	}

	/**
	 * recover if TM die
	 * load data from log file, and abort  transactions not in COMMITTED state
	*/
	private void recover() {
		// File dataDir = new File(DATA_ROOT);
		// if (!dataDir.exists()) {
		// 	dataDir.mkdirs();
		// }

		//load data from log file
		Object xidCounterTmp = utils.loadObject(TM_COUNTER_LOG);
		if (xidCounterTmp != null)
			this.xidCounter = (Integer) xidCounterTmp;

        Object xidsTmp = this.loadCache(TM_LOG);
        if (xidsTmp != null) {
            this.xids = (HashMap<Integer, String>) xidsTmp;
        }

        Object RMsTmp = this.loadCache(TM_RMs_LOG);
        if (RMsTmp != null) {
            this.RMs = (HashMap<Integer, HashSet<ResourceManager>>) RMsTmp;
        }

		if (xidsTmp != null) {
			Set<Integer> xidsKeys = this.xids.keySet();
	        for (Integer key : xidsKeys) {
				// if a xid not in COMMITTED state
				//it should be aborted
				if (!this.xids.get(key).equals(TransactionManager.COMMITTED)) {
					this.xids.put(key, TransactionManager.ABORTED);
				}
			}
		}
	}


	
    @Override
    public int start() throws RemoteException {
        synchronized (this.xidNum) {
            Integer curXid = this.xidNum++;
            this.storeToFile(TM_TRANSACTION_NUM_LOG_FILENAME, this.xidNum);

            synchronized (this.xids) {
                this.xids.put(curXid, TransactionManager.NEW);
                this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
            }

            synchronized (this.xidRMs) {
                this.xidRMs.put(curXid, new HashSet<>());
                this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);
            }

            return curXid;
        }
    }



    private Object loadCache(String filePath) {
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




	public boolean dieNow()
			throws RemoteException {
		System.exit(1);
		return true; // We won't ever get here since we exited above;
						// but we still need it to please the compiler.
	}

}
