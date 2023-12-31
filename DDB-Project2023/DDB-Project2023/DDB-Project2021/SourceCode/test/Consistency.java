package test;

import test.Connector;
import transaction.WorkflowController;
import transaction.TransactionAbortedException;
import java.rmi.RemoteException;

public class Consistency{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addRooms(xid, "SHANGHAI", 200, 350);
            wc.newCustomer(xid, "John");
            wc.newCustomer(xid, "Danny");
            wc.newCustomer(xid, "Jenny");
            
            int r1 = wc.queryRooms(xid, "SHANGHAI");
            check(200, r1);
            wc.commit(xid);
            xid = wc.start();
            wc.reserveRoom(xid, "John", "SHANGHAI");
            wc.reserveRoom(xid, "Danny", "SHANGHAI");
            wc.reserveRoom(xid, "Jenny", "SHANGHAI");
            wc.commit(xid);
            xid = wc.start();
            int r2 = wc.queryRooms(xid, "SHANGHAI");
            check(197, r2);
            System.out.println("Consistency Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("Consistency Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Consistency Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
