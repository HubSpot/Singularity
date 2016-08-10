import React, { PropTypes } from 'react';
import Immutable from 'immutable';
import { AutoSizer, VirtualScroll } from 'react-virtualized';

import TailerInfiniteLoader from './TailerInfiniteLoader';

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
        <TailerInfiniteLoader
          isLineLoaded={props.isLineLoaded}
          loadLines={props.loadLines}
          tailLog={props.tailLog}
          isTailing={props.isTailing}
          useOverscan={true}
        >
          {({onRowsRendered}) => ( // eslint-disable-line react/prop-types
            <VirtualScroll
              width={width}
              height={height}
              tabIndex={null}
              onRowsRendered={onRowsRendered}
              overscanRowCount={props.overscanRowCount}
              rowCount={props.lines.size}
              rowHeight={14}
              rowRenderer={rowRenderer}
            />
          )}
        </TailerInfiniteLoader>
      )}
    </AutoSizer>
  );
};

LogLines.propTypes = {
  isLoaded: PropTypes.bool.isRequired,
  lines: PropTypes.instanceOf(Immutable.List).isRequired,
  isLineLoaded: PropTypes.func.isRequired,
  loadLines: PropTypes.func.isRequired,
  overscanRowCount: PropTypes.number,
  isTailing: PropTypes.func.isRequired,
  tailLog: PropTypes.func.isRequired
};

LogLines.defaultProps = {
  overscanRowCount: 100
};

export default LogLines;
