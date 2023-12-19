package test;

import transaction.WorkflowController;

public class DieRMAfterEnlist {
    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 500);
            wc.addRooms(xid, "SHANGHAI", 300, 350);
            wc.addCars(xid, "BEIJING", 100, 30);
            wc.newCustomer(xid, "Mary");
            wc.commit(xid);

            xid = wc.start();
            wc.dieRMAfterEnlist("RMRooms");
            wc.addFlight(xid, "B1234", 100, 520);

            try {
                wc.reserveRoom(xid, "Mary", "SHANGHAI");
            } catch (Exception e) {
                // e.printStackTrace();
            }

            //launch RMRooms
            Connector.launch("RMRooms");
            wc.reconnect();

            try {
                wc.commit(xid);
            } catch (Exception e) {
                // e.printStackTrace();
            }

            xid = wc.start();
            int r1 = wc.queryFlight(xid, "B1234");
            check(100, r1, "queryFlight");

            int r2 = wc.queryFlightPrice(xid, "B1234");
            check(500, r2, "queryFlightPrice");

            int r3 = wc.queryRooms(xid, "SHANGHAI");
            check(300, r3, "queryRooms");

            int r4 = wc.queryRoomsPrice(xid, "SHANGHAI");
            check(350, r4, "queryRoomsPrice");

            int r5 = wc.queryCars(xid, "BEIJING");
            check(100, r5, "queryCars");

            int r6 = wc.queryCarsPrice(xid, "BEIJING");
            check(30, r6, "queryCarsPrice");

            int r7 = wc.queryCustomerBill(xid, "Mary");
            check(0, r7, "queryCustomerBill");

            wc.commit(xid);

            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DieRMAfterEnlist exception " + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("DieRMAfterEnlist Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}
