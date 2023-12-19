package test;

import transaction.WorkflowController;

public class DataAddCommit{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 100);
            wc.addRooms(xid, "shanghai", 100, 300);
            wc.addCars(xid, "BEIJING", 100, 100);
            wc.newCustomer(xid, "mary");
            wc.commit(xid);
            System.out.println("DataAddCommit Test pass");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DataAddCommit Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }
}
