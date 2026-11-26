import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const pagePath = resolve(process.cwd(), 'yaya.html');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const html = await readFile(pagePath, 'utf8');

assert(html.includes('<title>给牙牙姐的一封小信</title>'), '缺少页面标题');
assert(html.includes('class="letter-shell"'), '缺少信件主体容器');
assert(html.includes('今天先别逼自己'), '缺少停靠点文案');
assert(html.includes('署名'), '缺少结尾区域');
assert(html.includes('虽然我未必知道你今天具体经历了什么'), '缺少核心安慰文案');
assert(html.includes('@media (max-width: 640px)'), '缺少移动端适配');
assert(html.includes('@keyframes drift'), '缺少轻微动态效果');

console.log('yaya.html passed content checks');
