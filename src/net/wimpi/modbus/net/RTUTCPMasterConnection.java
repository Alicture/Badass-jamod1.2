package net.wimpi.modbus.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.io.ModbusRTUTCPTransport;
import net.wimpi.modbus.io.ModbusTransport;

public class RTUTCPMasterConnection implements MasterConnection {
	private Socket socket;
	private int socketTimeout = Modbus.DEFAULT_TIMEOUT;
	private boolean connected;
	private InetAddress slaveIPAddress;
	private int slaveIPPort;
	private ModbusRTUTCPTransport modbusRTUTCPTransport;

	public RTUTCPMasterConnection(InetAddress addr, int port) {
		this.slaveIPAddress = addr;
		this.slaveIPPort = port;
	}

	@Override
	public synchronized void connect() throws Exception {
		if (!this.connected) {
			this.socket = new Socket(this.slaveIPAddress, this.slaveIPPort);
			
			
			prepareTransport();
			this.modbusRTUTCPTransport.setTimeOut(this.socketTimeout);
			connected=true;
			System.out.println("success");
		}
	}

	public ModbusTransport getModbusTransport() {
		return this.modbusRTUTCPTransport;
	}

	// 准备IO
	private void prepareTransport() throws IOException {

		if (this.modbusRTUTCPTransport == null) {
			this.modbusRTUTCPTransport = new ModbusRTUTCPTransport(this.socket);
		} else {
			this.modbusRTUTCPTransport.setSocket(this.socket);
		}
	}
//设置超时时间
	public void setTimeout(int timeout) throws IOException {
		this.socketTimeout = timeout;
		
		if (this.socket != null) {
			try {
				this.socket.setSoTimeout(socketTimeout);
			
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public int getTimeout() {
		return this.socketTimeout;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void close() {
		if(connected==true)
		{
			try {
				this.modbusRTUTCPTransport.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.connected=false;
		}
	}

}
