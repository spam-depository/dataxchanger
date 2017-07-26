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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface defines a pluggable module, which manipulates a stream of data
 * 
 * @author Kornelius Podranski
 * 
 */
public interface StreamProcessor<T> {

	/**
	 * This method Processes a Stream of Data from an InputStream in and writes
	 * the result to an OutputStream out. <br>
	 * If the method does not manipulate the data itself (e.g. message digest
	 * calculation) the input should be copied to the output-stream. <br>
	 * user may call the method with null as out, if the are not interested in
	 * the processed data itself. Genarator-methods may accept in being null as
	 * well (defined in the implementing class). <br>
	 * Any secondary results may be reported via getResult() (e.g. the digest of
	 * a message digest). <br>
	 * The return value indicates if the method finished successfully (true) or
	 * not (false). If false is returned a following call to getException() must
	 * return a valid Exception.
	 * 
	 * 
	 * @param in
	 *            Stream data for processing is read from
	 * @param out
	 *            Stream processed data is written to
	 */
	public boolean process(InputStream in, OutputStream out);

	/**
	 * returns the Exception that occured during process()-call. If precess()
	 * returned false a valid Exception must be returnde by this method. In
	 * other cased this method may return null or throw an IllegalStateException
	 * (if process() was not called or did not return true).
	 * 
	 * @return the Exception that occured during process()
	 */
	public Exception getException();

	/**
	 * returns the result from process(), if any. May return null or throw a
	 * UnsupportedOperationException if process() does not produce any result
	 * apart from the OutpuStream. May return null or thrwo an
	 * IllegalStateException if called before process() is called or
	 * process()-call did finish.
	 * 
	 * @return the result of process() or null if there is no result to report.
	 */
	public T getResult();
}
