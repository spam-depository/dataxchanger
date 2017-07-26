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

package de.marburg.uni.brainimaging.dataxchanger.messagedigest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import de.marburg.uni.brainimaging.dataxchanger.StreamProcessor;

/**
 * This class provides a method to calculate a SHA-512 hash over some data.
 * 
 * @author Kornelius Podranski
 */
public class Sha512StreamDigest implements StreamProcessor<byte[]> {

	// name of the digest algorithm
	public static final String DIGEST = "SHA-512";
	// crypto-provider to use
	public static final String PROVIDER = 
			org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	// size of the streambuffer
	public static final int BUFFERSIZE = 1024; // 1MB

	private MessageDigest md;

	// store exception for StreamProcessor interface
	Exception exception;

	/**
	 * 
	 * @throws NoSuchAlgorithmException
	 *             if no provider for "SHA-512" is found
	 * @throws NoSuchProviderException
	 *             if the PROVIDER is not found
	 */
	public Sha512StreamDigest() throws NoSuchAlgorithmException,
			NoSuchProviderException {
		this.md = MessageDigest.getInstance(DIGEST, PROVIDER);
	}

	/**
	 * calculates the digest over the data read from in. the data will be copied
	 * to out. out may be null.
	 * 
	 * @param in
	 *            data to digest
	 * @param out
	 *            stream to copy data to
	 * @return the digest over the data from in
	 * @throws IOException
	 */
	public byte[] digest(InputStream in, OutputStream out) throws IOException {
		if (out == null) {
			DigestInputStream dIn = new DigestInputStream(in, md);
			for (int b = 0; b != -1; b = dIn.read())
				;
			dIn.close();
		} else {
			DigestOutputStream dOut = new DigestOutputStream(out, md);
			byte[] buffer = new byte[BUFFERSIZE];
			int inCount = in.read(buffer, 0, BUFFERSIZE);
			while (inCount != -1) {
				dOut.write(buffer, 0, inCount);
				inCount = in.read(buffer, 0, BUFFERSIZE);
			}
			dOut.close();
		}

		return md.digest();
	}

	// STREAMPROCESSOR INTERFACE
	/**
	 * calculates message digest over in. the stream i copied to out. the
	 * resulting digest can be retrieved via getResult() <br>
	 * {@inheritDoc}
	 */
	public boolean process(InputStream in, OutputStream out) {
		try {
			digest(in, out);
		} catch (Exception e) {
			exception = e;
			return false;
		}
		return true;
	}

	/**
	 * returns the resulting digest <br>
	 * {@inheritDoc}
	 */
	public byte[] getResult() {
		return md.digest();
	}

	/**
	 * {@inheritDoc}
	 */
	public Exception getException() {
		return exception;
	}
}
