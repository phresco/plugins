/**
 * Phresco Plugins
 *
 * Copyright (C) 1999-2014 Photon Infotech Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.photon.phresco.convertor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.photon.phresco.plugin.commons.MavenProjectInfo;
import com.photon.phresco.vo.CsvFileVO;
import com.photon.phresco.vo.ImageVO;

public class PHPCreator {

	public PHPCreator() {
	}

	public List<CsvFileVO> createPHPFile(List<CsvFileVO> list, MavenProjectInfo mavenProjectInfo) throws IOException,
			Exception {
		Set<String> uniqueFunctionNamesSet = new HashSet<String>();
		Set<String> uniqueVariableNamesSet = new HashSet<String>();
		for (CsvFileVO csvo : list) {
			uniqueFunctionNamesSet.add(csvo.getPhpFunction());
			uniqueVariableNamesSet.add(csvo.getPhpVariable());
		}
		StringBuffer phpFile = new StringBuffer();
		phpFile.append("<?php\n" + "/**\n" + "* @file\n" + "* ass\n" + "*/\n" + "/**\n"
				+ "* Check if the drush is executed from command line\n" + "*/\n" + "if (!drush_verify_cli()) {\n"
				+ "die('drush.php is designed to run via the command line.');\n" + "}\n");
		phpFile.append("include drupal_get_path('module', "
				+ "'itrinno_mobilecontent_api') . \"/includes/mobilecontent.admin.inc\";\n");
		Iterator<String> iter = uniqueFunctionNamesSet.iterator();
		while (iter.hasNext()) {
			String funcName = iter.next();
			if (funcName != null && funcName.length() > 0) {
				phpFile.append(funcName + "\n");
			}
		}
		Iterator<String> iterVar = uniqueVariableNamesSet.iterator();
		while (iterVar.hasNext()) {
			String varName = iterVar.next();
			if (varName != null && varName.length() > 0) {
				phpFile.append(varName + "\n");
			}
		}
		List<String> buildCreateContentLineList = getBuildCreateContentLines(list);
		for (String buildCreateContentLine : buildCreateContentLineList) {
			phpFile.append(buildCreateContentLine + "\n");
		}
		phpFile.append("?>");
		new File((mavenProjectInfo.getBaseDir() + File.separator
				+ mavenProjectInfo.getProject().getProperties().getProperty("phresco.content.php.file.deploy.dir")).replace("/",File.separator)).mkdirs();
		FileOutputStream fos = new FileOutputStream(new File((mavenProjectInfo.getBaseDir() + File.separator
				+ mavenProjectInfo.getProject().getProperties().getProperty("phresco.content.php.file.deploy.dir")
				+ File.separator
				+ mavenProjectInfo.getProject().getProperties().getProperty("phresco.content.php.file.name")).replace("/",File.separator)));
		Writer out = new OutputStreamWriter(fos, "UTF8");
		out.write(phpFile.toString());
		out.flush();
		out.close();
		return list;
	}

	private List<String> getBuildCreateContentLines(List<CsvFileVO> list) {
		List<String> buildCreateContentLineList = new ArrayList<String>();
		for (CsvFileVO csvo : list) {

			String buildCreateContentLine = "build_create_content('" + csvo.getLanguage() + "','"
					+ csvo.getContentType() + "','" + csvo.getContentTypeName() + "','";

			Map<String, String> map = csvo.getTitleMap();
			Set<String> keys = map.keySet();
			for (String k : keys) {
				buildCreateContentLine += map.get(k).replace("'", "\\'");
			}
			buildCreateContentLine += "','";
			map = csvo.getDescriptionMap();
			keys = map.keySet();
			for (String k : keys) {
				buildCreateContentLine += map.get(k).replace("'", "\\'");
			}
			buildCreateContentLine += "',";
			buildCreateContentLine += "array(";

			map = csvo.getExtraMap();
			keys = map.keySet();
			for (String k : keys) {
				buildCreateContentLine += "'" + k + "'=>";
				buildCreateContentLine += "'" + map.get(k).replace("'", "\\'") + "\'";
				buildCreateContentLine += ",";
			}
			buildCreateContentLine += "),array(";
			Map<String, ImageVO>imageMap = csvo.getImageMap();
			keys = imageMap.keySet();
			for (String k : keys) {
				buildCreateContentLine += "'" + k + "'=>";
				buildCreateContentLine += "'" + imageMap.get(k).getFileName().replace("'", "\\'") + "'";
				buildCreateContentLine += ",";
			}
			buildCreateContentLine += "),array(";
			map = csvo.getCategoryMap();
			keys = map.keySet();
			for (String k : keys) {
				buildCreateContentLine += "'" + k + "'=>";
				buildCreateContentLine += "'" + map.get(k).replace("'", "\\'") + "'";
				buildCreateContentLine += ",";
			}
			buildCreateContentLine += "),array(";
			map = csvo.getUrlMap();
			keys = map.keySet();
			for (String k : keys) {
				buildCreateContentLine += "'" + k + "'=>";
				buildCreateContentLine += "'" + map.get(k).replace("'", "\\'") + "'";
				buildCreateContentLine += ",";
			}
			buildCreateContentLine += "),array(";
			map = csvo.getMetadataMap();
			keys = map.keySet();
			for (String k : keys) {
				buildCreateContentLine += "\'" + k + "\'=>";
				buildCreateContentLine += "\'" + map.get(k).replace("'", "\\'") + "\'";
				buildCreateContentLine += ",";
			}
			buildCreateContentLine = buildCreateContentLine.replace(",)", ")");
			buildCreateContentLine += "));";
			buildCreateContentLineList.add(buildCreateContentLine);
		}
		return buildCreateContentLineList;
	}
}
