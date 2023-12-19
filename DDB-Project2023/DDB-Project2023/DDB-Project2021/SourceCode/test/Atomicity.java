package test;

import transaction.WorkflowController;
import transaction.TransactionAbortedException;

import java.rmi.RemoteException;

public class Atomicity {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");
        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 500);
            wc.dieTMBeforeCommit();
            wc.addFlight(xid, "B1235", 100, 500);
            try {
                wc.commit(xid);
            } catch (RemoteException e) {
                // e.printStackTrace();
            }
            Connector.launch("TM");
            wc.reconnect();
            int r1 = wc.queryFlight(xid, "B1234");
            check(-1, r1);
            int r2 = wc.queryFlight(xid, "B1235");
            check(-1, r2);
            try {
                wc.commit(xid);
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }
            System.out.println("Atomicity Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.err.println("Atomicity Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }
    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Atomicity Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
