package net.wimpi.modbus.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

/**
 * @author cswfq
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.msg.ModbusMessage;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.util.ModbusUtil;

/**
 * @author cswfq
 * 
 */
public class ModbusTCPTransport2 implements ModbusTransport {

	public static final String logId = "[ModbusRTUTCPcswfq]:";
	// The input stream from which reading the Modbus frames
	 
	private DataInputStream m_Input;

	// The output stream to which writing the Modbus frames
	private DataOutputStream m_Output;
	 
	// The Bytes output stream to use as output buffer for Modbus frames
	private BytesOutputStream outputBuffer;

	// The BytesInputStream wrapper for the transport input stream
	private BytesInputStream m_ByteIn;

	// The last request sent over the transport ?? useful ??
	private byte[] lastRequest = null;

	// the socket used by this transport
	private Socket socket;

	// the read timeout timer
	private Timer readTimeoutTimer;

	// the read timout
	private int readTimeout = 5000; // ms

	// the timeou flag
	private boolean isTimedOut;

	/**
	 * @param socket
	 * @throws IOException
	 * 
	 */
	public ModbusTCPTransport2(Socket socket) throws IOException {
		try {
			setSocket(socket);
		} catch (IOException ex) {
			if (Modbus.debug)
				System.out.println("ModbusTCPTransport::Socket invalid.");
			// @commentstart@
			throw new IllegalStateException("Socket invalid.");
			// @commentend@
		}
	}

	// set read time out
	public void setTimeOut(int ms) throws IOException {
		if (ms >= 1000)
			readTimeout = ms;
	}

	/**
	 * Stores the given {@link Socket} instance and prepares the related streams
	 * to use them for Modbus RTU over TCP communication.
	 * 
	 * @param socket
	 * @throws IOException
	 */
	public void setSocket(Socket socket) throws IOException {
		prepareStreams(socket);
	}

	/**
	 * writes the given ModbusMessage over the physical transport handled by
	 * this object.
	 * 
	 * @param msg
	 *            the {@link ModbusMessage} to be written on the transport.
	 */
	@Override
	public void writeMessage(ModbusMessage msg) throws ModbusIOException {
		try {
			// atomic access to the output buffer
			synchronized (outputBuffer) {

				// reset the output buffer
				outputBuffer.reset();

				// prepare the message for "virtual" serial transport
				msg.setHeadless();

				// write the message to the output buffer
				msg.writeTo(outputBuffer);

				// compute the CRC
				int[] crc = ModbusUtil.calculateCRC(outputBuffer.getBuffer(), 0, outputBuffer.size());

				// write the CRC on the output buffer
				outputBuffer.writeByte(crc[0]);
				outputBuffer.writeByte(crc[1]);

				// store the buffer length
				int bufferLength = outputBuffer.size();

				// store the raw output buffer reference
				byte rawBuffer[] = outputBuffer.getBuffer();

				// write the buffer on the socket
				m_Output.write(rawBuffer, 0, bufferLength); // PDU +
																// CRC
				m_Output.flush();

				// debug
				if (Modbus.debug)
					System.out.println("Sent: " + ModbusUtil.toHex(rawBuffer, 0, bufferLength));

				// store the written buffer as the last request
				lastRequest = new byte[bufferLength];
				System.arraycopy(rawBuffer, 0, lastRequest, 0, bufferLength);

				// sleep for the time needed to receive the request at the other
				// point of the connection
				Thread.sleep(bufferLength);
			}

		} catch (Exception ex) {
			throw new ModbusIOException("I/O failed to write");
		}

	}// writeMessage

	// This is required for the slave that is not supported
	@Override
	public ModbusRequest readRequest() throws ModbusIOException {
		throw new RuntimeException("Operation not supported.");
	} // readRequest

