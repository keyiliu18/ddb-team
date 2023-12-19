package test;

import transaction.WorkflowController;
import transaction.TransactionAbortedException;
import transaction.InvalidTransactionException;
import java.rmi.RemoteException;

public class BadXid{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
           
            try {
                wc.addFlight(123456, "MU5377", 100, 500);
            } catch (InvalidTransactionException e) {
                // e.printStackTrace();
            }
            wc.commit(xid);
            
            System.out.println("BadXid Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("BadXid Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

}
