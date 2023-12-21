package test;

import transaction.WorkflowController;

import java.rmi.RemoteException;

public class DieAll {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 500);
            wc.addRooms(xid, "HANDAN", 300, 350);
            wc.addCars(xid, "shanghai", 100, 30);
            wc.newCustomer(xid, "Mary");
            wc.commit(xid);

            try {
                wc.dieNow("ALL");
            } catch (RemoteException e) {
                // e.printStackTrace();
            }

            Connector.launch("ALL");
            wc = Connector.connectWC();

            xid = wc.start();
            int ret1 = wc.queryFlight(xid, "B1234");
            check(100, ret1, "queryFlight");
            int ret2 = wc.queryFlightPrice(xid, "B1234");
            check(500, ret2, "queryFlightPrice");
            int ret3 = wc.queryRooms(xid, "HANDAN");
            check(300, ret3, "queryRooms");
            int ret4 = wc.queryRoomsPrice(xid, "HANDAN");
            check(350, ret4, "queryRoomsPrice");
            int ret5 = wc.queryCars(xid, "shanghai");
            check(100, ret5, "queryCars");
            int ret6 = wc.queryCarsPrice(xid, "shanghai");
            check(30, ret6, "queryCarsPrice");
            int ret7 = wc.queryCustomerBill(xid, "Mary");
            check(0, ret7, "queryCustomerBill");

            wc.commit(xid);
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DieAll exception " + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.err.println("DieAll Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}
