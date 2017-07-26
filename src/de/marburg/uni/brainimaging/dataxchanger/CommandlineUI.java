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

package de.marburg.uni.brainimaging.dataxchanger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import de.marburg.uni.brainimaging.dataxchanger.asymmetriccryptography.PemPkcs8KeyReader;
import de.marburg.uni.brainimaging.dataxchanger.asymmetriccryptography.RsaSecretKeyEncryption;
import de.marburg.uni.brainimaging.dataxchanger.configurationfile.ReceiverConfigurationFile;
import de.marburg.uni.brainimaging.dataxchanger.dicomanonymizer.DicomWhitelist;
import de.marburg.uni.brainimaging.dataxchanger.dicomanonymizer.DicomWhitelistAnonymizer;
//import de.marburg.uni.brainimaging.dataxchanger.ftp.FtpClient;
import de.marburg.uni.brainimaging.dataxchanger.ftp.FtpDownloader;
import de.marburg.uni.brainimaging.dataxchanger.ftp.FtpUploader;
import de.marburg.uni.brainimaging.dataxchanger.messagedigest.Sha512StreamDigest;
import de.marburg.uni.brainimaging.dataxchanger.symmetriccryptography.Aes256CtrStreamDecryptor;
//import de.marburg.uni.brainimaging.dataxchanger.symmetriccryptography.Aes256CtrStreamEncryption;
import de.marburg.uni.brainimaging.dataxchanger.symmetriccryptography.Aes256CtrStreamEncryptor;

/**
 * This class defines a simple commandline interfce to the dataXchanger tool. It
 * can be started without instanciating by calling the start()-function and
 * ended at any time by calling the exit()-function.
 * 
 * @author Kornelius Podranski
 */
public class CommandlineUI {

	// exit codes
	public static final int EXIT_SUCCESS = 0;
	public static final int EXIT_ERROR = 1;

	// debug mode
	private static int debug = 0;

	// mode of operation
	private static enum OpMode {
		SEND, RECEIVE
	};

	// list of input files
	private static List<File> input = new ArrayList<File>();
	// dicom whitelist
	private static DicomWhitelist whitelist;
	// receiver's public key to encrypt with
	// private static PublicKey asymEncKey;
	private static List<PublicKey> asymEncKeys = new ArrayList<PublicKey>();
	// receiver's private key to decrypt with
	private static PrivateKey asymDecKey;
	// receiver configuration file
	private static List<ReceiverConfigurationFile> rconf = new ArrayList<ReceiverConfigurationFile>();
	// mode of operation
	private static OpMode opmode;
	// list of filenames on ftp-server
	// private static List<String> ftpFilename = new ArrayList<String>();
	// ftp-server address
	private static String ftpAddress;
	// ftp-server port (defaults to 21)
	private static int ftpPort = 21;
	// ftp-server username
	private static String ftpUser;
	// ftp-server password
	private static String ftpPassword;
	// use ftp-server in active mode?
	private static boolean ftpActive = false;
	// rename file before transmitting
	private static boolean anonymize_filenames = false;

	/**
	 * starts the commandline UI
	 * 
	 * @param args
	 *            the arguments which have been provided calling the tool at the
	 *            commandline.
	 */
	public static void start(String[] args) {
		parseArgs(args);
		validateAllSet();
		switch (opmode) {
		case RECEIVE:
			receive();
			break;
		case SEND:
			send();
			break;
		}
	}

