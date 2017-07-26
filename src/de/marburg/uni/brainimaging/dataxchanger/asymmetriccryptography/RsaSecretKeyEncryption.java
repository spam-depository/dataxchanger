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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class provides functions to asymmetrically encrypt and decrypt secret
 * keys with RSA.
 * 
 * @author Kornelius Podranski
 */
public class RsaSecretKeyEncryption {

	// JCE provider to use
	private static final String PROVIDER = 
			org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

	/**
	 * Encrypts a secret key with the given RSA-public-key.
	 * 
	 * @param sKey
	 *            the secret key to encrypt
	 * @param pubKey
	 *            the RSA public key to use for encryption
	 * @return the encrypted secret key
	 * @throws NoSuchAlgorithmException
	 *             if no provider for "RSA/ECB/OAEPWithSHA-1AndMGF1Padding" can
	 *             be found
	 * @throws NoSuchPaddingException
	 *             if no provider for "OAEPWithSHA-1AndMGF1Padding" can be found
	 * @throws InvalidKeyException
	 *             if the given pubKey is not a valid RSA-public-key
	 * @throws IllegalBlockSizeException
	 *             if the auto-selected algorithm is not able to handle the
	 *             keysize defined by pubKey
	 * @throws BadPaddingException
	 *             if something goes wrong during the padding step
	 * @throws NoSuchProviderException
	 *             if the Bouncycastle Provider is not found
	 */
	public static byte[] encryptSecretKey(SecretKey sKey, PublicKey pubKey)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchProviderException {
		byte[] result = null;
		Cipher cipher = Cipher.getInstance(
				"RSA/ECB/OAEPWithSHA-1AndMGF1Padding", PROVIDER);
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		result = cipher.doFinal(sKey.getEncoded());
		return result;
	}

	/**
	 * Decrypts an RSA-encrypted secret key with the given private-key.
	 * 
	 * @param xsKey
	 *            the encrypted secret key
	 * @param privKey
	 *            the RSA-private key corresponding to the public-key used for
	 *            encryption
	 * @return the decrypted secret key
	 * @throws NoSuchAlgorithmException
	 *             if no provider for "RSA/ECB/OAEPWithSHA-1AndMGF1Padding" can
	 *             be found
	 * @throws NoSuchPaddingException
	 *             if no provider for "OAEPWithSHA-1AndMGF1Padding" can be found
	 * @throws InvalidKeyException
	 *             if the given privKey is not a valid RSA-private-key
	 * @throws IllegalBlockSizeException
	 *             if the auto-selected algorithm is not able to handle the
	 *             keysize defined by privKey
	 * @throws BadPaddingException
	 *             if something goes wrong during the de-padding step
	 * @throws NoSuchProviderException
	 *             if the Bouncycastle Provider is not found
	 */
	public static SecretKey decryptSecretKey(byte[] xsKey, PrivateKey privKey)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchProviderException {
		SecretKey sKey = null;
		Cipher cipher;
		cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding",
				PROVIDER);
		cipher.init(Cipher.DECRYPT_MODE, privKey);
		byte[] encSKey = cipher.doFinal(xsKey);
		sKey = new SecretKeySpec(encSKey, "AES");
		return sKey;
	}

}
