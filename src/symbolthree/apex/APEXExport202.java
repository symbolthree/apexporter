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
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleDriver;
import oracle.sql.CLOB;

public class APEXExport202 extends APEXExportImpl  {
  Connection gConn;

  private final String gStmt = "select application_id, application_name from apex_applications where workspace_id = ? and build_status <> 'Run and Hidden' union select application_id, application_name from apex_ws_applications where workspace_id = ? order by application_id";

  private final String gStmt2 = "select application_id, application_name from apex_applications where workspace_id = ? union select application_id, application_name from apex_ws_applications where workspace_id = ? order by application_id";

  private final String gStmtInstance = "select application_id, application_name from apex_applications where 1=1 and build_status <> 'Run and Hidden' union select application_id, application_name from apex_ws_applications where 1=1 order by application_id";

  private final String gStmtWorkspaces = "select workspace_id, workspace from apex_workspaces where 1=1 order by workspace_id";

  private final String gStmtWorkspacesFeedback = "select distinct workspace_id, workspace_name from apex_team_feedback where 1=1 order by workspace_id";

  private final String gStmtWorkspace = "select workspace from apex_workspaces where workspace_id = ?";

  private final String gStmtSetSGID = "begin wwv_flow_api.set_security_group_id(p_security_group_id=>?); end;";

  private final String gStmtGetSGID = "select v('FLOW_SECURITY_GROUP_ID') from sys.dual";

  private final String gStmtIsWS = "select count(*) from apex_ws_applications where application_id = ?";

  private final String gStmtListAppChanges = "select to_char(last_updated_on,'yyyy-mm-dd hh24:mi') last_updated_on,\n       to_char(application_id),\n       application_name\n  from apex_applications\n where workspace_id = ?\n   and (? is null or last_updated_on >= to_date(?,'yyyy-mm-dd'))\n   and (? is null or upper(last_updated_by) = upper(?))\n order by 1,2";

  private final String gStmtListCompChanges = "select to_char(last_updated_on,'yyyy-mm-dd hh24:mi') last_udpated_on,\n       type_name||':'||id,\n       name\n  from apex_appl_export_comps\n where application_id = ?\n   and (? is null or last_updated_on >= to_date(?,'yyyy-mm-dd'))\n   and (? is null or upper(last_updated_by) = upper(?))\n order by 1, 2";

  public boolean debug = false;

  public boolean skipDate = false;

  public boolean instance = false;

  public boolean pubReports = false;

  public boolean savedReports = false;

  public boolean IRNotifications = false;

  public boolean expWorkspace = false;

  public boolean expMinimal = false;

  public boolean expTeamdevdata = false;

  public boolean expFeedback = false;

  public boolean expTranslations = false;

  public boolean expFiles = false;

  public boolean expOriginalIds = false;

  public boolean expLocked = false;

  public boolean split = false;

  public boolean expNoSubscriptions = false;

  public boolean expComments = false;

  public String expSupportingObjects = "";

  public boolean expACLAssignments = false;

  public boolean checksum = true;

  public boolean list = false;

  public String dir = "";

  public String expType = "APPLICATION_SOURCE";

  private ArrayList<String> outputFiles = new ArrayList<String>();
  
  public void dbg(String paramString) {
    if (this.debug)
      System.out.println(paramString);
  }

  private OracleCallableStatement get_export_stmt(String paramString) throws SQLException {
    dbg("  " + paramString.replace("\n", "\n  "));
    OracleCallableStatement oracleCallableStatement = (OracleCallableStatement)this.gConn.prepareCall(paramString);
    oracleCallableStatement.registerOutParameter(1, 2003, "PUBLIC.APEX_T_EXPORT_FILES");
    return oracleCallableStatement;
  }

  private void bind_yn(OracleCallableStatement paramOracleCallableStatement, int paramInt, boolean paramBoolean) throws SQLException {
    paramOracleCallableStatement.setString(paramInt, paramBoolean ? "Y" : "N");
  }

