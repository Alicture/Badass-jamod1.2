package net.wimpi.modbus.io;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.msg.ExceptionResponse;
import net.wimpi.modbus.msg.ModbusMessageImpl;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.net.RTUTCPMasterConnection;
import net.wimpi.modbus.util.AtomicCounter;
import net.wimpi.modbus.util.Mutex;

public class ModbusRTUTCPTransaction implements ModbusTransaction {

	private static AtomicCounter c_TransactionID = new AtomicCounter(Modbus.DEFAULT_TRANSACTION_ID);

	private RTUTCPMasterConnection m_Connection;
	private ModbusRTUTCPTransport m_IO;
	private ModbusRequest m_Request;
	private ModbusResponse m_Response;
	private boolean m_ValidityCheck = Modbus.DEFAULT_VALIDITYCHECK;
	private boolean m_Reconnecting = Modbus.DEFAULT_RECONNECTING;
	private int m_Retries = Modbus.DEFAULT_RETRIES;

	private Mutex m_TransactionLock = new Mutex();
	//writeMessage
	public void writeMessage (ModbusMessageImpl modbuscmd)
	{
		try {
			m_IO.writeMessage(modbuscmd);
		} catch (ModbusIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ModbusRTUTCPTransaction() {
		// TODO Auto-generated constructor stub
	}
	
	public boolean isReconnecting() {
		return m_Reconnecting;
	}

	public void setReconnecting(boolean m_Reconnecting) {
		this.m_Reconnecting = m_Reconnecting;
	}

	public ModbusRTUTCPTransaction(ModbusRequest req) {
		setRequest(req);
	}

	public ModbusRTUTCPTransaction(RTUTCPMasterConnection con) {
		setConnection(con);
	}

	public void setConnection(RTUTCPMasterConnection con) {
		m_Connection = con;
		m_IO = (ModbusRTUTCPTransport) con.getModbusTransport();
	}

	@Override
	public void setRequest(ModbusRequest req) {
		try {
			m_IO.writeMessage(req);
			m_Request=req;
		} catch (ModbusIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public ModbusRequest getRequest() {
		// TODO Auto-generated method stub
		return m_Request;
	}

	@Override
	public ModbusResponse getResponse() {
		// TODO Auto-generated method stub
		try {
			m_Response=m_IO.readResponse();
			
		} catch (ModbusIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return m_Response;
	}

	@Override
	public int getTransactionID() {
		// TODO Auto-generated method stub
		return c_TransactionID.get();
	}

	@Override
	public void setRetries(int retries) {
		m_Retries = retries;
	}

	@Override
	public int getRetries() {
		// TODO Auto-generated method stub
		return m_Retries;
	}

	@Override
	public void setCheckingValidity(boolean b) {
		m_ValidityCheck = b;

	}

	@Override
	public boolean isCheckingValidity() {
		// TODO Auto-generated method stub
		return m_ValidityCheck;
	}

	@Override
	public void execute() throws ModbusException {
		// TODO Auto-generated method stub
		if (m_Request == null || m_Connection == null) {
			throw new ModbusException("执行时发生错误");
		}

		try {
			m_TransactionLock.acquire();
			if (!m_Connection.isConnected()) {
				try {
					m_Connection.connect();
					m_IO = (ModbusRTUTCPTransport) m_Connection.getModbusTransport();
				} catch (Exception e) {
					throw new ModbusException("连接时发生异常");
				}
				int retryCounter = 0;

				while (retryCounter <= m_Retries) {
					try {
						// toggle and set the id
						m_Request.setTransactionID(c_TransactionID.increment());
						// 3. write request, and read response
						m_IO.writeMessage(m_Request);
						// read response message
						m_Response = m_IO.readResponse();
						break;
					} catch (ModbusIOException ex) {
						if (retryCounter == m_Retries) {
							throw new ModbusIOException("Executing transaction failed (tried " + m_Retries + " times)");
						} else {
							retryCounter++;
							continue;
						}
					}
				}
				// 5. deal with "application level" exceptions
				if (m_Response instanceof ExceptionResponse)
				{
					throw new ModbusSlaveException(((ExceptionResponse) m_Response).getExceptionCode());
				}
				
				// 6. close connection if reconnecting
				if (isReconnecting())
				{
					m_Connection.close();
				}
				
				// 7. Check transaction validity
				if (isCheckingValidity())
				{
					checkValidity();
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			m_TransactionLock.release();
		}
	}
	protected void checkValidity() throws ModbusException {
	  }//checkValidity
}