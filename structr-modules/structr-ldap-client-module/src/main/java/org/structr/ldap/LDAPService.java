/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.ldap;

import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.StructrServices;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;

/**
 * The LDAP synchronization service. This is a system service that requires
 * superuser permissions.
 */
public class LDAPService extends Thread implements RunnableService {

	private static final Logger logger = LoggerFactory.getLogger(LDAPService.class.getName());

	public static final String CONFIG_KEY_UPDATE_INTERVAL = "ldap.updateInterval";
	public static final String CONFIG_KEY_LDAP_BINDDN     = "ldap.bindDn";
	public static final String CONFIG_KEY_LDAP_SECRET     = "ldap.secret";
	public static final String CONFIG_KEY_LDAP_HOST       = "ldap.host";
	public static final String CONFIG_KEY_LDAP_PORT       = "ldap.port";
	public static final String CONFIG_KEY_LDAP_SSL        = "ldap.useSsl";
	public static final String CONFIG_KEY_LDAP_BASEDN     = "ldap.baseDn";
	public static final String CONFIG_KEY_LDAP_FILTER     = "ldap.filter";
	public static final String CONFIG_KEY_LDAP_SCOPE      = "ldap.scope";

	private long updateInterval = 1800;		// completely arbitrary update interval, set your own in structr.conf!
	private String host         = "localhost";
	private String binddn       = null;
	private String secret       = null;
	private String baseDn       = null;
	private String filter       = null;
	private String scope        = null;
	private boolean useSsl      = true;
	private boolean doRun       = false;
	private int port            = 389;

	public LDAPService() {

		super("Structr LDAP Service");
		this.setDaemon(true);
	}

	// ----- public methods -----
	public String fetchObjectInfo(final String dn) {

		final LdapConnection connection = new LdapNetworkConnection(host, port, useSsl);
		final StringBuilder buf         = new StringBuilder();

		if (connection != null) {

			try {
				if (connection.connect()) {

					if (StringUtils.isNotBlank(binddn) && StringUtils.isNotBlank(secret)) {

						connection.bind(binddn, secret);

					} else if (StringUtils.isNotBlank(binddn)) {

						connection.bind(binddn);
					}

					final EntryCursor cursor = connection.search(dn, "(objectclass=*)", SearchScope.OBJECT);
					while (cursor.next()) {

						buf.append(cursor.get());
						buf.append("\n");
					}

					cursor.close();

					connection.close();
				}

				connection.close();

			} catch (CursorException | LdapException | IOException ex) {
				logger.warn("", ex);
			}
		}

		return buf.toString();
	}

	public boolean canSuccessfullyBind(final String dn, final String secret) {

		final LdapConnection connection = new LdapNetworkConnection(host, port, useSsl);
		if (connection != null) {

			try {
				if (connection.connect()) {

					connection.bind(dn, secret);
					connection.unBind();
				}

				connection.close();

				return true;

			} catch (LdapException | IOException ex) {
				logger.warn("", ex);
			}
		}

		return false;
	}

	public void doUpdate() throws IOException, LdapException, CursorException, FrameworkException {

		final LdapConnection connection = new LdapNetworkConnection(host, port, useSsl);
		final App app                   = StructrApp.getInstance();

		if (connection != null) {

			// make connection persistent
			connection.setTimeOut(0);

			if (connection.connect()) {

				logger.info("Updating user/group information from LDAP server {}:{}..", new Object[]{host, port});

				if (StringUtils.isNotBlank(binddn) && StringUtils.isNotBlank(secret)) {

					connection.bind(binddn, secret);

				} else if (StringUtils.isNotBlank(binddn)) {

					connection.bind(binddn);
				}

				// step 1: fetch / update all users from LDAP server
				final EntryCursor cursor = connection.search(baseDn, filter, SearchScope.valueOf(scope));
				while (cursor.next()) {

					final Entry entry = cursor.get();
					synchronizeUserEntry(connection, entry);
				}

				// step 2: examine local users and refresh / remove
				try (final Tx tx = app.tx()) {

					for (final LDAPUser user : app.nodeQuery(LDAPUser.class).getAsList()) {

						final String dn = user.getDistinguishedName();
						if (dn != null) {

							final Entry userEntry = connection.lookup(dn);
							if (userEntry != null) {

								// update user information
								user.initializeFrom(userEntry);

							} else {

								logger.info("User {} doesn't exist in LDAP directory, deleting.", user);
								app.delete(user);
							}

						} else {

							logger.warn("User {} doesn't have an LDAP distinguished name, ignoring.", user);
						}
					}

					tx.success();
				}

				cursor.close();
				connection.close();

			} else {

				logger.info("Connection to LDAP server {} failed", host);
			}
		}
	}

