# -*- coding: utf-8 -*-
import sys, re, html

def inline(s):
    s = html.escape(s)
    # inline code
    s = re.sub(r'`([^`]+)`', r'<code>\1</code>', s)
    # bold
    s = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', s)
    # links [text](url)
    s = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', s)
    return s

def is_table_sep(line):
    cells = [c.strip() for c in line.strip().strip('|').split('|')]
    return len(cells) > 0 and all(re.fullmatch(r':?-{2,}:?', c) for c in cells if c != '') and any(c for c in cells)

def convert(md):
    lines = md.split('\n')
    out = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        stripped = line.strip()

        # fenced code block ```...```
        if stripped.startswith('```'):
            i += 1
            buf = []
            while i < n and not lines[i].strip().startswith('```'):
                buf.append(html.escape(lines[i]))
                i += 1
            i += 1  # skip closing fence
            out.append('<pre><code>' + '\n'.join(buf) + '</code></pre>')
            continue

        # horizontal rule
        if re.fullmatch(r'-{3,}', stripped):
            out.append('<hr/>')
            i += 1
            continue

        # headings
        m = re.match(r'(#{1,6})\s+(.*)', stripped)
        if m:
            level = len(m.group(1))
            out.append(f'<h{level}>{inline(m.group(2))}</h{level}>')
            i += 1
            continue

        # table: current line starts with | and next line is separator
        if stripped.startswith('|') and i + 1 < n and is_table_sep(lines[i+1]):
            header = [c.strip() for c in stripped.strip('|').split('|')]
            out.append('<table>')
            out.append('<thead><tr>' + ''.join(f'<th>{inline(c)}</th>' for c in header) + '</tr></thead>')
            out.append('<tbody>')
            i += 2
            while i < n and lines[i].strip().startswith('|'):
                row = [c.strip() for c in lines[i].strip().strip('|').split('|')]
                out.append('<tr>' + ''.join(f'<td>{inline(c)}</td>' for c in row) + '</tr>')
                i += 1
            out.append('</tbody></table>')
            continue

        # blockquote (consecutive > lines)
        if stripped.startswith('>'):
            buf = []
            while i < n and lines[i].strip().startswith('>'):
                buf.append(inline(re.sub(r'^\s*>\s?', '', lines[i])))
                i += 1
            out.append('<blockquote>' + '<br/>'.join(buf) + '</blockquote>')
            continue

        # unordered list (- or * , incl checkboxes)
        if re.match(r'[-*]\s+', stripped):
            out.append('<ul>')
            while i < n and re.match(r'[-*]\s+', lines[i].strip()):
                item = re.sub(r'^[-*]\s+', '', lines[i].strip())
                item = re.sub(r'^\[ \]\s*', '☐ ', item)
                item = re.sub(r'^\[x\]\s*', '☑ ', item, flags=re.I)
                out.append(f'<li>{inline(item)}</li>')
                i += 1
            out.append('</ul>')
            continue

        # ordered list
        if re.match(r'\d+\.\s+', stripped):
            out.append('<ol>')
            while i < n and re.match(r'\d+\.\s+', lines[i].strip()):
                item = re.sub(r'^\d+\.\s+', '', lines[i].strip())
                out.append(f'<li>{inline(item)}</li>')
                i += 1
            out.append('</ol>')
            continue

        # blank line
        if stripped == '':
            i += 1
            continue

        # paragraph
        out.append(f'<p>{inline(stripped)}</p>')
        i += 1

    return '\n'.join(out)

CSS = """
body { font-family: 'Calibri','Segoe UI',sans-serif; font-size: 11pt; line-height: 1.4; color: #1a1a1a; }
h1 { font-family: 'Cambria','Georgia',serif; font-size: 22pt; color: #1F5C3F; border-bottom: 2px solid #1F5C3F; padding-bottom: 4pt; }
h2 { font-family: 'Cambria','Georgia',serif; font-size: 16pt; color: #1F5C3F; margin-top: 18pt; }
h3 { font-family: 'Cambria','Georgia',serif; font-size: 13pt; color: #2C6E49; margin-top: 12pt; }
p { margin: 6pt 0; }
ul, ol { margin: 6pt 0; }
li { margin: 2pt 0; }
code { font-family: 'Consolas','Courier New',monospace; font-size: 10pt; background: #f2f4f3; padding: 0 2px; }
pre { font-family: 'Consolas','Courier New',monospace; font-size: 9.5pt; background: #f2f4f3; border: 1px solid #d8e0db; border-left: 3px solid #2C6E49; padding: 6pt 10pt; margin: 8pt 0; white-space: pre-wrap; word-break: break-word; line-height: 1.3; }
pre code { background: none; padding: 0; font-size: 9.5pt; }
blockquote { border-left: 3px solid #2C6E49; margin: 8pt 0; padding: 4pt 12pt; background: #f5f9f6; }
table { border-collapse: collapse; width: 100%; margin: 8pt 0; font-size: 10.5pt; }
th { background: #1F5C3F; color: #ffffff; border: 1px solid #1F5C3F; padding: 4pt 6pt; text-align: left; }
td { border: 1px solid #b8c9bf; padding: 4pt 6pt; vertical-align: top; }
hr { border: none; border-top: 1px solid #cccccc; margin: 12pt 0; }
strong { color: #14301f; }
"""

if __name__ == '__main__':
    src, dst = sys.argv[1], sys.argv[2]
    with open(src, encoding='utf-8') as f:
        md = f.read()
    body = convert(md)
    doc = f"""<!DOCTYPE html>
<html lang="ru"><head><meta charset="utf-8"><style>{CSS}</style></head>
<body>{body}</body></html>"""
    with open(dst, 'w', encoding='utf-8') as f:
        f.write(doc)
    print('HTML written:', dst)