  private boolean target_needs_update(File paramFile1, File paramFile2) throws IOException {
    if (!this.checksum ||
      !paramFile2.exists() || paramFile1
      .length() != paramFile2.length())
      return true;
    BufferedReader bufferedReader1 = new BufferedReader(new FileReader(paramFile1), 32767);
    BufferedReader bufferedReader2 = new BufferedReader(new FileReader(paramFile2), 32767);
    boolean bool1 = true;
    boolean bool2 = true;
    try {
      while (bool1 && bool2) {
        String str1 = bufferedReader1.readLine();
        String str2 = bufferedReader2.readLine();
        bool2 = (str1 != null) ? true : false;
        bool1 = ((str1 == null && str2 == null) || (str1 != null && str2 != null && str1.equals(str2))) ? true : false;
      }
    } finally {
      close_stream_ignore_errors(bufferedReader1);
      close_stream_ignore_errors(bufferedReader2);
    }
    return !bool1;
  }

  private void close_stream_ignore_errors(Closeable paramCloseable) {
    try {
      if (paramCloseable != null)
        paramCloseable.close();
    } catch (IOException iOException) {}
  }

  private void copy_file(File paramFile1, File paramFile2) throws IOException {
    FileInputStream fileInputStream = new FileInputStream(paramFile1);
    FileOutputStream fileOutputStream = new FileOutputStream(paramFile2);
    try {
      byte[] arrayOfByte = new byte[4096];
      int i;
      while ((i = fileInputStream.read(arrayOfByte)) > 0)
        fileOutputStream.write(arrayOfByte, 0, i);
    } finally {
      close_stream_ignore_errors(fileInputStream);
      close_stream_ignore_errors(fileOutputStream);
    }
  }

