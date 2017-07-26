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

import java.io.InputStream;
import java.io.OutputStream;

import de.marburg.uni.brainimaging.dataxchanger.StreamProcessor;

/**
 * This class implements a FTP-client for uploading files as a stream line
 * processor
 * 
 * @author Kornelius Podranski
 */
public class FtpDownloader extends FtpClient implements StreamProcessor<Void> {

	// stores exception on error during process
	private Exception exception = null;
	// the path of the file to retrieve
	private String path;

	public FtpDownloader(String address, int port, String username,
			String password, boolean active, String path) {
		super(address, port, username, password, active);
		this.path = path;
	}

	/**
	 * Downloads the file defined on initialisation from a FTP-server and
	 * streams it to out. <br>
	 * InputStream is ignored and may be null. <br>
	 * {@inheritDoc}
	 */
	public boolean process(InputStream in, OutputStream out) {
		try {
			connect();
			get(path, out);
			disconnect();
		} catch (Exception e) {
			exception = e;
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public Void getResult() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Exception getException() {
		return exception;
	}
}
