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

package de.marburg.uni.brainimaging.dataxchanger.dicomanonymizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;

import de.marburg.uni.brainimaging.dataxchanger.StreamProcessor;

/**
 * This class provides a functionality to apply a DicomWhitelist to a
 * DICOM-object.
 * 
 * @author Kornelius Podranski
 */
public class DicomWhitelistAnonymizer implements StreamProcessor<Void> {

	// stores occoured exceptions for StreamProcessor interface
	private Exception processException = null;
	// stores the whitelist for StreamProcessor interface
	private DicomWhitelist whitelist;

	public DicomWhitelistAnonymizer(DicomWhitelist whitelist) {
		this.whitelist = whitelist;
	}

	/**
	 * Applies the given DicomWhitelist to the DICOM-object read from in and
	 * writes the resulting DICOM-object to out. Applying means changing the
	 * values of data elements if a value is given in the whitelist for a
	 * certain data element and removing data elements if they are not part of
	 * the whitelist.
	 * 
	 * @param in
	 *            stream providing a dicom object
	 * @param out
	 *            stream the resulting dicom object will be written to
	 * @param whitelist
	 *            whitelist to apply to the dicom object
	 * @throws IOException
	 */
	public static void anonymize(InputStream in, OutputStream out,
			DicomWhitelist whitelist) throws IOException {
		DicomInputStream dIn = new DicomInputStream(in);
		DicomObject dcmObj = dIn.readDicomObject();
		dIn.close();

		DicomObject anonDcmObj = new BasicDicomObject();
		Iterator<DicomElement> it = dcmObj.iterator();
		while (it.hasNext()) {
			DicomElement e = it.next();
			// System.out.println("debug: " + e.toString());
			int tag = e.tag();
			if (whitelist.hasTag(tag)) {
				if (whitelist.hasValue(tag)) { // replace value with constant
					anonDcmObj.add(whitelist.getValue(tag));
					// DicomElement valueElement = whitelist.getValue(tag);
					// VR valueVr = valueElement.vr();
					// byte[] valueValue = valueElement.getBytes();
					// anonDcmObj.putBytes(tag, valueVr, valueValue);
				} else { // copy value from source
					anonDcmObj.add(e);
					// VR vr = e.vr();
					// byte[] value = e.getBytes();
					// anonDcmObj.putBytes(tag, vr, value);
				}
			}
		}

		try {
			DicomOutputStream dOut = new DicomOutputStream(out);
			dOut.writeDicomFile(anonDcmObj);
			dOut.close();
		} catch (IllegalArgumentException e) {
			throw new IOException("could not write DICOM-file: "
					+ e.getMessage());
		}
	}

	// STREAMPROCESSOR INTERFACE
	/**
	 * anonymizes a dicom object contained in the in-stream and writes the
	 * result to out.
	 */
	public boolean process(InputStream in, OutputStream out) {
		try {
			anonymize(in, out, whitelist);
		} catch (Exception e) {
			processException = e;
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
		return processException;
	}
}
