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
			wc.addFlight(xid, "MU5377", 100, 500);
			wc.addRooms(xid, "SHANGHAI", 300, 350);
			wc.addCars(xid, "BYD", 100, 30);
			wc.commit(xid);	
		
			xid = wc.start();
			wc.addFlight(xid, "MU5377", 100, 520);
			wc.addRooms(xid, "SHANGHAI", 200, 300);
			wc.addCars(xid, "BYD", 100, 60);
			wc.dieTMAfterCommit();
			try {
				wc.commit(xid);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
			Connector.launch("TM");
            wc.dieNow("RMFlights");
            Connector.launch("RMFlights");
            wc.dieNow("RMRooms");
            Connector.launch("RMRooms");
            wc.dieNow("RMCars");
            Connector.launch("RMCars");
            wc.reconnect();

			xid = wc.start();
            int r1 = wc.queryFlight(xid, "MU5377");
            check(200, r1);
            int r2 = wc.queryFlightPrice(xid, "MU5377");
            check(520, r2);
            int r3 = wc.queryRooms(xid, "SHANGHAI");
            check(500, r3);
            int r4 = wc.queryRoomsPrice(xid, "SHANGHAI");
            check(300, r4);
            int r5 = wc.queryCars(xid, "BYD");
            check(200, r5);
            int r6 = wc.queryCarsPrice(xid, "BYD");
            check(60, r6);

			Connector.cleanUpExit(0);

		 }catch (Exception e) {
            	System.out.println("DieTMAfterCommit exception "+e.getMessage());
            	Connector.cleanUpExit(1);
       	 	}
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
