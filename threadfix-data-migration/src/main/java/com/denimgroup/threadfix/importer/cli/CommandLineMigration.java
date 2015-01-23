////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2015 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////

package com.denimgroup.threadfix.importer.cli;

import com.denimgroup.threadfix.importer.config.SpringConfiguration;
import com.denimgroup.threadfix.importer.util.RegexUtils;
import com.denimgroup.threadfix.logging.SanitizedLogger;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.denimgroup.threadfix.CollectionUtils.newMap;

@Component
public class CommandLineMigration {

    private static final SanitizedLogger LOGGER = new SanitizedLogger(CommandLineMigration.class);

    private static final String TABLE_PATTERN = "CREATE MEMORY TABLE (.*)";
    private static final String INSERT_PATTERN = "INSERT INTO (.*) VALUES";
    private static final String ACUNETIX_ESCAPE = "Cross-Site Scripting in HTML \\''script\\'' tag";
    private static final String ACUNETIX_ESCAPE_REPLACE = "Cross-Site Scripting in HTML &quot;script&quot; tag";

    private static Map<String, String> tableMap = newMap();


    public static void main(String[] args) throws IOException {
        if (!check(args))
            return;

        long startTime = System.currentTimeMillis();

        String inputScript = args[0];
        String inputMySqlConfig = args[1];
        String outputScript = "import.sql";
        String outputMySqlConfigTemp = "jdbc_temp.properties";

        copyFile(inputMySqlConfig, outputMySqlConfigTemp);

        ScriptRunner scriptRunner = SpringConfiguration.getContext().getBean(ScriptRunner.class);

        File file = new File(inputScript);
        List<String> lines = FileUtils.readLines(file);
        StringBuffer sqlContent = new StringBuffer();
        sqlContent.append("SET FOREIGN_KEY_CHECKS=0;\n");

        String table;
        for (String line : lines) {
            if (line != null && line.toUpperCase().startsWith("CREATE MEMORY TABLE ")) {
                table = RegexUtils.getRegexResult(line, TABLE_PATTERN);
                System.out.println("table:" + table);
                String[] tableName = table.split("\\(", 2);
                if (tableName.length == 2) {
                    StringBuffer fieldsStr = new StringBuffer();
                    String[] fields = tableName[1].trim().split(",");
                    fieldsStr.append(fields[0].split(" ")[0]);
                    for (int i = 1; i< fields.length; i++) {
                        if (!"CONSTRAINT".equalsIgnoreCase(fields[i].trim().split(" ")[0]))
                            fieldsStr.append(", " + fields[i].trim().split(" ")[0]);
                    }
                    tableMap.put(tableName[0].toUpperCase(), "(" + fieldsStr.toString() + ")");
                }
            } else if (line != null && line.toUpperCase().startsWith("INSERT INTO ")) {
                table = RegexUtils.getRegexResult(line, INSERT_PATTERN);
                if (tableMap.get(table) != null) {
                    line = line.replaceFirst(" " + table + " ", " " + table + tableMap.get(table) + " ");
                    if (line.contains(ACUNETIX_ESCAPE)) {
                        line = line.replace(ACUNETIX_ESCAPE, ACUNETIX_ESCAPE_REPLACE);
                    }
                    line = line + ";\n";
                    sqlContent.append(line);
//                    scriptRunner.execute(line);
                }

            }
        }
        sqlContent.append("SET FOREIGN_KEY_CHECKS=1;\n");

        System.out.println("Saving sql script file to " + outputScript);
        FileUtils.writeStringToFile(new File(outputScript), sqlContent.toString());

        System.out.println("Sending sql script file to MySQL server");
        scriptRunner.run(outputScript, outputMySqlConfigTemp);

        deleteFile(outputMySqlConfigTemp);
        deleteFile(outputScript);
        LOGGER.info("Initialization finished in " + (System.currentTimeMillis() - startTime) + " ms");

    }

    private static boolean check(String[] args) {
        if (args.length != 2) {
            System.out.println("This program accepts two argument, threadfix script and mysql config files.");
            return false;
        }

        for (String arg: args) {
            File file = new File(arg);
            if (!file.exists()) {
                System.out.println(arg + ": The file must exist.");
                return false;
            }

            if (file.isDirectory()) {
                System.out.println(arg + ": The file must not be a directory.");
                return false;
            }
        }
        return true;
    }

    private static void copyFile(String oldFilePath, String newFilePath) throws IOException {
        FileUtils.writeStringToFile(new File(newFilePath), FileUtils.readFileToString(new File(oldFilePath)));
        System.out.println("Copied from " + oldFilePath + " to " + newFilePath);
    }

    private static void deleteFile(String oldFilePath) throws IOException {

        File fouput = new File(oldFilePath);
        if (fouput.exists() && fouput.isFile())
            if (fouput.delete())
                System.out.println("File " + oldFilePath + " has been deleted");
            else
                System.err.println("File " + oldFilePath + " has not been deleted");
    }
}