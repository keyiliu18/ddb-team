package transaction;
import transaction.models.*;
import transaction.InvalidIndexException;
import transaction.InvalidTransactionException;
import transaction.TransactionAbortedException;


import java.rmi.*;
import java.util.*;
import java.io.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import lockmgr.DeadlockException;
import java.net.Socket;
import transaction.InvalidTransactionException;

/** 
 * Workflow Controller for the Distributed Travel Reservation System.
 * 
 * Description: toy implementation of the WC.  In the real
 * implementation, the WC should forward calls to either RM or TM,
 * instead of doing the things itself.
 */


// import org.jcp.xml.dsig.internal.dom.Utils;
public class WorkflowControllerImpl
    extends java.rmi.server.UnicastRemoteObject
    implements WorkflowController {

    // protected int flightcounter, flightprice, carscounter, carsprice, roomscounter, roomsprice; 
    // protected int xidCounter;
    
	// If WC die, restart and load xids from "data/wc_xids.log". refer
	protected final static String WC_TRANSACTION_LOG_FILENAME = "data/wc_xids.log";

	// xids
	protected Set<Integer> xids;

    protected ResourceManager rmFlights = null;
    protected ResourceManager rmRooms = null;
    protected ResourceManager rmCars = null;
    protected ResourceManager rmCustomers = null;
    protected TransactionManager tm = null;

    public static void main(String args[]) {
	System.setSecurityManager(new RMISecurityManager());

	String rmiPort = System.getProperty("rmiPort");
	if (rmiPort == null) {
	    rmiPort = "";
	} else if (!rmiPort.equals("")) {
	    rmiPort = "//:" + rmiPort + "/";
	}

	try {
	    WorkflowControllerImpl obj = new WorkflowControllerImpl();
	    // Naming.rebind(rmiPort + WorkflowController.RMIName, obj);
		Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);
		registry.rebind(rmiPort + WorkflowController.RMIName, obj);
	    System.out.println("WC bound");
	}
	catch (Exception e) {
	    System.err.println("WC not bound:" + e);
	    System.exit(1);
	}
    }
    
    
    public WorkflowControllerImpl() throws RemoteException {
	// flightcounter = 0;
	// flightprice = 0;
	// carscounter = 0;
	// carsprice = 0;
	// roomscounter = 0;
	// roomsprice = 0;
	// flightprice = 0;

	// xidCounter = 1;
	xids = new HashSet<>();

	// recover from die
	this.recover();

	while (!reconnect()) {
	    // would be better to sleep a while
		   try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
	} 
    }

	// refer
	private Set<Integer> loadTransactionLogs() {
		File xidLog = new File(WC_TRANSACTION_LOG_FILENAME);
		ObjectInputStream oin = null;
		try {
			oin = new ObjectInputStream(new FileInputStream(xidLog));
			return (HashSet<Integer>) oin.readObject();
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (oin != null)
					oin.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	private void recover() {
		Set<Integer> cacheXids = loadTransactionLogs();

		if (cacheXids != null) {
			this.xids = cacheXids;
		}
	}
	private void storeTransactionLogs(Set<Integer> xids) {
		File xidLog = new File(WC_TRANSACTION_LOG_FILENAME);
		xidLog.getParentFile().mkdirs();
		ObjectOutputStream oout = null;
		try {
			oout = new ObjectOutputStream(new FileOutputStream(xidLog));
			oout.writeObject(xids);
			oout.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (oout != null)
					oout.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

    // TRANSACTION INTERFACE
    public int start()
	throws RemoteException {
		System.out.println("stary wc");
		int xid=tm.start();//todo
		this.xids.add(xid);
		// return (xidCounter++);

		this.storeTransactionLogs(this.xids);
		return xid;
    }

    public boolean commit(int xid)
	throws RemoteException, 
	       TransactionAbortedException, 
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "commit");
		}
		System.out.println("Committing");
		// try{
			boolean ret = tm.commit(xid);//todo
			this.xids.remove(xid);

			this.storeTransactionLogs(this.xids);
			return ret;
		// }
		// catch (DeadlockException e) {
        //     this.abort(xid);
        //     throw new TransactionAbortedException(xid, e.getMessage());
        // }
    }

    public void abort(int xid)
	throws RemoteException, 
			InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "commit");
		}
		System.out.println("Abort!");		
		tm.abort(xid);//todo
		this.xids.remove(xid);

		this.storeTransactionLogs(this.xids);
    	// return;
    }


    // ADMINISTRATIVE INTERFACE
    public boolean addFlight(int xid, String flightNum, int numSeats, int price) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "addFlight");
		}
		// validate input
		if(flightNum == null || numSeats<0){
			return false;
		}
		ResourceItem resourceItem;
		try{
			resourceItem=this.rmFlights.query(xid, this.rmFlights.getID(),flightNum);
		}catch(DeadlockException e){
			System.out.println("query deaadlock when addFlight");
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// create a new flight
		if(resourceItem==null){
			Flight flight = new Flight(flightNum, price, numSeats, numSeats);
			
			try{
				return this.rmFlights.insert(xid, this.rmFlights.getID(),(ResourceItem) flight);
			}catch(DeadlockException e){
				System.out.println("insert deaadlock when addFlight");
				this.abort(xid);
				throw new TransactionAbortedException(xid, e.getMessage());
			}
		}
		// Adding to an existing flight
		Flight flight = (Flight) resourceItem;
		flight.addSeats(numSeats);
		// leave price at 0 if price<0
		if(price>=0){
			flight.setPrice(price);
		}
		// update
		try{
			return this.rmFlights.update(xid, this.rmFlights.getID(),flightNum, (ResourceItem)flight);
		}catch(DeadlockException e){
			System.out.println("update deaadlock when addFlight");
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// return true;
    }

    public boolean deleteFlight(int xid, String flightNum)
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
	// flightcounter = 0;
	// flightprice = 0;
		if(!this.xids.contains(xid)){
			throw new InvalidTransactionException(xid,"deleteFlight");
		}
		if(flightNum==null){
			return false;
		}
		// query
		try{
			ResourceItem resourceItem = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
			if (resourceItem == null) {
                return false;
            }
			// has reservations?
			Collection queryReservations = this.rmCustomers.query(xid,ResourceManager.TableNameReservations,Reservation.INDEX_CUSTNAME,flightNum);
			if(!queryReservations.isEmpty()){
				return false;
			}
			resourceItem.delete();
			return this.rmFlights.delete(xid, this.rmFlights.getID(), flightNum);

		} catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }catch (InvalidIndexException e) {//query need
            e.printStackTrace();
            return false;
        }
		// return true;
    }
		
    public boolean addRooms(int xid, String location, int numRooms, int price) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		// roomscounter += numRooms;
		// roomsprice = price;
		if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "addRooms");
        }
		// validate input
		if(numRooms<0 || location==null){
			return false;
		}
		ResourceItem resourceItem;
		try {
			resourceItem=this.rmRooms.query(xid, this.rmRooms.getID(), location);
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// create a new room
        if (resourceItem == null) {
            Hotel hotel = new Hotel(location, price, numRooms, numRooms);
            try {
                return this.rmRooms.insert(xid, this.rmRooms.getID(), (ResourceItem) hotel);
            } catch (DeadlockException e) {
                this.abort(xid);
                throw new TransactionAbortedException(xid, e.getMessage());
            }
        }
		// Adding to an existing hotel
		Hotel hotel = (Hotel) resourceItem;
        hotel.addRooms(numRooms);
		if(price>=0){
        	hotel.setPrice(price);
		}
		// update
		try {
			return this.rmRooms.update(xid, this.rmRooms.getID(), location, (ResourceItem)hotel);
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// return true;
    }

    public boolean deleteRooms(int xid, String location, int numRooms) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
	// roomscounter = 0;
	// roomsprice = 0;
		if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "deleteRooms");
        }
		if (location == null || numRooms < 0) {
			return false;
		}
		// query
		try {
            ResourceItem resourceItem = this.rmRooms.query(xid, this.rmRooms.getID(), location);
            if (resourceItem == null) {
                return false;
            }
            Hotel hotel = (Hotel) resourceItem;
            if (!hotel.reduceRooms(numRooms)) {
                return false;
            }
            return this.rmRooms.update(xid, this.rmRooms.getID(), location, (ResourceItem)hotel);
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
		// return true;
    }

    public boolean addCars(int xid, String location, int numCars, int price) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		// carscounter += numCars;
		// carsprice = price;
		if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "addCars");
        }
        if (location == null || numCars < 0) {
            return false;
        }		
        ResourceItem resourceItem;
        try {
            resourceItem = this.rmCars.query(xid, this.rmCars.getID(), location);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
        if (resourceItem == null) {
            Car car = new Car(location, price, numCars, numCars);
            try {
                return this.rmCars.insert(xid, this.rmCars.getID(), (ResourceItem)car);
            } catch (DeadlockException e) {
                this.abort(xid);
                throw new TransactionAbortedException(xid, e.getMessage());
            }
        }
        Car car = (Car) resourceItem;
        car.addCars(numCars);
		if(price>0){
        	car.setPrice(price);			
		}
		try {
            return this.rmCars.update(xid, this.rmCars.getID(), location, (ResourceItem)car);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
		// return true;
    }

    public boolean deleteCars(int xid, String location, int numCars) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		// carscounter = 0;
		// carsprice = 0;
		if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "deleteCars");
        }
        if (location == null || numCars < 0) {
            return false;
        }
		try {
            ResourceItem resourceItem = this.rmCars.query(xid, this.rmCars.getID(), location);
            if (resourceItem == null) {
                return false;
            }
            Car car = (Car) resourceItem;
            boolean reduce = car.reduceCars(numCars);
            if (!reduce) {
                return false;
            }
            return this.rmCars.update(xid, this.rmCars.getID(), location, (ResourceItem)car);
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
		// return true;
    }

    public boolean newCustomer(int xid, String custName) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "newCustomer");
		}
		if (custName == null) {//null
			return false;
		}
		ResourceItem resourceItem;
        try {
            resourceItem = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
			if (resourceItem != null) {//already have
				return true;
			}
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
		Customer customer = new Customer(custName);
        try {
            return this.rmCustomers.insert(xid, this.rmCustomers.getID(), (ResourceItem)customer);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
	// return true;
    }

    public boolean deleteCustomer(int xid, String custName) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "deleteCustomer");
		}
		if (custName == null) {
			return false;
		}
		try {
            ResourceItem resourceItem = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceItem == null) {
                return false;
            }
			// cancel all reservations
			Collection reservations = this.rmCustomers.query(
				xid,
				ResourceManager.TableNameReservations,
				Reservation.INDEX_CUSTNAME,
				custName
			);
			for (Object resv : reservations) {
				Reservation reservation = (Reservation) resv;
				String resvKey = reservation.getResvKey();
				int resvType = reservation.getResvType();
				if (resvType == Reservation.RESERVATION_TYPE_FLIGHT) {
					Flight flight = (Flight) this.rmFlights.query(xid, this.rmFlights.getID(), resvKey);
					flight.cancelResv();
					this.rmFlights.update(xid, this.rmFlights.getID(), resvKey, (ResourceItem)flight);
				} else if (resvType == Reservation.RESERVATION_TYPE_HOTEL) {
					Hotel hotel = (Hotel) this.rmRooms.query(xid, this.rmRooms.getID(), resvKey);
					hotel.cancelResv();
					this.rmRooms.update(xid, this.rmRooms.getID(), resvKey, (ResourceItem)hotel);
				} else if (resvType == Reservation.RESERVATION_TYPE_CAR) {
					Car car = (Car) this.rmCars.query(xid, this.rmCars.getID(), resvKey);
					car.cancelResv();
					this.rmCars.update(xid, this.rmCars.getID(), resvKey, (ResourceItem)car);
				}
			}
			// then delete
			this.rmCustomers.delete(
				xid,
				ResourceManager.TableNameReservations,
				Reservation.INDEX_CUSTNAME,
				custName);
				return this.rmCustomers.delete(xid, this.rmCustomers.getID(), custName);
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		} catch (InvalidIndexException e) {// delete need
			e.printStackTrace();
			return false;
		}
			// return true;
    }


    // QUERY INTERFACE
    public int queryFlight(int xid, String flightNum)
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "queryFlight");
		}
		if (flightNum == null) {//null
			return -1;
		}
		try {
			ResourceItem resourceItem = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
			if (resourceItem == null) {// no exist
				return -1;
			}else{
				System.out.println(resourceItem);
				System.out.println(((Flight) resourceItem).getNumAvail());
				return ((Flight) resourceItem).getNumAvail();
			}
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// return flightcounter;
    }

    public int queryFlightPrice(int xid, String flightNum)
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "queryFlightPrice");
		}
		if (flightNum == null) {
			return -1;
		}
		try {
			ResourceItem resourceItem = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
			if (resourceItem == null) {
				return -1;
			}else{
				return ((Flight) resourceItem).getPrice();
			}
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// return flightprice;
    }

    public int queryRooms(int xid, String location)
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "queryRooms");
		}
		if (location == null) {
			return -1;
		}
		try {
			ResourceItem resourceItem = this.rmRooms.query(xid, this.rmRooms.getID(), location);
			if (resourceItem == null)
				return -1;
			else
				return ((Hotel) resourceItem).getNumAvail();
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// return roomscounter;
    }

    public int queryRoomsPrice(int xid, String location)
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "queryRoomsPrice");
		}
		if (location == null) {
			return -1;
		}
		try {
			ResourceItem resourceItem = this.rmRooms.query(xid, this.rmRooms.getID(), location);
			if (resourceItem == null)
				return -1;
			else
				return ((Hotel) resourceItem).getPrice();
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// return roomsprice;
    }

    public int queryCars(int xid, String location)
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "queryCars");
		}
		if (location == null) {
			return -1;
		}
		try {
			ResourceItem resourceItem = this.rmCars.query(xid, this.rmCars.getID(), location);
			if (resourceItem == null) {
				return -1;
			}else{
				return ((Car) resourceItem).getNumAvail();
			}
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// return carscounter;
    }

    public int queryCarsPrice(int xid, String location)
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "queryCarsPrice");
		}
		if (location == null) {
			return -1;
		}
		try {
			ResourceItem resourceItem = this.rmCars.query(xid, this.rmCars.getID(), location);
			if (resourceItem == null) {
				return -1;
			}else{
				return ((Car) resourceItem).getPrice();
			}
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
		// return carsprice;
    }

    public int queryCustomerBill(int xid, String custName)
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "queryCustomerBill");
		}
		if (custName == null) {//null
            return -1;
        }
        try {
            ResourceItem resourceItem = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceItem == null) {// no exist
                return -1;
            }
            Collection reservations = this.rmCustomers.query(
                    xid,
                    ResourceManager.TableNameReservations,
                    Reservation.INDEX_CUSTNAME,
                    custName
            );
			int totalBill = 0;
            for (Object resv : reservations) {
                Reservation reservation = (Reservation) resv;
                String resvKey = reservation.getResvKey();
                int resvType = reservation.getResvType();
                if (resvType == Reservation.RESERVATION_TYPE_FLIGHT) {
                    Flight flight = (Flight) this.rmFlights.query(xid, this.rmFlights.getID(), resvKey);
                    totalBill += flight.getPrice();
                } else if (resvType == Reservation.RESERVATION_TYPE_HOTEL) {
                    Hotel hotel = (Hotel) this.rmRooms.query(xid, this.rmRooms.getID(), resvKey);
                    totalBill += hotel.getPrice();
                } else if (resvType == Reservation.RESERVATION_TYPE_CAR) {
                    Car car = (Car) this.rmCars.query(xid, this.rmCars.getID(), resvKey);
                    totalBill += car.getPrice();
                }
            }
			return totalBill;
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        } catch (InvalidIndexException e) {//query need
            e.printStackTrace();
            return -1;
        }
			// return 0;
    }


    // RESERVATION INTERFACE
    public boolean reserveFlight(int xid, String custName, String flightNum) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
	// flightcounter--;
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "reserveFlight");
		}
        if (custName == null || flightNum == null) {// null
            return false;
        }
		try {
			// cust or flight doesn't exist
            ResourceItem resourceCustomer = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceCustomer == null) {
                return false;
            }
            ResourceItem resourceItemFlight = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
            if (resourceItemFlight == null) {
                return false;
            }
            Flight flight = (Flight) resourceItemFlight;
            if (!flight.addResv()) {// no seats left
                return false;
            }
            this.rmFlights.update(xid, this.rmFlights.getID(), flightNum, (ResourceItem)flight);
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_FLIGHT, flightNum);
			// ResourceItem reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_FLIGHT, flightNum);
            return this.rmCustomers.insert(xid, ResourceManager.TableNameReservations, reservation);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
		// return true;
    }
 
    public boolean reserveCar(int xid, String custName, String location) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
	// carscounter--;
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "reserveCar");
		}
		if (custName == null || location == null) {//null
			return false;
		}
        try {// cust or Car doesn't exist
            ResourceItem resourceCustomer = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceCustomer == null) {
                return false;
            }
            ResourceItem resourceItemCar = this.rmCars.query(xid, this.rmCars.getID(), location);
            if (resourceItemCar == null) {
                return false;
            }
            Car car = (Car) resourceItemCar;
            if (!car.addResv()) {// no seats left
                return false;
            }
            this.rmCars.update(xid, this.rmCars.getID(), location, (ResourceItem)car);
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_CAR, location);
            return this.rmCustomers.insert(xid, ResourceManager.TableNameReservations, reservation);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
		// return true;
    }

    public boolean reserveRoom(int xid, String custName, String location) 
	throws RemoteException, 
	       TransactionAbortedException,
	       InvalidTransactionException {
	// roomscounter--;
		if (!this.xids.contains(xid)) {
			throw new InvalidTransactionException(xid, "reserveRoom");
		}
		if (custName == null || location == null) {//null
			return false;
		}
		try {// cust or Room doesn't exist
			ResourceItem resourceCustomer = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
			if (resourceCustomer == null) {
				return false;
			}
			ResourceItem resourceItemRoom = this.rmRooms.query(xid, this.rmRooms.getID(), location);
			if (resourceItemRoom == null) {
				return false;
			}
			Hotel hotel = (Hotel) resourceItemRoom;
			if (!hotel.addResv()) {//no room
				return false;
			}
			this.rmRooms.update(xid, this.rmRooms.getID(), location, (ResourceItem)hotel);
			Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_HOTEL, location);
			return this.rmCustomers.insert(xid, ResourceManager.TableNameReservations, reservation);
		} catch (DeadlockException e) {
			this.abort(xid);
			throw new TransactionAbortedException(xid, e.getMessage());
		}
	// return true;
    }

    public boolean reserveItinerary(int xid, String custName, List flightNumList, String location, boolean needCar, boolean needRoom)
        throws RemoteException,
				TransactionAbortedException,
				InvalidTransactionException {
		if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "reserveItinerary");
        }
		if (custName == null || location == null || flightNumList == null) {//null
            return false;
        }
        try {
            ResourceItem resourceCustomer = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceCustomer == null) {// cust don't exist
                return false;
            }
            List<String> flights = new ArrayList<>();
            for (Object obj : flightNumList) {
                String flightNum = (String) obj;
                ResourceItem resourceFlight = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
                if (resourceFlight == null) {// flight don't exist
                    return false;
                }
                Flight flight = (Flight) resourceFlight;
                if (flight.getNumAvail() < 1) {// no seats
                    return false;
                }
                flights.add(flightNum);
            }
			for (String flightNum : flights) {
                boolean resv = this.reserveFlight(xid, custName, flightNum);
                if (!resv) {
                    return false;
                }
            }
			if (needCar) {
                ResourceItem resourceCar = this.rmCars.query(xid, this.rmCars.getID(), location);
                if (resourceCar == null) {
                    return false;
                }
                Car car = (Car) resourceCar;
                if (car.getNumAvail() < 1) {// no car
                    return false;
                }
				if (!this.reserveCar(xid, custName, location)) {
                    return false;
                }
            }
            if (needRoom) {
                ResourceItem resourceRoom = this.rmRooms.query(xid, this.rmRooms.getID(), location);
                if (resourceRoom == null) {
                    return false;
                }
                Hotel hotel = (Hotel) resourceRoom;
                if (hotel.getNumAvail() < 1) {//no room
                    return false;
                }
				if (!this.reserveRoom(xid, custName, location)) {
                    return false;
                }
            }
            return true;
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
		// return true;
    }


    // TECHNICAL/TESTING INTERFACE
    public boolean reconnect()
	throws RemoteException {
	String rmiPort = System.getProperty("rmiPort");
	if (rmiPort == null) {
	    rmiPort = "";
	} else if (!rmiPort.equals("")) {
	    rmiPort = "//:" + rmiPort + "/";
	}

	try {
	    // rmFlights =
		// (ResourceManager)Naming.lookup(rmiPort +
		// 			       ResourceManager.RMINameFlights);
	    // System.out.println("WC bound to RMFlights");
	    // rmRooms =
		// (ResourceManager)Naming.lookup(rmiPort +
		// 			       ResourceManager.RMINameRooms);
	    // System.out.println("WC bound to RMRooms");
	    // rmCars =
		// (ResourceManager)Naming.lookup(rmiPort +
		// 			       ResourceManager.RMINameCars);
	    // System.out.println("WC bound to RMCars");
	    // rmCustomers =
		// (ResourceManager)Naming.lookup(rmiPort +
		// 			       ResourceManager.RMINameCustomers);
	    // System.out.println("WC bound to RMCustomers");
	    // tm =
		// (TransactionManager)Naming.lookup(rmiPort +
		// 				  TransactionManager.RMIName);
	    // System.out.println("WC bound to TM");
		Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);

		rmFlights = (ResourceManager) registry.lookup(rmiPort + ResourceManager.RMINameFlights);
		System.out.println("WC bound to RMFlights");

		rmRooms = (ResourceManager) registry.lookup(rmiPort + ResourceManager.RMINameRooms);
		System.out.println("WC bound to RMRooms");

		rmCars = (ResourceManager) registry.lookup(rmiPort + ResourceManager.RMINameCars);
		System.out.println("WC bound to RMCars");

		rmCustomers = (ResourceManager) registry.lookup(rmiPort + ResourceManager.RMINameCustomers);
		System.out.println("WC bound to RMCustomers");

		tm = (TransactionManager) registry.lookup(rmiPort + TransactionManager.RMIName);
		System.out.println("WC bound to TM");
	} 
	catch (Exception e) {
	    System.err.println("WC cannot bind to some component:" + e);
	    return false;
	}

	try {
	    if (rmFlights.reconnect() && rmRooms.reconnect() &&
		rmCars.reconnect() && rmCustomers.reconnect()) {
		return true;
	    }
	} catch (Exception e) {
	    System.err.println("Some RM cannot reconnect:" + e);
	    return false;
	}

	return false;
    }

    public boolean dieNow(String who)
	throws RemoteException {
	if (who.equals(TransactionManager.RMIName) ||
	    who.equals("ALL")) {
	    try {
		tm.dieNow();
	    } catch (RemoteException e) {}
	}
	if (who.equals(ResourceManager.RMINameFlights) ||
	    who.equals("ALL")) {
	    try {
		rmFlights.dieNow();
	    } catch (RemoteException e) {}
	}
	if (who.equals(ResourceManager.RMINameRooms) ||
	    who.equals("ALL")) {
	    try {
		rmRooms.dieNow();
	    } catch (RemoteException e) {}
	}
	if (who.equals(ResourceManager.RMINameCars) ||
	    who.equals("ALL")) {
	    try {
		rmCars.dieNow();
	    } catch (RemoteException e) {}
	}
	if (who.equals(ResourceManager.RMINameCustomers) ||
	    who.equals("ALL")) {
	    try {
		rmCustomers.dieNow();
	    } catch (RemoteException e) {}
	}
	if (who.equals(WorkflowController.RMIName) ||
	    who.equals("ALL")) {
	    System.exit(1);
	}
	return true;
    }
	private boolean dieRMTime(String who, String time) {
        ResourceManager resourceManager = null;
		

        switch (who) {
            case ResourceManager.RMINameFlights:
                resourceManager = this.rmFlights;
                break;
            case ResourceManager.RMINameRooms:
                resourceManager = this.rmRooms;
                break;
            case ResourceManager.RMINameCars:
                resourceManager = this.rmCars;
                break;
            case ResourceManager.RMINameCustomers:
                resourceManager = this.rmCustomers;
                break;
            default:
                System.err.println("Invalid RMIName");
                break;
        }

        try {
            resourceManager.setDieTime(time);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }
    public boolean dieRMAfterEnlist(String who)
	throws RemoteException {
		return this.dieRMTime(who, "AfterEnlist");
    }
    public boolean dieRMBeforePrepare(String who)
	throws RemoteException {
		return this.dieRMTime(who, "BeforePrepare");
    }
    public boolean dieRMAfterPrepare(String who)
	throws RemoteException {
		return this.dieRMTime(who, "AfterPrepare");
    }
    public boolean dieTMBeforeCommit()
	throws RemoteException {
		this.tm.setDieTime("BeforeCommit");
	return true;
    }
    public boolean dieTMAfterCommit()
	throws RemoteException {
		this.tm.setDieTime("AfterCommit");
	return true;
    }
    public boolean dieRMBeforeCommit(String who)
	throws RemoteException {
		return this.dieRMTime(who, "BeforeCommit");
    }
    public boolean dieRMBeforeAbort(String who)
	throws RemoteException {
		return this.dieRMTime(who, "BeforeAbort");
    }
}
