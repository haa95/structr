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
package org.structr.core.function;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class TrimFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_TRIM = "Usage: ${trim(string)}. Example: ${trim(this.text)}";

	@Override
	public String getName() {
		return "trim()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (!arrayHasLengthAndAllElementsNotNull(sources, 1)) {

				return null;
			}

			if (sources[0] instanceof Collection) {

				final List<String> cleanList = new LinkedList<>();

				for (final Object obj : (Collection)sources[0]) {

					if (StringUtils.isBlank(obj.toString())) {

						cleanList.add("");

					} else {

						cleanList.add(obj.toString().trim());
					}
				}

				return cleanList;
			}

			if (StringUtils.isBlank(sources[0].toString())) {
				return "";
			}

			return sources[0].toString().trim();

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_TRIM;
	}

	@Override
	public String shortDescription() {
		return "Removes whitespace at the edges of the given string";
	}

}