  private void exec_and_write_files(OracleCallableStatement paramOracleCallableStatement) throws SQLException, IOException {
    dbg("  Start " + new Date());
    paramOracleCallableStatement.execute();
    Object[] arrayOfObject = (Object[])((Array)paramOracleCallableStatement.getObject(1)).getArray();
    for (Object object : arrayOfObject) {
      Struct struct = (Struct)object;
      String str = (String)struct.getAttributes()[0];
      CLOB cLOB = (CLOB)struct.getAttributes()[1];
      if (this.dir != "")
        str = this.dir.replaceAll("/$", "") + "/" + str;
      dbg("  " + str + ": Copying from clob to temp file.");
      File file1 = File.createTempFile("apex", ".sql");
      BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file1), "UTF-8"));
      BufferedReader bufferedReader = new BufferedReader(cLOB.getCharacterStream());
      try {
        String str1;
        while ((str1 = bufferedReader.readLine()) != null) {
          str1 = str1 + "\n";
          bufferedWriter.write(str1, 0, str1.length());
        }
      } finally {
        close_stream_ignore_errors(bufferedReader);
        close_stream_ignore_errors(bufferedWriter);
      }
      dbg("  Temp file written, now checking for changes.");
      File file2 = new File(str);
      if (target_needs_update(file1, file2)) {
        file2.mkdirs();
        file2.delete();
        dbg("  Copying temp file " + file1.getAbsolutePath());
        copy_file(file1, file2);
        file1.delete();
        System.out.println(this.debug ? ("  " + file2.getAbsolutePath() + ": " + file2.length() + " bytes") : ("  " + str));
        outputFiles.add(file2.getAbsolutePath());            
      } else {
        file1.delete();
        dbg("  not changed: " + file2.getAbsolutePath());
      }
    }
    paramOracleCallableStatement.close();
    dbg("  Completed at " + new Date());
  }

  private void ExportFiles(BigDecimal paramBigDecimal1, BigDecimal paramBigDecimal2, String paramString1, String paramString2) throws SQLException, IOException {
    if (this.instance) {
      String str = null;
      PreparedStatement preparedStatement = this.gConn.prepareStatement("select application_id, application_name from apex_applications where 1=1 and build_status <> 'Run and Hidden' union select application_id, application_name from apex_ws_applications where 1=1 order by application_id");
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        BigDecimal bigDecimal = resultSet.getBigDecimal(1);
        str = resultSet.getString(2);
        System.out.println("Exporting Application " + bigDecimal + ":'" + str + "'");
        ExportFile(bigDecimal, true, paramString2);
      }
      resultSet.close();
      preparedStatement.close();
    } else if (paramBigDecimal1 != null && paramBigDecimal1.longValue() != 0L) {
      System.out.println("Exporting Application " + paramBigDecimal1);
      ExportFile(paramBigDecimal1, false, paramString2);
    } else {
      BigDecimal bigDecimal = new BigDecimal(0);
      String str = null;
      PreparedStatement preparedStatement = this.gConn.prepareStatement("begin wwv_flow_api.set_security_group_id(p_security_group_id=>?); end;");
      preparedStatement.setBigDecimal(1, paramBigDecimal2);
      preparedStatement.executeUpdate();
      preparedStatement.close();
      preparedStatement = this.gConn.prepareStatement("select v('FLOW_SECURITY_GROUP_ID') from sys.dual");
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next())
        bigDecimal = resultSet.getBigDecimal(1);
      resultSet.close();
      preparedStatement.close();
      if (!bigDecimal.equals(paramBigDecimal2)) {
        System.out.println("Invalid Workspace ID '" + paramBigDecimal2 + "' for User '" + paramString1 + "'");
        System.exit(1);
      }
      if (!this.expLocked) {
        preparedStatement = this.gConn.prepareStatement("select application_id, application_name from apex_applications where workspace_id = ? and build_status <> 'Run and Hidden' union select application_id, application_name from apex_ws_applications where workspace_id = ? order by application_id");
      } else {
        preparedStatement = this.gConn.prepareStatement("select application_id, application_name from apex_applications where workspace_id = ? union select application_id, application_name from apex_ws_applications where workspace_id = ? order by application_id");
      }
      preparedStatement.setBigDecimal(1, paramBigDecimal2);
      resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        BigDecimal bigDecimal1 = resultSet.getBigDecimal(1);
        str = resultSet.getString(2);
        System.out.println("Exporting Application " + bigDecimal1 + ":'" + str + "'");
        ExportFile(bigDecimal1, true, paramString2);
      }
      resultSet.close();
      preparedStatement.close();
    }
  }

  private void ExportWorkspaces(BigDecimal paramBigDecimal, boolean paramBoolean1, boolean paramBoolean2) throws SQLException, IOException {
    if (paramBigDecimal != null && paramBigDecimal.longValue() != 0L) {
      String str = null;
      PreparedStatement preparedStatement = this.gConn.prepareStatement("select workspace from apex_workspaces where workspace_id = ?");
      preparedStatement.setBigDecimal(1, paramBigDecimal);
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        str = resultSet.getString(1);
        System.out.println("Exporting Workspace " + paramBigDecimal + ":'" + str + "'");
        ExportWorkspace(paramBigDecimal, paramBoolean1, paramBoolean2);
      }
      resultSet.close();
      preparedStatement.close();
    } else {
      String str = null;
      PreparedStatement preparedStatement = this.gConn.prepareStatement("select workspace_id, workspace from apex_workspaces where 1=1 order by workspace_id");
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        BigDecimal bigDecimal = resultSet.getBigDecimal(1);
        str = resultSet.getString(2);
        System.out.println("Exporting Workspace " + bigDecimal + ":'" + str + "'");
        ExportWorkspace(bigDecimal, paramBoolean1, paramBoolean2);
      }
      resultSet.close();
      preparedStatement.close();
    }
  }

  private void ExpFeed(BigDecimal paramBigDecimal, String paramString, java.sql.Date paramDate) throws SQLException, IOException {
    if (paramBigDecimal != null && paramBigDecimal.longValue() != 0L) {
      String str = null;
      PreparedStatement preparedStatement = this.gConn.prepareStatement("select workspace from apex_workspaces where workspace_id = ?");
      preparedStatement.setBigDecimal(1, paramBigDecimal);
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        str = resultSet.getString(1);
        System.out.println("Exporting Feedback for Workspace " + paramBigDecimal + ":'" + str + "'");
        ExportFeedback(paramBigDecimal, paramString, paramDate);
      }
      resultSet.close();
      preparedStatement.close();
    } else {
      String str = null;
      PreparedStatement preparedStatement = this.gConn.prepareStatement("select distinct workspace_id, workspace_name from apex_team_feedback where 1=1 order by workspace_id");
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        BigDecimal bigDecimal = resultSet.getBigDecimal(1);
        str = resultSet.getString(2);
        System.out.println("Exporting Feedback for Workspace " + bigDecimal + ":'" + str + "'");
        ExportFeedback(bigDecimal, paramString, paramDate);
      }
      resultSet.close();
      preparedStatement.close();
    }
  }

  public void ExportFile(BigDecimal paramBigDecimal, boolean paramBoolean, String paramString) throws SQLException, IOException {
    OracleCallableStatement oracleCallableStatement = get_export_stmt("begin\n    ? := apex_export.get_application (\n             p_application_id          => ?,\n             p_type                    => ?,\n             p_split                   => ?='Y',\n             p_with_date               => ?='Y',\n             p_with_ir_public_reports  => ?='Y',\n             p_with_ir_private_reports => ?='Y',\n             p_with_ir_notifications   => ?='Y',\n             p_with_translations       => ?='Y',\n             p_with_pkg_app_mapping    => ?='Y',\n             p_with_original_ids       => ?='Y',\n             p_with_no_subscriptions   => ?='Y',\n             p_with_comments           => ?='Y',\n             p_with_supporting_objects => ?,\n             p_with_acl_assignments    => ?='Y',\n             p_components              => apex_string.split(?,'#') );\nend;");
    oracleCallableStatement.setBigDecimal(2, paramBigDecimal);
    oracleCallableStatement.setString(3, this.expType);
    bind_yn(oracleCallableStatement, 4, this.split);
    bind_yn(oracleCallableStatement, 5, !this.skipDate);
    bind_yn(oracleCallableStatement, 6, this.pubReports);
    bind_yn(oracleCallableStatement, 7, this.savedReports);
    bind_yn(oracleCallableStatement, 8, this.IRNotifications);
    bind_yn(oracleCallableStatement, 9, this.expTranslations);
    bind_yn(oracleCallableStatement, 10, paramBoolean);
    bind_yn(oracleCallableStatement, 11, this.expOriginalIds);
    bind_yn(oracleCallableStatement, 12, this.expNoSubscriptions);
    bind_yn(oracleCallableStatement, 13, this.expComments);
    oracleCallableStatement.setString(14, this.expSupportingObjects);
    bind_yn(oracleCallableStatement, 15, this.expACLAssignments);
    oracleCallableStatement.setString(16, paramString);
    exec_and_write_files(oracleCallableStatement);
  }

  public void ExportWorkspace(BigDecimal paramBigDecimal, boolean paramBoolean1, boolean paramBoolean2) throws SQLException, IOException {
    OracleCallableStatement oracleCallableStatement = get_export_stmt("begin\n    ? := apex_export.get_workspace (\n             p_workspace_id          => ?,\n             p_with_team_development => ?='Y',\n             p_with_misc             => ?='Y',\n             p_with_date             => ?='Y' );\nend;");
    oracleCallableStatement.setBigDecimal(2, paramBigDecimal);
    bind_yn(oracleCallableStatement, 3, paramBoolean1);
    bind_yn(oracleCallableStatement, 4, !paramBoolean2);
    bind_yn(oracleCallableStatement, 5, !this.skipDate);
    exec_and_write_files(oracleCallableStatement);
  }

  public void ExportStaticFiles(BigDecimal paramBigDecimal) throws SQLException, IOException {
    OracleCallableStatement oracleCallableStatement = get_export_stmt("begin\n    ? := apex_export.get_workspace_files (\n             p_workspace_id          => ?,\n             p_with_date             => ?='Y' );\nend;");
    oracleCallableStatement.setBigDecimal(2, paramBigDecimal);
    bind_yn(oracleCallableStatement, 3, !this.skipDate);
    exec_and_write_files(oracleCallableStatement);
  }

  public void ExportFeedback(BigDecimal paramBigDecimal, String paramString, java.sql.Date paramDate) throws SQLException, IOException {
    OracleCallableStatement oracleCallableStatement = get_export_stmt("begin\n    ? := apex_export.get_feedback (\n             p_workspace_id      => ?,\n             p_with_date         => ?='Y',\n             p_since             => ?,\n             p_deployment_system => ? );\nend;");
    oracleCallableStatement.setBigDecimal(2, paramBigDecimal);
    bind_yn(oracleCallableStatement, 3, !this.skipDate);
    oracleCallableStatement.setDate(4, paramDate);
    oracleCallableStatement.setString(5, paramString);
    exec_and_write_files(oracleCallableStatement);
  }

  public void List(BigDecimal paramBigDecimal1, BigDecimal paramBigDecimal2, String paramString1, String paramString2) throws SQLException, IOException {
    PreparedStatement preparedStatement;
    if (paramBigDecimal2 != null) {
      preparedStatement = this.gConn.prepareStatement("select to_char(last_updated_on,'yyyy-mm-dd hh24:mi') last_udpated_on,\n       type_name||':'||id,\n       name\n  from apex_appl_export_comps\n where application_id = ?\n   and (? is null or last_updated_on >= to_date(?,'yyyy-mm-dd'))\n   and (? is null or upper(last_updated_by) = upper(?))\n order by 1, 2");
      preparedStatement.setBigDecimal(1, paramBigDecimal2);
    } else {
      preparedStatement = this.gConn.prepareStatement("select to_char(last_updated_on,'yyyy-mm-dd hh24:mi') last_updated_on,\n       to_char(application_id),\n       application_name\n  from apex_applications\n where workspace_id = ?\n   and (? is null or last_updated_on >= to_date(?,'yyyy-mm-dd'))\n   and (? is null or upper(last_updated_by) = upper(?))\n order by 1,2");
      preparedStatement.setBigDecimal(1, paramBigDecimal1);
    }
    preparedStatement.setString(2, paramString1);
    preparedStatement.setString(3, paramString1);
    preparedStatement.setString(4, paramString2);
    preparedStatement.setString(5, paramString2);
    ResultSet resultSet = preparedStatement.executeQuery();
    System.out.println("Date             ID                                      Name");
    System.out.println("---------------- --------------------------------------- -----------------------");
    while (resultSet.next()) {
      String str1 = resultSet.getString(1);
      String str2 = resultSet.getString(2);
      String str3 = resultSet.getString(3);
      System.out.printf("%s %-39s %s\n", new Object[] { str1, str2, str3 });
    }
    preparedStatement.close();
  }

  private static void usage() {
    System.out.println("Usage: java oracle.apex.APEXExport [-options]");
    System.out.println("Available options:");
    System.out.println("    -h:                    Print this help");
    System.out.println("    -db:                   Database connection string in JDBC URL format");
    System.out.println("    -user:                 Database username");
    System.out.println("    -password:             Database password");
    System.out.println("    -applicationid:        ID for application to be exported");
    System.out.println("    -workspaceid:          Workspace ID for which all applications to be exported or the workspace to be exported");
    System.out.println("    -instance:             Export all applications");
    System.out.println("    -expWorkspace:         Export all workspaces or a single workspace identified by -workspaceid");
    System.out.println("    -expMinimal:           Only export workspace definition, users, and groups");
    System.out.println("    -expFiles:             Export all workspace files identified by -workspaceid");
    System.out.println("    -skipExportDate:       Exclude export date from application export files");
    System.out.println("    -expPubReports:        Export all user saved public interactive reports");
    System.out.println("    -expSavedReports:      Export all user saved interactive reports");
    System.out.println("    -expIRNotif:           Export all interactive report notifications");
    System.out.println("    -expTranslations:      Export the translation mappings and all text from the translation repository");
    System.out.println("    -expFeedback:          Export team development feedback for all workspaces or a single workspace identified by -workspaceid");
    System.out.println("    -expTeamdevdata:       Export team development data for all workspaces or a single workspace identified by -workspaceid");
    System.out.println("    -deploymentSystem:     Deployment system for exported feedback");
    System.out.println("    -expFeedbackSince:     Export team development feedback since date in the format YYYYMMDD");
    System.out.println("    -expOriginalIds:       If specified, the application export will emit IDs as they were when the application was imported");
    System.out.println("    -expNoSubscriptions:   Do not export references to subscribed components");
    System.out.println("    -expComments:          Export developer comments");
    System.out.println("    -expSupportingObjects: Pass (Y)es, (N)o or (I)nstall to override the default");
    System.out.println("    -expACLAssignments:    Export ACL User Role Assignments");
    System.out.println("    -expType:              Type of export: APPLICATION_SOURCE (default) or EMBEDDED_CODE");
    System.out.println("    -dir:                  Save all files in the specified directory instead of the current directory");
    System.out.println("    -list:                 List all changed applications in the workspace or components in the application");
    System.out.println("    -changesSince:         Expects date parameter (yyyy-mm-dd). Limit -list values to changes since the given date");
    System.out.println("    -changesBy:            Expects string parameter. Limit -list values to changes by the given user");
    System.out.println("    -expComponents:        Export application components. All remaining parameters must be of form TYPE:ID");
    System.out.println("    -debug:                Print debug output");
    System.out.println("    -nochecksum:           Overwrite existing files even if the contents have not changed");
    System.out.println("    -split:                Split applications into multiple files");
    System.out.println("    ");
    System.out.println("    Application Example:");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user scott -password scotts_password -applicationid 31500");
    System.out.println("       APEXExport -db //localhost:1521/ORCL.example.com -user scott -password scotts_password -applicationid 31500");
    System.out.println("       APEXExport -db tns_entry -user scott -password scotts_password -applicationid 31500");
    System.out.println("    Workspace Example:");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user scott -password scotts_password -workspaceid 9999");
    System.out.println("    Instance Example:");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user system -password systems_password -instance");
    System.out.println("    Components Example:");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user scott -password scotts_password -applicationid 31500 -expComponents PAGE:1 PAGE:2 AUTHORIZATION:12345678");
    System.out.println("    List Example:");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user scott -password scotts_password -workspaceid 9999 -list -changesSince 2019-07-29");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user scott -password scotts_password -applicationid 31500 -list -changesBy EXAMPLE_USER");
    System.out.println("    Export All Workspaces Example:");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user system -password systems_password -expWorkspace");
    System.out.println("    Export Feedback to development environment:");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user scott -password scotts_password -workspaceid 9999 -expFeedback");
    System.out.println("    Export Feedback to deployment environment EA2 since 20100308:");
    System.out.println("       APEXExport -db localhost:1521:ORCL -user scott -password scotts_password -workspaceid 9999 -expFeedback -deploymentSystem EA2 -expFeedbackSince 20100308");
    System.exit(1);
  }

  @Override
