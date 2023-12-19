package test;

import transaction.WorkflowController;
import transaction.TransactionAbortedException;

public class Ldeadlock {
    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "347", 100, 310);
            wc.addRooms(xid, "Stanford", 200, 150);
            wc.addCars(xid, "SFO", 300, 30);
            wc.newCustomer(xid, "John");
            wc.commit(xid);

            int xid1 = wc.start();
            int xid2 = wc.start();
            wc.reserveFlight(xid2, "John", "347");
            wc.addRooms(xid2, "Stanford", 200, 300);
            wc.addCars(xid1, "SFO", 300, 60);

            try {
                wc.queryCarsPrice(xid2, "SFO");
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }
            wc.commit(xid1);

            xid = wc.start();
            int r1 = wc.queryFlight(xid, "347");
            check(100, r1);
            int r2 = wc.queryFlightPrice(xid, "347");
            check(310, r2);
            int r3 = wc.queryRooms(xid, "Stanford");
            check(200, r3);
            int r4 = wc.queryRoomsPrice(xid, "Stanford");
            check(150, r4);
            int r5 = wc.queryCars(xid, "SFO");
            check(600, r5);
            int r6 = wc.queryCarsPrice(xid, "SFO");
            check(60, r6);
            int r7 = wc.queryCustomerBill(xid, "John");
            check(0, r7);

            System.out.println("Ldeadlock Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("Ldeadlock exception " + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.err.println(expect + " " + real);
            System.err.println("Ldeadlock Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
