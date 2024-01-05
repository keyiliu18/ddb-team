package test;

import test.Connector;
import transaction.WorkflowController;
import transaction.TransactionAbortedException;


public class DieRMBeforeCommit {

	public static void main(String[] a){
		Connector.cleanData();
		Connector.launch("ALL");

		WorkflowController wc = Connector.connectWC();
		 try{
			int xid;
			xid = wc.start();
			wc.dieRMBeforeCommit("RMRooms");
			wc.dieRMBeforeCommit("RMCars");
			wc.addRooms(xid, "Shanghai", 1000, 999);
			wc.addCars(xid, "BYD", 1000, 879);			
			wc.commit(xid);				
			
			Connector.launch("RMRooms");
			Connector.launch("RMCars");
            wc.reconnect();

			xid = wc.start();
            int ret3 = wc.queryRooms(xid, "Shanghai");
            check(1000, ret3, "queryRooms");
            int ret4 = wc.queryRoomsPrice(xid, "Shanghai");
            check(999, ret4, "queryRoomsPrice");
			int ret5 = wc.queryCars(xid, "BYD");
            check(1000, ret5, "queryCars");
            int ret6 = wc.queryCarsPrice(xid, "BYD");
            check(879, ret6, "queryCarsPrice");

			System.out.println("Test pass.");
			Connector.cleanUpExit(0);
		 }catch (Exception e) {
            System.out.println("Test fail:" + e);
            Connector.cleanUpExit(1);
        }
	}
	private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.err.println("DieRMBeforePrepare Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}