import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import 'react-virtualized/styles.css';
import '../styles/index.scss';

import { AutoSizer, InfiniteLoader, VirtualScroll } from 'react-virtualized';

import { sandboxGetLength, sandboxFetchChunk } from '../actions';
import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

const Line = ({data, isScrolling}) => {
  if (data.isMissingMarker) {
    const missingBytes = data.end - data.start;
    return <div style={{backgroundColor: '#ddd'}} key={`${data.start}-${data.end}`}>{missingBytes} bytes</div>;
  }

  if (data.ansi) {
    const ansiStyled = data.ansi.map((part, i) => (
      <span key={i} className={null || part.classes}>
        {part.content}
      </span>
    ));

    return <div key={`${data.start}-${data.end}`}>{ansiStyled}</div>;
  }

  return <div key={`${data.start}-${data.end}`}>{data.text}</div>;
};

Line.propTypes = {
  data: PropTypes.object.isRequired,
  isScrolling: PropTypes.bool
};

const Log = ({id, isLoaded, lines, fileSize, fetchLength, fetchChunk, config}) => {
  let maybeLog;

  const overscanRowCount = 100;

  const isRowLoaded = ({index}) => {
    return index < lines.size && !lines.get(index).isMissingMarker;
  };

  const loadMoreRows = ({startIndex, stopIndex}) => {
    let byteRangeStart;
    let byteRangeEnd;
    if (startIndex < lines.size) {
      byteRangeStart = lines.get(startIndex).start;
    } else {
      byteRangeStart = lines.last().end;
    }

    if (stopIndex < lines.size) {
      byteRangeEnd = lines.get(stopIndex).end;
    } else {
      byteRangeEnd = byteRangeStart + 65535;
    }

    return fetchChunk(byteRangeStart, byteRangeEnd);
  };

  const remoteRowCount = Math.max(
    Math.ceil((isLoaded && fileSize || 0) / 150),
    (isLoaded && fileSize || 0)
  ); // real solid math

  console.log(remoteRowCount);

  if (isLoaded) {
    maybeLog = (
      <AutoSizer>
        {({width, height}) => (
          <InfiniteLoader
            isRowLoaded={isRowLoaded}
            loadMoreRows={loadMoreRows}
            rowCount={remoteRowCount}
          >
            {({onRowsRendered, registerChild}) => (
              <VirtualScroll
                ref={registerChild}
                width={width}
                height={height}
                tabIndex={null}
                onRowsRendered={onRowsRendered}
                overscanRowCount={overscanRowCount}
                rowCount={lines.size}
                rowHeight={({index}) => 14}
                rowRenderer={({index, isScrolling}) => <Line data={lines.get(index)} />}
                rowClassName="log-row"
              />
            )}
          </InfiniteLoader>
        )}
      </AutoSizer>
    );
  }

  const buttons = (
    <div>
      <button onClick={() => fetchLength()}>
        load length
      </button>
      <button onClick={() => fetchChunk(0, fileSize)}>
        load whole
      </button>
      <button onClick={() => {
        const intervalId = setInterval(() => {
          const start = Math.floor(Math.random() * 65536);
          const length = Math.floor(Math.random() * 2000);
          fetchChunk(start, start + length);
        }, 50);
        setTimeout(() => clearInterval(intervalId), 10000);
      }}>
        load more
      </button>
    </div>
  );

  return (
    <div>
      <section className="log-view">
        <header>
          {id}
        </header>
        <div className="infinite-wrapper">
          {maybeLog}
        </div>
        <footer>
          {buttons}
        </footer>
      </section>
    </div>
  );
};

const mapStateToProps = (state, ownProps) => {
  const tailerState = ownProps.getTailerState(state);
  const file = tailerState.files[ownProps.id];

  const getLines = Selectors.makeGetEnhancedLines();

  return {
    isLoaded: !!file,
    fileSize: file && file.fileSize,
    lines: getLines(state, ownProps),
    config: tailerState.config
  };
};

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchLength: (config) => dispatch(sandboxGetLength(ownProps.id, config)),
  fetchChunk: (start, end, config) => dispatch(
    sandboxFetchChunk(ownProps.id, start, end, config)
  ),
});

const mergeProps = (stateProps, dispatchProps, ownProps) => {
  return {
    ...stateProps,
    ...ownProps,
    fetchLength: () => dispatchProps.fetchLength(stateProps.config),
    fetchChunk: (start, end) => dispatchProps.fetchChunk(start, end, stateProps.config),
  };
};

export default connectToTailer(connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(Log));
