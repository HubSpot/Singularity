import React, { PropTypes } from 'react';
import Immutable from 'immutable';
import { AutoSizer, VirtualScroll } from 'react-virtualized';

import Line from './Line';

import '../styles/ansi.scss';

const SimpleLogLines = (props) => {
  if (!props.isLoaded) {
    return <div>Not loaded</div>;
  }

  const rowRenderer = (rowProps) => (
    <Line data={props.lines.get(rowProps.index)} />
  );

  return (
    <div>
      {props.lines.map((l) => {
        return <Line data={l} />;
      })}
    </div>
  );
};

SimpleLogLines.propTypes = {
  isLoaded: PropTypes.bool.isRequired,
  lines: PropTypes.instanceOf(Immutable.List).isRequired,
  onRowsRendered: PropTypes.func,
  overscanRowCount: PropTypes.number
};

SimpleLogLines.defaultProps = {
  overscanRowCount: 100
};

export default SimpleLogLines;
