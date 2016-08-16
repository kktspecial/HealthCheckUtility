package com.jamfsoftware.jss.healthcheck;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;
import com.jamfsoftware.jss.healthcheck.ui.UserPrompt;
import com.jamfsoftware.jss.healthcheck.ui.UserPromptHeadless;
import com.jamfsoftware.jss.healthcheck.ui.component.MonitorGraph;
import com.jamfsoftware.jss.healthcheck.util.EnvironmentUtil;

/**
 * Main.java - Written by Jacob Schultz 12/2015
 * This class handles the initial load of the program.
 * It detects the OS, and then opens the text interface for linux and a GUI for Windows and Mac.
 * Can be run with a -h flag to open the text interface on Mac and Windows
 * [STUB] If ran with the -m flag it will start a POC for the health monitor.
 */
public class Main {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-mm-dd HH:mm");
	private static final String LOG_FORMAT = "%s %d %d %d %d\n";
	
	/*
	DATE_FORMAT.format(date) + " " + mobileDeviceLength + " " + computerLength + " " + Runtime.getRuntime().freeMemory() + " " + Runtime.getRuntime().totalMemory() + "\n"
	 */
	
	//This preferences bundle stores the location of the configuration XML
	private Preferences prefs = Preferences.userNodeForPackage(UserPrompt.class);
	private final String[] args;
	
	public Main(String... args) {
		this.args = args;
	}
	
	private void start() {
		if (isFlag("-m")) { // Monitor
			startMonitor();
		}
		
		if (isFlag("-g")) { // Graph
			new MonitorGraph("");
		}
		
		startUI(isFlag("-h")
				|| GraphicsEnvironment.isHeadless()
				|| EnvironmentUtil.isLinux());
	}
	
	private void startMonitor() {
		Scanner scanner = new Scanner(System.in);
		ConfigurationController con = new ConfigurationController();
		
		while (!con.canGetFile()) {
			if (!(con.attemptAutoDiscover())) {
				System.out.println("Path to Config.xml not found. Please type the full path below or type 'exit' to close the program. ");
				String path = scanner.next();
				if (path.equals("exit")) {
					System.exit(0);
				} else {
					prefs.put("config_xml_path", path);
				}
			}
		}
		
		String xmlPath = prefs.get("config_xml_path", "Path to file '/Users/user/desktop/config.xml'");
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(xmlPath);
		try {
			Document document = builder.build(xmlFile);
			Element root = document.getRootElement();
			HealthCheck hc = new HealthCheck();
			int mobile_device_length = hc.checkAPILength(root.getChild("jss_url").getValue(), root.getChild("jss_username").getValue(), root.getChild("jss_password").getValue(), "mobiledevicecommands");
			int computer_length = hc.checkAPILength(root.getChild("jss_url").getValue(), root.getChild("jss_username").getValue(), root.getChild("jss_password").getValue(), "computercommands");
			writeLogEntry(mobile_device_length, computer_length);
		} catch (Exception e) {
			LOGGER.error("Config XML file damaged. Unable to run monitor.", e);
		}
	}
	
	private void startUI(boolean headless) {
		if (headless) {
			new UserPromptHeadless();
		} else {
			ConfigurationController con = new ConfigurationController();
			while (!con.canGetFile() && !con.attemptAutoDiscover()) {
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Select Configuration XML File");
				
				FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Files", "xml");
				chooser.setFileFilter(filter);
				
				int returnVal = chooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					prefs.put("config_xml_path", chooser.getSelectedFile().getAbsolutePath());
				} else {
					System.exit(0);
				}
			}
			
			try {
				new UserPrompt();
			} catch (Exception e) {
				LOGGER.error("A fatal error has occurred", e);
				JOptionPane.showMessageDialog(new JFrame(), "A fatal error has occurred.\n" + e, "Fatal Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void writeLogEntry(int mobileDeviceLength, int computerLength) {
		try {
			FileWriter writer = new FileWriter("", true);
			writer.write(String.format(LOG_FORMAT,
					DATE_FORMAT.format(new Date()),
					mobileDeviceLength,
					computerLength,
					Runtime.getRuntime().freeMemory(),
					Runtime.getRuntime().totalMemory()));
			writer.close();
		} catch (IOException e) {
			LOGGER.error("Error writing to log", e);
		}
	}
	
	private boolean isFlag(String arg) {
		return Stream.of(args).anyMatch(arg::equals);
	}
	
	public static void main(String[] args) throws Exception {
		new Main(args).start();
	}
	
}
