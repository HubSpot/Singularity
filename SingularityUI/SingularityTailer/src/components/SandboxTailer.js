import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable from 'immutable';

import { sandboxFetchChunk, sandboxFetchLength } from '../actions';
import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

import SimpleLog from './SimpleLog';

class SandboxTailer extends Component {
  constructor() {
    super();

    this.initializeFile = this.initializeFile.bind(this);
    this.loadLines = this.loadLines.bind(this);
    this.tailLog = this.tailLog.bind(this);

    this.sandboxMaxBytes = 65535;
  }

  initializeFile(atOffset) {
    this.props.fetchLength();

    if (atOffset !== undefined) {
      this.fetchSafe(atOffset, atOffset + this.sandboxMaxBytes);
    }
  }

  fetchSafe(byteRangeStart, byteRangeEnd) {
    const { fetchChunk } = this.props;
    // if already in flight, don't request again
    if (!this.props.requests.has(byteRangeStart)) {
      return fetchChunk(byteRangeStart, byteRangeEnd);
    }
    return undefined;
  }

  loadLines(startIndex, stopIndex, lines) {
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
      byteRangeEnd = Math.min(
        lines.get(stopIndex).end,
        byteRangeStart + this.sandboxMaxBytes
      );
    } else {
      byteRangeEnd = byteRangeStart + this.sandboxMaxBytes;
    }

    this.fetchSafe(byteRangeStart, byteRangeEnd);
  }

  tailLog(lines) {
    if (lines.size) {
      const lastLine = lines.last();
      this.fetchSafe(lastLine.end, lastLine.end + this.sandboxMaxBytes);
    } else {
      this.fetchSafe(0, this.sandboxMaxBytes);
    }
  }

  render() {
    return (
      <SimpleLog
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
  requests: PropTypes.instanceOf(Immutable.Map).isRequired,
  fetchChunk: PropTypes.func.isRequired,
  fetchLength: PropTypes.func.isRequired
};

const mapStateToProps = (state, ownProps) => ({
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

export default connectToTailer(connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(SandboxTailer));
