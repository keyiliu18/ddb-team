package transaction;

import java.rmi.*;

import javax.transaction.InvalidTransactionException;

/**
 * Interface for the Transaction Manager of the Distributed Travel
 * Reservation System.
 * <p>
 * Unlike WorkflowController.java, you are supposed to make changes
 * to this file.
 */
/**
 * Start(),commit(),abort()由WC调用，分别用来启动、提交、中断一个事务。启动一个事务时，记录当前事务的信息，commit或abort成功时，删除当前的事务信息
 * enlist()函数由RM来使用，用来通知TM哪个RM参与了哪个事务
 * 
 * Resource Manager Interface 被Transaction Manager调用的方法 Prepare/Commit/Abort()
 * transaction Manager Interface 被Resource Manager调用的方法Enlist()
 * TM要解决的问题:
 * 当发生错误时适时合理地中止事务
 * 事务恢复和回滚
 * 在错误解决时恢复原本可以正确执行的操作/把事务已做的操作全部撤销
 * 
 * 
 */
public interface TransactionManager extends Remote {

    public boolean dieNow() throws RemoteException;

    public void ping() throws RemoteException;

    public void enlist(int xid, ResourceManager rm) throws RemoteException;

    public void setDieTime(String time) throws RemoteException;

    /**
     * Start a new transaction, and return its transaction id.
     *
     * @return A unique transaction ID > 0. Return <=0 if server is not accepting
     *         new transactions.
     *
     * @throws RemoteException on communications failure.
     */
    public int start() throws RemoteException;

    /**
     * Commit transaction.
     *
     * @param xid id of transaction to be committed.
     * @return true on success, false on failure.
     *
     * @throws RemoteException             on communications failure.
     * @throws TransactionAbortedException if transaction was aborted.
     * @throws InvalidTransactionException if transaction id is invalid.
     */
    public boolean commit(int xid)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException;

    /**
     * Abort transaction.
     *
     * @param xid id of transaction to be aborted.
     *
     * @throws RemoteException             on communications failure.
     * @throws InvalidTransactionException if transaction id is invalid.
     */
    public void abort(int xid)
            throws RemoteException,
            InvalidTransactionException;

    /** The RMI name a TransactionManager binds to. */
    public static final String RMIName = "TM";

    /** The transaction's status */
    public static final String START = "START";
    public static final String PREPARED = "PREPARED";
    public static final String COMMITTED = "COMMITTED";
    public static final String ABORTED = "ABORTED";
}
