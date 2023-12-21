package test;

import transaction.WorkflowController;

public class DataAddDelete {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 500);
            wc.addRooms(xid, "HANDAN", 300, 350);
            wc.addCars(xid, "BEIJING", 100, 30);
            wc.newCustomer(xid, "mary");

            wc.deleteRooms(xid, "HANDAN", 300);
            wc.deleteCars(xid, "BEIJING", 100);
            wc.deleteCustomer(xid, "mary");
            wc.commit(xid);

            System.out.println("DataAddDelete Test pass");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DataAddDelete Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }
}
