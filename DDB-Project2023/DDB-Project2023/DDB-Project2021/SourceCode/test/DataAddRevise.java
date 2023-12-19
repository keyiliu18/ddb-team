package test;

import transaction.WorkflowController;

public class DataAddRevise {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 500);
            wc.addRooms(xid, "shanghai", 300, 350);
            wc.addCars(xid, "BEIJING", 100, 30);

            wc.addFlight(xid, "B1234", 100, 420);
            wc.addRooms(xid, "shanghai", 200, 300);
            wc.commit(xid);

            System.out.println("DataAddRevise Test pass");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DataAddRevise Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }
}
