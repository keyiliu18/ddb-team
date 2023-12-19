package test;

import test.Connector;
import transaction.WorkflowController;

public class DataAddCommitReserve {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 200);
            wc.addRooms(xid, "shanghai", 100, 300);
            wc.addCars(xid, "BEIJING", 100, 200);
            wc.newCustomer(xid, "mary");
            wc.commit(xid);

            xid = wc.start();
            wc.reserveFlight(xid, "mary", "B1234");
            wc.reserveRoom(xid, "mary", "shanghai");
            wc.reserveCar(xid, "mary", "BEIJING");
            wc.commit(xid);
            System.out.println("DataAddCommitReserve Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DataAddCommitReserve Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }
}
