/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.projects.internal;

import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.BuildOptionsBuilder;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageManifest;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.SemanticVersion;
import io.ballerina.projects.TomlDocument;
import io.ballerina.projects.util.FileUtils;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.toml.semantic.TomlType;
import io.ballerina.toml.semantic.ast.TomlArrayValueNode;
import io.ballerina.toml.semantic.ast.TomlBooleanValueNode;
import io.ballerina.toml.semantic.ast.TomlKeyValueNode;
import io.ballerina.toml.semantic.ast.TomlStringValueNode;
import io.ballerina.toml.semantic.ast.TomlTableArrayNode;
import io.ballerina.toml.semantic.ast.TomlTableNode;
import io.ballerina.toml.semantic.ast.TomlValueNode;
import io.ballerina.toml.semantic.ast.TopLevelNode;
import io.ballerina.toml.validator.TomlValidator;
import io.ballerina.toml.validator.schema.Schema;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.compiler.CompilerOptionName;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.projects.util.ProjectUtils.guessOrgName;
import static io.ballerina.projects.util.ProjectUtils.guessPkgName;

/**
 * Build Manifest using toml files.
 *
 * @since 2.0.0
 */
public class ManifestBuilder {

    private TomlDocument ballerinaToml;
    private Optional<TomlDocument> dependenciesToml;
    private DiagnosticResult diagnostics;
    private List<Diagnostic> diagnosticList;
    private PackageManifest packageManifest;
    private BuildOptions buildOptions;
    private Path projectPath;

    private static final String PACKAGE = "package";
    private static final String VERSION = "version";
    private static final String LICENSE = "license";
    private static final String AUTHORS = "authors";
    private static final String REPOSITORY = "repository";
    private static final String KEYWORDS = "keywords";
    private static final String EXPORTED = "exported";

    private ManifestBuilder(TomlDocument ballerinaToml, TomlDocument dependenciesToml, Path projectPath) {
        this.projectPath = projectPath;
        this.ballerinaToml = ballerinaToml;
        this.dependenciesToml = Optional.ofNullable(dependenciesToml);
        this.diagnosticList = new ArrayList<>();
        this.packageManifest = parseAsPackageManifest();
        this.buildOptions = parseBuildOptions();
    }

    public static ManifestBuilder from(TomlDocument ballerinaToml, TomlDocument dependenciesToml, Path projectPath) {
        return new ManifestBuilder(ballerinaToml, dependenciesToml, projectPath);
    }

    public DiagnosticResult diagnostics() {
        if (diagnostics != null) {
            return diagnostics;
        }

        // Add toml syntax diagnostics
        this.diagnosticList.addAll(ballerinaToml.toml().diagnostics());
        dependenciesToml.ifPresent(tomlDocument -> this.diagnosticList.addAll(tomlDocument.toml().diagnostics()));
        diagnostics = new DefaultDiagnosticResult(this.diagnosticList);
        return diagnostics;
    }

    public String getErrorMessage() {
        StringBuilder message = new StringBuilder();
        message.append("Ballerina.toml contains errors\n");
        for (Diagnostic diagnostic : diagnostics().errors()) {
            message.append(convertDiagnosticToString(diagnostic));
            message.append("\n");
        }
        return message.toString();
    }

    public PackageManifest packageManifest() {
        return this.packageManifest;
    }

    public BuildOptions buildOptions() {
        return this.buildOptions;
    }

