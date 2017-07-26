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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.data.VRMap;

/**
 * This class provides a representation of a whitelist of DICOM-tags. The list
 * will be read from disk.
 * 
 * @author Kornelius Podranski
 */
public class DicomWhitelist {
	public static final boolean bigEdian = false;
	private File file;
	private Map<Integer, DicomElement> tagList;

	/**
	 * Creates an instance of this class containing the tags of the given file.
	 * 
	 * @param file
	 *            a DICOM-tag-whitelist to read from disk
	 * @throws IOException
	 * @throws IllegalArgumentException
	 *             if mandatory tags are missing in the whitelist
	 */
	public DicomWhitelist(File file) throws IOException,
			IllegalArgumentException {
		this.file = file;
		tagList = new HashMap<Integer, DicomElement>();
		read();
		check();
	}

	/**
	 * checks if all IOD-independent mandatory tags are present.
	 * 
	 * @throws IllegalArgumentException
	 *             if the check is not passed
	 */
	private void check() throws IllegalArgumentException {
		if (tagList == null)
			throw new IllegalStateException();
		// check if whitelist contains "Transfer Syntax UID" necessary for
		// writing files
		if (!tagList.containsKey(Tag.TransferSyntaxUID))
			throw new IllegalArgumentException(
					"0002,0010 Transfer Syntax UID missing in whitelist");

	}

	/**
	 * reads a DICOM-tag-whitelist from disk.
	 * 
	 * @throws IOException
	 */
	private void read() throws IOException {
		if (file == null)
			throw new IllegalStateException();
		// if (!file.exists())
		// throw new FileNotFoundException();
		// if (!file.canRead())
		// throw new IOException();
		FileReader fin = new FileReader(file);
		BufferedReader in = new BufferedReader(fin);
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (!line.startsWith("#") || !line.isEmpty())
				tagList.putAll(parseTag((line)));
		}
		in.close();
	}

	/**
	 * parses a tag-sting of the form (gggg,eeee)=value into a map used for
	 * internal representation. <br>
	 * The tag has to be in the well known notation with surrounding braces,
	 * hexadecimal numbers and a comma seperating group and element number. <br>
	 * An 'x' may be used as a wildcard for any digit of the tag and is replaced
	 * by all possible numbers resulting an a map containing more than one
	 * entry. <br>
	 * The value or euqal sign+value may be omitted. If the equal sign is
	 * present and no value is given an explicit null value is assumed. If the
	 * equal sign and value are not present preservation of the original value
	 * is assumed.
	 * 
	 * @param tag
	 *            the tag representation as described above
	 * @return a map of DicomElements for the given tag-string
	 * @throws IOException
	 *             if the tag string is not valid
	 */
	private static Map<Integer, DicomElement> parseTag(String tag)
			throws IOException {
		Map<Integer, DicomElement> map = new HashMap<Integer, DicomElement>();

		// handling of x as wildcard number
		// tag = tag.replaceAll("x", "0"); // x-values are represented as 0 in
		// dcm4che
		int xPosition = tag.indexOf('x');
		if (xPosition != -1) {
			for (int i = 0; i < 16; i++) {
				String newTag = tag.substring(0, xPosition)
						+ Integer.toHexString(i) + tag.substring(xPosition + 1);
				Map<Integer, DicomElement> m = parseTag(newTag);
				map.putAll(m);
			}
			return map;
		}

		// transform key into dcm4chee int-value
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.put(parseUnsignedByte(tag.substring(0, 2)));
		buffer.put(parseUnsignedByte(tag.substring(2, 4)));
		buffer.put(parseUnsignedByte(tag.substring(5, 7)));
		buffer.put(parseUnsignedByte(tag.substring(7, 9)));
		// buffer.flip();
		int key = buffer.getInt(0);

		// TODO check if tag is known and supported (optional, because rules out
		// private tags)

		// process value of tag
		DicomElement dicomElement = null; // value is null by default
		if (tag.length() > 9) { // is a value provided?
			if (tag.charAt(9) != '=') // separator must be =
				throw new IOException(); // TODO
			// get VR of tag
			VRMap vrMap = VRMap.getVRMap();
			VR vr = vrMap.vrOf(key);
			// System.out.println(vr.toString()); //DEBUG
			// TODO if vr is UN (tag is not known/private) -> error

			// create DicomObject to parse value into a DicomElement
			DicomObject dicomObject = new BasicDicomObject();
			if (tag.length() == 10) { // if no more chars it is an explicit null
										// value
				// TODO if null and vr does not allow null -> error
				dicomElement = dicomObject.putNull(key, vr);
			} else { // a value is provided
				String value = tag.substring(10);
				dicomElement = dicomObject.putString(key, vr, value);
				// put into value object
			}
		}
		// Map<Integer, DicomElement> map = new HashMap<Integer,
		// DicomElement>();
		map.put(key, dicomElement);
		return map;
	}

	/**
	 * Parses string into an unsigned byte.
	 * 
	 * @param s
	 *            string representing an unsigned byte
	 * @return the byte value represented by the given string
	 */
	private static byte parseUnsignedByte(String s) {
		return (byte) Integer.parseInt(s, 16);
	}

	/**
	 * Checks if a specific DICOM-tag is contained in this whitelist.
	 * 
	 * @param tag
	 *            the tag to look up
	 * @return true if the tag was found in this whitelist, false otherwise
	 */
	public boolean hasTag(int tag) {
		return tagList.containsKey(tag);
	}

	/**
	 * Checks if the DICOM-tag is associated with a value.
	 * 
	 * @param tag
	 *            the tag to look up
	 * @return true if a value is associated with the given tag, false otherwise
	 * @throws IllegalArgumentException
	 *             if the tag is not part of this whitelist.
	 */
	public boolean hasValue(int tag) throws IllegalArgumentException {
		if (!hasTag(tag))
			throw new IllegalArgumentException();
		return tagList.get(tag) != null;
	}

	/**
	 * Looks up the DicomElement representing a certain tag in this whitelist.
	 * 
	 * @param tag
	 *            the tag to look up
	 * @return the DicomElement representing the Whitelist entry of the given
	 *         tag
	 * @throws IllegalArgumentException
	 *             if the tag is not part of this whitelist.
	 */
	public DicomElement getValue(int tag) {
		if (!hasTag(tag))
			throw new IllegalArgumentException();
		return tagList.get(tag);
	}
}
