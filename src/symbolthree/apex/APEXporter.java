/******************************************************************************
 *
 * A P E X p o r t e r
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import oracle.jdbc.OracleDriver;

/**
 * APEX_RELEASE TABLE (core/flow_release.sql, flow_version.sql, package wwv_flow_imp)
 *
 * VERSION_NO     API_COMPATIBILITY    NOTE
 * 2.1.0.00.39                         (10g XE DB) select FLOWS_020100.WWV_FLOWS_RELEASE from dual;
 * 2.2.1.00.04                         FLOWS_020100.WWV_FLOWS_RELEASE / APEX 2.2.1
 * 3.0.1.00.08    2007.05.25
 * 3.1.2.00.02    2007.09.06
 * 3.2.1.00.12    2009.01.12
 * 4.0.2.00.09    2010.05.13           APEX_RELEASE  (11g & 11gR2 XE)
 * 4.1.1.00.23    2011.02.12
 * 4.2.6.00.03    2012.01.01           APEX_RELEASE
 * 5.0.3.00.03    2013.01.01           APEX_RELEASE
 * 5.0.4.00.12    2013.01.01           APEX_RELEASE
 * 5.1.0.00.45    2016.08.24           APEX_RELEASE
 * 5.1.4.00.08    2016.08.24           APEX_RELEASE
 * 18.1.0.00.45   2018.04.04           APEX_RELEASE
 * 18.2.0.00.12   2018.05.24           APEX_RELEASE
 * 19.1.0.00.15   2019.03.31           APEX_RELEASE
 * 19.2.0.00.18	  2019.10.04           APEX_RELEASE
 * 20.1.0.00.13	  2020.03.31           APEX_RELEASE
 * 20.2.0.00.20	  2020.10.01           APEX_RELEASE
 * 21.1.0         2021.04.15           APEX_RELEASE
 * 21.2.0         2021.10.15           APEX_RELEASE
 * 22.1.0         2022.04.12           APEX_RELEASE
 * 22.2.0         2022.10.07           APEX_RELEASE
 * 23.1.0         2023.04.28           APEX_RELEASE
 * 23.2.0         2023.10.31           APEX_RELEASE
 * 24.1.6         2024.05.31           APEX_RELEASE
 * 24.2.0         2024.11.30           APEX_RELEASE
 *
 *
 */
public class APEXporter implements Constants {
  private static File inputFile;
  private String      version    = "";
  private boolean     exitPrompt = true;

  public APEXporter() {}

  public static void main(String[] args) {
	showBanner();
    APEXporter exporter = new APEXporter();

    if (args.length == 1) {
      inputFile = new File(args[0]);
    } else {
      inputFile = new File(System.getProperty("user.dir"), CONFG_FILE_NAME);
    }

    if (!inputFile.exists()) {
      System.out.println("Unable to find " + inputFile.getAbsolutePath());
      promptEnterKey();
    }

    exporter.run();
  }

  private static void promptEnterKey() {
    System.out.println("Press  \"E N T E R\"  to exit");

    Scanner scanner = new Scanner(System.in);

    scanner.nextLine();
    System.exit(0);
  }

