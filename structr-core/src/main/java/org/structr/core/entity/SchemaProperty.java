package org.structr.core.entity;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.relationship.SchemaNodeProperty;
import org.structr.core.entity.relationship.SchemaViewProperty;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.parser.NotionPropertyParser;
import org.structr.schema.parser.PropertyDefinition;

/**
 *
 * @author Christian Morgner
 */
public class SchemaProperty extends SchemaReloadingNode implements PropertyDefinition {

	public static final Property<AbstractSchemaNode> schemaNode        = new StartNode<>("schemaNode", SchemaNodeProperty.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));
	public static final Property<List<SchemaView>>   schemaViews       = new StartNodes<>("schemaViews", SchemaViewProperty.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));

	public static final Property<String>             declaringClass    = new StringProperty("declaringClass");
	public static final Property<String>             defaultValue      = new StringProperty("defaultValue");
	public static final Property<String>             propertyType      = new StringProperty("propertyType");
	public static final Property<String>             contentType       = new StringProperty("contentType");
	public static final Property<String>             dbName            = new StringProperty("dbName");
	public static final Property<String>             format            = new StringProperty("format");
	public static final Property<Boolean>            notNull           = new BooleanProperty("notNull");
	public static final Property<Boolean>            unique            = new BooleanProperty("unique");
	public static final Property<Boolean>            isDynamic         = new BooleanProperty("isDynamic");
	public static final Property<Boolean>            isBuiltinProperty = new BooleanProperty("isBuiltinProperty");
	public static final Property<Boolean>            isDefaultInUi     = new BooleanProperty("isDefaultInUi");
	public static final Property<Boolean>            isDefaultInPublic = new BooleanProperty("isDefaultInPublic");
	public static final Property<String>             contentHash       = new StringProperty("contentHash");

	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		name, dbName, schemaNode, schemaViews, propertyType, contentType, format, notNull, unique, defaultValue, isBuiltinProperty, declaringClass, isDynamic
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		name, dbName, schemaNode, schemaViews, propertyType, contentType, format, notNull, unique, defaultValue, isBuiltinProperty, declaringClass, isDynamic
	);

	public static final View schemaView = new View(SchemaProperty.class, "schema",
		name, dbName, schemaNode, schemaViews, propertyType, contentType, format, notNull, unique, defaultValue, isBuiltinProperty, isDefaultInUi, isDefaultInPublic, declaringClass, isDynamic
	);

	public static final View exportView = new View(SchemaMethod.class, "export",
		id, type, name, schemaNode, schemaViews, dbName, propertyType, contentType, format, notNull, unique, defaultValue, isBuiltinProperty, isDefaultInUi, isDefaultInPublic, declaringClass, isDynamic
	);

	private NotionPropertyParser notionPropertyParser = null;

	@Override
	public String getPropertyName() {
		return getProperty(name);
	}

	@Override
	public Type getPropertyType() {
		return Type.valueOf(getProperty(propertyType));
	}

	@Override
	public String getContentType() {
		return getProperty(contentType);
	}

	@Override
	public boolean isNotNull() {

		final Boolean isNotNull = getProperty(notNull);
		if (isNotNull != null && isNotNull) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isUnique() {

		final Boolean isUnique = getProperty(unique);
		if (isUnique != null && isUnique) {

			return true;
		}

		return false;
	}

	@Override
	public String getRawSource() {
		return "";
	}

	@Override
	public String getSource() {
		return "";
	}

	@Override
	public String getDbName() {
		return getProperty(dbName);
	}

	@Override
	public String getDefaultValue() {
		return getProperty(defaultValue);
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		// automatically add new property to the ui view..
		final AbstractSchemaNode parent = getProperty(SchemaProperty.schemaNode);
		if (parent != null) {

			for (final SchemaView view : parent.getProperty(AbstractSchemaNode.schemaViews)) {

				if (PropertyView.Ui.equals(view.getName())) {

					final Set<SchemaProperty> properties = new LinkedHashSet<>(view.getProperty(SchemaView.schemaProperties));

					properties.add(this);

					view.setProperty(SchemaView.schemaProperties, new LinkedList<>(properties));

					break;
				}
			}
		}

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		// prevent modification of properties using a content hash value
		if (getProperty(isBuiltinProperty) && !getContentHash().equals(getProperty(contentHash))) {
			throw new FrameworkException(403, "Modification of built-in properties not permitted.");
		}

		return super.onModification(securityContext, errorBuffer);
	}

	public String getContentHash() {

		int _contentHash = 77;

		_contentHash = addContentHash(defaultValue,      _contentHash);
		_contentHash = addContentHash(propertyType,      _contentHash);
		_contentHash = addContentHash(contentType,       _contentHash);
		_contentHash = addContentHash(dbName,            _contentHash);
		_contentHash = addContentHash(format,            _contentHash);
		_contentHash = addContentHash(notNull,           _contentHash);
		_contentHash = addContentHash(unique,            _contentHash);
		_contentHash = addContentHash(isDynamic,         _contentHash);
		_contentHash = addContentHash(isBuiltinProperty, _contentHash);
		_contentHash = addContentHash(isDefaultInUi,     _contentHash);
		_contentHash = addContentHash(isDefaultInPublic, _contentHash);

		return Integer.toHexString(_contentHash);
	}

	@Override
	public String getFormat() {

		String _format = getProperty(SchemaProperty.format);
		if (_format != null) {

			_format = _format.trim();
		}

		return _format;
	}

	public boolean isRequired() {
		return getProperty(SchemaProperty.notNull);
	}

	public String getSourceContentType() {

		final String source = getFormat();
		if (source != null) {

			if (source.startsWith("{") && source.endsWith("}")) {

				return "text/javascript";

			} else {

				return "text/structrscript";
			}
		}

		return "";
	}

	public Set<String> getEnumDefinitions() {

		final String _format    = getProperty(SchemaProperty.format);
		final Set<String> enums = new LinkedHashSet<>();

		if (_format != null) {

			for (final String source : _format.split("[, ]+")) {

				final String trimmed = source.trim();
				if (StringUtils.isNotBlank(trimmed)) {

					enums.add(trimmed);
				}
			}
		}

		return enums;
	}

	public boolean isPropertySetNotion() {
		return getNotionPropertyParser().isPropertySet();
	}

	public String getTypeReferenceForNotionProperty() {
		return getNotionPropertyParser().getValueType();

	}

	public Set<String> getPropertiesForNotionProperty() {

		final Set<String> properties = new LinkedHashSet<>();

		for (final String property : getNotionPropertyParser().getProperties()) {

			if (property.contains(".")) {

				final String[] parts = property.split("[.]+");
				if (parts.length > 1) {

					final String type = parts[0];
					final String name = parts[1];

					properties.add(name);
				}

			} else {

				properties.add(property);
			}
		}

		return properties;
	}

	public String getNotionBaseProperty() {
		return getNotionPropertyParser().getBaseProperty();
	}

	public String getNotionMultiplicity() {
		return getNotionPropertyParser().getMultiplicity();
	}

	// ----- private methods -----
	private int addContentHash(final PropertyKey key, final int contentHash) {

		final Object value = getProperty(key);
		if (value != null) {

			return contentHash ^ value.hashCode();
		}

		return contentHash;
	}

	private NotionPropertyParser getNotionPropertyParser() {

		if (notionPropertyParser == null) {

			try {
				notionPropertyParser = new NotionPropertyParser(new ErrorBuffer(), getName(), this);
				notionPropertyParser.getPropertySource(new StringBuilder(), getProperty(SchemaProperty.schemaNode));

			} catch (FrameworkException fex) {

				fex.printStackTrace();
			}
		}

		return notionPropertyParser;
	}
}