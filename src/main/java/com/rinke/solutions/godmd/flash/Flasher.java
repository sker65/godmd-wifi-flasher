package com.rinke.solutions.godmd.flash;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.rinke.solutions.godmd.flash.CountingHttpEntity.ProgressListener;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Flasher {

	private static final String HOSTNAME = "hostname";
	private static final String FIRMWARE = "firmware";
	JFrame frame;
	private JTextField txtHostname;
	private JTextField txtFirmwareArchive;
	private JButton btnFlashFirmware;

	public Flasher() {
		frame = new JFrame("Wifi Flasher for goDMD");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 96, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 29, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		frame.getContentPane().setLayout(gridBagLayout);

		JLabel lblFirmware = new JLabel("Firmware:");
		GridBagConstraints gbc_lblFirmware = new GridBagConstraints();
		gbc_lblFirmware.insets = new Insets(0, 0, 5, 5);
		gbc_lblFirmware.anchor = GridBagConstraints.EAST;
		gbc_lblFirmware.gridx = 0;
		gbc_lblFirmware.gridy = 0;
		frame.getContentPane().add(lblFirmware, gbc_lblFirmware);

		txtFirmwareArchive = new JTextField();
		String fw = ApplicationProperties.get(FIRMWARE);
		if (fw != null) {
			txtFirmwareArchive.setText(fw);
			isValidFirmware(fw);
		}
		GridBagConstraints gbc_txtFirmwareArchive = new GridBagConstraints();
		gbc_txtFirmwareArchive.insets = new Insets(0, 0, 5, 5);
		gbc_txtFirmwareArchive.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtFirmwareArchive.gridx = 1;
		gbc_txtFirmwareArchive.gridy = 0;
		frame.getContentPane().add(txtFirmwareArchive, gbc_txtFirmwareArchive);
		txtFirmwareArchive.setColumns(10);
		JButton browseFirmware = new JButton("Browse");
		browseFirmware.addActionListener(e -> chooseFirmware());
		GridBagConstraints gbc_browseFirmware = new GridBagConstraints();
		gbc_browseFirmware.anchor = GridBagConstraints.EAST;
		gbc_browseFirmware.insets = new Insets(0, 0, 5, 0);
		gbc_browseFirmware.fill = GridBagConstraints.VERTICAL;
		gbc_browseFirmware.gridx = 2;
		gbc_browseFirmware.gridy = 0;
		frame.getContentPane().add(browseFirmware, gbc_browseFirmware);

		JLabel lblGodmdHostname = new JLabel("   goDMD Hostname:");
		GridBagConstraints gbc_lblGodmdHostname = new GridBagConstraints();
		gbc_lblGodmdHostname.insets = new Insets(0, 0, 5, 5);
		gbc_lblGodmdHostname.gridx = 0;
		gbc_lblGodmdHostname.gridy = 1;
		frame.getContentPane().add(lblGodmdHostname, gbc_lblGodmdHostname);

		txtHostname = new JTextField();
		String hostn = ApplicationProperties.get(HOSTNAME);
		if (hostn != null) {
			txtHostname.setText(hostn);
		}
		GridBagConstraints gbc_txtHostname = new GridBagConstraints();
		gbc_txtHostname.insets = new Insets(0, 0, 5, 5);
		gbc_txtHostname.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtHostname.gridx = 1;
		gbc_txtHostname.gridy = 1;
		frame.getContentPane().add(txtHostname, gbc_txtHostname);
		txtHostname.setColumns(10);
		txtHostname.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				check();
			}

			public void removeUpdate(DocumentEvent e) {
				check();
			}

			public void insertUpdate(DocumentEvent e) {
				check();
			}

			private void check() {
				btnFlashFirmware.setEnabled(!txtHostname.getText().isEmpty() && firmwareValid );
			}

		});

		btnFlashFirmware = new JButton("Flash Firmware");
		btnFlashFirmware.addActionListener(e -> flash());
		btnFlashFirmware.setEnabled(!txtHostname.getText().isEmpty() && firmwareValid );
		GridBagConstraints gbc_btnFlashFirmware = new GridBagConstraints();
		gbc_btnFlashFirmware.insets = new Insets(0, 0, 0, 5);
		gbc_btnFlashFirmware.anchor = GridBagConstraints.WEST;
		gbc_btnFlashFirmware.gridx = 1;
		gbc_btnFlashFirmware.gridy = 2;
		frame.getContentPane().add(btnFlashFirmware, gbc_btnFlashFirmware);

		JButton btnAbout = new JButton("About");
		btnAbout.addActionListener(e -> new About(this.frame));
		GridBagConstraints gbc_btnAbout = new GridBagConstraints();
		gbc_btnAbout.anchor = GridBagConstraints.EAST;
		gbc_btnAbout.gridx = 2;
		gbc_btnAbout.gridy = 2;
		frame.getContentPane().add(btnAbout, gbc_btnAbout);
		frame.pack();
		frame.setVisible(true);
	}

	Progress progress;

	private class Uploader extends SwingWorker<Pair<Integer, String>, Pair<Integer, String>> implements ProgressListener {

		@Override
		public void transferred(long transferedBytes) {
			int val = (int) (((float) transferedBytes / (float) size) * 80.0) + 15;
			publish(new Pair<Integer, String>(val, "uploading " + line + " " + transferedBytes + "..."));
		}

		long size = 1;
		String line;
		String hostname;
		HttpClient client = new DefaultHttpClient();
		private boolean success;

		public Uploader(String hostname) {
			super();
			this.hostname = hostname;
			client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_0);
		}

		@Override
		protected Pair<Integer, String> doInBackground() throws Exception {
			HttpGet request = new HttpGet("http://" + hostname + "/flash/next");
			try {
				publish(new Pair<Integer, String>(1, "connecting " + hostname + " ..."));
				HttpResponse response = client.execute(request);
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				line = rd.readLine();
				publish(new Pair<Integer, String>(10, "about to flash " + line + " ..."));
				// System.out.println(line);
				Thread.sleep(500);
				rd.close();

				HttpPost post = new HttpPost("http://" + hostname + "/flash/upload");
				post.setProtocolVersion(HttpVersion.HTTP_1_0); // suppress
																// chunked
				RequestConfig config = RequestConfig.custom().setContentCompressionEnabled(false).build();
				post.setConfig(config);
				ZipFile zf = new ZipFile(txtFirmwareArchive.getText());
				ZipEntry entry = zf.getEntry(line);
				size = entry.getSize();
				InputStream instream = zf.getInputStream(zf.getEntry(line));
				InputStreamEntity isEntity = new InputStreamEntity(instream, size);
				isEntity.setChunked(false);
				CountingHttpEntity countingHttpEntity = new CountingHttpEntity(isEntity, this);
				post.setEntity(countingHttpEntity);
				publish(new Pair<Integer, String>(15, "uploading " + line + " ..."));
				response = client.execute(post);
				instream.close();
				EntityUtils.consumeQuietly(response.getEntity());

				if (response.getStatusLine().getStatusCode() == 200) {
					// System.out.println("all good");
					// trigger reboot
					publish(new Pair<Integer, String>(86, "upload complete waiting for reboot ..."));
					Thread.sleep(500);
					HttpGet reboot = new HttpGet("http://" + hostname + "/flash/reboot");
					response = client.execute(reboot);
					EntityUtils.consumeQuietly(response.getEntity());
					Thread.sleep(2500);
					if (response.getStatusLine().getStatusCode() == 200) {
						String newFirmware = waitForReboot();
						if (line.equals("user1.bin") && "user2.bin".equals(newFirmware)) {
							success();
						}
						if (line.equals("user2.bin") && "user1.bin".equals(newFirmware)) {
							success();
						}
					}
				}
				zf.close();

			} catch (IOException e) {
				publish(new Pair<Integer, String>(100, e.getMessage()));
				return new Pair<Integer, String>(99, e.getMessage());
			}
			publish(success ? new Pair<Integer, String>(100, "Success") : new Pair<Integer, String>(100, "Error on Reboot"));
			return null;
		}

		private void success() {
			success = true;
			publish(new Pair<Integer, String>(100, "Reboot successful"));
		}

		private String waitForReboot() {
			int count = 0;
			long start = System.currentTimeMillis();
			while (count < 80 && System.currentTimeMillis() - start < 60000) {
				HttpGet request = new HttpGet("http://" + hostname + "/flash/next");
				RequestConfig config = RequestConfig.custom().setContentCompressionEnabled(false).setConnectionRequestTimeout(400).setConnectTimeout(200)
						.build();
				request.setConfig(config);
				try {
					HttpResponse response = client.execute(request);
					BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

					String res = rd.readLine();
					rd.close();
					return res;
				} catch (IOException e) {
				}
				count++;
				publish(new Pair<Integer, String>((int) (85 + (count / 80.0) * 15), "upload complete waiting for reboot ..."));
			}
			return null;
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

	}

	private void flash() {
		Uploader worker = new Uploader(txtHostname.getText());
		ApplicationProperties.put(HOSTNAME, txtHostname.getText());
		progress = new Progress(frame, worker);
		worker.execute();
		progress.setVisible(true);
		try {
			worker.get();
		} catch (InterruptedException | ExecutionException e) {
		}
	}

	boolean firmwareValid = false;

	private void chooseFirmware() {
		FileDialog fc = new FileDialog(frame, "Choose a file", FileDialog.LOAD);
		fc.setVisible(true);
		String fn = fc.getDirectory() + fc.getFile();
		// check if this is a valid firmware
		if (isValidFirmware(fn)) {
			txtFirmwareArchive.setText(fn);
			if (!txtHostname.getText().isEmpty())
				btnFlashFirmware.setEnabled(true);
			ApplicationProperties.put(FIRMWARE, fn);
		} else {
			JOptionPane.showMessageDialog(frame, "Looks like this archive does not contain a valid firmware.", "firmware archive error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private boolean isValidFirmware(String fn) {
		try (ZipFile zf = new ZipFile(new File(fn))) {
			if (zf.getEntry("user1.bin") != null && zf.getEntry("user2.bin") != null) {
				firmwareValid = true;
				return true;
			}
		} catch (IOException e) {
			firmwareValid = false;
			return false;
		}
		firmwareValid = false;
		return false;
	}

	public static void main(String[] a) {
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
		//System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");

		Flasher flasher = new Flasher();
		flasher.frame.setVisible(true);
	}
}
