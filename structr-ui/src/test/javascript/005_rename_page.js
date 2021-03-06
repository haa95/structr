/*
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
var s = require('../setup'),
	login = require('../templates/login'),
	createPage = require('../templates/createPage');

var testName = '005_rename_page';
var heading = "Rename Page", sections = [];
var desc = "This animation shows how a new page can be renamed.";
var numberOfTests = 4;
var renamedPageName = 'renamed-page';

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

	casper.start(s.url);

	sections.push('<a href="004_create_page_test.html">Login and create a page.</a>');

	login.init(test, 'admin', 'admin');

	createPage.init(test, 'test-page');

	sections.push('You can rename a page by simply clicking on the name on the preview tab. After entering a new name, press return or tab, or click outside the input field.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#previewTabs li.page.active", wait: 0});
	});

	casper.waitForSelector('#previewTabs li input.new-name', function() {
		s.animatedType(this, '#previewTabs li input.new-name', false, renamedPageName, true);
	});

	casper.wait(1000);

	casper.then(function() {
		test.assertSelectorHasText('#previewTabs li.page.active .name_', renamedPageName);
	});

	casper.wait(1000);

	casper.then(function() {
		s.animateHtml(testName, heading, sections);
	});

	casper.run();

});