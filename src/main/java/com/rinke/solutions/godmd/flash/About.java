package com.rinke.solutions.godmd.flash;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class About extends JDialog {

	private JPanel contentPane;

	/**
	 * Create the frame.
	 */
	public About(JFrame parent) {
		super(parent, "About goDMD Flasher", true);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 451, 273);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setBounds(5, 5, 189, 105);
		lblNewLabel.setIcon(new ImageIcon(About.class.getResource("/logo.png")));
		contentPane.add(lblNewLabel);

		JTextPane txtpnFlashUtilityFor = new JTextPane();
		txtpnFlashUtilityFor.setBounds(198, 6, 246, 104);
		txtpnFlashUtilityFor.setBackground(UIManager.getColor("Button.background"));
		txtpnFlashUtilityFor.setEditable(false);
		txtpnFlashUtilityFor.setText("Flash Utility for flashing goDMD Wifi firmware over the air.\n");
		contentPane.add(txtpnFlashUtilityFor);

		JButton btnOk = new JButton("Ok");
		btnOk.setBounds(189, 216, 75, 29);
		contentPane.add(btnOk);
		btnOk.addActionListener(e -> {
			this.setVisible(false);
			this.dispose();	
		});

		LinkLabel lblcreditsToEsplink = new LinkLabel(
				"<html>Credits to esp-link (<a href='https://github.com/jeelabs/esp-link'>https://github.com/jeelabs/esp-link</a>).<br/>\n\ngoDMD - <a href='http://go-dmd.de/'>http://go-dmd.de/</a><br/>\n<br/>\nSource code: <a href='https://github.com/sker65/godmd-wifi-flasher/'>https://github.com/sker65/godmd-wifi-flasher/</a><br/>  \n<br/>\n(c) 2016 by Stefan Rinke</html>");
		lblcreditsToEsplink.setBounds(15, 109, 414, 105);
		contentPane.add(lblcreditsToEsplink);
		this.setVisible(true);
	}
}
