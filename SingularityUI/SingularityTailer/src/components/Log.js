import React from 'react';
import { connect } from 'react-redux';

import { sandboxGetLength, sandboxFetchChunk } from '../actions';

import connectToTailer from './connectToTailer';

const Log = ({id, data, fetchLength, fetchChunk}) => {
  let maybeLog;
  if (data) {
    const logLines = data.lines.map((l) => {
      return <p key={l.start}>{l.text}</p>;
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
      <button onClick={() => fetchChunk((data && data.fileSize || 0), (data && data.fileSize || 0) + 100)}>
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
