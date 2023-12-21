package test;

import transaction.WorkflowController;
import java.util.ArrayList;
import java.util.List;

public class QueryforReserveFail {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "B1234", 100, 200);
            wc.addFlight(xid, "B1235", 150, 250);
            wc.addRooms(xid, "shanghai", 100, 300);
            wc.addCars(xid, "BEIJING", 100, 200);
            wc.newCustomer(xid, "mary");
            wc.commit(xid);

            xid = wc.start();
            wc.reserveCar(xid, "mary", "BEIJING");
            List<String> flights = new ArrayList<>();
            flights.add("B1234");
            flights.add("B1235");
            wc.reserveItinerary(xid, "mary", flights, "shanghai", true, true);
            wc.commit(xid);

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
            check(99, r5);
            int r6 = wc.queryCarsPrice(xid, "BEIJING");
            check(200, r6);
            int r7 = wc.queryCustomerBill(xid, "mary");
            check(200, r7);
            wc.commit(xid);
            System.out.println("QueryforReserveFail Test pass.");

            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("QueryforReserveFail Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("QueryforReserveFail Test fail");
            Connector.cleanUpExit(1);
        }
    }

}
