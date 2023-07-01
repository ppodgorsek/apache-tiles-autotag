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
package org.apache.tiles.autotag.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.tiles.autotag.core.runtime.ModelBody;
import org.apache.tiles.autotag.core.runtime.annotation.Parameter;
import org.apache.tiles.autotag.model.TemplateClass;
import org.apache.tiles.autotag.model.TemplateMethod;
import org.apache.tiles.autotag.model.TemplateParameter;
import org.apache.tiles.autotag.model.TemplateSuite;
import org.apache.tiles.autotag.model.TemplateSuiteFactory;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;

/**
 * Creates a template suite using QDox.
 *
 * @version $Rev$ $Date$
 */
public class QDoxTemplateSuiteFactory implements TemplateSuiteFactory {

    /**
     * The suffix of parsed classes.
     */
    private static final String TEMPLATE_SUFFIX = "Model";

    /**
     * The Javadoc builder.
     */
    private JavaProjectBuilder builder;

    /**
     * The name of the suite.
     */
    private String suiteName;

    /**
     * The documentation of the suite.
     */
    private String suiteDocumentation;

    /**
     * The request class the suite.
     */
    private String requestClass;

    /**
     * Constructor.
     *
     * @param sourceFiles All the source files to parse.
     */
    public QDoxTemplateSuiteFactory(File... sourceFiles) {
        builder = new JavaProjectBuilder();
        builder.setEncoding(StandardCharsets.UTF_8.name());

        try {
            for (File file : sourceFiles) {
                builder.addSource(file);
            }
        } catch (IOException e) {
            throw new ClassParseException(
                    "I/O Exception when adding source files", e);
        }
    }

    /**
     * Constructor.
     *
     * @param urls All the URLs of source files to parse.
     */
    public QDoxTemplateSuiteFactory(URL... urls) {
        builder = new JavaProjectBuilder();
        builder.setEncoding(StandardCharsets.UTF_8.name());

        try {
            for (URL url : urls) {
                builder.addSource(url);
            }
        } catch (IOException e) {
            throw new ClassParseException(
                    "I/O Exception when adding source files", e);
        }
    }

    /**
     * Sets the suite name to assign to the created suite.
     *
     * @param suiteName The suite name.
     */
    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    /**
     * Sets the suite documentation to assign to the suite.
     *
     * @param suiteDocumentation The suite documentation.
     */
    public void setSuiteDocumentation(String suiteDocumentation) {
        this.suiteDocumentation = suiteDocumentation;
    }

    /**
     * Sets the request class used by the suite.
     *
     * @param requestClass The request class name.
     */
    public void setRequestClass(String requestClass) {
        this.requestClass = requestClass;
    }

    @Override
    public TemplateSuite createTemplateSuite() {
        List<TemplateClass> classes = new ArrayList<TemplateClass>();
        for (JavaClass clazz : builder.getClasses()) {
            String tagClassPrefix = getTagClassPrefix(clazz);
            if (tagClassPrefix != null) {
                String tagName = tagClassPrefix.substring(0, 1).toLowerCase()
                        + tagClassPrefix.substring(1);
                TemplateMethod executeMethod = null;
                for (JavaMethod method : clazz.getMethods()) {
                    if (isFeasible(method)) {
                        executeMethod = createMethod(method);
                    }
                }
                if (executeMethod != null) {
                    TemplateClass templateClass = new TemplateClass(clazz
                            .getFullyQualifiedName(), tagName, tagClassPrefix,
                            executeMethod);
                    templateClass.setDocumentation(clazz.getComment());
                    classes.add(templateClass);
                }
            }
        }
        return new TemplateSuite(suiteName, suiteDocumentation, classes);
    }

    /**
     * Computes the tag class prefix.
     *
     * @param clazz The parsed class.
     * @return The tag class prefix.
     */
    private String getTagClassPrefix(JavaClass clazz) {
        String tagName;
        String simpleClassName = clazz.getName();
        if (simpleClassName.endsWith(TEMPLATE_SUFFIX)
                && simpleClassName.length() > TEMPLATE_SUFFIX.length()) {
            tagName = simpleClassName.substring(0, 1).toUpperCase()
                    + simpleClassName.substring(1, simpleClassName.length()
                            - TEMPLATE_SUFFIX.length());
        } else {
            tagName = null;
        }
        return tagName;
    }

    /**
     * Creates a template method descriptor from a parsed method.
     *
     * @param method The parsed method.
     * @return The template method descriptor.
     */
    private TemplateMethod createMethod(JavaMethod method) {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        for (JavaParameter parameter : method.getParameters()) {
            String exportedName = parameter.getName();
            boolean required = false;
            String defaultValue = null;
            List<JavaAnnotation> annotations = parameter.getAnnotations();
            if (annotations != null && annotations.size() > 0) {
                boolean found = false;
                for (int i = 0; i < annotations.size() && !found; i++) {
                    if (Parameter.class.getName().equals(annotations.get(i).getType().getFullyQualifiedName())) {
                        found = true;
                        String candidateName = (String) annotations.get(i).getNamedParameter("name");
                        if (candidateName != null && candidateName.length() > 2) {
                            exportedName = candidateName.substring(1, candidateName.length() - 1);
                        }
                        required = "true".equals(annotations.get(i).getNamedParameter("required"));
                        candidateName = (String) annotations.get(i).getNamedParameter("defaultValue");
                        if (candidateName != null && candidateName.length() > 2) {
                            defaultValue = candidateName.substring(1, candidateName.length() - 1);
                        }
                    }
                }
            }
            String parameterType = parameter.getType()
                    .getFullyQualifiedName();
            TemplateParameter templateParameter = new TemplateParameter(
                    parameter.getName(), exportedName, parameterType, defaultValue, required,
                    requestClass.equals(parameterType));
            params.add(templateParameter);
        }
        TemplateMethod templateMethod = new TemplateMethod(method.getName(),
                params);
        templateMethod.setDocumentation(method.getComment());
        List<DocletTag> tags = method.getTagsByName("param");
        for (DocletTag tag : tags) {
            List<String> tagParams = tag.getParameters();
            if (tagParams.size() > 0) {
                TemplateParameter templateParameter = templateMethod
                        .getParameterByName(tagParams.get(0));
                if (templateParameter != null) {
                    String tagValue = tag.getValue();
                    int pos = tagValue.indexOf(" ");
                    templateParameter.setDocumentation(tagValue.substring(pos)
                            .trim());
                }
            }
        }
        return templateMethod;
    }

    /**
     * Verifies if the method can be used as an "execute" method.
     *
     * @param method The parsed method.
     * @return <code>true</code> if it is an execute method.
     */
    private boolean isFeasible(JavaMethod method) {
        JavaClass returns = method.getReturns();
        if ("execute".equals(method.getName()) && returns != null
                && "void".equals(returns.getFullyQualifiedName())
                && method.isPublic() && !method.isStatic()
                && !method.isAbstract()) {
            List<JavaParameter> params = method.getParameters();
            if (params.size() > 0) {
                JavaParameter param = params.get(params.size() - 1);
                if (requestClass.equals(
                        param.getType().getFullyQualifiedName())) {
                    return true;
                }
            }
            if (params.size() >= 2) {
                JavaParameter param1 = params.get(params.size() - 2);
                JavaParameter param2 = params.get(params.size() - 1);
                if (requestClass.equals(
                        param1.getType().getFullyQualifiedName())
                        && ModelBody.class.getName().equals(
                                param2.getType().getFullyQualifiedName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
