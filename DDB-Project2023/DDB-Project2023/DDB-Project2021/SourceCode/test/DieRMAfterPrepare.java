package test;

import transaction.WorkflowController;
import transaction.TransactionAbortedException;

public class DieRMAfterPrepare {
	public static void main(String[] a){
		Connector.cleanData();
		Connector.launch("ALL");

		WorkflowController wc = Connector.connectWC();
		 try{
			int xid;

			xid = wc.start();			
			wc.addFlight(xid, "B1234", 100, 500);
            wc.addRooms(xid, "HANDAN", 300, 350);
            wc.addCars(xid, "BEIJING", 100, 30);
            wc.newCustomer(xid, "Mary");		
			wc.commit(xid);			
			
			xid = wc.start();
			wc.addFlight(xid, "B1234", 100, 520);
			wc.addRooms(xid, "HANDAN", 200, 300);	
			wc.addCars(xid, "BEIJING", 300, 60);	
			wc.dieRMAfterPrepare("RMFlights");
			
			try {
                wc.commit(xid);
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }
			// launch RMFlights
			Connector.launch("RMFlights");
			wc.reconnect();

			xid = wc.start();
			int ret1 = wc.queryFlight(xid, "B1234");
            check(100, ret1, "queryFlight");

            int ret2 = wc.queryFlightPrice(xid, "B1234");
            check(500, ret2, "queryFlightPrice");

            int ret3 = wc.queryRooms(xid, "HANDAN");
            check(300, ret3, "queryRooms");

            int ret4 = wc.queryRoomsPrice(xid, "HANDAN");
            check(350, ret4, "queryRoomsPrice");

            int ret5 = wc.queryCars(xid, "BEIJING");
            check(100, ret5, "queryCars");

            int ret6 = wc.queryCarsPrice(xid, "BEIJING");
            check(30, ret6, "queryCarsPrice");

            int ret7 = wc.queryCustomerBill(xid, "Mary");
            check(0, ret7, "queryCustomerBill");
            wc.commit(xid);

			Connector.cleanUpExit(0);
		 }catch(Exception e){
			 System.out.println("DieRMAfterPrepare exception "+e.getMessage());
			 Connector.cleanUpExit(1);
		 }
	}
	private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("DieRMAfterPrepare Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}
