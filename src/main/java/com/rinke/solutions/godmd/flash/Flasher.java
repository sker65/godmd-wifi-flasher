package com.rinke.solutions.godmd.flash;

import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Flasher {

	private static final String HOSTNAME = "hostname";
	private static final String FIRMWARE = "firmware";
	private static final String GODMDFIRMWARE = "godmdfirmware";
	private static final int GODMD = 0;
	private static final int WIFI = 1;
	JFrame frame;
	private JTextField txtHostname;
	private JTextField txtWifiFirmwareArchive;
	private JButton btnFlashWifiFirmware;

	public Flasher() {
		frame = new JFrame("Flasher for goDMD");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 96, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 29, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		frame.getContentPane().setLayout(gridBagLayout);

		JLabel lblFirmware = new JLabel("Wifi Firmware:");
		GridBagConstraints gbc_lblFirmware = new GridBagConstraints();
		gbc_lblFirmware.insets = new Insets(0, 0, 5, 5);
		gbc_lblFirmware.anchor = GridBagConstraints.EAST;
		gbc_lblFirmware.gridx = 0;
		gbc_lblFirmware.gridy = 0;
		frame.getContentPane().add(lblFirmware, gbc_lblFirmware);

		txtWifiFirmwareArchive = new JTextField();
		String fw = ApplicationProperties.get(FIRMWARE);
		if (fw != null) {
			txtWifiFirmwareArchive.setText(fw);
			isValidFirmware(fw);
		}
		GridBagConstraints gbc_txtFirmwareArchive = new GridBagConstraints();
		gbc_txtFirmwareArchive.insets = new Insets(0, 0, 5, 5);
		gbc_txtFirmwareArchive.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtFirmwareArchive.gridx = 1;
		gbc_txtFirmwareArchive.gridy = 0;
		frame.getContentPane().add(txtWifiFirmwareArchive, gbc_txtFirmwareArchive);
		txtWifiFirmwareArchive.setColumns(10);
		
		btnBrowseWifiFirmware = new JButton("Browse");
		btnBrowseWifiFirmware.addActionListener(e -> chooseFirmware(WIFI));
		GridBagConstraints gbc_browseFirmware = new GridBagConstraints();
		gbc_browseFirmware.anchor = GridBagConstraints.EAST;
		gbc_browseFirmware.insets = new Insets(0, 0, 5, 5);
		gbc_browseFirmware.fill = GridBagConstraints.VERTICAL;
		gbc_browseFirmware.gridx = 2;
		gbc_browseFirmware.gridy = 0;
		frame.getContentPane().add(btnBrowseWifiFirmware, gbc_browseFirmware);
		
		btnFlashWifiFirmware = new JButton("Flash WiFi");
		btnFlashWifiFirmware.addActionListener(e -> flash());
		GridBagConstraints gbc_btnFlashFirmware = new GridBagConstraints();
		gbc_btnFlashFirmware.insets = new Insets(0, 0, 5, 0);
		gbc_btnFlashFirmware.anchor = GridBagConstraints.WEST;
		gbc_btnFlashFirmware.gridx = 3;
		gbc_btnFlashFirmware.gridy = 0;
		frame.getContentPane().add(btnFlashWifiFirmware, gbc_btnFlashFirmware);
		
		lblGodmdFirmeware = new JLabel("goDMD Firmware:");
		GridBagConstraints gbc_lblGodmdFirmeware = new GridBagConstraints();
		gbc_lblGodmdFirmeware.anchor = GridBagConstraints.EAST;
		gbc_lblGodmdFirmeware.insets = new Insets(0, 0, 5, 5);
		gbc_lblGodmdFirmeware.gridx = 0;
		gbc_lblGodmdFirmeware.gridy = 1;
		frame.getContentPane().add(lblGodmdFirmeware, gbc_lblGodmdFirmeware);
		
		String goDMDfw = ApplicationProperties.get(GODMDFIRMWARE);

		textgoDMDFirmware = new JTextField();
		textgoDMDFirmware.setText(goDMDfw);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 1;
		frame.getContentPane().add(textgoDMDFirmware, gbc_textField);
		textgoDMDFirmware.setColumns(10);
		
		btnBrowseGoDMDFirmware = new JButton("Browse");
		btnBrowseGoDMDFirmware.addActionListener(e -> chooseFirmware(GODMD));

		GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
		gbc_btnBrowse.insets = new Insets(0, 0, 5, 5);
		gbc_btnBrowse.gridx = 2;
		gbc_btnBrowse.gridy = 1;
		frame.getContentPane().add(btnBrowseGoDMDFirmware, gbc_btnBrowse);
		
		btnFlashGodmd = new JButton("Flash goDMD");
		btnFlashGodmd.addActionListener(e -> flashGoDmd());

		GridBagConstraints gbc_btnFlashGodmd = new GridBagConstraints();
		gbc_btnFlashGodmd.insets = new Insets(0, 0, 5, 0);
		gbc_btnFlashGodmd.gridx = 3;
		gbc_btnFlashGodmd.gridy = 1;
		frame.getContentPane().add(btnFlashGodmd, gbc_btnFlashGodmd);

		JLabel lblGodmdHostname = new JLabel("   goDMD Hostname:");
		GridBagConstraints gbc_lblGodmdHostname = new GridBagConstraints();
		gbc_lblGodmdHostname.insets = new Insets(0, 0, 5, 5);
		gbc_lblGodmdHostname.gridx = 0;
		gbc_lblGodmdHostname.gridy = 2;
		frame.getContentPane().add(lblGodmdHostname, gbc_lblGodmdHostname);

		txtHostname = new JTextField();
		String hostn = ApplicationProperties.get(HOSTNAME);
		if (hostn != null) {
			txtHostname.setText(hostn);
		}

		btnFlashWifiFirmware.setEnabled(!txtHostname.getText().isEmpty() && firmwareValid );
		btnFlashGodmd.setEnabled(!txtHostname.getText().isEmpty() );

		GridBagConstraints gbc_txtHostname = new GridBagConstraints();
		gbc_txtHostname.insets = new Insets(0, 0, 5, 5);
		gbc_txtHostname.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtHostname.gridx = 1;
		gbc_txtHostname.gridy = 2;
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
				btnFlashWifiFirmware.setEnabled(!txtHostname.getText().isEmpty() && firmwareValid );
				btnFlashGodmd.setEnabled(!txtHostname.getText().isEmpty());
			}

		});
						
		JButton btnAbout = new JButton("About");
		btnAbout.addActionListener(e -> new About(this.frame));
		GridBagConstraints gbc_btnAbout = new GridBagConstraints();
		gbc_btnAbout.anchor = GridBagConstraints.EAST;
		gbc_btnAbout.gridx = 3;
		gbc_btnAbout.gridy = 4;
		frame.getContentPane().add(btnAbout, gbc_btnAbout);
								
		frame.pack();
		frame.setVisible(true);
	}
	

	private void flashGoDmd() {
		ApplicationProperties.put(HOSTNAME, txtHostname.getText());
		ApplicationProperties.put(GODMDFIRMWARE,textgoDMDFirmware.getText());
		DfuFlasher worker = new DfuFlasher(txtHostname.getText(), textgoDMDFirmware.getText());
		progress = new Progress(frame, worker);
		worker.setProgress(progress);
		worker.execute();
		progress.setVisible(true);
		try {
			worker.get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("error while flashing", e);
		}
		log.info("finish flashing firmware");
	}
	
	Progress progress;

	private void flash() {
		Uploader worker = new Uploader(txtHostname.getText(), txtWifiFirmwareArchive.getText());
		ApplicationProperties.put(HOSTNAME, txtHostname.getText());
		progress = new Progress(frame, worker);
		worker.setProgress(progress);

		worker.execute();
		progress.setVisible(true);
		try {
			worker.get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("error while flashing", e);
		}
	}

	boolean firmwareValid = false;
	private JLabel lblGodmdFirmeware;
	private JTextField textgoDMDFirmware;
	private JButton btnBrowseGoDMDFirmware;
	private JButton btnFlashGodmd;
	private JButton btnBrowseWifiFirmware;

	private void chooseFirmware(int which) {
		FileDialog fc = new FileDialog(frame, "Choose a file", FileDialog.LOAD);
		fc.setVisible(true);
		String fn = fc.getDirectory() + fc.getFile();
		// check if this is a valid firmware
		if( which == WIFI ) {
			if (isValidFirmware(fn)) {
				txtWifiFirmwareArchive.setText(fn);
				if (!txtHostname.getText().isEmpty())
					btnFlashWifiFirmware.setEnabled(true);
				ApplicationProperties.put(FIRMWARE, fn);
			} else {
				JOptionPane.showMessageDialog(frame, "Looks like this archive does not contain a valid firmware.", "firmware archive error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
		if( which == GODMD) {
			textgoDMDFirmware.setText(fn);
			if (!txtHostname.getText().isEmpty())
				btnFlashGodmd.setEnabled(true);

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
