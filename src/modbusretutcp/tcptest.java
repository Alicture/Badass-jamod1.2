/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package modbusretutcp;

import net.wimpi.modbus.io.ModbusRTUTCPTransport;

//import java.net.InetAddress;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusCoupler;
//import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.RTUTCPMasterConnection;
//import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.Register;
//import net.wimpi.modbus.msg.ModbusMessageImpl;
import net.wimpi.modbus.util.BitVector;

import java.net.InetAddress;
//import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Calendar;
 
//import net.wimpi.modbus.io.BytesInputStream;
import net.wimpi.modbus.io.BytesOutputStream;
import net.wimpi.modbus.io.ModbusRTUTCPTransaction;
import net.wimpi.modbus.util.ModbusUtil;

/**
 *
 * @author Administrator
 */
public class tcptest {

	// 1. Setup parameters
	private static int requestNumber = 0;
	RTUTCPMasterConnection con =null;
	ModbusRTUTCPTransaction trans = null;
	ModbusRTUTCPTransport rtutcptansaction;
	Socket cliente;
	int port = 29684;// 502
	int slaveId = 1; // Same as TCPSlaveTest.java
	String IPaddress = "60.2.213.172";
	ReadMultipleRegistersRequest ReadMreq =null;
	ReadMultipleRegistersResponse ReadMres =null;
	WriteSingleRegisterRequest WSRreq=null;
	WriteSingleRegisterResponse WSRres=null;
	WriteCoilResponse WCres;
	WriteCoilRequest WCreq;
	ReadCoilsResponse RCres;
	ReadCoilsRequest RCreq;

	public tcptest() {

	}

