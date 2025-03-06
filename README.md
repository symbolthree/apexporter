# APEXporter

## License

> APExporter (c) by [christopher.ho@SymbolThree.com](mailto:christopher.ho@SymbolThree.com) 2025

> APEXporter is licensed under GNU Public License (GPLv3). You should have received a copy of the license along with this work. If not, see [here](https://www.gnu.org/licenses/gpl-3.0.en.html).

## Background

In each Oracle APEX release, the Export program (APEXExport.class) works only for that specific version, and a specific JRE version is needed to run this Java class.  Using a series of argument also makes the program very difficult to use.  I write this small utility to do the export simple and easy.

Former releases can be found in [SourceForge](https://sourceforge.net/projects/apexexporter/)

## Features

- Works for Oracle APEX from version 19.1 to 24.2
- Run under JRE 8 or higher; packaged as Window executable or self-contained Jar file
- Allows to export any workspace and applications (including the APEX internal apps)
- Single executable file or Jar file, one config file.

## How to Use

- Unzip file `APEXExporter_3.0.xx.zip` to any directory.
- For Windows user, double click `APEXporter.exe` (`java.exe` must be in the PATH)
- For *nix user, use the command `java –jar APEXExporter.jar`
- The default config file is `APEXporter.conf`, located under the same directory as the executable file. 
- To use other config file, specify it as the argument e.g. `AEXPExporter.exe c:\test\f101.conf`.

# Configuration Parameters

- The parameter names have prefix of version number. This version number is the minimum APEX version that this parameter supports.  Version 0.0 means this parameter is version independent.
- In the config file (APEXporter.conf), leave parameter value to empty if it is not used.  Do not remove or add any parameter.

| **Parameter** | **Min. Ver.** | **Value Type** | **Description** |
| --- | --- | --- | --- |
| db | 0.0 | String | Database connect URL in JDBC format |
| user | 0.0 | String | Database username |
| password | 0.0 | String | Database password |
| dir | 0.0 | String | Directory which store the output files. |
| debug | 0.0 | Number | debug level 0 (no message), 1 (info only, default), 2 (verbose) |
| logtofile | 0.0 | True/False | Create log file APEXExporter.log under the same running directory |
| exitprompt | 0.0 | True/False | User needs to response before program exit |
| applicationid | 2.2 | Number | ID for application to be exported |
| workspaceid | 2.2 | Number | Workspace ID for which all applications to be exported or the workspace to be exported |
| instance | 2.2 | True/False | Export all applications |
| expWorkspace | 4.0 | True/False | Export workspace identified by workspaceid or all workspaces if workspaceid not specified |
| expMinimal | 4.2 | True/False | Only export workspace definition, users, and groups |
| expFiles | 4.1 | True/False | Export all workspace files identified by workspaceid |
| skipExportDate | 2.2 | True/False | Exclude export date from application export files |
| expPubReports | 4.0 | True/False | Export all user saved public interactive reports |
| expSavedReports | 3.2 | True/False | Export all user saved interactive reports |
| expIRNotif | 4.0 | True/False | Export all interactive report notifications |
| expTranslations | 4.1 | True/False | Export the translation mappings and all text from the translation repository |
| expFeedback | 4.0 | True/False | Export team development feedback for all workspaces or identified by workspaceid to development or deployment |
| expTeamdevdata | 4.0 | True/False | Export team development data for all workspaces or identified by workspaceid |
| deploymentSystem | 4.0 | True/False | Deployment system for exported feedback |
| expFeedbackSince | 4.0 | String | Export team development feedback since date in the format YYYYMMDD |
| expOriginalIds | 5.0 | Number | If specified, the application export will emit ids as they were when the application was imported |
| expNoSubscriptions | 8.1 | True/False | Do not export references to subscribed components |
| expComments | 8.1 | True/False | Export developer comments |
| expSupportingObjects | 8.1 | String | Pass (Y)es, (N)o or (I)nstall to override the default |
| expACLAssignments | 8.1 | True/False | Export ACL User Role Assignments. |
| nochecksum | 8.1 | True/False | Overwrite existing files even if the contents have not changed |
| expComponents | 9.2 |String | Export application components. All remaining parameters must be of form TYPE:ID |
