/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/
package org.eclipse.winery.yaml.converter;

import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFile;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileParser;
import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.yaml.TServiceTemplate;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.yaml.common.Utils;
import org.eclipse.winery.yaml.common.exception.MultiException;
import org.eclipse.winery.yaml.common.exception.YAMLParserException;
import org.eclipse.winery.yaml.common.reader.yaml.Reader;
import org.eclipse.winery.yaml.converter.xml.X2YConverter;
import org.eclipse.winery.yaml.converter.yaml.Y2XConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Converter {
	private static final Logger LOGGER = LoggerFactory.getLogger(Converter.class);

	private final IRepository repository;

	public Converter() {
		this.repository = RepositoryFactory.getRepository();
	}

	public Converter(IRepository repository) {
		this.repository = repository;
	}

	public Definitions convertY2X(TServiceTemplate serviceTemplate, String name, String namespace, Path path, Path outPath) {
		return new Y2XConverter(this.repository).convert(serviceTemplate, name, namespace, path, outPath);
	}

	public void convertY2X(InputStream zip) {
		Path path = Utils.unzipFile(zip);
		LOGGER.debug("Unzip path: {}", path);
		try {
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.{yml,yaml}");
			Arrays.stream(Optional.ofNullable(path.toFile().listFiles()).orElse(new File[]{}))
				.map(File::toPath)
				.filter(file -> matcher.matches(file.getFileName()))
				.map(file -> {
					Reader reader = Reader.getReader();
					try {
						String id = file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf("."));
						Path fileName = file.subpath(path.getNameCount(), file.getNameCount());
						Path fileOutPath = path.resolve("tmp");
						String namespace = reader.getNamespace(path, fileName);
						TServiceTemplate serviceTemplate = reader.parse(path, fileName);
						LOGGER.debug("Convert filePath = {}, fileName = {}, id = {}, namespace = {}, fileOutPath = {}",
							path, fileName, id, namespace, fileOutPath);
						this.convertY2X(serviceTemplate, id, namespace, path, fileOutPath);
					} catch (YAMLParserException e) {
						return new MultiException().add(e);
					} catch (MultiException e) {
						return e;
					}
					return null;
				})
				.filter(Objects::nonNull)
				.reduce(MultiException::add)
				.ifPresent(e -> LOGGER.error("MultiException: ", e));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public InputStream convertX2Y(InputStream csar) {
		Path filePath = Utils.unzipFile(csar);
		Path fileOutPath = filePath.resolve("tmp");
		try {
			TOSCAMetaFileParser parser = new TOSCAMetaFileParser();
			TOSCAMetaFile metaFile = parser.parse(filePath.resolve("TOSCA-Metadata").resolve("TOSCA.meta"));

			org.eclipse.winery.yaml.common.reader.xml.Reader reader = new org.eclipse.winery.yaml.common.reader.xml.Reader();
			try {
				String fileName = metaFile.getEntryDefinitions();
				Definitions definitions = reader.parse(filePath, Paths.get(fileName));
				this.convertX2Y(definitions, fileOutPath);
			} catch (MultiException e) {
				LOGGER.error("Convert TOSCA XML to TOSCA YAML error", e);
			}
			return Utils.zipPath(fileOutPath);
		} catch (Exception e) {
			LOGGER.error("Error", e);
			throw new AssertionError();
		}
	}

	public InputStream convertX2Y(DefinitionsChildId id) {
		Path path = Utils.getTmpDir(Paths.get(id.getQName().getLocalPart()));
		convertX2Y(repository.getDefinitions(id), path);
		return Utils.zipPath(path);
	}

	public Map<File, TServiceTemplate> convertX2Y(Definitions definitions, Path outPath) {
		return new X2YConverter(this.repository, outPath).convert(definitions, outPath);
	}
}
