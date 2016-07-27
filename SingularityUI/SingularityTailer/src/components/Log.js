import React from 'react';
import { connect } from 'react-redux';
import 'react-virtualized/styles.css';
import '../styles/index.scss';
import Anser from 'anser';

import classNames from 'classnames';


import { AutoSizer, InfiniteLoader, VirtualScroll } from 'react-virtualized';

import { sandboxGetLength, sandboxFetchChunk } from '../actions';

import connectToTailer from './connectToTailer';

const Line = ({data, isScrolling}) => {
  if (data.isMissingMarker) {
    const missingBytes = data.end - data.start;
    return <div style={{backgroundColor: '#ddd'}} key={`${data.start}-${data.end}`}>{missingBytes} bytes</div>;
  }

  const ansiStyled = Anser.ansiToJson(data.text, {use_classes: true}).map((p, i) => {
    const { content, fg, bg, decoration } = p;
    const classes = classNames(
      fg,
      bg ? `${bg}-bg` : undefined,
      decoration ? `ansi-${decoration}` : undefined
    );
    return <span key={i} className={classes}>{content}</span>;
  });

  return <div key={`${data.start}-${data.end}`}>{ansiStyled}</div>;
};

const Log = ({id, data, fetchLength, fetchChunk}) => {
  let maybeLog;

  const overscanRowCount = 100;

  const isRowLoaded = ({index}) => {
    return index < data.lines.size && !data.lines.get(index).isMissingMarker;
  };

  const loadMoreRows = ({startIndex, stopIndex}) => {
    console.log('loadMoreRows', startIndex, stopIndex);
    let byteRangeStart;
    let byteRangeEnd;
    if (startIndex < data.lines.size) {
      byteRangeStart = data.lines.get(startIndex).start;
    } else {
      byteRangeStart = data.lines.last().end;
    }

    if (stopIndex < data.lines.size) {
      byteRangeEnd = data.lines.get(stopIndex).end;
    } else {
      byteRangeEnd = byteRangeStart + 65535;
    }

    return fetchChunk(byteRangeStart, byteRangeEnd);
  };

  const remoteRowCount = Math.max(
    Math.ceil((data && data.fileSize || 0) / 150),
    (data && data.fileSize)
  ); // real solid math

  if (data) {
    maybeLog = (
      <AutoSizer disableHeight>
        {({width}) => (
          <InfiniteLoader
            isRowLoaded={isRowLoaded}
            loadMoreRows={loadMoreRows}
            rowCount={remoteRowCount}
          >
            {({onRowsRendered, registerChild}) => (
              <VirtualScroll
                ref={registerChild}
                width={width}
                height={500}
                onRowsRendered={onRowsRendered}
                overscanRowCount={overscanRowCount}
                rowCount={data.lines.size}
                rowHeight={({index}) => 20}
                rowRenderer={({index, isScrolling}) => <Line data={data.lines.get(index)} />}
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
      <button onClick={() => fetchChunk(0, data.fileSize)}>
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
    <div style={{position: 'relative'}}>
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

const mapStateToProps = (state, ownProps) => ({
  data: ownProps.getTailerState(state).files[ownProps.id]
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchLength: () => dispatch(sandboxGetLength(ownProps.id)),
  fetchChunk: (start, end) => dispatch(
    sandboxFetchChunk(ownProps.id, start, end)
  ),
});

export default connectToTailer(connect(
  mapStateToProps,
  mapDispatchToProps
)(Log));
