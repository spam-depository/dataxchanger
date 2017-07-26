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

package de.marburg.uni.brainimaging.dataxchanger.asymmetriccryptography;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 * 
 * This Class is a utility class to read PEM-files. By now only RSA public and
 * private keys are supported.
 * 
 * @author Kornelius Podranski
 * 
 */
public class PemPkcs8KeyReader {

	// JCE provider to use
	private static final String PROVIDER = 
			org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

	// static utility class no instantiation necessary
	private PemPkcs8KeyReader() {
	}

	/**
	 * Reads a RSA private key from given file.
	 * 
	 * The file must only contain the corresponding key data in PKCS#8 PEM
	 * encoding.
	 * 
	 * @param file
	 *            file containing PEM encoded RSA private key in PKCS#8 format
	 * @return RSA private key contained in the file
	 * @throws IOException
	 *             if the file contains no valid PEM Object or any reason of
	 *             usual file I/O
	 * @throws InvalidKeySpecException
	 *             if the files PEM-content is not a valid PKCS#8 encoded key
	 * @throws NoSuchAlgorithmException
	 *             if the JRE cannot find any provider for RSA KeyFactory
	 * @throws NoSuchProviderException
	 *             if the Bouncycastle Provider is not found
	 */
	public static PrivateKey readRsaPrivateKey(File file) throws IOException,
			InvalidKeySpecException, NoSuchAlgorithmException,
			NoSuchProviderException {
		PrivateKey key = null;
		PemObject pem = readPemObject(file);
		// TODO check PEM header if correct key type
		byte[] bytes = pem.getContent();
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
		KeyFactory kf;
		kf = KeyFactory.getInstance("RSA", PROVIDER);
		key = kf.generatePrivate(keySpec);

		return key;
	}

	/**
	 * Reads a RSA public key from given file.
	 * 
	 * The file must only contain the corresponding key data in X509 PEM
	 * encoding.
	 * 
	 * @param file
	 *            file containing PEM encoded RSA public key in X509 format
	 * @return RSA public key contained in the file
	 * @throws IOException
	 *             if the file contains no valid PEM Object or any reason of
	 *             usual file I/O
	 * @throws InvalidKeySpecException
	 *             if the files PEM-content is not a valid PKCS#8 encoded key
	 * @throws NoSuchAlgorithmException
	 *             if the JRE cannot find any provider for RSA KeyFactory
	 * @throws NoSuchProviderException
	 *             if the Bouncycastle Provider is not found
	 */
	public static PublicKey readPublicKey(File file) throws IOException,
			InvalidKeySpecException, NoSuchAlgorithmException,
			NoSuchProviderException {
		PublicKey key = null;
		PemObject pem = readPemObject(file);
		// TODO check PEM header if correct key type
		byte[] bytes = pem.getContent();
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
		KeyFactory kf;
		kf = KeyFactory.getInstance("RSA", PROVIDER);
		key = kf.generatePublic(keySpec);

		return key;
	}

	/**
	 * Reads a PEM-object from file.
	 * 
	 * @param file
	 *            a file containing a PEM-object
	 * @return the PEM-object read from file
	 * @throws IOException
	 */
	private static PemObject readPemObject(File file) throws IOException {
		FileReader fileReader = new FileReader(file);
		BufferedReader in = new BufferedReader(fileReader);
		PemReader pemIn = new PemReader(in);
		PemObject pem = pemIn.readPemObject();
		pemIn.close();
		in.close();
		fileReader.close();
		if (pem == null)
			throw new IOException("Invalid PEM Object");
		return pem;
	}

}
