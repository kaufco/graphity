// Adds to the generated Javadoc, under each class and method that a page of the guide uses, the
// links to those pages ("In the guide:"). The guide publishes which of its pages use which part of
// the API as api-pages.json; this is a step of deploying the Javadoc, after `javadoc:aggregate`, so
// that the sources and the Javadoc jar do not depend on the guide.
//
// Usage: node add-guide-links.mjs <javadoc-dir> <api-pages.json> [<guide-url>]
// <guide-url> replaces the address of the guide that api-pages.json names, for a local preview.
// An element of api-pages.json that the Javadoc does not have, as when the guide already uses an API
// newer than this release, is reported and left out.
import fs from 'node:fs';
import path from 'node:path';

const [javadoc, data, guideUrl] = process.argv.slice(2);
if (!javadoc || !data) {
	console.error('Usage: node add-guide-links.mjs <javadoc-dir> <api-pages.json> [<guide-url>]');
	process.exit(2);
}
const api = JSON.parse(fs.readFileSync(data, 'utf8'));
const guide = (guideUrl ?? api.guide).replace(/\/?$/, '/');
// The guide opens in the language of this documentation.
const language = 'java';

const escape = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

/** The entry for the notes of an element: a link to each page, with its number and title. */
function notesEntry(pages) {
	const items = pages.map((p) => {
		const { number, title } = api.pages[p];
		return `<li><a href="${escape(`${guide}${p}?lang=${language}`)}">${escape(`${number} ${title}`)}</a></li>`;
	});
	return `<dt>In the guide:</dt>\n<dd><ul class="guide-links">\n${items.join('\n')}\n</ul></dd>\n`;
}

/**
 * Adds the entry to the section of an element: to the notes after its description, or in a list of
 * notes of its own at the end of the section. Returns the new text, or null if the element is not there.
 */
function addTo(html, anchor, entry) {
	const open = anchor === null ? '<section class="class-description" id="class-description">' : `<section class="detail" id="${escape(anchor)}">`;
	const start = html.indexOf(open);
	if (start < 0) return null;
	const end = html.indexOf('</section>', start);
	const section = html.slice(start, end);
	if (section.includes('<ul class="guide-links">')) return html;
	const block = Math.max(section.indexOf('<div class="block">'), 0);
	const notes = section.indexOf('<dl class="notes">', block);
	let at;
	if (notes >= 0) {
		at = start + section.indexOf('</dl>', notes);
	} else {
		const lastDiv = section.lastIndexOf('</div>');
		at = start + (section.slice(lastDiv).trim() === '</div>' ? lastDiv : section.length);
		entry = `<dl class="notes">\n${entry}</dl>\n`;
	}
	return html.slice(0, at) + entry + html.slice(at);
}

const byFile = new Map();
for (const [element, pages] of Object.entries(api.java ?? {})) {
	const [file, anchor] = element.split('#');
	if (!byFile.has(file)) byFile.set(file, []);
	byFile.get(file).push({ anchor: anchor ?? null, pages });
}

let linked = 0;
const missing = [];
for (const [file, elements] of byFile) {
	const target = path.join(javadoc, file);
	if (!fs.existsSync(target)) {
		missing.push(...elements.map((e) => `${file}${e.anchor === null ? '' : `#${e.anchor}`}`));
		continue;
	}
	let html = fs.readFileSync(target, 'utf8');
	for (const { anchor, pages } of elements) {
		const next = addTo(html, anchor, notesEntry(pages));
		if (next === null) missing.push(`${file}#${anchor}`);
		else if (next !== html) {
			html = next;
			linked++;
		}
	}
	fs.writeFileSync(target, html);
}
console.log(`Linked ${linked} classes and methods to the guide at ${guide}.`);
if (missing.length) console.log(`Not in this Javadoc, left out:\n  ${missing.join('\n  ')}`);
