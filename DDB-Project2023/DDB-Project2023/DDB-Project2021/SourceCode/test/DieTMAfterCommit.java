package test;

import transaction.WorkflowController;
import transaction.TransactionAbortedException;
import java.rmi.RemoteException;

public class DieTMAfterCommit {
	public static void main(String[] a){

		Connector.cleanData();
		Connector.launch("ALL");
		
		 WorkflowController wc = Connector.connectWC();
		 try{
			int xid;
			   
			xid = wc.start();               
			wc.addFlight(xid, "B1234", 100, 500);
			wc.addRooms(xid, "SHANGHAI", 300, 350);
			wc.addCars(xid, "BEIJING", 100, 30);
			wc.newCustomer(xid, "Mary");
			wc.commit(xid);	
		
			xid = wc.start();
			wc.addFlight(xid, "B1234", 100, 520);
			wc.addRooms(xid, "SHANGHAI", 200, 300);
			wc.addCars(xid, "BEIJING", 100, 60);
			wc.dieTMAfterCommit();
			try {
               			wc.commit(xid);
            		} catch (RemoteException e) {
                		// e.printStackTrace();
            		}
			Connector.cleanUpExit(0);

		 }catch (Exception e) {
            	System.out.println("DieTMAfterCommit exception "+e.getMessage());
            	Connector.cleanUpExit(1);
       	 	}
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("DieTMAfterCommit Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
