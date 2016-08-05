import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable from 'immutable';

import { sandboxFetchChunk, sandboxFetchLength } from '../actions';
import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

const sandboxId = (Wrapped, taskId, path) => {
  return (
    <Wrapped
      tailerId={`${taskId}/${path}`}
      taskId={taskId}
      path={path}
    />
  );
};

const sandboxTailer = (WrappedLog) => {
  class SandboxTailer extends Component {
    constructor() {
      super();

      this.initializeFile = this.initializeFile.bind(this);
      this.loadLines = this.loadLines.bind(this);
      this.tailLog = this.tailLog.bind(this);

      this.sandboxMaxBytes = 65535;
    }

    initializeFile() {
      this.props.fetchLength();
    }

    loadLines(startIndex, stopIndex) {
      const { lines, fetchChunk } = this.props;

      let byteRangeStart;
      let byteRangeEnd;
      if (startIndex < lines.size) {
        byteRangeStart = lines.get(startIndex).start;
      } else if (lines.size === 0) {
        byteRangeStart = 0;
      } else {
        byteRangeStart = lines.last().end;
      }

      if (stopIndex < lines.size) {
        byteRangeEnd = lines.get(stopIndex).end;
      } else {
        byteRangeEnd = byteRangeStart + this.sandboxMaxBytes;
      }

      // if already in flight, don't request again
      if (!this.props.requests.has(byteRangeStart)) {
        fetchChunk(byteRangeStart, byteRangeEnd);
      }
    }

    tailLog() {

    }

    render() {
      return (
        <WrappedLog
          tailerId={this.props.tailerId}
          initializeFile={this.initializeFile}
          loadLines={this.loadLines}
          tailLog={this.tailLog}
        />
      );
    }
  }

  SandboxTailer.propTypes = {
    tailerId: PropTypes.string.isRequired,
    taskId: PropTypes.string.isRequired,
    path: PropTypes.string.isRequired,
    lines: PropTypes.instanceOf(Immutable.List).isRequired,
    requests: PropTypes.instanceOf(Immutable.Map).isRequired,
    fetchChunk: PropTypes.func.isRequired
  };

  const mapStateToProps = (state, ownProps) => ({
    isLoaded: Selectors.getIsLoaded(state, ownProps),
    fileSize: Selectors.getFileSize(state, ownProps),
    lines: Selectors.getLines(state, ownProps),
    requests: Selectors.getRequests(state, ownProps),
    config: Selectors.getConfig(state, ownProps)
  });

  const mapDispatchToProps = (dispatch, ownProps) => ({
    fetchLength: (config) => dispatch(
      sandboxFetchLength(
        ownProps.tailerId,
        ownProps.taskId,
        ownProps.path,
        config
      )
    ),
    fetchChunk: (start, end, config) => dispatch(
      sandboxFetchChunk(
        ownProps.tailerId,
        ownProps.taskId,
        ownProps.path,
        start,
        end,
        config
      )
    )
  });

  const mergeProps = (stateProps, dispatchProps, ownProps) => ({
    ...stateProps,
    ...ownProps,
    fetchLength: () => dispatchProps.fetchLength(stateProps.config),
    fetchChunk: (start, end) => dispatchProps.fetchChunk(
      start,
      end,
      stateProps.config
    ),
  });

  return connectToTailer(connect(
    mapStateToProps,
    mapDispatchToProps,
    mergeProps
  )(SandboxTailer));
};

export default (c, taskId, path) => () => sandboxId(
  sandboxTailer(c),
  taskId,
  path
);