	/**
	 * parses the commandline arguments and sets the variables according to
	 * them.
	 * 
	 * @args the commandline arguments
	 */
	private static void parseArgs(String[] args) {
		if (args.length == 0) {
			printHelpMessage();
			exit(EXIT_ERROR);
		}

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.equals("--anonymize-filenames")) {
				anonymize_filenames = true;
				continue;
			}
			if (arg.equals("--conf")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a PATH for "
							+ "\"--conf\". exiting.");
				arg = args[i];
				checkFile(arg, false);
				ReceiverConfigurationFile rcf = new ReceiverConfigurationFile(
						arg);
				try {
					rcf.load();
				} catch (IOException e) {
					error("error reading receiver configuration file \"%s\".\n"
							+ "message was: %s\nexiting.", arg, e.toString());
				}
				rconf.add(rcf);
				continue;
			}
			if (arg.equals("--debug")) {
				debug = 1;
				continue;
			}
			if (arg.equals("--dec-key")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a PATH for "
							+ "\"--dec-key\". exiting.");
				arg = args[i];
				try {
					File file = checkFile(arg, false);
					asymDecKey = PemPkcs8KeyReader.readRsaPrivateKey(file);
				} catch (InvalidKeySpecException e) {
					error("internal error constructing decryption key from "
							+ "file \"%s\".\nmessage was: %s\nexiting.", arg,
							e.toString());
				} catch (NoSuchAlgorithmException e) {
					error("algorithm of decryption key \"%s\" not "
							+ "available in your JRE.\nmessage was: "
							+ "%s\nexiting.", arg, e.toString());
				} catch (NoSuchProviderException e) {
					error("Bouncycastle-Provider not "
							+ "available in your JRE.\nmessage was: "
							+ "%s\nexiting.", e.toString());
				} catch (IOException e) {
					error("parsing decryption key file \"%s\" failed.\n"
							+ "message was: %s\nexiting.", arg, e.toString());
				}
				continue;
			}
			if (arg.equals("--enc-key")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a PATH for "
							+ "\"--enc-key\". exiting.");
				arg = args[i];
				try {
					File file = checkFile(arg, false);
					PublicKey asymEncKey = PemPkcs8KeyReader
							.readPublicKey(file);
					asymEncKeys.add(asymEncKey);
				} catch (InvalidKeySpecException e) {
					error("internal error constructing encryption key from "
							+ "file \"%s\".\nmessage was: %s\nexiting.", arg,
							e.toString());
				} catch (NoSuchAlgorithmException e) {
					error("algorithm of encryption key \"%s\" not "
							+ "available in your JRE.\nmessage was: "
							+ "%s\nexiting.", arg, e.toString());
				} catch (NoSuchProviderException e) {
					error("Bouncycastle-Provider not "
							+ "available in your JRE.\nmessage was: "
							+ "%s\nexiting.", e.toString());
				} catch (IOException e) {
					error("parsing encryption key file \"%s\" failed.\n"
							+ "message was: %s\nexiting.", arg, e.toString());
				}
				continue;
			}
			if (arg.equals("--ftp-active")) {
				ftpActive = true;
				continue;
			}
			if (arg.equals("--ftp-password")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a PASSWORD for "
							+ "\"--ftp-password\". exiting.");
				arg = args[i];
				ftpPassword = arg;
				continue;
			}
			if (arg.equals("--ftp-port")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a PORTNUMBER for "
							+ "\"--ftp-port\". exiting.");
				arg = args[i];
				try {
					ftpPort = Integer.parseInt(arg);
				} catch (NumberFormatException e) {
					error("not valid portnumber \"%s\". exiting.", arg);
				}
				continue;
			}
			if (arg.equals("--ftp-server")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a ADDRESS for "
							+ "\"--ftp-server\". exiting.");
				arg = args[i];
				ftpAddress = arg;
				continue;
			}
			if (arg.equals("--ftp-user")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a USERNAME for "
							+ "\"--ftp-user\". exiting.");
				arg = args[i];
				ftpUser = arg;
				continue;
			}
			if (arg.equals("--help")) {
				printHelpMessage();
				exit(EXIT_SUCCESS);
			}
			if (arg.equals("--input")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a PATH for "
							+ "\"--input\". exiting.");
				arg = args[i];
				input.add(checkFile(arg, false));
				continue;
			}
			if (arg.equals("--receive")) {
				if (opmode != null)
					error("\"--receive\" and \"--send\" are exclusive. exiting.");
				opmode = OpMode.RECEIVE;
				continue;
			}
			if (arg.equals("--send")) {
				if (opmode != null)
					error("\"--receive\" and \"--send\" are exclusive. exiting.");
				opmode = OpMode.SEND;
				continue;
			}
			if (arg.equals("--version")) {
				printVersion();
				exit(EXIT_SUCCESS);
			}
			if (arg.equals("--whitelist")) {
				i++;
				if (i == args.length)
					error("not enough arguments. you must specify a PATH for "
							+ "\"--whitelist\". exiting.");
				arg = args[i];
				try {
					File file = checkFile(arg, false);
					whitelist = new DicomWhitelist(file);
				} catch (IOException e) {
					error("parsing whitelist \"%s\" failed.\nmessage was: %s\n"
							+ "exiting.", arg, e.toString());
				} catch (IllegalArgumentException e) {
					error("whitelist not o.k.\nmessage was: %s\n" + "exiting.",
							e.getMessage());
				}
				continue;
			}
			// if we reach here the argument is unknown
			error("unknown argument: %s", arg);
		}
	}

	/**
	 * tests if all variables for correct operation have been set. call this
	 * after parsing the commandline arguments. <br>
	 * If something is not correct an error message will be printed to stderr
	 * and the exit()-function will be called.
	 */
	private static void validateAllSet() {
		// check if opmode is set
		if (opmode == null)
			error("no mode of operation specified. you must specify "
					+ "\"--receive\" or \"--send\". exiting.");
		// check necessary parameters depending on mode
		switch (opmode) {
		case RECEIVE:
			if (asymDecKey == null)
				error("no decryption key given. you must specify "
						+ "\"--dec-key PATH\" in receive mode. exiting.");
			if (rconf.isEmpty())
				error("no receiver configuration files given. you must specify "
						+ "\"--conf PATH\" at least once. exiting.");
			break;
		case SEND:
			if (input.isEmpty())
				error("no input files given. you must specify \"--input PATH\" "
						+ "at least once. exiting.");
			if (asymEncKeys.isEmpty())
				error("no encryption key given. you must specify "
						+ "\"--enc-key PATH\" at least once in send mode. "
						+ "exiting.");
			if (whitelist == null)
				error("no whitelist given. you must specify "
						+ "\"--whitelist PATH\" in send mode. exiting.");
			if (ftpAddress == null)
				error("no FTP-server address given. you must specify "
						+ "\"--ftp-server ADDRESS\" in send mode. exiting.");
			if (ftpUser == null)
				error("no username for FTP-server given. you must specify "
						+ "\"--ftp-user USERNAME\" in send mode. exiting.");
			if (ftpPassword == null)
				error("no password for FTP-server given. you must specify "
						+ "\"--ftp-password PASSWORD\" in send mode. exiting.");
			break;
		}
	}

	/**
	 * executes the receive operation mode.
	 */
	private static void receive() {
		Iterator<ReceiverConfigurationFile> itRconf = rconf.iterator();
		while (itRconf.hasNext()) {
			// File file = itInput.next();
			ReceiverConfigurationFile rcf = itRconf.next();

			// get file from server
			String ftpServer = rcf.getFtpServer();
			int ftpPort = rcf.getFtpPort();
			String ftpUser = rcf.getFtpUser();
			String ftpPassword = rcf.getFtpPassword();
			String ftpFilename = rcf.getFtpFilename();
			String dataFilename = rcf.getDataFilename();
			File file = new File("encrypted_" + dataFilename);
			OutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(file));
			} catch (IOException e) {
				error("cannot create local file \"%s\".\n"
						+ "message was: %s\nexiting.", file.getName(),
						e.toString());
			}
			StreamProcessor<Void> ftp = new FtpDownloader(ftpServer, ftpPort,
					ftpUser, ftpPassword, ftpActive, ftpFilename);
			if (!ftp.process(null, out)) {
				error("downloading from ftp-server failed.\n"
						+ "message was: %s\nexiting.", ftp.getException()
						.toString());
			}
			try {
				out.close();
			} catch (IOException e) {
				// ignore
			}
			out = null;
			ftp = null;

			// check digest
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				error("inputfile \"%s\" not found. exitig.", file.getName());
			}
			byte[] digest = null;
			try {
				StreamProcessor<byte[]> digester = new Sha512StreamDigest();
				if (!digester.process(in, null)) {
					error("internal error digesting data.\n"
							+ "message was: %s\nexiting.", digester
							.getException().toString());
				}
				digest = digester.getResult();
			} catch (NoSuchAlgorithmException e) {
				error("internal error digesting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			} catch (NoSuchProviderException e) {
				error("internal error digesting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			}
			if (!MessageDigest.isEqual(digest, rcf.getDigest()))
				error("message digest of receiver config file \"%s\" and "
						+ "datafile \"%s\" do not match. exiting.",
						rcf.getFilename(), file.getName());

			// read private rsa key
			byte[] xsKey = rcf.getEncryptedSecretKey();

			// decrypt secret key
			SecretKey sKey = null;
			try {
				sKey = RsaSecretKeyEncryption.decryptSecretKey(xsKey,
						asymDecKey);
			} catch (NoSuchProviderException e) {
				error("Bouncycastle-Provider not "
						+ "available in your JRE.\nmessage was: "
						+ "%s\nexiting.", e.toString());
			} catch (InvalidKeyException e) {
				error("decryption key is invalid.\nmessage was: %s\n"
						+ "exiting.", e.toString());
			} catch (NoSuchAlgorithmException e) {
				error("internal error unwrapping secret key.\nmessage was: %s\n"
						+ "exiting.", e.toString());
			} catch (NoSuchPaddingException e) {
				error("internal error unwrapping secret key.\nmessage was: %s\n"
						+ "exiting.", e.toString());
			} catch (IllegalBlockSizeException e) {
				error("internal error unwrapping secret key.\nmessage was: %s\n"
						+ "exiting.", e.toString());
			} catch (BadPaddingException e) {
				error("internal error unwrapping secret key.\nmessage was: %s\n"
						+ "exiting.", e.toString());
			}

			// decrypt data
			// open input file
			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				error("inputfile \"%s\" not found. exitig.", file.getName());
			}
			// open output file
			try {
				out = new FileOutputStream(dataFilename);
			} catch (FileNotFoundException e) {
				error("can not write outputfile \"%s\". exiting.", dataFilename);
			}
			// decrypt
			StreamProcessor<Void> cipher;
			try {
				cipher = new Aes256CtrStreamDecryptor(sKey);
				if (!cipher.process(in, out)) {
					error("internal error decrypting data.\n"
							+ "message was: %s\nexiting.", cipher
							.getException().toString());
				}
			} catch (InvalidKeyException e) {
				error("internal error decrypting data: invalid secret key.\n"
						+ "message was: %s\nexiting.", e.toString());
			} catch (NoSuchAlgorithmException e) {
				error("internal error decrypting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			} catch (NoSuchProviderException e) {
				error("internal error decrypting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			} catch (NoSuchPaddingException e) {
				error("internal error decrypting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			}
			// close files
			try {
				in.close();
				out.close();
			} catch (IOException e) {
				error("internal error closing input or output files.\n"
						+ "message was: %s\nexiting.", e.toString());
			}

			// cleanup
			if (debug > 0) // do cleanup only if no debug mode
				return;
			File rmFile = new File("encrypted_" + dataFilename);
			if (!rmFile.delete()) {
				error("can not delete temporary file \"%s\".\nexiting.",
						rmFile.getName());
			}
			rmFile = null;
		}
	}

	/**
	 * executes the send operation mode.
	 */
	private static void send() {
		for (File file : input) {
			String filename = file.getName();
			InputStream in = null;
			OutputStream out = null;
			// anonymize
			try {
				in = new FileInputStream(file);
				filename = "anonymized_" + filename;
				out = new FileOutputStream(filename);
				StreamProcessor<Void> anonymizer = new DicomWhitelistAnonymizer(
						whitelist);
				if (!anonymizer.process(in, out))
					error("error during anonymization\nmessage was: \"%s\"\n"
							+ "exiting.", anonymizer.getException().toString());
				in.close();
				in = null;
				out.close();
				out = null;
			} catch (IOException e) {
				error("i/o error during anonymization\nmessage was: \"%s\"\n"
						+ "exiting.", e.toString());
			}
			// open input file
			try {
				in = new FileInputStream(filename);
			} catch (FileNotFoundException e) {
				error("inputfile \"%s\" not found. exitig.", file.getName());
			}
			// open output file
			filename = "encrypted_" + filename;
			try {
				out = new FileOutputStream(filename);
			} catch (FileNotFoundException e) {
				error("can not write outputfile \"%s\". exiting.", filename);
			}
			// encrypt
			StreamProcessor<SecretKey> cipher = null;
			try {
				cipher = new Aes256CtrStreamEncryptor();
				if (!cipher.process(in, out)) {
					error("internal error decrypting data.\n"
							+ "message was: %s\nexiting.", cipher
							.getException().toString());
				}
			} catch (InvalidKeyException e) {
				error("internal error encrypting data: invalid secret key.\n"
						+ "message was: %s\nexiting.", e.toString());
			} catch (NoSuchAlgorithmException e) {
				error("internal error encrypting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			} catch (NoSuchProviderException e) {
				error("internal error encrypting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			} catch (NoSuchPaddingException e) {
				error("internal error encrypting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			}
			// close files
			try {
				in.close();
				in = null;
				out.close();
				out = null;
			} catch (IOException e) {
				error("internal error closing input or output files.\n"
						+ "message was: %s\nexiting.", e.toString());
			}

			// calculate digest of encrypted file
			try {
				in = new FileInputStream(filename);
			} catch (FileNotFoundException e) {
				error("inputfile \"%s\" not found. exitig.", file.getName());
			}
			byte[] digest = null;
			try {
				StreamProcessor<byte[]> digester = new Sha512StreamDigest();
				if (!digester.process(in, null)) {
					error("internal error digesting data.\n"
							+ "message was: %s\nexiting.", digester
							.getException().toString());
				}
				digest = digester.getResult();
			} catch (NoSuchAlgorithmException e) {
				error("internal error digesting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			} catch (NoSuchProviderException e) {
				error("internal error digesting data.\n"
						+ "message was: %s\nexiting.", e.toString());
			}

			// encrypt secret keys
			// byte[] xsKey = {};
			byte[][] xsKeys = new byte[asymEncKeys.size()][];
			Iterator<PublicKey> asymEncKeyIt = asymEncKeys.iterator();
			for (int i = 0; i < xsKeys.length && asymEncKeyIt.hasNext(); i++) {
				try {
					xsKeys[i] = RsaSecretKeyEncryption.encryptSecretKey(
							cipher.getResult(), asymEncKeyIt.next());
				} catch (NoSuchProviderException e) {
					error("Bouncycastle-Provider not "
							+ "available in your JRE.\nmessage was: "
							+ "%s\nexiting.", e.toString());
				} catch (InvalidKeyException e) {
					error("internal error wrapping secret key: invalid secret "
							+ "key.\nmessage was: %s\nexiting.", e.toString());
				} catch (NoSuchAlgorithmException e) {
					error("internal error wrapping secret key.\n"
							+ "message was: %s\nexiting.", e.toString());
				} catch (NoSuchPaddingException e) {
					error("internal error wrapping secret key.\n"
							+ "message was: %s\nexiting.", e.toString());
				} catch (IllegalBlockSizeException e) {
					error("internal error wrapping secret key.\n"
							+ "message was: %s\nexiting.", e.toString());
				} catch (BadPaddingException e) {
					error("internal error wrapping secret key.\n"
							+ "message was: %s\nexiting.", e.toString());
				}
			}
			asymEncKeyIt = null;

			// upload to ftp-server
			try {
				in = new BufferedInputStream(new FileInputStream(filename));
			} catch (IOException e) {
				error("cannot open input file \"%s\" for ftp transfer.\n"
						+ "message was: %s\nexiting.", filename, e.toString());
			}
			StreamProcessor<String> ftp = new FtpUploader(ftpAddress, ftpPort,
					ftpUser, ftpPassword, ftpActive);
			if (!ftp.process(in, null)) {
				error("uploading to ftp-server failed.\n"
						+ "message was: %s\nexiting.", ftp.getException()
						.toString());
			}
			String ftpFilename = ftp.getResult();
			try {
				in.close();
			} catch (IOException e) {
				// ignore
			}
			in = null;
			ftp = null;

			// write receiver configs
			String basename = file.getName().split("\\.(?=[^\\.]+$)")[0];
			for (int i = 0; i < xsKeys.length; i++) {
				String confFilename = basename + "_" + i + ".rconf";
				ReceiverConfigurationFile rconf = new ReceiverConfigurationFile(
						confFilename);
				rconf.setEncryptedSecretKey(xsKeys[i]);
				rconf.setDigest(digest);
				rconf.setFtpServer(ftpAddress);
				rconf.setFtpPort(ftpPort);
				rconf.setFtpUser(ftpUser);
				rconf.setFtpPassword(ftpPassword);
				rconf.setFtpFilename(ftpFilename);
				if (anonymize_filenames) {
					rconf.setDataFilename(ftpFilename + "_dataXchanger_dicomfile");
				} else {
					rconf.setDataFilename(file.getName());
				}
				try {
					rconf.store();
				} catch (IOException e) {
					error("can not write receiver configuration file.\n"
							+ "message was: %s\nexiting.", e.toString());
				}
			}

			// cleanup
			if (debug > 0) // do cleanup only if no debug mode
				return;
			File rmFile = new File("anonymized_" + file.getName());
			if (!rmFile.delete()) {
				error("can not delete temporary file \"%s\".\nexiting.",
						rmFile.getName());
			}
			rmFile = new File("encrypted_anonymized_" + file.getName());
			if (!rmFile.delete()) {
				error("can not delete temporary file \"%s\".\nexiting.",
						rmFile.getName());
			}
			rmFile = null;
		}
	}

	/**
	 * executes the help mode.
	 */
	private static void printHelpMessage() {
		System.out
				.printf("syntax: %s [mode] [options]\n"
						+ "MODES:\n"
						+ "--help\t\t\tprint this help message and exit\n"
						+ "--version\t\tprint program version and exit\n"
						+ "--receive\t\tuse program in receive mode (download and decrypt)\n"
						+ "--send\t\t\tuse program in send mode (anonymize, encrypt and uplaod)\n"
						+ "\n"
						+ "OPTIONS RECEIVE:\n"
						+ "--conf PATH\t\trevceiver configuration file\n"
						+ "--dec-key PATH\t\tfile with private key for decryption\n"
						+ "\n"
						+ "OPTIONS SEND:\n"
						+ "--anonymize-filenames\tdo not send the original filename to the receiver\n"
						+ "--enc-key PATH\t\tfile with public key for encryption\n"
						+ "\t\t\t(can be used multiple times)\n"
						+ "--ftp-password PASSWORD\tpassword for ftp-server login\n"
						+ "--ftp-port PORTNUMBER\tport of ftp-server if other than 21\n"
						+ "--ftp-server ADDRESS\tadress or ip of ftp-server\n"
						+ "--ftp-user USERNAME\tusername for ftp-server login\n"
						+ "--input PATH\t\tdicom file to process (can be used multiple times)\n"
						+ "--whitelist PATH\tfile with dicom-tag-whitelist for anonymization\n"
						+ "\n"
						+ "GENERAL OPTIONS:\n"
						+ "--debug\t\t\tdo not delete temporary files\n"
						+ "--ftp-active\t\tuse active ftp-mode (default is passive)\n",
						DataXchanger.NAME);
	}

	/**
	 * executes the version mode.
	 */
	private static void printVersion() {
		System.out.printf("%s version %s\n", DataXchanger.NAME,
				DataXchanger.VERSION);
	}

	private static File checkFile(String path, boolean checkWritable) {
		File file = new File(path);
		if (!file.exists())
			error("file \"%s\" does not exist. exiting.", path);
		if (!file.isFile())
			error("file \"%s\" is not a file. exiting.", path);
		if (!file.canRead())
			error("file \"%s\" is not readable. exiting.", path);
		if (checkWritable && !file.canWrite())
			error("file \"%s\" is not writeable. exiting.", path);

		return file;
	}

	/**
	 * prints given error message to stderr and exits ungracefully
	 * (System.exit). the string "error: " will be prepended to the message an a
	 * newline "\n" will be added to the message
	 * 
	 * @param format
	 *            errormessage which may be a format string (see
	 *            java.util.Formatter)
	 * @param args
	 *            args according to format string (see java.util.Formatter)
	 */
	private static void error(String format, Object... args) {
		System.err.printf("error: " + format + "\n", args);
		exit(EXIT_ERROR);
	}

	/**
	 * do some cleanup and exit with the given exitcode.
	 * 
	 * @param exitcode
	 *            one of the defined EXIT_ constants
	 */
	public static void exit(final int exitcode) {
		// TODO cleanup
		System.exit(exitcode);
	}
}