    private PackageManifest parseAsPackageManifest() {
        TomlValidator ballerinaTomlValidator;
        try {
            ballerinaTomlValidator = new TomlValidator(
                    Schema.from(FileUtils.readFileAsString("ballerina-toml-schema.json")));
        } catch (IOException e) {
            throw new ProjectException("Failed to read the Ballerina.toml validator schema file.");
        }

        // Validate ballerinaToml using ballerina toml schema
        ballerinaTomlValidator.validate(ballerinaToml.toml());

        TomlTableNode tomlAstNode = ballerinaToml.toml().rootNode();
        PackageDescriptor packageDescriptor = getPackageDescriptor(tomlAstNode);

        List<String> license = Collections.emptyList();
        List<String> authors = Collections.emptyList();
        List<String> keywords = Collections.emptyList();
        List<String> exported = Collections.emptyList();
        String repository = "";

        if (!tomlAstNode.entries().isEmpty()) {
            TomlTableNode pkgNode = (TomlTableNode) tomlAstNode.entries().get(PACKAGE);
            if (pkgNode != null && pkgNode.kind() != TomlType.NONE && pkgNode.kind() == TomlType.TABLE) {
                license = getStringArrayFromPackageNode(pkgNode, LICENSE);
                authors = getStringArrayFromPackageNode(pkgNode, AUTHORS);
                keywords = getStringArrayFromPackageNode(pkgNode, KEYWORDS);
                exported = getStringArrayFromPackageNode(pkgNode, EXPORTED);
                repository = getStringValueFromPackageNode(pkgNode, REPOSITORY, "");
            }
        }

        // Do not mutate toml tree
        Map<String, TopLevelNode> otherEntries = new HashMap<>();
        if (!tomlAstNode.entries().isEmpty()) {

            for (Map.Entry<String, TopLevelNode> entry : tomlAstNode.entries().entrySet()) {
                if (entry.getKey().equals(PACKAGE)) {
                    continue;
                }
                otherEntries.put(entry.getKey(), entry.getValue());
            }
        }

        // Process dependencies
        var dependencies = getDependencies();

        // Process platforms
        TopLevelNode platformNode = otherEntries.remove("platform");
        Map<String, PackageManifest.Platform> platforms = getPlatforms(platformNode);

        return PackageManifest.from(packageDescriptor, dependencies, platforms, otherEntries, diagnostics(), license,
                                    authors, keywords, exported, repository);
    }

