// Adds to the start page of the generated Javadoc a sidebar with its sections and packages, as the
// pages of the packages and classes have one. javadoc writes none for the overview and has no option
// for it. This is a step of deploying the Javadoc, after `javadoc:aggregate`, so that the Javadoc jar
// stays as javadoc generates it.
//
// Usage: node add-start-page-contents.mjs <javadoc-dir>
import fs from 'node:fs';
import path from 'node:path';

const [javadoc] = process.argv.slice(2);
if (!javadoc) {
	console.error('Usage: node add-start-page-contents.mjs <javadoc-dir>');
	process.exit(2);
}
const file = path.join(javadoc, 'index.html');
const html = fs.readFileSync(file, 'utf8');
if (html.includes('class="toc"')) {
	console.log('The start page already has a sidebar.');
	process.exit(0);
}

const sections = [...html.matchAll(/<h1 id="([^"]+-heading)">([^<]+)<\/h1>/g)].map(([, id, title]) => ({ href: `#${id}`, title }));
const packages = [...html.matchAll(/<a href="([^"]+\/package-summary\.html)">([^<]+)<\/a><\/div>/g)].map(([, href, title]) => ({ href, title }));
const grid = '<div class="main-grid">\n';
if (sections.length === 0 || packages.length === 0 || !html.includes(grid)) {
	console.error(`${file} does not look as expected: ${sections.length} sections, ${packages.length} packages.`);
	process.exit(1);
}

const item = ({ href, title }, list = '') => `<li><a href="${href}" tabindex="0">${title}</a>${list}</li>`;
const list = (items) => `<ol class="toc-list" tabindex="-1">\n${items.join('\n')}\n</ol>`;
const nested = `\n<ol class="toc-list">\n${packages.map((p) => item(p)).join('\n')}\n</ol>\n`;
const entries = sections.map((s, i) => item(s, i === sections.length - 1 ? nested : ''));
const nav = `<nav role="navigation" class="toc" aria-label="Table of contents">
<div class="toc-header">Contents</div>
${list(entries)}
<button class="hide-sidebar"><span>Hide sidebar&nbsp;</span><img src="resource-files/left.svg" alt="Hide sidebar"></button><button class="show-sidebar"><img src="resource-files/right.svg" alt="Show sidebar"><span>&nbsp;Show sidebar</span></button></nav>
`;
fs.writeFileSync(file, html.replace(grid, grid + nav));
console.log(`Added a sidebar with ${sections.length} sections and ${packages.length} packages to the start page.`);
