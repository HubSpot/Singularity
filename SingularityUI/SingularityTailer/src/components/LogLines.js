import React, { PropTypes } from 'react';
import Immutable from 'immutable';
import { AutoSizer, InfiniteLoader, VirtualScroll } from 'react-virtualized';

import Line from './Line';

import '../styles/ansi.scss';

const LogLines = (props) => {
  if (!props.isLoaded) {
    return <div>Not loaded</div>;
  }

  return (
    <AutoSizer>
      {({width, height}) => ( // eslint-disable-line react/prop-types
        <InfiniteLoader
          isRowLoaded={props.isRowLoaded}
          loadMoreRows={props.loadMoreRows}
          rowCount={props.remoteRowCount}
        >
          {({onRowsRendered, registerChild}) => ( // eslint-disable-line react/prop-types
            <VirtualScroll
              ref={registerChild}
              width={width}
              height={height}
              tabIndex={null}
              onRowsRendered={onRowsRendered}
              onScroll={props.onScroll}
              overscanRowCount={props.overscanRowCount}
              rowCount={props.lines.size}
              rowHeight={14}
              rowRenderer={(rowProps) => (
                <Line data={props.lines.get(rowProps.index)} />
              )}
              rowClassName="log-row"
            />
          )}
        </InfiniteLoader>
      )}
    </AutoSizer>
  );
};

LogLines.propTypes = {
  isLoaded: PropTypes.bool.isRequired,
  lines: PropTypes.instanceOf(Immutable.List).isRequired,
  overscanRowCount: PropTypes.number,
  onScroll: PropTypes.func,
  isRowLoaded: PropTypes.func.isRequired,
  loadMoreRows: PropTypes.func.isRequired,
  remoteRowCount: PropTypes.number.isRequired
};

LogLines.defaultProps = {
  overscanRowCount: 100
};

export default LogLines;
