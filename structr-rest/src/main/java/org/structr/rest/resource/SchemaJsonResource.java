/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import java.net.URISyntaxException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaHelper;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonSchema;

public class SchemaJsonResource extends Resource {
	private static final String resourceIdentifier = "_schemaJson";
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SchemaResource.class.getName());
	private String uriPart = null;

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return null;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;

		if (part.equals(resourceIdentifier)) {
			this.uriPart = part;
			return true;
		}

		return false;
	}

	@Override
	public String getUriPart() {
		return this.uriPart;
	}

	@Override
	public Class<? extends GraphObject> getEntityClass() {
		return null;
	}

	@Override
	public String getResourceSignature() {
		return SchemaHelper.normalizeEntityName(getUriPart());
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {
		return false;
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException {
		final GraphObjectMap schema = new GraphObjectMap();
		int resultCount = 0;

		try {
			final JsonSchema jsonSchema = StructrSchema.createFromDatabase(StructrApp.getInstance());
			schema.setProperty(new StringProperty("schema"), jsonSchema.toString());
			resultCount = 1;
		} catch (URISyntaxException ex) {
			logger.error("Error while creating JsonSchema: " + ex.getMessage());
		}

		Result res = new Result(schema, true);
		res.setRawResultCount(resultCount);
		return res;

	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		if(propertySet != null && propertySet.containsKey("schema")) {
			final App app = StructrApp.getInstance(securityContext);
			String schemaJson = (String)propertySet.get("schema");
			try {
				StructrSchema.replaceDatabaseSchema(app, StructrSchema.createFromSource(schemaJson));
			} catch (URISyntaxException | InvalidSchemaException ex) {
				logger.error("Error while importing JsonSchema: " + ex.getMessage());
			}
			return new RestMethodResult(200, "Schema import started");
		}
		return new RestMethodResult(400, "Invalid request body. Specify schema json string as 'schema' in request body.");
	}

}