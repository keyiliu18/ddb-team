package test;

import transaction.WorkflowController;

public class LQueryforDataAdd {
    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid1 = wc.start();
            int xid2 = wc.start();
            wc.addFlight(xid1, "B1234", 100, 500);
            wc.addRooms(xid2, "SHANGHAI", 300, 350);
            wc.addCars(xid1, "BEIJING", 100, 30);
            wc.commit(xid2);
            wc.commit(xid1);

            int xid3 = wc.start();
            int ret1 = wc.queryFlight(xid3, "B1234");
            check(100, ret1, "queryFlight");
            int ret2 = wc.queryFlightPrice(xid3, "B1234");
            check(500, ret2, "queryFlightPrice");
            int ret3 = wc.queryRooms(xid3, "SHANGHAI");
            check(300, ret3, "queryRooms");
            int ret4 = wc.queryRoomsPrice(xid3, "SHANGHAI");
            check(350, ret4, "queryRoomsPrice");
            int ret5 = wc.queryCars(xid3, "BEIJING");
            check(100, ret5, "queryCars");
            int ret6 = wc.queryCarsPrice(xid3, "BEIJING");
            check(30, ret6, "queryCarsPrice");

            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("LQueryforDataAdd exception " + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.err.println("LQueryforDataAdd Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}
