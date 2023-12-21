package test;

import transaction.WorkflowController;

public class QueryforDataAddAbort {
    public static void main(String[] args) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid;
            xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 200);
            wc.addRooms(xid, "shanghai", 100, 300);
            wc.addCars(xid, "BEIJING", 100, 200);
            wc.newCustomer(xid, "mary");
            wc.commit(xid);

            xid = wc.start();
            wc.addFlight(xid, "B1234", 200, 120);
            wc.addRooms(xid, "shanghai", 200, 400);
            wc.addCars(xid, "BEIJING", 300, 60);
            wc.abort(xid);

            xid = wc.start();
            int r1 = wc.queryFlight(xid, "B1234");
            check(100, r1);

            int r2 = wc.queryFlightPrice(xid, "B1234");
            check(200, r2);

            int r3 = wc.queryRooms(xid, "shanghai");
            check(100, r3);

            int r4 = wc.queryRoomsPrice(xid, "shanghai");
            check(300, r4);

            int r5 = wc.queryCars(xid, "BEIJING");
            check(100, r5);

            int r6 = wc.queryCarsPrice(xid, "BEIJING");
            check(200, r6);

            int r7 = wc.queryCustomerBill(xid, "mary");
            check(0, r7);
            wc.commit(xid);
            System.out.println("QueryforDataAddAbort Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.err.println("QueryforDataAddAbort Test fail:" + e);
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
