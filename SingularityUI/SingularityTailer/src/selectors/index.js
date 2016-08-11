import { createSelector } from 'reselect';

import { List, Map, Range } from 'immutable';

import Anser from 'anser';
import classNames from 'classnames';

import { createMissingMarker } from '../reducers/files';

const ansiEnhancer = (line) => {
  return Anser.ansiToJson(
    line.text,
    { use_classes: true }
  ).map((p) => {
    const { content, fg, bg, decoration } = p;
    const classes = classNames({
      [`${fg}`]: !!fg,
      [`${bg}-bg`]: bg,
      [`ansi-${decoration}`]: decoration
    });

    return {
      content,
      classes
    };
  });
};

export const getFile = (state, props) => {
  return props.getTailerState(state).files[props.tailerId];
};

export const getIsLoaded = (state, props) => {
  return !!getFile(state, props);
};

export const getFileSize = (state, props) => {
  const isLoaded = getIsLoaded(state, props);
  return isLoaded
    ? getFile(state, props).fileSize
    : null;
};

export const getLines = (state, props) => {
  const file = getFile(state, props);
  if (file) {
    return file.lines;
  }
  return new List();
};

// experimental
export const getExpandedLines = (state, props) => {
  const file = getFile(state, props);
  if (file) {
    const unflattened = file.lines.map((l) => {
      if (l.isMissingMarker) {
        const chunks = Math.ceil(l.byteLength / 65535);
        return new Range(0, chunks).map((index) => {
          return createMissingMarker(
            l.start + (index * 65535),
            Math.min(l.end, l.start + ((index + 1) * 65535))
          );
        });
      }
      return new List().push(l);
    });
    return unflattened.flatten();
  }
  return new List();
};

export const getRequests = (state, props) => (
  props.getTailerState(state).requests[props.tailerId] || new Map()
);

export const getConfig = (state, props) => (
  props.getTailerState(state).config
);

const getEnhancedLine = (line, requests, config) => {
  const enhancedLine = {
    ...line
  };

  if (line.isMissingMarker) {
    enhancedLine.isLoading = requests.has(line.start);
  }

  // don't parse ansi for long lines
  // TODO: move magic number elsewhere
  if (config.parseAnsi && !line.isMissingMarker && line.byteLength < 1000) {
    enhancedLine.ansi = ansiEnhancer(line);
  }

  return enhancedLine;
};

export const makeGetEnhancedLines = () => {
  return createSelector(
    [getLines, getRequests, getConfig],
    (lines, requests, config) => {
      return lines.map((line) => getEnhancedLine(line, requests, config));
    }
  );
};

export const getScroll = (state, props) => (
  props.getTailerState(state).scroll[props.tailerId] || {}
);
