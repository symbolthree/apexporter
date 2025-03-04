/******************************************************************************
 *
 * A P E X E x p o r t e r
 *
 * Copyright (C) 2025 Christopher Ho / www.symbolthree.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * E-mail: christopher.ho@symbolthree.com
 *
******************************************************************************/

package symbolthree.apex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger implements Constants {

  public static void pre(String str) {
      System.out.println(str);
      boolean logtofile = Boolean.parseBoolean(System.getProperty("apex.logtofile", "false"));
      if (logtofile) writeLog(str);
  }

  public static void log(int debug, Object message) {
	    int debugLevel = Integer.parseInt(System.getProperty("apex.debug", "1"));
	    boolean logtofile = Boolean.parseBoolean(System.getProperty("apex.logtofile", "false"));
	    File logFile = new File(System.getProperty("user.dir"), LOG_FILE_NAME);

	    if (debug <= debugLevel) {
	      String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	      String msg = timeStamp + " | " + message;
	      System.out.println(msg);

	      if (logtofile) writeLog(msg);
	    }
  }

  private static void writeLog(String msg) {
	    File logFile = new File(System.getProperty("user.dir"), LOG_FILE_NAME);
		try {
			  BufferedWriter bfw = new BufferedWriter(new FileWriter(logFile, true));
			  bfw.append(msg + System.lineSeparator());
			  bfw.flush();
			} catch (Exception e) {}
  }
}
