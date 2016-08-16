package com.jamfsoftware.jss.healthcheck.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DetectVM.java Created by Jacob Schultz on 9.2.16.
 * This class checks the operating system version, then
 * runs a system command to check with relative certainty
 * if the host is a VM.
 */
public class DetectVM {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DetectVM.class);
	
	private final boolean isVM;
	
	/**
	 * Constructor that detects the OS Type,
	 * then attempts to determine if it is a VM.
	 */
	public DetectVM() {
		if (EnvironmentUtil.isLinux()) {
			this.isVM = getVMStatusLinux();
		} else if (EnvironmentUtil.isWindows()) {
			this.isVM = getVMStatusWindows();
		} else if (EnvironmentUtil.isMac()) {
			this.isVM = getVMStatusOSX();
		} else {
			LOGGER.warn("Unable to detect OS type.");
			this.isVM = false;
		}
	}
	
	public DetectVM(String rootPassword) {
		if (EnvironmentUtil.isLinux()) {
			this.isVM = getVMStatusLinux(rootPassword);
		} else if (EnvironmentUtil.isWindows()) {
			this.isVM = getVMStatusWindows();
		} else if (EnvironmentUtil.isMac()) {
			this.isVM = getVMStatusOSX();
		} else {
			LOGGER.warn("Unable to detect OS type.");
			this.isVM = false;
		}
	}
	
	/**
	 * Method that returns private VM boolean.
	 *
	 * @return boolean of if it is a VM or Not
	 */
	public boolean getIsVM() {
		return this.isVM;
	}
	
	private static boolean getVMStatusLinux() {
		String[] command = { "/bin/sh", "-c", "ls -l /dev/disk/by-id/" };
		String value = executeCommand(command);
		
		return value.contains("QEMU")
				|| value.contains("VMware")
				|| value.contains("VirtualBox")
				|| value.contains("KVM")
				|| value.contains("Bochs")
				|| value.contains("Parallels");
	}
	
	private static boolean getVMStatusLinux(String rootPassword) {
		String[] command = { "echo " + rootPassword + " | sudo -S dmidecode -s system-product-name" };
		String value = executeCommand(command);
		if (value.contains("VMware Virtual Platform")
				|| value.contains("VirtualBox")
				|| value.contains("KVM")
				|| value.contains("Bochs")
				|| value.contains("Parallels")) {
			return true;
		}
		
		String[] command2 = { "echo " + rootPassword + " | sudo -S dmidecode egrep -i 'manufacturer|product'" };
		value = executeCommand(command2);
		
		return value.contains("Microsoft Corporation")
				&& value.contains("Virtual Machine");
	}
	
	private static boolean getVMStatusWindows() {
		String[] command = { "SYSTEMINFO" };
		String value = executeCommand(command);
		
		return value.contains("VMWare")
				|| value.contains("VirtualBox")
				|| value.contains("KVM")
				|| value.contains("Bochs")
				|| value.contains("Parallels");
	}
	
	private static boolean getVMStatusOSX() {
		String[] command = { "/bin/sh", "-c", "ioreg -l | grep -e Manufacturer -e 'Vendor Name'" };
		String value = executeCommand(command);
		
		return value.contains("VirtualBox")
				|| value.contains("VMware")
				|| value.contains("Oracle")
				|| value.contains("Bochs")
				|| value.contains("Parallels");
	}
	
	/**
	 * This method executes a command on the host system.
	 *
	 * @return Command output
	 */
	private static String executeCommand(String[] command) {
		String s;
		String output = "";
		try {
			Process p = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = stdInput.readLine()) != null) {
				output += s;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}
}