	private void ReadMultipleRegisters(int D, int num) {

		// modbus cmd Modbus命令
		try {
			trans = new ModbusRTUTCPTransaction(con);
			ReadMreq = new ReadMultipleRegistersRequest(320, 10);
			ReadMreq.setUnitID(slaveId);
			trans.setRequest(ReadMreq);
			trans.setRetries(5);
			trans.execute();
			Thread.sleep(1000);
			ReadMres=(ReadMultipleRegistersResponse) trans.getResponse();
			
			for (int n = 0; n < ReadMres.getWordCount(); n++) {
				int ad= D+n;
				System.out.println("Word " + ad + "=" + ReadMres.getRegisterValue(n));
			} 
			
			 
			System.out.println("end reading modbus " + " D:" + D + " num:" + num);
			System.out.println(System.getProperty("line.separator"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void WriteSingleRegister(int D, int v) {
		// modbus cmd Modbus命令
		try {
			Register r = ModbusCoupler.getReference().getProcessImageFactory().createRegister();
			r.setValue(v);
			trans = new ModbusRTUTCPTransaction(con);
			WSRreq = new WriteSingleRegisterRequest(D, r);
			WSRreq.setUnitID(slaveId);
			trans.setRequest(WSRreq);
			
			trans.execute();
			Thread.sleep(1000);
			WSRres=(WriteSingleRegisterResponse) trans.getResponse();
		
			
			System.out.println("WriteSingleRegister" + " D:" + D + " val:" + v);
			
			Thread.sleep(500);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	private void WriteSingleCoil(int D, Boolean v) {
		// modbus cmd Modbus命令
		try {
			trans = new ModbusRTUTCPTransaction(con);
			WCreq = new WriteCoilRequest(D, v);
			WCreq.setUnitID(slaveId);
			trans.setRequest(WCreq);
			trans.execute();
			
			System.out.println("WriteSingleCoil" + " D:" + D + " num:" + v);
			
			Thread.sleep(500);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	private void ReadCoils(int D, int num) {

		// modbus cmd Modbus命令
		try {
			trans = new ModbusRTUTCPTransaction(con);
			RCreq = new ReadCoilsRequest(D, num);
			RCreq.setUnitID(slaveId);
			trans.setRequest(RCreq);
			trans.setRetries(5);
			trans.execute();
			RCres = (ReadCoilsResponse) trans.getResponse();
			for (int n = 0; n < RCres.getBitCount(); n++) {
				System.out.println("Word " + D+n + "=" + RCres.getCoilStatus(n));
			}

			System.out.println("end ReadCoils " + " D:" + D + " num:" + num);
			System.out.println(System.getProperty("line.separator"));
 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void test() {

		try {

			con = new RTUTCPMasterConnection(InetAddress.getByName(IPaddress), port);
			con.setTimeout(3000);
			con.connect();
//			trans = new ModbusRTUTCPTransaction(con);
//			ReadMreq = new ReadMultipleRegistersRequest(320, 10);
//			ReadMreq.setUnitID(slaveId);
//			trans.setRequest(ReadMreq);
//			
//			trans.execute();
//			Thread.sleep(1000);
//			ReadMres=(ReadMultipleRegistersResponse) trans.getResponse();
//			for(int i=0;i<ReadMres.getWordCount();i++)
//			{
//			System.out.println(ReadMres.getRegisterValue(i));
//			}
			ReadMultipleRegisters(320, 10);
			Thread.sleep(1000);
			ReadCoils(0, 16);
			
			con.close();
			//
//			Thread.sleep(1000);
//			trans = new ModbusRTUTCPTransaction(con);
//			
//			req = new ReadMultipleRegistersRequest(320, 10);
//
//			
//			trans.setRequest(req);
//			
//			trans.execute();
//			Thread.sleep(1500);
//			res = (ReadMultipleRegistersResponse) trans.getResponse();
//			Thread.sleep(1500);
//			System.out.println(res.getRegisterValue(0));
//			con.close();
//			rtutcptansaction = new ModbusRTUTCPTransport(cliente);
//			rtutcptansaction.setTimeOut(3000);
//			//读多个保持型D寄存器
//			 ReadMultipleRegisters(501, 10);  
//			 ReadMultipleRegisters(300, 32);  
//			 ReadMultipleRegisters(800, 16);  
//			 ReadMultipleRegisters(8030, 1);  
//			 ReadMultipleRegisters(348, 32);  
//			 ReadMultipleRegisters(389, 20);  
//			 ReadMultipleRegisters(250, 7);  
//			 
// 
//			  Thread.sleep(1000);
////			//读多个线圈 
////			 ReadCoils(0, 16);   
////			  Thread.sleep(1000);
////			//写单个  D寄存器
////			  WriteSingleRegister(303, 69);
////			  Thread.sleep(1000); 
////			//写单个  线圈 
////			  WriteSingleCoil(3, true);
////			  Thread.sleep(1000);
//			  
//			 
//			 rtutcptansaction.close();
//			cliente.close();
//			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO code application logic here

		tcptest Modbusretutcp = new tcptest();
		Modbusretutcp.test();
	}

	private static ModbusRequest getRequest(int cmd) {
		// Note: simple process image uses 0-based register addresses
		switch (cmd) {
		case 0:
			return new WriteCoilRequest(0, true);// Func:05 0xxxxx 写线圈
		case 1:
			return new ReadCoilsRequest(0, 16);// Func:01 0xxxxx 读线圈
		case 2:
			return new ReadInputDiscretesRequest(0, 10);// Func:02 1xxxxx 读输入状态
		case 3:
			return new ReadInputRegistersRequest(1, 3);//// Func:04 3xxxxx
														//// 读输入寄存器
		case 4:
			return new ReadMultipleRegistersRequest(300, 32);// Func:03 4xxxxx
																// 读寄存器
		case 5:
			Register r = ModbusCoupler.getReference().getProcessImageFactory().createRegister();
			r.setValue(18);
			return new WriteSingleRegisterRequest(0, r);// Func:06 4xxxxx 写单个寄存器
		case 6:// Func:15 0xxxxx 写多个线圈
				// public WriteMultipleCoilsRequest(int ref, BitVector bv)
			BitVector bv = new BitVector(13);// 位数13个bit
			byte[] val = new byte[2];
			val[0] = (byte) 255;
			val[1] = (byte) 255;
			bv.setBytes(val);
			return new WriteMultipleCoilsRequest(0, bv);// Func:15 0xxxxx 写多个线圈
		case 7:// Func:16 4xxxxx 写多个寄存器
				// public WriteMultipleRegistersRequest(int ref, Register[]
				// registers)
			Register[] registers = new Register[5];

			registers[0] = ModbusCoupler.getReference().getProcessImageFactory().createRegister();
			registers[1] = ModbusCoupler.getReference().getProcessImageFactory().createRegister();
			registers[2] = ModbusCoupler.getReference().getProcessImageFactory().createRegister();
			registers[3] = ModbusCoupler.getReference().getProcessImageFactory().createRegister();
			registers[4] = ModbusCoupler.getReference().getProcessImageFactory().createRegister();

			registers[0].setValue((short) 11);
			registers[1].setValue((short) 22);
			registers[2].setValue((short) 33);
			registers[3].setValue((short) 44);
			registers[4].setValue((short) 55);
			return new WriteMultipleRegistersRequest(0, registers);// Func:16
																	// 4xxxxx
																	// 写多个寄存器
		default:
			return null;
		}
	}
}
