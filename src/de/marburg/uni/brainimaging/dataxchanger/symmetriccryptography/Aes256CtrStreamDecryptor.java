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

package de.marburg.uni.brainimaging.dataxchanger.symmetriccryptography;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import de.marburg.uni.brainimaging.dataxchanger.StreamProcessor;

/**
 * This class implements the decrypting StreamProcessor interface for the
 * Aes256CtrStreamEncryption.
 * 
 * @author Kornelius Podranski
 */
public class Aes256CtrStreamDecryptor extends Aes256CtrStreamEncryption
		implements StreamProcessor<Void> {

	// strores exception for StreamProcessor interface
	Exception exception = null;

	public Aes256CtrStreamDecryptor(SecretKey key)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			NoSuchPaddingException, InvalidKeyException {
		super(key);
	}

	/**
	 * AES256/CTR/NoPadding decrypts in with the key given at initialisation. <br>
	 * {@inheritDoc}
	 */
	public boolean process(InputStream in, OutputStream out) {
		try {
			decrypt(in, out);
		} catch (Exception e) {
			exception = e;
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * {@inheritDoc}
	 */
	public Void getResult() {
		return null;
	}

}
