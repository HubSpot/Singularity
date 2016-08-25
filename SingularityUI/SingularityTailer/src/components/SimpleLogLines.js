import React, { PropTypes } from 'react';
import Immutable from 'immutable';

import Line from './Line';

import '../styles/ansi.scss';

const SimpleLogLines = (props) => {
  if (!props.isLoaded) {
    return <div>Not loaded</div>;
  }

  return (
    <div>
      <div style={{height: props.fakeLineCount * 14}} key="fakeLine">
        fake line.
      </div>
      {props.lines.map((data) => {
        return <Line key={`${data.start}-${data.end}`} data={data} />;
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
