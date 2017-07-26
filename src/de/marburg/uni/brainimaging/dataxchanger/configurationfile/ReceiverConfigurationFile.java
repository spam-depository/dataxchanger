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

package de.marburg.uni.brainimaging.dataxchanger.configurationfile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.bouncycastle.util.encoders.Base64;

/**
 * This class provides a configuration file for one dataXchanger transfer. The
 * configuration file contains certain elements that can be written to or read
 * from disk.
 * 
 * @author Kornelius Podranski
 */
public class ReceiverConfigurationFile extends Properties {
	// UID for Serializable interface
	private static final long serialVersionUID = 2L;

	// comment that will be written to first line of the file
	protected static String COMMENT = "dataXchanger receiver configuration file";

	// keys that are supported for this property
	protected static final String KEY = "xskey";
	protected static final String DIGEST = "digest";
	protected static final String DATAFILENAME = "filename";
	protected static final String FTPFILENAME = "ftpfilename";
	protected static final String FTPSERVER = "ftpserver";
	protected static final String FTPPORT = "ftpport";
	protected static final String FTPUSER = "ftpuser";
	protected static final String FTPPASSWORD = "ftppwd";

	private File file;

	public ReceiverConfigurationFile(String filename) {
		this.file = new File(filename);
	}

	public String getFilename() {
		return file.getName();
	}

	public byte[] getEncryptedSecretKey() {
		byte[] base64 = toByteArray(getProperty(KEY));
		return Base64.decode(base64);
	}

	public void setEncryptedSecretKey(byte[] encryptedSecretKey) {
		String base64 = toString(Base64.encode(encryptedSecretKey));
		setProperty(KEY, base64);
	}

	public byte[] getDigest() {
		return Base64.decode(toByteArray(getProperty(DIGEST)));
	}

	public void setDigest(byte[] digest) {
		setProperty(DIGEST, toString(Base64.encode(digest)));
	}

	public String getDataFilename() {
		return getProperty(DATAFILENAME);
	}

	public void setDataFilename(String filename) {
		setProperty(DATAFILENAME, filename);
	}

	public String getFtpFilename() {
		return getProperty(FTPFILENAME);
	}

	public void setFtpFilename(String filename) {
		setProperty(FTPFILENAME, filename);
	}

	public String getFtpServer() {
		return getProperty(FTPSERVER);
	}

	public void setFtpServer(String serverAddress) {
		setProperty(FTPSERVER, serverAddress);
	}

	/**
	 * May throw a NumberFormatException if the value is not parseable as an
	 * integer value.
	 * 
	 * @return portnumber to connect FTP-server on
	 */
	public int getFtpPort() {
		return Integer.parseInt(getProperty(FTPPORT));
	}

	public void setFtpPort(int portnumber) {
		setProperty(FTPPORT, Integer.toString(portnumber));
	}

	public String getFtpUser() {
		return getProperty(FTPUSER);
	}

	public void setFtpUser(String username) {
		setProperty(FTPUSER, username);
	}

	public String getFtpPassword() {
		return getProperty(FTPPASSWORD);
	}

	public void setFtpPassword(String password) {
		setProperty(FTPPASSWORD, password);
	}

	/**
	 * loads the configuration file from disk. All variables of the instance
	 * will be overwritten.
	 * 
	 * @throws IOException
	 * @throws IllegalStateException
	 *             if the configuration file on disk does not contain all
	 *             necessary elements
	 */
	public void load() throws IOException {
		Reader in = new BufferedReader(new FileReader(file));
		load(in);
		in.close();
		// check if all necessary elements are there
		if (!allElementsSet())
			throw new IllegalStateException("not all necessary elements could "
					+ "be loaded");
		// check for correct format/value-range
		try {
			getFtpPort();
		} catch (NumberFormatException e) {
			throw new IllegalStateException(FTPPORT + " is not a valid integer");
		}
	}

	/**
	 * writes the configuration file to disk.
	 * 
	 * @throws IOException
	 */
	public void store() throws IOException {
		// check if all necessary elements are set before we (over)write the
		// file
		if (!allElementsSet())
			throw new IllegalStateException();
		Writer out = new BufferedWriter(new FileWriter(file));
		store(out, COMMENT);
		out.close();
	}

	/**
	 * checks if all necessary variables have been set.
	 * 
	 * @return true if all variables contain values, false otherwise
	 */
	private boolean allElementsSet() {
		Set<String> all = new HashSet<String>();
		all.add(KEY);
		all.add(DIGEST);
		all.add(DATAFILENAME);
		all.add(FTPFILENAME);
		all.add(FTPSERVER);
		all.add(FTPPORT);
		all.add(FTPUSER);
		all.add(FTPPASSWORD);

		if (stringPropertyNames().containsAll(all))
			return true;
		return false;
	}

	/**
	 * converts an array of bytes into a string, each byte as one character.
	 * 
	 * @param bytes
	 *            the byte array to convert
	 * @return the resulting string
	 */
	private static String toString(byte[] bytes) {
		char[] chars = new char[bytes.length];

		for (int i = 0; i < chars.length; i++)
			chars[i] = (char) (bytes[i] & 0xff);

		return new String(chars);
	}

	/**
	 * converts a string into an array of bytes. only the first byte of each
	 * char will be converted, thus only ASCII character will be represented
	 * correctly.
	 * 
	 * @param string the string to convert
	 * @return the resulting array of bytes
	 */
	private static byte[] toByteArray(String string) {
		byte[] bytes = new byte[string.length()];
		char[] chars = string.toCharArray();

		for (int i = 0; i < chars.length; i++)
			bytes[i] = (byte) chars[i];

		return bytes;
	}
}
