package net.wimpi.modbus.net;

public interface MasterConnection {
	public void connect() throws Exception;
	public boolean isConnected();
	public void close();
	
}
