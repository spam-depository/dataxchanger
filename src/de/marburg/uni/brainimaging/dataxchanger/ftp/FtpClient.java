/**
 * Copyright 2013 Kornelius Podranski
 *
 * This file is part of dataXchanger.
 *
 *  dataXchanger is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  dataXchanger is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with dataXchanger.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.marburg.uni.brainimaging.dataxchanger.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * This class provides a simple FTP client, which uploads files to or download
 * files from an FTP-Server.
 * 
 * @author Kornelius Podranski
 */
public class FtpClient {

	FTPClient ftp;
	String address;
	int port;
	String username;
	String password;
	boolean active = false;

	/**
	 * 
	 * @param address
	 *            the doamin name or ip-address of the ftp-server
	 * @param port
	 *            the port number of the ftp-server
	 * @param username
	 *            login for the ftp-server
	 * @param password
	 *            password for the given username on the ftp-server
	 * @param active
	 *            use active or passive ftp
	 */
	public FtpClient(String address, int port, String username,
			String password, boolean active) {
		this(address, port, username, password);
		this.active = active;
	}

	/**
	 * Initialized this instance using passive mode.
	 * 
	 * @param address
	 *            the doamin name or ip-address of the ftp-server
	 * @param port
	 *            the port number of the ftp-server
	 * @param username
	 *            login for the ftp-server
	 * @param password
	 *            password for the given username on the ftp-server
	 */
	public FtpClient(String address, int port, String username, String password) {
		this.address = address;
		this.port = port;
		this.username = username;
		this.password = password;
		ftp = new FTPClient();
		// ftp.addProtocolCommandListener(new PrintCommandListener(
		// new PrintWriter(System.out), true)); // debugging
	}

	/**
	 * Try to open a connection to the FTP-Server and authenticate.
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		try {
			int reply;

			// connect
			if (port > 0) {
				ftp.connect(address, port);
			} else {
				ftp.connect(address);
			}

			// check reply code to verify success
			reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				throw new IOException("FTP server refused connection");
			}

			// login
			if (!ftp.login(username, password)) {
				ftp.logout();
				throw new IOException("FTP server did not accept credentials");
			}
		} catch (IOException e) {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException f) {
					// do nothing
				}
			}
			throw new IOException("Could not connect to server");
		}
	}

	/**
	 * Close the connection to the FTP-Server.
	 * 
	 * @throws FTPConnectionClosedException
	 *             if connection has already been closed
	 * @throws IOException
	 *             if communication on control channel does not work as expected
	 */
	public void disconnect() throws FTPConnectionClosedException, IOException {
		if (ftp.isConnected()) {
			if (!FTPReply.isPositiveCompletion(ftp.noop())) // check that
															// control
															// connection is
															// working OK
				throw new IOException("control channel is broken");
			if (!ftp.logout())
				throw new IOException("could not logout");
		}
	}

	/**
	 * Uploads in to the FTP-server's root directory. The filename will be the
	 * next available number.
	 * 
	 * @param in
	 *            data to be uploaded
	 * @return the path of the file on the FTP-Server
	 * @throws FTPConnectionClosedException
	 *             if connection has already been closed
	 * @throws IOException
	 */
	public String put(InputStream in) throws FTPConnectionClosedException,
			IOException {
		String filename = nextFileNumber();
		if (filename == null) {
			throw new IOException("no free filenames available");
		}
		put(in, filename);
		return filename;
	}

	/**
	 * 
	 * @param in
	 *            data to be uploaded
	 * @param path
	 *            fileme to create fr data on the FTP-server.
	 * @throws FTPConnectionClosedException
	 *             if connection has already been closed
	 * @throws IOException
	 */
	public void put(InputStream in, String path)
			throws FTPConnectionClosedException, IOException {
		if (ftp.isConnected()) {
			ftp.noop(); // check that control connection is working OK
			check(ftp);
			ftp.setFileType(FTP.BINARY_FILE_TYPE); // set binary transfer mode
			check(ftp);
			if (active) {
				ftp.enterLocalActiveMode(); // set active mode
				check(ftp);
			} else {
				ftp.enterLocalPassiveMode(); // set passive mode
				check(ftp);
				ftp.setUseEPSVwithIPv4(true); // set passive mode
				check(ftp);
			}
			ftp.storeFile(path, in); // upload file
			check(ftp);
		} else {
			throw new IOException("not connected");
		}
	}

	/**
	 * Downloads a file from the FTP-Server and wites it to out.
	 * 
	 * @param path
	 *            the path (on the FTP-server) of the file to download
	 * @param out
	 *            stream downloaded data will be written to
	 * @throws FTPConnectionClosedException
	 *             if connection has already been closed
	 * @throws IOException
	 */
	public void get(String path, OutputStream out)
			throws FTPConnectionClosedException, IOException {
		if (ftp.isConnected()) {
			ftp.noop(); // check that control connection is working OK
			check(ftp);
			ftp.setFileType(FTP.BINARY_FILE_TYPE); // set binary transfer mode
			check(ftp);
			if (active) {
				ftp.enterLocalActiveMode(); // set active mode
				check(ftp);
			} else {
				ftp.enterLocalPassiveMode(); // set passive mode
				check(ftp);
				ftp.setUseEPSVwithIPv4(true); // set passive mode
				check(ftp);
			}
			ftp.retrieveFile(path, out); // download file
			check(ftp);
		} else {
			throw new IOException("not connected");
		}
	}

	/**
	 * this method tries to find filenames in the form of a positive (long)
	 * number in the current directory of the ftp-server and tries to determine
	 * the next free "number". this number might be greater or less than the
	 * existing filenames, but alway in the range 0 to Long.MAX_VALUE. <br>
	 * If no free Number is found null will be returned. <br>
	 * The FTPClient must be connected before calling this method!
	 * 
	 * @return the next free filename
	 * @throws IOException
	 *             see FTPClient.listFiles() or if not connected
	 */
	// caching of the numbers would be nice but is not applicable if the ftp
	// server can be used concurrent
	private String nextFileNumber() throws IOException {
		long min = Long.MAX_VALUE;
		long max = -1L;
		if (ftp.isConnected()) {
			if (active) {
				ftp.enterLocalActiveMode(); // set active mode
				check(ftp);
			} else {
				ftp.enterLocalPassiveMode(); // set passive mode
				check(ftp);
				ftp.setUseEPSVwithIPv4(true); // set passive mode
				check(ftp);
			}
			for (FTPFile f : ftp.listFiles(null)) {
				try {
					long i = Long.parseLong(f.getName());
					max = i > max ? i : max;
					min = i < min && i >= 0 ? i : min;
				} catch (NumberFormatException e) {
					// do nothing
				}
			}
		} else {
			throw new IOException("not connected");
		}
		// first fill small numbers
		if (min < Long.MAX_VALUE && min > 0)
			return Long.toString(min - 1);
		// then fill big numbers
		if (max >= 0 && max < Long.MAX_VALUE)
			return Long.toString(max + 1);
		// maybe there is no number filename yet, then start with 0
		if (max == -1 && min == Long.MAX_VALUE)
			return "0";
		// if we reach here all is filled up or something else went wrong
		return null;
	}

	/**
	 * Checks if last command was successfully executed by FTP-server.
	 * 
	 * @param ftp the ftp-client to check
	 * @throws IOException if command was not successfully executed
	 */
	private static void check(FTPClient ftp) throws IOException {
		if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
			String reply = ftp.getReplyString();
			ftp.disconnect();
			throw new IOException(reply);
		}
	}
}