	// ----- private methods -----
	private String synchronizeUserEntry(final LdapConnection connection, final Entry entry) {

		final PropertyKey<String> dnKey = StructrApp.key(LDAPUser.class, "distinguishedName");
		final App app                   = StructrApp.getInstance();
		final Dn dn                     = entry.getDn();
		final String dnString           = dn.toString();

		try (final Tx tx = app.tx()) {

			LDAPUser user = app.nodeQuery(LDAPUser.class).and(dnKey, dnString).getFirst();
			if (user == null) {

				user = app.create(LDAPUser.class, new NodeAttribute(dnKey, dnString));
				user.initializeFrom(entry);

				final String uuid = user.getUuid();
				if (user.getEntryUuid() == null) {

					try {
						// try to set "our" UUID in the remote database
						final Modification addUuid = new DefaultModification(ModificationOperation.ADD_ATTRIBUTE, "entryUUID", normalizeUUID(uuid));
						connection.modify(dn, addUuid);

					} catch (LdapException ex) {
						logger.warn("Unable to set entryUUID: {}", ex.getMessage());
					}
				}
			}

			tx.success();

			return user.getUuid();

		} catch (FrameworkException | LdapInvalidAttributeValueException fex) {
			logger.warn("Unable to update LDAP information", fex);
		}

		return null;
	}

	// ----- class Thread -----
	@Override
	public void run() {

		doRun = true;

		// wait for service layer to be fully initialized
		while (!Services.getInstance().isInitialized()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException itex) {
			}
		}

		while (doRun) {

			try {

				doUpdate();

			} catch (Throwable t) {
				logger.warn("Unable to update LDAP information: {}", t.getMessage());
			}

			// sleep until next update
			try { Thread.sleep(updateInterval * 1000); } catch (InterruptedException itex) { }
		}
	}

	// ----- interface RunnableService -----
	@Override
	public void startService() throws Exception {

		logger.info("Starting LDAPService, update interval {} s", updateInterval);
		this.start();
	}

	@Override
	public void stopService() {
		doRun = false;
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return doRun;
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		this.updateInterval = Settings.getOrCreateIntegerSetting(CONFIG_KEY_UPDATE_INTERVAL).getValue(1800);

		this.binddn         = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_BINDDN).getValue("");
		this.secret         = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_SECRET).getValue("");

		this.host           = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_HOST, "localhost").getValue();
		this.baseDn         = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_BASEDN, "ou=system").getValue();
		this.filter         = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_FILTER, "(objectclass=*)").getValue();
		this.scope          = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_SCOPE, "SUBTREE").getValue();

		this.port           = Settings.getOrCreateIntegerSetting(CONFIG_KEY_LDAP_PORT).getValue(339);
		this.useSsl         = Settings.getOrCreateBooleanSetting(CONFIG_KEY_LDAP_SSL).getValue(true);

		return true;
	}

	@Override
	public void shutdown() {
		doRun = false;
	}

	@Override
	public void initialized() {
	}

	@Override
	public boolean isVital() {
		return false;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "ldap-client";
	}

	// ----- private methods -----
	private String normalizeUUID(final String uuid) {

		final StringBuilder buf = new StringBuilder(uuid);

		buf.insert( 8, "-");
		buf.insert(13, "-");
		buf.insert(18, "-");
		buf.insert(23, "-");

		return buf.toString();
	}
}

