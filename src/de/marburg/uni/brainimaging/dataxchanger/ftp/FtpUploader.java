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
public class FtpUploader extends FtpClient implements StreamProcessor<String> {

	// stores exception on error during process
	private Exception exception = null;
	// stores result of put()
	private String result = null;

	public FtpUploader(String address, int port, String username,
			String password, boolean active) {
		super(address, port, username, password, active);
	}

	/**
	 * Uploads a stream to an FTP-server. after upload the file-path on the
	 * server can be retrieved via getResult(). <br>
	 * OutputStream is ignored and may be null. <br>
	 * {@inheritDoc}
	 */
	public boolean process(InputStream in, OutputStream out) {
		try {
			connect();
			result = put(in);
			disconnect();
		} catch (Exception e) {
			exception = e;
			return false;
		}
		return true;
	}

	/**
	 * returns the path of the created file on the server. <br>
	 * {@inheritDoc}
	 */
	public String getResult() {
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public Exception getException() {
		return exception;
	}
}
