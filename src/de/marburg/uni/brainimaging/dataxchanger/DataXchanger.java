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

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * This is the main class of the dataXchanger program. its main purpose is to do
 * some initialisaton work and the call the UI for further processing. it also
 * defines the essential global constants like program version.
 * 
 * @author Kornelius Podranski
 * 
 */
public class DataXchanger {

	public static final String NAME = "dataXchanger";
	public static final String VERSION = "20130731";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// do initialization
		Security.addProvider(new BouncyCastleProvider());
		// start UI
		CommandlineUI.start(args);
		CommandlineUI.exit(CommandlineUI.EXIT_SUCCESS);
	}

}
