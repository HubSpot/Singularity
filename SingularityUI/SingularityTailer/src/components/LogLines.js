import React, { PropTypes } from 'react';
import Immutable from 'immutable';
import { AutoSizer, VirtualScroll } from 'react-virtualized';

import Line from './Line';

import '../styles/ansi.scss';

const LogLines = (props) => {
  if (!props.isLoaded) {
    return <div>Not loaded</div>;
  }

  const rowRenderer = (rowProps) => (
    <Line data={props.lines.get(rowProps.index)} />
  );

  return (
    <AutoSizer>
      {({width, height}) => ( // eslint-disable-line react/prop-types
        <VirtualScroll
          width={width}
          height={height}
          tabIndex={null}
          onRowsRendered={props.onRowsRendered}
          overscanRowCount={props.overscanRowCount}
          rowCount={props.lines.size}
          rowHeight={({index}) => {
            const line = props.lines.get(index);
            if (line.isMissingMarker) {
              return 80 * 14;
              // return Math.ceil(line.byteLength / 250) * 14;
            }
            return 14;
          }}
          rowRenderer={rowRenderer}
        />
      )}
    </AutoSizer>
  );
};

LogLines.propTypes = {
  isLoaded: PropTypes.bool.isRequired,
  lines: PropTypes.instanceOf(Immutable.List).isRequired,
  onRowsRendered: PropTypes.func,
  overscanRowCount: PropTypes.number
};

LogLines.defaultProps = {
  overscanRowCount: 100
};

export default LogLines;
