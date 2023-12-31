package test;

import test.Connector;
import transaction.WorkflowController;
import transaction.TransactionAbortedException;
import java.util.ArrayList;
import java.util.List;

public class DieRM {
	
	public static void main(String[] a){
		Connector.cleanData();
		Connector.launch("ALL");

		WorkflowController wc = Connector.connectWC();
		 try{
			int xid =  wc.start();
			wc.addFlight(xid, "B1234", 100, 500);
			wc.addRooms(xid, "shanghai", 300, 350);	
			wc.addCars(xid, "BYD", 100, 30);
			wc.newCustomer(xid, "Mary");		
			wc.commit(xid);			
			
			xid = wc.start();
			List<String> flights = new ArrayList<>();
            flights.add("B1234");
            wc.reserveItinerary(xid, "Mary", flights, "BYD", true, false);
			wc.addRooms(xid, "shanghai", 200, 300);
			wc.dieNow("RMCars");
			
			Connector.launch("RMCars");
			wc.reconnect();
			try {
			    wc.commit(xid);
			} catch (TransactionAbortedException e) {
				//except transaction.exceptions.TransactionAbortedException
			}
			xid = wc.start();
			int r1 = wc.queryFlight(xid, "B1234");
			check(100, r1, "queryFlight");

			int r2 = wc.queryFlightPrice(xid, "B1234");
			check(500, r2, "queryFlightPrice");

			int r3 = wc.queryRooms(xid, "shanghai");
			check(300, r3, "queryRooms");

			int r4 = wc.queryRoomsPrice(xid, "shanghai");
			check(350, r4, "queryRoomsPrice");

			int r5 = wc.queryCars(xid, "BYD");
			check(100, r5, "queryCars");

			int r6 = wc.queryCarsPrice(xid, "BYD");
			check(30, r6, "queryCarsPrice");

			int r7 = wc.queryCustomerBill(xid, "Mary");
			check(0, r7, "queryCustomerBill");

			wc.commit(xid);

			Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DieRM Test fail:" + e);
            Connector.cleanUpExit(1);
        }
        
	}

	private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("DieRM Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}
