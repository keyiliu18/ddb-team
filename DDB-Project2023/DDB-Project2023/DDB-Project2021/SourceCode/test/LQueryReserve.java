package test;

import transaction.WorkflowController;
import transaction.TransactionAbortedException;
import java.util.ArrayList;
import java.util.List;

public class LQueryReserve {
    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 500);
            wc.addFlight(xid, "B1235", 150, 300);
            wc.addRooms(xid, "SHANGHAI", 300, 350);
            wc.addCars(xid, "BEIJING", 100, 30);
            wc.newCustomer(xid, "mary");
            wc.commit(xid);

            int xid1 = wc.start();
            int xid2 = wc.start();
            List<String> flights = new ArrayList<>();
            flights.add("B1234");
            flights.add("B1235");
            wc.reserveItinerary(xid1, "mary", flights, "SHANGHAI", false, true);
            try {
                wc.queryFlight(xid2, "B1234");
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }
            
            wc.commit(xid1);
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("LQueryReserve exception " + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }
}
