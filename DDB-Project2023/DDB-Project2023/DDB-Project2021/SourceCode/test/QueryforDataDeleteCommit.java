package test;

import transaction.WorkflowController;

public class QueryforDataDeleteCommit {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 200);
            wc.addRooms(xid, "shanghai", 100, 300);
            wc.addCars(xid, "BEIJING", 100, 200);
            
            wc.deleteRooms(xid, "shanghai", 50);
            wc.deleteCars(xid, "BEIJING", 50);
            wc.commit(xid);

            xid = wc.start();
            int r1 = wc.queryFlight(xid, "B1234");
            check(100, r1);
            int r2 = wc.queryFlightPrice(xid, "B1234");
            check(200, r2);
            int r3 = wc.queryRooms(xid, "shanghai");
            check(50, r3);
            int r4 = wc.queryRoomsPrice(xid, "shanghai");
            check(300, r4);
            int r5 = wc.queryCars(xid, "BEIJING");
            check(50, r5);
            int r6 = wc.queryCarsPrice(xid, "BEIJING");
            check(200, r6);
            wc.commit(xid);
            System.out.println("QueryforDataDeleteCommit Test pass.");

            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("QueryforDataDeleteCommit Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("QueryforDataDeleteCommit Test fail");
            Connector.cleanUpExit(1);
        }
    }

}
