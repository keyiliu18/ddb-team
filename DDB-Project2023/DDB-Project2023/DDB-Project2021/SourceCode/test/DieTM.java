package test;

import transaction.WorkflowController;
import transaction.TransactionAbortedException;

public class DieTM {
    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 500);
            wc.addRooms(xid, "shanghai", 300, 350);
            wc.addCars(xid, "BEIJING", 100, 30);
            wc.newCustomer(xid, "Mary");
            wc.commit(xid);

            xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 620);
            wc.addRooms(xid, "shanghai", 200, 300);
            wc.addCars(xid, "BEIJING", 300, 60);

            wc.dieNow("TM");
            // launch TM
            Connector.launch("TM");
            wc.reconnect();
            try {
                wc.commit(xid);
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }

            wc.dieNow("RMFlights");
            //  launch RMFlights
            Connector.launch("RMFlights");
            wc.dieNow("RMRooms");
            //  launch RMRooms
            Connector.launch("RMRooms");
            wc.dieNow("RMCars");
            //  launch RMCars
            Connector.launch("RMCars");
            wc.reconnect();

            xid = wc.start();
            int r1 = wc.queryFlight(xid, "B1234");
            check(100, r1);
            int r2 = wc.queryFlightPrice(xid, "B1234");
            check(500, r2);
            int r3 = wc.queryRooms(xid, "shanghai");
            check(300, r3);
            int r4 = wc.queryRoomsPrice(xid, "shanghai");
            check(350, r4);
            int r5 = wc.queryCars(xid, "BEIJING");
            check(100, r5);
            int r6 = wc.queryCarsPrice(xid, "BEIJING");
            check(30, r6);
            int r7 = wc.queryCustomerBill(xid, "Mary");
            check(0, r7);
            System.out.println("DieTM Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DieTM Test fail:" + e);
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("DieTM Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