  private void run() {
    try {
      FileInputStream fis  = new FileInputStream(inputFile);
      Properties      prop = new Properties();

      prop.load(fis);
      System.setProperty("apex.debug", prop.getProperty("00.debug"));
      System.setProperty("apex.logtofile", prop.getProperty("00.logtofile"));
      exitPrompt = Boolean.parseBoolean(prop.getProperty("00.exitprompt"));
      Logger.pre("Version " + getVersion());
      Logger.pre("=================================================");
      DriverManager.registerDriver(new OracleDriver());

      Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@" + prop.getProperty("00.db"),
                                                    prop.getProperty("00.user"),
                                                    prop.getProperty("00.password"));
      String    schemaName = null;
      ResultSet rs         = null;

      Logger.log(1, "Connect to " + prop.getProperty("00.db"));
      rs = conn.createStatement()
               .executeQuery("select table_owner from all_synonyms where synonym_name='APEX_RELEASE'");

      while (rs.next()) {
        schemaName = rs.getString(1);
      }

      if (schemaName == null) {
        schemaName = "FLOWS_020100";
      }

      rs = conn.createStatement().executeQuery("select version_no from " + schemaName + "." + "APEX_RELEASE");

      while (rs.next()) {
        version = rs.getString(1);
      }

      rs.close();
      conn.close();
      Logger.log(1, "APEX version " + version);

      String apexClass = "symbolthree.apex.APEXExport" + versionLookup(version);

      Logger.log(1, "APEXExport Class = " + apexClass);

      ArrayList<String>   args      = new ArrayList<>();

      Enumeration<Object> keys      = prop.keys();

      while (keys.hasMoreElements()) {
        String fullKey = (String) keys.nextElement();
        String val = prop.getProperty(fullKey);

        if (fullKey.equals("00.debug") || fullKey.equals("00.logtofile") || fullKey.equals("00.exitprompt")) {
          continue;
        }
        String keyVersion = fullKey.split("\\.")[0];

        //if (Integer.parseInt(key.substring(0, 2)) <= Integer.parseInt(versionLookup(version))) {
        if (Integer.parseInt(keyVersion) <= Integer.parseInt(versionLookup(version))) {
          if ((val != null) &&!val.trim().equals("")) {
            val = val.trim();
            String key = fullKey.split("\\.")[1];

            // only parameter with true value will be added to final argument array
            if (val.equalsIgnoreCase("TRUE")) {
                args.add("-" + key);

            } else if (val.equalsIgnoreCase("FALSE")) {
              // do nothing
            } else {
                args.add("-" + key);
                args.add(val);
            }
          }
        }
      }

      String[]       argsArray  = args.toArray(new String[args.size()]);
      Object         clazz      = Class.forName(apexClass).newInstance();
      APEXExportImpl apexexport = (APEXExportImpl) clazz;

      // mask password
      int pwdPos = 0;
      String pwd = null;
      for (int i=0;i<argsArray.length;i++) {
    	  if (argsArray[i].equals("-password")) {
    		  pwdPos = i+1;
    		  pwd = argsArray[i+1];
    		  argsArray[i+1] = String.join("", Collections.nCopies(argsArray[i+1].length(), "*"));
    		  break;
    	  }
      }

      Logger.log(1, Arrays.toString(argsArray));
      argsArray[pwdPos] = pwd;
      apexexport.main(argsArray);

      List<String> files = apexexport.getOutputFiles();

      for (String file : files) {
        Logger.log(1, "Output File: " + file);
      }

      Logger.log(1, "********* Finished **********");
    } catch (ClassNotFoundException cnfe) {
      Logger.log(1, "APEX version " + version + " is not supported");
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter  pw = new PrintWriter(sw);

      e.printStackTrace(pw);
      Logger.log(1, e.toString());
      Logger.log(2, sw.toString());
    }

    if (exitPrompt) {
      promptEnterKey();
    }
  }

  private String versionLookup(String _version) {
    String majorVer = _version.substring(0, 4);

    majorVer = majorVer.replaceAll("\\.", "");

    return majorVer;
  }

  private static void showBanner() {
    try {
	    InputStream is = APEXporter.class.getClass().getResourceAsStream("/symbolthree/apex/banner.txt");
	    InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
	    BufferedReader reader = new BufferedReader(streamReader);
	    String line;
		  while ((line = reader.readLine()) != null) {
			  System.out.println(line);
		  }
	  } catch (IOException e) {
       System.out.println("========================");
       System.out.println("A P E X  E X P O R T E R");
       System.out.println("========================");
    }
  }

  private String getVersion() {
    try {
      Properties buildVer = new Properties();

      buildVer.load(this.getClass().getResourceAsStream("/build.properties"));

      String str =  buildVer.getProperty("build.version") + " build " + buildVer.getProperty("build.number");
      str = str + " (" + buildVer.getProperty("build.time") + ")";
      return str;
    } catch (Exception e) {
      return "";
    }
  }
}
