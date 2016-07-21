import { TextEncoder } from 'text-encoding'; // polyfill

const TE = new TextEncoder();

// see big comment at bottom of file for perf info
export const splitChunkIntoLines = (chunk) => {
  const { data, offset, length } = chunk;

  const lines = data.split('\n');
  const byteLengths = lines.map((line) => TE.encode(line).byteLength);

  const partialLines = [];

  let currentOffset = offset;
  lines.forEach((line, i) => {
    const hasNewline = i !== lines.length - 1;
    // add newline byte
    const lineLength = byteLengths[i] + (hasNewline ? 1 : 0);
    partialLines[i] = {
      text: line,
      byteLength: lineLength,
      start: currentOffset,
      end: currentOffset += lineLength,
      hasNewline
    };
  });

  return partialLines;
};

/*
Justifying the algorithm used:

hipster text actual byte length: 2399 (generated at: http://hipsum.co/?paras=4&type=hipster-centric)

Speed tests:

benchmark = (f) => { then = Date.now(); for(let i=0; i<100000; i++) { f() } console.log(Date.now() - then); }
const te = new TextEncoder();

Byte count
----------

TextEncoder:
benchmark(() => { te.encode(hipster).byteLength; });
-> 1216

byteCount:
function byteCount(s) {
  return encodeURI(s).split(/%..|./).length - 1;
}
benchmark(() => { byteCount(hipster); });
-> 19114

For small strings, byteCount is way faster, because it doesn't need to make a Uint8Array
For big strings, TextEncoder is way faster, because it's not a hack.

Split
-----

Uint8Array:
benchmark(() => {
  byteArray.reduce((newLines, b, index) => ((b === 10) ? newLines.concat([index]) : newLines), [])
});
-> 13624

let newLines;
benchmark(() => {
  newLines = []; byteArray.forEach((b, index) => { (b === 10) && newLines.push(index); });
});
-> 23690

String split:
benchmark(() => { hipster.split('\n') });
-> 19


Combination
-----------
String split then byte count with byteCount:
benchmark(() => hipster.split("p").map((l) => byteCount(l)))
-> 23771

benchmark(() => hipster.split("p").map((l) => te.encode(l).byteLength))
-> 8267
*/
