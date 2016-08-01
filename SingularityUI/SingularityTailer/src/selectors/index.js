import { createSelector } from 'reselect';

import { List } from 'immutable';

import Anser from 'anser';
import classNames from 'classnames';

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

const getLines = (state, props) => {
  const file = props.getTailerState(state).files[props.id];
  if (file) {
    return file.lines;
  }
  return new List();
};

const getConfig = (state, props) => (
  props.getTailerState(state).config
);

const getEnhancedLine = (line, config) => {
  const enhancedLine = {
    ...line
  };

  if (config.parseAnsi && !line.isMissingMarker) {
    enhancedLine.ansi = ansiEnhancer(line);
  }

  return enhancedLine;
};

export const makeGetEnhancedLines = () => {
  return createSelector(
    [getLines, getConfig],
    (lines, config) => {
      return lines.map((line) => getEnhancedLine(line, config));
    }
  );
};