    private PackageDescriptor getPackageDescriptor(TomlTableNode tomlTableNode) {
        // set defaults
        PackageOrg defaultOrg = PackageOrg.from(guessOrgName());
        PackageName defaultName = PackageName.from(guessPkgName(Optional.ofNullable(this.projectPath.getFileName())
                                                                        .map(Path::toString).orElse("")));
        PackageVersion defaultVersion = PackageVersion.from(ProjectConstants.INTERNAL_VERSION);
        String org;
        String name;
        String version;

        if (tomlTableNode.entries().isEmpty()) {
            return PackageDescriptor.from(defaultOrg, defaultName, defaultVersion);
        }

        TomlTableNode pkgNode = (TomlTableNode) tomlTableNode.entries().get(PACKAGE);
        if (pkgNode == null || pkgNode.kind() == TomlType.NONE) {
            return PackageDescriptor.from(defaultOrg, defaultName, defaultVersion);
        }

        if (pkgNode.entries().isEmpty()) {
            return PackageDescriptor.from(defaultOrg, defaultName, defaultVersion);
        }

        org = getStringValueFromPackageNode(pkgNode, "org", defaultOrg.value());
        name = getStringValueFromPackageNode(pkgNode, "name", defaultName.value());
        version = getStringValueFromPackageNode(pkgNode, VERSION, defaultVersion.value().toString());

        // check org is valid identifier
        boolean isValidOrg = ProjectUtils.validateOrgName(org);
        if (!isValidOrg) {
            org = defaultOrg.value();
        }

        // check that the package name is valid
        boolean isValidPkg = ProjectUtils.validatePackageName(name);
        if (!isValidPkg) {
            name = defaultName.value();
        }

        // check version is compatible with semver
        try {
            SemanticVersion.from(version);
        } catch (ProjectException e) {
            version = defaultVersion.value().toString();
        }

        return PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name), PackageVersion.from(version));
    }

    private BuildOptions parseBuildOptions() {
        TomlTableNode tomlTableNode = ballerinaToml.toml().rootNode();
        return setBuildOptions(tomlTableNode);
    }

    private List<PackageManifest.Dependency> getDependencies() {
        if (dependenciesToml.isEmpty()) {
            return Collections.emptyList();
        }

        TomlValidator dependenciesTomlValidator;
        try {
            dependenciesTomlValidator = new TomlValidator(
                    Schema.from(FileUtils.readFileAsString("dependencies-toml-schema.json")));
        } catch (IOException e) {
            throw new ProjectException("Failed to read the Dependencies.toml validator schema file.");
        }
        // Validate dependencies toml using dependencies toml schema
        dependenciesTomlValidator.validate(dependenciesToml.get().toml());

        List<PackageManifest.Dependency> dependencies = new ArrayList<>();
        TomlTableNode tomlTableNode = dependenciesToml.get().toml().rootNode();

        if (tomlTableNode.entries().isEmpty()) {
            return Collections.emptyList();
        }

        TopLevelNode dependencyEntries = tomlTableNode.entries().get("dependency");
        if (dependencyEntries == null || dependencyEntries.kind() == TomlType.NONE) {
            return Collections.emptyList();
        }

        if (dependencyEntries.kind() == TomlType.TABLE_ARRAY) {
            TomlTableArrayNode dependencyTableArray = (TomlTableArrayNode) dependencyEntries;

            for (TomlTableNode dependencyNode : dependencyTableArray.children()) {
                PackageName depName = PackageName.from(getStringValueFromDependencyNode(dependencyNode, "name"));
                PackageOrg depOrg = PackageOrg.from(getStringValueFromDependencyNode(dependencyNode, "org"));
                PackageVersion depVersion = PackageVersion
                        .from(getStringValueFromDependencyNode(dependencyNode, VERSION));
                if (dependencyNode.entries().containsKey("repository")) {
                    String repository = getStringValueFromDependencyNode(dependencyNode, "repository");
                    dependencies.add(new PackageManifest.Dependency(depName, depOrg, depVersion, repository));
                }
                dependencies.add(new PackageManifest.Dependency(depName, depOrg, depVersion));
            }
        }
        return dependencies;
    }

    private Map<String, PackageManifest.Platform> getPlatforms(TopLevelNode platformNode) {
        Map<String, PackageManifest.Platform> platforms = new HashMap<>();
        if (platformNode == null || platformNode.kind() == TomlType.NONE) {
            platforms = Collections.emptyMap();
        } else {
            if (platformNode.kind() == TomlType.TABLE) {
                TomlTableNode platformTable = (TomlTableNode) platformNode;
                Set<String> platformCodes = platformTable.entries().keySet();
                if (platformCodes.size() != 1) {
                    return platforms;
                }

                String platformCode = platformCodes.stream().findFirst().get();
                TopLevelNode platformCodeNode = platformTable.entries().get(platformCode);

                if (platformCodeNode.kind() == TomlType.TABLE) {
                    TomlTableNode platformCodeTable = (TomlTableNode) platformCodeNode;
                    TopLevelNode dependencyNode = platformCodeTable.entries().get("dependency");

                    if (dependencyNode.kind() == TomlType.NONE) {
                        return platforms;
                    }

                    if (dependencyNode.kind() == TomlType.TABLE_ARRAY) {
                        TomlTableArrayNode dependencyTableArray = (TomlTableArrayNode) dependencyNode;
                        List<TomlTableNode> children = dependencyTableArray.children();

                        if (!children.isEmpty()) {
                            List<Map<String, Object>> platformEntry = new ArrayList<>();

                            for (TomlTableNode platformEntryTable : children) {
                                if (!platformEntryTable.entries().isEmpty()) {
                                    Map<String, Object> platformEntryMap = new HashMap<>();
                                    platformEntryMap.put("path",
                                            getStringValueFromPlatformEntry(platformEntryTable, "path"));
                                    platformEntryMap.put("groupId",
                                            getStringValueFromPlatformEntry(platformEntryTable, "groupId"));
                                    platformEntryMap.put("artifactId",
                                            getStringValueFromPlatformEntry(platformEntryTable, "artifactId"));
                                    platformEntryMap.put(VERSION,
                                            getStringValueFromPlatformEntry(platformEntryTable, VERSION));
                                    platformEntryMap.put("scope",
                                            getStringValueFromPlatformEntry(platformEntryTable, "scope"));
                                    platformEntry.add(platformEntryMap);
                                }
                            }

                            PackageManifest.Platform platform = new PackageManifest.Platform(platformEntry);
                            platforms.put(platformCode, platform);
                        }
                    }
                }
            }
        }

        return platforms;
    }

    private BuildOptions setBuildOptions(TomlTableNode tomlTableNode) {
        TomlTableNode tableNode = (TomlTableNode) tomlTableNode.entries().get("build-options");
        if (tableNode == null || tableNode.kind() == TomlType.NONE) {
            return null;
        }

        BuildOptionsBuilder buildOptionsBuilder = new BuildOptionsBuilder();

        boolean skipTests = getBooleanFromBuildOptionsTableNode(tableNode, CompilerOptionName.SKIP_TESTS.toString());
        boolean offline = getBooleanFromBuildOptionsTableNode(tableNode, CompilerOptionName.OFFLINE.toString());
        boolean observabilityIncluded =
                getBooleanFromBuildOptionsTableNode(tableNode, CompilerOptionName.OBSERVABILITY_INCLUDED.toString());
        boolean testReport =
                getBooleanFromBuildOptionsTableNode(tableNode, BuildOptions.OptionName.TEST_REPORT.toString());
        boolean codeCoverage =
                getBooleanFromBuildOptionsTableNode(tableNode, BuildOptions.OptionName.CODE_COVERAGE.toString());
        final TopLevelNode topLevelNode = tableNode.entries().get(CompilerOptionName.CLOUD.toString());
        String cloud = "";
        if (topLevelNode != null) {
            cloud = getStringFromTomlTableNode(topLevelNode);
        }
        boolean taintCheck =
                getBooleanFromBuildOptionsTableNode(tableNode, CompilerOptionName.TAINT_CHECK.toString());
        boolean listConflictedClasses =
                getBooleanFromBuildOptionsTableNode(tableNode, CompilerOptionName.LIST_CONFLICTED_CLASSES.toString());

        return buildOptionsBuilder
                .skipTests(skipTests)
                .offline(offline)
                .observabilityIncluded(observabilityIncluded)
                .testReport(testReport)
                .codeCoverage(codeCoverage)
                .cloud(cloud)
                .taintCheck(taintCheck)
                .listConflictedClasses(listConflictedClasses)
                .build();
    }

    private boolean getBooleanFromBuildOptionsTableNode(TomlTableNode tableNode, String key) {
        TopLevelNode topLevelNode = tableNode.entries().get(key);
        if (topLevelNode == null || topLevelNode.kind() == TomlType.NONE) {
            return false;
        }

        if (topLevelNode.kind() == TomlType.KEY_VALUE) {
            TomlKeyValueNode keyValueNode = (TomlKeyValueNode) topLevelNode;
            TomlValueNode value = keyValueNode.value();
            if (value.kind() == TomlType.BOOLEAN) {
                TomlBooleanValueNode tomlBooleanValueNode = (TomlBooleanValueNode) value;
                return tomlBooleanValueNode.getValue();
            }
        }
        return false;
    }

    private String getStringValueFromPackageNode(TomlTableNode pkgNode, String key, String defaultValue) {
        TopLevelNode topLevelNode = pkgNode.entries().get(key);
        if (topLevelNode == null || topLevelNode.kind() == TomlType.NONE) {
            // return default value
            return defaultValue;
        }
        String value = getStringFromTomlTableNode(topLevelNode);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private List<String> getStringArrayFromPackageNode(TomlTableNode pkgNode, String key) {
        List<String> elements = new ArrayList<>();
        TopLevelNode topLevelNode = pkgNode.entries().get(key);
        if (topLevelNode == null || topLevelNode.kind() == TomlType.NONE) {
            return elements;
        }
        TomlValueNode valueNode = ((TomlKeyValueNode) topLevelNode).value();
        if (valueNode.kind() == TomlType.NONE) {
            return elements;
        }
        if (valueNode.kind() == TomlType.ARRAY) {
            TomlArrayValueNode arrayValueNode = (TomlArrayValueNode) valueNode;
            for (TomlValueNode value : arrayValueNode.elements()) {
                if (value.kind() == TomlType.STRING) {
                    elements.add(((TomlStringValueNode) value).getValue());
                }
            }
        }
        return elements;
    }

    private String getStringValueFromDependencyNode(TomlTableNode pkgNode, String key) {
        TopLevelNode topLevelNode = pkgNode.entries().get(key);
        return getStringFromTomlTableNode(topLevelNode);
    }

    private String getStringValueFromPlatformEntry(TomlTableNode pkgNode, String key) {
        TopLevelNode topLevelNode = pkgNode.entries().get(key);
        if (topLevelNode == null || topLevelNode.kind() == TomlType.NONE) {
            return null;
        }
        return getStringFromTomlTableNode(topLevelNode);
    }

    private String getStringFromTomlTableNode(TopLevelNode topLevelNode) {
        if (topLevelNode.kind() == TomlType.KEY_VALUE) {
            TomlKeyValueNode keyValueNode = (TomlKeyValueNode) topLevelNode;
            TomlValueNode value = keyValueNode.value();
            if (value.kind() == TomlType.STRING) {
                TomlStringValueNode stringValueNode = (TomlStringValueNode) value;
                return stringValueNode.getValue();
            }
        }
        return null;
    }

    private String convertDiagnosticToString(Diagnostic diagnostic) {
        LineRange lineRange = diagnostic.location().lineRange();

        LineRange oneBasedLineRange = LineRange.from(
                lineRange.filePath(),
                LinePosition.from(lineRange.startLine().line(), lineRange.startLine().offset() + 1),
                LinePosition.from(lineRange.endLine().line(), lineRange.endLine().offset() + 1));

        return diagnostic.diagnosticInfo().severity().toString() + " [" +
                oneBasedLineRange.filePath() + ":" + oneBasedLineRange + "] " + diagnostic.message();
    }
}
