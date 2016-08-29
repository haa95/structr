/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.property;

import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NumberToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;

/**
 * A property that stores and retrieves a simple Double value.
 *
 *
 */
public class DoubleProperty extends AbstractPrimitiveProperty<Double> implements NumericalPropertyKey<Double> {

	public DoubleProperty(final String name) {
		this(name, name, null);
	}

	public DoubleProperty(final String jsonName, final String dbName) {
		this(jsonName, dbName, null);
	}

	public DoubleProperty(final String name, final PropertyValidator<Double>... validators) {
		this(name, name, null, validators);
	}

	public DoubleProperty(final String name, final Double defaultValue, final PropertyValidator<Double>... validators) {
		this(name, name, defaultValue, validators);
	}

	public DoubleProperty(final String jsonName, final String dbName, final Double defaultValue, final PropertyValidator<Double>... validators) {

		super(jsonName, dbName, defaultValue);

		if (jsonName.equals("latitude") || jsonName.equals("longitude")) {

			// add layer node index and make
			// this property be indexed at the
			// end of the transaction instead
			// of on setProperty
			passivelyIndexed();
		}

		for (PropertyValidator<Double> validator : validators) {
			addValidator(validator);
		}
	}

	@Override
	public String typeName() {
		return "Double";
	}

	@Override
	public Class valueType() {
		return Double.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Double;
	}

	@Override
	public PropertyConverter<Double, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Double, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Double> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Double convertToNumber(Double source) {
		return source;
	}

	protected class InputConverter extends PropertyConverter<Object, Double> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext);
		}

		@Override
		public Object revert(Double source) throws FrameworkException {

			if (source == null) {
				return null;
			}

			final boolean lenient = Boolean.parseBoolean(StructrApp.getConfigurationValue("json.lenient", "false"));

			if (!lenient) {

				if (Double.isNaN(source)) {
					return null;
				}

				if (Double.isInfinite(source)) {
					return null;
				}

			}

			return source;
		}

		@Override
		public Double convert(Object source) throws FrameworkException {

			if (source == null) {
				return null;
			}

			if (source instanceof Number) {

				return ((Number) source).doubleValue();

			}

			if (source instanceof String && StringUtils.isNotBlank((String) source)) {

				try {
					return Double.valueOf(source.toString());

				} catch (Throwable t) {

					throw new FrameworkException(422, "Cannot parse input for property " + jsonName(), new NumberToken(declaringClass.getSimpleName(), DoubleProperty.this));
				}
			}

			return null;
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof Double) {
				return value;
			}

			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}

			try {

				return Double.parseDouble(value.toString());

			} catch (Throwable t) {

				// no chance, give up..
			}
		}

		return null;
	}

	@Override
	public void index(GraphObject entity, Object value) {
		super.index(entity, fixDatabaseProperty(value));
	}

	// ----- CMIS support -----
	@Override
	public PropertyType getDataType() {
		return PropertyType.DECIMAL;
	}
}
