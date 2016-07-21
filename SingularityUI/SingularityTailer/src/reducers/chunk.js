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

const initialState = {

};

const createMissingMarker = (start, end = undefined) => ({
  isMissingMarker: true,
  byteLength: (end !== undefined) ? (end - start) : undefined,
  start,
  end,
  hasNewline: false
});

/*
Lines can be (and usually are) incomplete, let's place the new lines in the
list of partial lines, combining lines where necessary.
*/
export const combinePartialLines = (existingPartialLines, newPartialLines) => {
  if (!newPartialLines.length) {
    return existingPartialLines;
  }

  // the first line and last line could be the same
  const firstLine = newPartialLines[0];
  const lastLine = newPartialLines[newPartialLines.length - 1];

  const firstOffset = firstLine.start;
  const lastOffset = lastLine.end;

  let combinedLines = [];

  // make sure the log has been initialized
  if (!existingPartialLines.length) {
    const afterMissingMarker = createMissingMarker(lastOffset);
    if (firstOffset !== 0) {
      // create a missing marker for the chunk before this
      const beforeMissingMarker = createMissingMarker(0, firstOffset);

      combinedLines = [
        beforeMissingMarker,
        ...newPartialLines,
        afterMissingMarker
      ];

      return combinedLines;
    }

    // looks like we're starting this file from the beginning
    combinedLines = [
      ...newPartialLines,
      afterMissingMarker
    ];

    return combinedLines;
  }

  // looks like we already have some of this file, let's merge these new lines in

  // search for the line/marker that contains our first offset
  const intersectIndex = existingPartialLines.findIndex((pl) => {
    return pl.start >= firstOffset;
  });

  if (intersectIndex === -1) {
    // I can't think of how this would happen, but it probably can
    console.error( // eslint-disable-line no-console
      'Assertion failed: intersectIndex === -1',
      existingPartialLines,
      newPartialLines
    );

    // bail
    return existingPartialLines;
  }

  // Okay, we have an intersection point
  const intersection = existingPartialLines[intersectIndex];
  if (intersection.hasOwnProperty('text')) {
    // this is a loaded piece, handle carefully \u{1F52B}

    // we want to replace the part that we have with the part
    // TODO: some of this

    // repeat intersection finding until we've exited this
  }
  // okay, this is a marker we're looking at, let's figure out if we need to
  // shrink it, remove it, or shrink it and put a new one at the end.
};

const chunkReducer = (state = initialState, action) => {

};

export default chunkReducer;

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
