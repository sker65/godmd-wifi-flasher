package com.rinke.solutions.godmd.flash;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.JProgressBar;

public class Progress extends JDialog {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	public JProgressBar progressBar;
	private JLabel lblStatusText;
	private boolean abort;
	private SwingWorker worker;
	private JButton btnAbort;

	
	public void setValue(int n) {
		progressBar.setValue(n);
	}
	
	public void setStatus(String status) {
		lblStatusText.setText(status);
	}
	
	public void setButtonText(String text) {
		btnAbort.setText(text);
	}
	
	/**
	 * Create the frame.
	 * @param worker 
	 */
	public Progress(JFrame parent, SwingWorker worker) {
		super(parent, "Flashing ...", true);
		this.worker = worker;
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 475, 142);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setValue(25);
		progressBar.setBounds(6, 32, 462, 37);
		Border border = BorderFactory.createTitledBorder("Flashing ...");
	    progressBar.setBorder(border);
	    //content.add(progressBar, BorderLayout.NORTH);
		contentPane.add(progressBar);
		
		btnAbort = new JButton("Abort");
		btnAbort.setBounds(178, 81, 117, 29);
		btnAbort.addActionListener(e-> {
			worker.cancel(true);
			this.setAbort( true );
			this.setVisible(false);
		});
		contentPane.add(btnAbort);
		
		lblStatusText = new JLabel("Status text");
		lblStatusText.setBounds(6, 6, 462, 16);
		contentPane.add(lblStatusText);

	}

	public boolean isAbort() {
		return abort;
	}

	public void setAbort(boolean abort) {
		this.abort = abort;
	}
}
