package com.rinke.solutions.godmd.flash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;

@Slf4j
class DfuFlasher extends SwingWorker<Pair<Integer, String>, Pair<Integer, String>> implements ProgressListener, Cancelable {

	private long totalBytes;
	private long lastTransfer;
	private String hostname;
	private String firmware;
	private Progress progress;

	public DfuFlasher(String hostname, String firmware) {
		this.hostname = hostname;
		this.firmware = firmware;
	}

	@Override
	public void transferred(long transferedBytes) {
		long tr = transferedBytes;
		if( transferedBytes < lastTransfer ) {
			pass++;
		}
		tr += pass * totalBytes/2;
		lastTransfer = transferedBytes;
		String msg = tr < totalBytes/2 ? "flashing" : "verifing";
		int val = (int) ((double) tr / (double)totalBytes * 100.0);
		publish(pair(val,msg));
	}
	
	int pass = 0;

	@Override
	protected Pair<Integer, String> doInBackground() throws Exception {
		Socket s;
		try {
			publish(pair(1,"connecting to "+hostname));
			s = new Socket(hostname,2323);
			s.setSoTimeout(5000);
			s.setTcpNoDelay(true);
			InputStream is = s.getInputStream();
			OutputStream os = s.getOutputStream();
			byte[] esc = "?\r\n".getBytes();		// esc sequence to switch esp-link in transparent mode
			os.write(esc);
			os.flush();
			File file = new File(firmware);
			int size = (int) file.length();
			totalBytes = size*2;
			byte[] buf = IOUtils.readFully(new FileInputStream(file), size);
			if( isCancelled() ) return null;

			DfuController controller = new DfuController(os, is, this, this);
			controller.initDevice();
			publish(pair(2,"getting device info"));
			controller.getInfo();
			int[] pageList = controller.getPageList(size);
			publish(pair(2,"erase flash pages"));
			controller.erasePages(pageList);
			if( isCancelled() ) return null;
			controller.writeMemory(0x08000000, buf);
			if( isCancelled() ) return null;
			byte[] compare = new byte[buf.length];
			controller.readMemory(0x08000000, compare);
			if( !Arrays.equals(buf, compare) ) {
				log.error("verify failed");
				publish(pair(100,"verify failed"));
				JOptionPane.showMessageDialog(null, "verification failed", "Verify failed", JOptionPane.INFORMATION_MESSAGE);
			} else {
				publish(pair(100,"success"));
			}
			s.close();
		} catch( RuntimeException e ) {
			log.error("problem while flashing",e);
			publish(pair(100,"error + "+e.getMessage()));
		} catch (IOException e) {
			log.error("problem while flashing",e);
			publish(pair(100,"error + "+e.getMessage()));
		}
		
		return null;
	}
	
	private Pair<Integer,String> pair(int i, String msg) {
		return new Pair<Integer, String>(i, msg);
	}
	
	@Override
	protected void process(List<Pair<Integer, String>> chunks) {
		for (Pair<Integer, String> pair : chunks) {
			progress.setValue(pair.left);
			progress.setStatus(pair.right);
			if (pair.left == 100) {
				progress.setButtonText("Close");
			}
		}
	}

	public void setProgress(Progress progress) {
		this.progress = progress;
	}
	
}