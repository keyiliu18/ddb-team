package test;

import transaction.WorkflowController;

public class QueryforDataAdd{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 250);
            wc.addRooms(xid, "shanghai", 250, 350);
            wc.addCars(xid, "BEIJING", 100, 30);
            int r1 = wc.queryFlight(xid, "B1234");
            check(100, r1);
            int r2 = wc.queryFlightPrice(xid, "B1234");
            check(250, r2);
            int r3 = wc.queryRooms(xid, "shanghai");
            check(250, r3);
            int r4 = wc.queryRoomsPrice(xid, "shanghai");
            check(350, r4);
            int r5 = wc.queryCars(xid, "BEIJING");
            check(100, r5);
            int r6 = wc.queryCarsPrice(xid, "BEIJING");
            check(30, r6);
            wc.commit(xid);

            System.out.println("QueryforDataAdd Test pass");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("QueryforDataAdd Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("QueryforDataAdd Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