	@Override
	/**
	 * Lazy implementation: avoid CRC validation...
	 */
	public ModbusResponse readResponse() throws ModbusIOException {
		// the received response
		ModbusResponse response = null;

		// reset the timed out flag
		isTimedOut = false;

		// init and start the timeout timer
		readTimeoutTimer = new Timer();
		readTimeoutTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				isTimedOut = true;
			}
		}, readTimeout);

		try {
			// atomic access to the input buffer
			synchronized (m_ByteIn) {
				// clean the input buffer
				m_ByteIn.reset(new byte[Modbus.MAX_MESSAGE_LENGTH]);

				// sleep for the time needed to receive the first part of the
				// response
				int available = m_Input.available();
				while ((available < 4) && (!isTimedOut)) {
					Thread.yield(); // 1ms * #bytes (4bytes in the worst case)
					available = m_Input.available();

					if (Modbus.debug)
						System.out.println("Available bytes: " + available);
				}

				// check if timedOut
				if (isTimedOut)
					throw new ModbusIOException("I/O exception - read timeout.\n");

				// get a reference to the inner byte buffer
				byte inBuffer[] = m_ByteIn.getBuffer();

				// read the first 2 bytes from the input stream
				m_Input.read(inBuffer, 0, 2);
				// inputStream.readFully(inBuffer);

				// read the progressive id
				int packetId = m_ByteIn.readUnsignedByte();

				// debug
				if (Modbus.debug)
					System.out.println(ModbusTCPTransport2.logId + "Read packet with progressive id: " + packetId);

				// read the function code
				int functionCode = m_ByteIn.readUnsignedByte();

				// debug
				if (Modbus.debug)
					System.out.println(" uid: " + packetId + ", function code: " + functionCode);

				// compute the number of bytes composing the message (including
				// the CRC = 2bytes)
				int packetLength = computePacketLength(functionCode);

				// sleep for the time needed to receive the first part of the
				// response
				while ((m_Input.available() < (packetLength - 3)) && (!isTimedOut)) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException ie) {
						// do nothing
						System.err.println("Sleep interrupted while waiting for response body...\n" + ie);
					}
				}

				// check if timedOut
				if (isTimedOut)
					throw new ModbusIOException("I/O exception - read timeout.\n");

				// read the remaining bytes
				if (functionCode == 5 || functionCode == 6 || functionCode == 15 || functionCode == 16)
					m_Input.read(inBuffer, 2, packetLength);
				else
					m_Input.read(inBuffer, 3, packetLength);

				// debug
				if (Modbus.debug)
					System.out.println(" bytes: " + ModbusUtil.toHex(inBuffer, 0, packetLength) + ", desired length: "
							+ packetLength);

				// compute the CRC
				int crc[] = ModbusUtil.calculateCRC(inBuffer, 0, packetLength - 2);

				// check the CRC against the received one...
				if (ModbusUtil.unsignedByteToInt(inBuffer[packetLength - 2]) != crc[0]
						|| ModbusUtil.unsignedByteToInt(inBuffer[packetLength - 1]) != crc[1]) {
					throw new IOException("CRC Error in received frame: " + packetLength + " bytes: "
							+ ModbusUtil.toHex(inBuffer, 0, packetLength));
				}

				// reset the input buffer to the given packet length (excluding
				// the CRC)
				m_ByteIn.reset(inBuffer, packetLength - 2);

				// create the response
				response = ModbusResponse.createModbusResponse(functionCode);
				response.setHeadless();

				// read the response
				response.readFrom(m_ByteIn);
				// inBuffer=inputBuffer.reset(inBuffer, packetLength).
				// response.readData(inputStream);
			}
		} catch (IOException e) {
			// debug
			System.err.println(ModbusTCPTransport2.logId + "Error while reading from socket: " + e);

			// clean the input stream
			try {
				while (m_Input.read() != -1)
					;
			} catch (IOException e1) {
				// debug
				System.err.println(ModbusTCPTransport2.logId + "Error while emptying input buffer from socket: " + e);
			}

			// wrap and re-throw
			throw new ModbusIOException("I/O exception - failed to read.\n" + e);
		}

		// reset the timeout timer
		readTimeoutTimer.cancel();

		// return the response read from the socket stream
		return response;

		/*-------------------------- SERIAL IMPLEMENTATION -----------------------------------
		
		try
		{
			do
			{
				// block the input stream
				synchronized (byteInputStream)
				{
					// get the packet uid
					int uid = inputStream.read();
					
					if (Modbus.debug)
						System.out.println(ModbusRTUTCPTransport.logId + "UID: " + uid);
					
					// if the uid is valid (i.e., > 0) continue
					if (uid != -1)
					{
						// get the function code
						int fc = inputStream.read();
						
						if (Modbus.debug)
							System.out.println(ModbusRTUTCPTransport.logId + "Function code: " + uid);
						
						//bufferize the response
						byteOutputStream.reset();
						byteOutputStream.writeByte(uid);
						byteOutputStream.writeByte(fc);
						
						// create the Modbus Response object to acquire length of message
						response = ModbusResponse.createModbusResponse(fc);
						response.setHeadless();
						
						// With Modbus RTU, there is no end frame. Either we
						// assume the message is complete as is or we must do
						// function specific processing to know the correct length. 
						
						//bufferize the response according to the given function code
						getResponse(fc, byteOutputStream);
						
						//compute the response length without considering the CRC
						dlength = byteOutputStream.size() - 2; // less the crc
						
						//debug
						if (Modbus.debug)
							System.out.println("Response: "
									+ ModbusUtil.toHex(byteOutputStream.getBuffer(), 0, dlength + 2));
						
						//TODO: check if needed (restore the buffer state, cursor at 0, same content)
						byteInputStream.reset(inputBuffer, dlength);
						
						// cmopute the buffer CRC
						int[] crc = ModbusUtil.calculateCRC(inputBuffer, 0, dlength); 
						
						// check the CRC against the received one...
						if (ModbusUtil.unsignedByteToInt(inputBuffer[dlength]) != crc[0]
								|| ModbusUtil.unsignedByteToInt(inputBuffer[dlength + 1]) != crc[1])
						{
							throw new IOException("CRC Error in received frame: " + dlength + " bytes: "
									+ ModbusUtil.toHex(byteInputStream.getBuffer(), 0, dlength));
						}
					}
					else
					{
						throw new IOException("Error reading response");
					}
					
					// restore the buffer state, cursor at 0, same content
					byteInputStream.reset(inputBuffer, dlength);
					
					//actually read the response
					if (response != null)
					{
						response.readFrom(byteInputStream);
					}
					
					//flag completion...
					done = true;
					
				}// synchronized
			}
			while (!done);
			return response;
		}
		catch (Exception ex)
		{
			System.err.println("Last request: " + ModbusUtil.toHex(lastRequest));
			System.err.println(ex.getMessage());
			throw new ModbusIOException("I/O exception - failed to read");
		}
		
		------------------------------------------------------------------------------*/
	}// readResponse

	private int computePacketLength(int functionCode) throws IOException {
		// packet length by function code:
		int length = 0;

		switch (functionCode) {
		case 0x01:
		case 0x02:
		case 0x03:
		case 0x04:
		case 0x0C:
		case 0x11: // report slave ID version and run/stop state
		case 0x14: // read log entry (60000 memory reference)
		case 0x15: // write log entry (60000 memory reference)
		case 0x17: {
			// get a reference to the inner byte buffer
			byte inBuffer[] = m_ByteIn.getBuffer();
			m_Input.read(inBuffer, 2, 1);
			int dataLength = m_ByteIn.readUnsignedByte();
			length = dataLength + 5; // UID+FC+CRC(2bytes)
			break;
		}
		case 0x05:
		case 0x06:

		case 0x0B:
		case 0x0F:
		case 0x10: {
			// read status: only the CRC remains after address and
			// function code
			length = 8;
			break;
		}
		case 0x07:
		case 0x08: {
			length = 3;
			break;
		}
		case 0x16: {
			length = 8;
			break;
		}
		case 0x18: {
			// get a reference to the inner byte buffer
			byte inBuffer[] = m_ByteIn.getBuffer();
			m_Input.read(inBuffer, 2, 2);
			length = m_ByteIn.readUnsignedShort() + 6;// UID+FC+CRC(2bytes)
			break;
		}
		case 0x83: {
			// error code
			length = 5;
			break;
		}
		}

		return length;
	}

	@Override
	public void close() throws IOException {
		m_Input.close();
		m_Output.close();
		outputBuffer.close();
		m_ByteIn.close();
	}// close

	private void prepareStreams(Socket socket) throws IOException {

		m_Input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		m_Output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		m_ByteIn = new BytesInputStream(Modbus.MAX_MESSAGE_LENGTH);

		outputBuffer = new BytesOutputStream(Modbus.MAX_MESSAGE_LENGTH);

	}// prepareStreams

}