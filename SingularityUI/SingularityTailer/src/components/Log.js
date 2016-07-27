import React from 'react';
import { connect } from 'react-redux';

import { sandboxGetLength, sandboxFetchChunk } from '../actions';

import connectToTailer from './connectToTailer';

const Log = ({id, data, fetchLength, fetchChunk}) => {
  let maybeLog;
  if (data) {
    const logLines = data.lines.map((l) => {
      if (l.isMissingMarker) {
        const approxLines = Math.ceil((l.end - l.start) / 120); // this is an opinion
        return <div style={{height: 1 * 20, backgroundColor: '#ddd'}} key={`${l.start}-${l.end}`}>{approxLines}</div>;
      }
      return <div style={{fontSize: 5, backgroundColor: '#fff'}} key={`${l.start}-${l.end}`}>{l.text}</div>;
    });
    maybeLog = (
      <div>
        {logLines}
        {data.fileSize}
      </div>
    );
  } else {
    maybeLog = <div>Log not loaded</div>;
  }
  return (
    <div className="log-view">
      {id}
      <button onClick={() => fetchLength()}>
        load length
      </button>
      {maybeLog}
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
};

const mapStateToProps = (state, ownProps) => ({
  data: ownProps.getTailerState(state).files[ownProps.id]
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchLength: () => dispatch(sandboxGetLength(ownProps.id)),
  fetchChunk: (start, end) => dispatch(
    sandboxFetchChunk(ownProps.id, start, end)
  )
});

export default connectToTailer(connect(
  mapStateToProps,
  mapDispatchToProps
)(Log));
