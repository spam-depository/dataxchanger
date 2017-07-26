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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * This Class provides AES-256 encryption and decryption in counter mode (ctr).
 * The class needs to be initialized with null for encryption with automatic key
 * generation or with a specific key of 256 bit for decryption and encryption
 * using this key.
 * 
 * The IV is auto generated and written to the OutputStream before the encrypted
 * data on encryption and expeted at the beginning of the InputStream on
 * decryption.
 * 
 * @author Kornelius Podranski
 * 
 */
public class Aes256CtrStreamEncryption {
	public static final String PROVIDER = 
			org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	public static final String CIPHER = "AES";
	public static final String MODE = "CTR";
	public static final String PADDING = "NoPadding"; // no padding necessary
														// for CTR
	public static final int KEYSIZE = 256; // bits
	public static final int IVSIZE = 128; // bits
	public static final int BUFFERSIZE = 1024; // 1MB

	// attributes
	private SecretKey key;
	// private byte[] iv;
	private Cipher cipher;

	/**
	 * 
	 * @param key
	 *            secret key of correct bitlength or null to randomly generate
	 *            key
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 */
	public Aes256CtrStreamEncryption(SecretKey key)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			NoSuchPaddingException, InvalidKeyException {
		if (key == null) {
			KeyGenerator generator = KeyGenerator.getInstance(CIPHER, PROVIDER);
			generator.init(KEYSIZE);
			this.key = generator.generateKey();
		} else {
			if (key.getEncoded().length != KEYSIZE / 8)
				throw new InvalidKeyException("wrong keysize");
			this.key = key;
		}
		cipher = Cipher.getInstance(CIPHER + "/" + MODE + "/" + PADDING,
				PROVIDER);
	}

	/**
	 * Encrypts in with the key set at initialisation.
	 * 
	 * @param in
	 *            data to encrypt
	 * @param out
	 *            stream encrypted data is written to
	 * @throws IOException
	 * @throws InvalidKeyException
	 *             if the secret key provided during initialisation is not valid
	 */
	public void encrypt(InputStream in, OutputStream out) throws IOException,
			InvalidKeyException {
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] iv = cipher.getIV();
		out.write(iv);
		CipherOutputStream cOut = new CipherOutputStream(out, cipher);

		byte[] buffer = new byte[BUFFERSIZE];
		int inCount = in.read(buffer, 0, BUFFERSIZE);
		while (inCount != -1) {
			cOut.write(buffer, 0, inCount);
			inCount = in.read(buffer, 0, BUFFERSIZE);
		}
		cOut.close();

		in.close();
		out.close();
	}

	/**
	 * Decrypt in with the key set at initialisation.
	 * 
	 * @param in
	 *            encrypted data
	 * @param out
	 *            stream decrypted data is written to
	 * @throws IOException
	 * @throws InvalidKeyException
	 *             if the secret key provided at initialisation is not valid
	 * @throws InvalidAlgorithmParameterException
	 *             if no provider for AES/CTR/NoPadding can be found
	 */
	public void decrypt(InputStream in, OutputStream out) throws IOException,
			InvalidKeyException, InvalidAlgorithmParameterException {
		int ivSize = IVSIZE / 8;
		byte[] ivBuffer = new byte[ivSize];
		int inSum = 0;
		int inCount = in.read(ivBuffer, 0, ivSize);
		inSum += inCount;
		while (inSum < ivSize && inCount != -1) {
			inCount = in.read(ivBuffer, inCount, ivSize - inCount);
			inSum += inCount;
		}

		IvParameterSpec iv = new IvParameterSpec(ivBuffer);

		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		CipherOutputStream cOut = new CipherOutputStream(out, cipher);

		byte[] buffer = new byte[BUFFERSIZE];
		inCount = in.read(buffer, 0, BUFFERSIZE);
		while (inCount != -1) {
			cOut.write(buffer, 0, inCount);
			inCount = in.read(buffer, 0, BUFFERSIZE);
		}
		cOut.close();

		in.close();
		out.close();
	}

	/**
	 * secret key set or generated during initialisation.
	 * 
	 * @return secret key of this instance
	 */
	public SecretKey getKey() {
		return key;
	}

}
