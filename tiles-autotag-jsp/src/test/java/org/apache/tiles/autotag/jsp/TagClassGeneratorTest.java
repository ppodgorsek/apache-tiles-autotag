/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tiles.autotag.jsp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tiles.autotag.core.DirectoryOutputLocator;
import org.apache.tiles.autotag.core.OutputLocator;
import org.apache.tiles.autotag.core.runtime.ModelBody;
import org.apache.tiles.autotag.model.TemplateClass;
import org.apache.tiles.autotag.model.TemplateMethod;
import org.apache.tiles.autotag.model.TemplateParameter;
import org.apache.tiles.autotag.model.TemplateSuite;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Test;

/**
 * Tests {@link TagClassGenerator}.
 *
 * @version $Rev$ $Date$
 */
public class TagClassGeneratorTest {

    public static final String REQUEST_CLASS = "org.apache.tiles.autotag.jsp.test.Request";

    /**
     * Test method for {@link TagClassGenerator#generate(File, String, TemplateSuite, TemplateClass, Map)}.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void testGenerate() throws Exception {
        Properties props = new Properties();
        InputStream propsStream = getClass().getResourceAsStream("/org/apache/tiles/autotag/velocity.properties");
        props.load(propsStream);
        propsStream.close();
        VelocityEngine velocityEngine = new VelocityEngine(props);

        TagClassGenerator generator = new TagClassGenerator(velocityEngine);
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "autotag");
        OutputLocator locator = new DirectoryOutputLocator(tempDir);
        tempDir.deleteOnExit();
        TemplateSuite suite = new TemplateSuite("tldtest", "Test for TLD docs.");
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("taglibURI", "http://www.initrode.net/tags/test");

        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        TemplateParameter param = new TemplateParameter("one", "one", "java.lang.String", null, true, false);
        param.setDocumentation("Parameter one.");
        params.add(param);
        param = new TemplateParameter("two", "two", "int", null, false, false);
        param.setDocumentation("Parameter two.");
        params.add(param);
        param = new TemplateParameter("three", "three", "boolean", null, false, false);
        param.setDocumentation("Parameter three.");
        params.add(param);
        param = new TemplateParameter("request", "request", REQUEST_CLASS, null, false, true);
        param.setDocumentation("The request.");
        params.add(param);
        param = new TemplateParameter("modelBody", "modelBody", ModelBody.class.getName(), null, false, false);
        param.setDocumentation("The body.");
        params.add(param);
        TemplateMethod executeMethod = new TemplateMethod("execute", params);

        TemplateClass clazz = new TemplateClass("org.apache.tiles.autotag.template.DoStuffTemplate",
                "doStuff", "DoStuff", executeMethod);
        clazz.setDocumentation("Documentation of the DoStuff class.");

        generator.generate(locator, "org.apache.tiles.autotag.jsp.test", suite, clazz, parameters,
                           "org.apache.tiles.autotag.jsp.test.Runtime", REQUEST_CLASS);

        InputStream expected = getClass().getResourceAsStream("/org/apache/tiles/autotag/jsp/test/DoStuffTag.java");
        File effectiveFile = new File(tempDir, "/org/apache/tiles/autotag/jsp/test/DoStuffTag.java");
        assertTrue(effectiveFile.exists());
        InputStream effective = new FileInputStream(effectiveFile);
        assertTrue("The contents of both input streams for DoStuffTag.java should be equal", IOUtils.contentEquals(effective, expected));
        effective.close();
        expected.close();

        suite.addTemplateClass(clazz);
        params = new ArrayList<TemplateParameter>();
        param = new TemplateParameter("one", "one", "java.lang.Double", null, true, false);
        param.setDocumentation("Parameter one.");
        params.add(param);
        param = new TemplateParameter("two", "two", "float", null, false, false);
        param.setDocumentation("Parameter two.");
        params.add(param);
        param = new TemplateParameter("three", "three", "java.util.Date", null, false, false);
        param.setDocumentation("Parameter three.");
        params.add(param);
        param = new TemplateParameter("request", "request", REQUEST_CLASS, null, false, true);
        param.setDocumentation("The request.");
        params.add(param);
        executeMethod = new TemplateMethod("execute", params);

        clazz = new TemplateClass("org.apache.tiles.autotag.template.DoStuffNoBodyTemplate",
                "doStuffNoBody", "DoStuffNoBody", executeMethod);
        clazz.setDocumentation("Documentation of the DoStuffNoBody class.");

        suite.addTemplateClass(clazz);

        generator.generate(locator, "org.apache.tiles.autotag.jsp.test", suite, clazz, parameters,
                           "org.apache.tiles.autotag.jsp.test.Runtime", REQUEST_CLASS);

        expected = getClass().getResourceAsStream("/org/apache/tiles/autotag/jsp/test/DoStuffNoBodyTag.java");
        effectiveFile = new File(tempDir, "/org/apache/tiles/autotag/jsp/test/DoStuffNoBodyTag.java");
        assertTrue(effectiveFile.exists());
        effective = new FileInputStream(effectiveFile);
        assertTrue("The contents of both input streams for DoStuffNoBodyTag.java should be equal", IOUtils.contentEquals(effective, expected));
        effective.close();
        expected.close();

        FileUtils.deleteDirectory(tempDir);
    }

}
