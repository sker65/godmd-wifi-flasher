package com.rinke.solutions.godmd.flash;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DfuController {
	private OutputStream os;
	private InputStream is;
	private final ProgressListener listener;
	byte[] cmdCode = new byte[12];
	
	Map<Integer,String> cmdNameMap = new HashMap<>();
	
	public static final byte STM32_ACK	      = 0x79;
	public static final byte STM32_NACK	      = 0x1F;
	public static final byte STM32_CMD_INIT   =	0x7F;
	public static final byte STM32_CMD_GET	  = 0x00;
	private Cancelable cancelable;
	
	public DfuController(OutputStream os, InputStream is, ProgressListener listener, Cancelable cancelable) {
		super();
		this.listener = listener;
		this.os = os;
		this.is = is;
		this.cancelable = cancelable;
		cmdNameMap.put( 0x44, "erase pages ext");
		cmdNameMap.put( 0x43, "erase pages");
		cmdNameMap.put( 0x31, "write memory");
		cmdNameMap.put( 0x11, "read memory");
		cmdNameMap.put( 0x00, "get info");
	}

	public void initDevice() throws IOException {
		log.info("init device");
		os.write(STM32_CMD_INIT);
		if( is.read() != STM32_ACK ) throw new RuntimeException("no ack on init");
	}

	public void getInfo() throws IOException {
		log.info("get info from device");
		sendCommand(0x00);
		int number = is.read();
		log.info("bootloader version: {}", String.format("0x%02x", is.read()));
		int i = 0;
		while(number>0) {
			cmdCode[i] = (byte) is.read();
			log.info("cmdCode[{}]: {}", i, String.format("0x%02x", cmdCode[i]));
			number--;
			i++;
		}
		if( is.read() != STM32_ACK ) throw new RuntimeException("no ack on get info list");
	}
	
	public void sendCommand( int iCmd) throws IOException {
		log.info("send cmd {} ({})", Integer.toHexString(iCmd), cmdNameMap.get(iCmd));
		byte[] cmd = new byte[2];
		cmd[0] = (byte) iCmd;
		cmd[1] = (byte) (iCmd ^ 0xFF );
		os.write(cmd);
		os.flush();
		if( is.read() != STM32_ACK ) throw new RuntimeException(String.format("no ack on cmd 0x%02x", iCmd));
	}
	
	public void sendAdress(int c) throws IOException {
		log.debug("send address: ", Integer.toHexString(c));
		byte[] buf = new byte[5];
		buf[0] =(byte) ((c >> 24) & 0xFF);
		buf[1] =(byte) ((c >> 16) & 0xFF);
		buf[2] =(byte) ((c >>  8) & 0xFF);
		buf[3] =(byte) ((c >>  0) & 0xFF);
		for( int i= 0; i < 4; i++) buf[4] ^= buf[i];
		os.write(buf);
		os.flush();
		if( is.read() != STM32_ACK ) throw new RuntimeException(String.format("no ack on send address 0x%04x", c));
	}
	
	private int[] sectorSize = { 16384, 16384, 16384, 16384, 65536, 131072, 131072, 131072, 131072, 131072, 131072, 131072, };
	
	public int[] getPageList(int size) {
		List<Integer> res = new ArrayList<>(); 
		int i=0;
		while(size > 0 ) {
			res.add(i);
			size -= sectorSize[i];
			i++;
		}
		return res.stream().mapToInt(j->j).toArray();
	}
	
	public void erasePages(int[] pages) throws IOException {
		log.info("erase pages: {}", Arrays.toString(pages));
		sendCommand(cmdCode[6]);
		byte[] buf;
		if( useExtended() ) {
			int size = pages.length*2+3;
			buf = new byte[size];
			buf[0] = (byte) ((pages.length-1) >> 8);
			buf[size-1] ^= buf[0];
			buf[1] = (byte) ((pages.length-1) & 0xFF);
			buf[size-1] ^= buf[1];
			for(int i = 0; i < pages.length; i++) {
				buf[i*2+2] = (byte) (pages[i] >> 8);
				buf[size-1] ^= buf[i*2+2];
				buf[i*2+3] = (byte) (pages[i] & 0xFF);
				buf[size-1] ^= buf[i*2+3];
			}
		} else {
			buf = new byte[pages.length+2];
			buf[0] = (byte) (pages.length-1);
			buf[pages.length+1] ^= buf[0];
			for(int i = 0; i < pages.length; i++) {
				buf[i+1] = (byte) pages[i];
				buf[pages.length+1] ^= buf[i+1];
			}
		}
		os.write(buf);
		os.flush();
		if( is.read() != STM32_ACK ) throw new RuntimeException(String.format("no ack on send page list %s", Arrays.toString(pages)));
	}
	
	private boolean useExtended() {
		return cmdCode[6] == (byte)0x44;
	}

	public void writeMemory(int startAdress, byte[] buf) throws IOException {
		log.info("write memory starting at {}", Integer.toHexString(startAdress) );
		int remain = buf.length;
		int offset = 0;
		while( remain > 0 ) {
			sendCommand(0x31);
			sendAdress(startAdress+offset);
			log.info("written {}/{}",offset, buf.length);
			if( listener!= null ) listener.transferred(offset);
			if( cancelable.isCancelled() ) break;
			int toSend = Math.min(remain,256);
			sendMemoryInternal(buf, offset, toSend);
			remain -= toSend;
			offset += toSend;
		}
	}

	private void sendMemoryInternal(byte[] buf, int offset, int count) throws IOException {
		if( (count % 4) != 0 ) {
			count = ((count / 4)+1 ) * 4;
		}
		byte[] send = new byte[count+2];
		send[0] = (byte) (count - 1);
		send[count+1] ^= send[0];
		for( int i = 0; i < count; i++) {
			if( i+offset < buf.length ) {
				send[i+1] = buf[i+offset];
				send[count+1] ^= send[i+1];
			} else {
				send[i+1] = (byte) 0xFF;
				send[count+1] ^= send[i+1];
			}
		}
		os.write(send);
		if( is.read() != STM32_ACK ) throw new RuntimeException(String.format("no ack on send bytes"));
	}

	public void readMemory(int address, byte[] buf) throws IOException {
		log.info("read memory starting at {}", Integer.toHexString(address) );
		int remain = buf.length;
		int offset = 0;
		while( remain > 0 ) {
			if( listener!= null ) listener.transferred(offset);
			if( cancelable.isCancelled() ) break;
			sendCommand(0x11);
			sendAdress(address+offset);
			log.info("read memory {}/{}", offset, buf.length );
			int toRead = Math.min(remain, 256);
			sendCommand(toRead-1); // like command
			for( int i = 0; i < toRead; i++) {
				buf[i+offset] = (byte) is.read();
			}
			remain -= toRead;
			offset += toRead;
		}
		log.info("read memory done {}", buf.length );		
	}
}