public void main(String[] paramArrayOfString) throws Exception {
    String str1 = null;
    String str2 = null;
    String str3 = null;
    String str4 = null;
    BigDecimal bigDecimal1 = null;
    BigDecimal bigDecimal2 = null;
    Date date = null;
    java.sql.Date date1 = null;
    String str5 = "";
    String str6 = "";
    String str7 = "";
    APEXExport202 apexexport = new APEXExport202();
    for (byte b = 0; b < paramArrayOfString.length; b++) {
      apexexport.dbg("Parameter:" + paramArrayOfString[b]);
      if (paramArrayOfString[b].equalsIgnoreCase("-h")) {
        usage();
        return;
      }
      if (paramArrayOfString[b].equalsIgnoreCase("-db")) {
        str1 = paramArrayOfString[++b];
      } else if (paramArrayOfString[b].equalsIgnoreCase("-user")) {
        str2 = paramArrayOfString[++b];
      } else if (paramArrayOfString[b].equalsIgnoreCase("-password")) {
        str3 = paramArrayOfString[++b];
      } else if (paramArrayOfString[b].equalsIgnoreCase("-workspaceid")) {
        bigDecimal2 = new BigDecimal(paramArrayOfString[++b]);
      } else if (paramArrayOfString[b].equalsIgnoreCase("-applicationid")) {
        bigDecimal1 = new BigDecimal(paramArrayOfString[++b]);
      } else if (paramArrayOfString[b].equalsIgnoreCase("-debug")) {
        apexexport.debug = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-skipExportDate")) {
        apexexport.skipDate = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expPubReports")) {
        apexexport.pubReports = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expSavedReports")) {
        apexexport.savedReports = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expIRNotif")) {
        apexexport.IRNotifications = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expTranslations")) {
        apexexport.expTranslations = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-instance")) {
        apexexport.instance = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expWorkspace")) {
        apexexport.expWorkspace = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expMinimal")) {
        apexexport.expMinimal = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expFiles")) {
        apexexport.expFiles = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expFeedback")) {
        apexexport.expFeedback = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expTeamdevdata")) {
        apexexport.expTeamdevdata = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-deploymentSystem")) {
        str4 = paramArrayOfString[++b];
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expFeedbackSince")) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        try {
          date = simpleDateFormat.parse(paramArrayOfString[++b]);
          date1 = new java.sql.Date(date.getTime());
        } catch (ParseException parseException) {
          System.out.println("Invalid date format: " + paramArrayOfString[++b]);
        }
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expOriginalIds")) {
        apexexport.expOriginalIds = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-split")) {
        apexexport.split = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expNoSubscriptions")) {
        apexexport.expNoSubscriptions = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expComments")) {
        apexexport.expComments = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expSupportingObjects")) {
        apexexport.expSupportingObjects = paramArrayOfString[++b];
        if (!Arrays.<String>asList(new String[] { "Y", "I", "N" }).contains(apexexport.expSupportingObjects)) {
          usage();
          return;
        }
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expACLAssignments")) {
        apexexport.expACLAssignments = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expLocked")) {
        apexexport.expLocked = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-nochecksum")) {
        apexexport.checksum = false;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-dir")) {
        if (b == paramArrayOfString.length - 1) {
          usage();
          return;
        }
        apexexport.dir = paramArrayOfString[++b];
      } else if (paramArrayOfString[b].equalsIgnoreCase("-list")) {
        apexexport.list = true;
      } else if (paramArrayOfString[b].equalsIgnoreCase("-changesSince")) {
        if (b == paramArrayOfString.length - 1) {
          usage();
          return;
        }
        str5 = paramArrayOfString[++b];
      } else if (paramArrayOfString[b].equalsIgnoreCase("-changesBy")) {
        if (b == paramArrayOfString.length - 1) {
          usage();
          return;
        }
        str6 = paramArrayOfString[++b];
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expComponents")) {
        while (++b < paramArrayOfString.length)
          str7 = str7 + "#" + paramArrayOfString[b];
      } else if (paramArrayOfString[b].equalsIgnoreCase("-expType")) {
        apexexport.expType = paramArrayOfString[++b];
      } else {
        System.out.println("ERROR: Unknown parameter: " + paramArrayOfString[b] + "\n");
        usage();
        return;
      }
    }
    if (apexexport.debug) {
      System.out.println(str1);
      System.out.println(str2);
      System.out.println(bigDecimal2);
      System.out.println(bigDecimal1);
      System.out.println(apexexport.expType);
      System.out.println(apexexport.skipDate);
      System.out.println(apexexport.pubReports);
      System.out.println(apexexport.savedReports);
      System.out.println(apexexport.IRNotifications);
      System.out.println(apexexport.expTranslations);
      System.out.println(apexexport.instance);
      System.out.println(apexexport.expWorkspace);
      System.out.println(apexexport.expMinimal);
      System.out.println(apexexport.expFiles);
      System.out.println(apexexport.expFeedback);
      System.out.println(apexexport.expTeamdevdata);
      System.out.println(str4);
      System.out.println(date);
      System.out.println(apexexport.dir);
      System.out.println(apexexport.list);
      System.out.println(str5);
      System.out.println(str6);
      System.out.println(str7);
    }
    if (str1 == null || str2 == null || str3 == null || (bigDecimal1 != null && bigDecimal1
      .longValue() == 0L && bigDecimal2 != null && bigDecimal2
      .longValue() == 0L)) {
      System.out.println("ERROR: Either -applicationid or -workspaceid must be set\n");
      usage();
    }
    DriverManager.registerDriver(new OracleDriver());
    apexexport.gConn = DriverManager.getConnection("jdbc:oracle:thin:@" + str1, str2, str3);
    apexexport.gConn.setAutoCommit(true);
    if (apexexport.expWorkspace) {
      apexexport.ExportWorkspaces(bigDecimal2, apexexport.expTeamdevdata, apexexport.expMinimal);
    } else if (apexexport.expFeedback) {
      apexexport.ExpFeed(bigDecimal2, str4, date1);
    } else if (apexexport.expFiles) {
      apexexport.ExportStaticFiles(bigDecimal2);
    } else if (apexexport.list) {
      apexexport.List(bigDecimal2, bigDecimal1, str5, str6);
    } else {
      apexexport.ExportFiles(bigDecimal1, bigDecimal2, str2, str7);
    }
    apexexport.gConn.close();
  }

@Override
public java.util.List<String> getOutputFiles() {
	return outputFiles;
}
}
