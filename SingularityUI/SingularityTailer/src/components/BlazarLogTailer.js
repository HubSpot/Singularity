import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable from 'immutable';

import {
  unloadFileChunk,
  unloadFile,
  blazarLogFetchChunk,
  blazarLogFetchLength,
  blazarLogFetchTail,
  startTailing,
  stopTailing,
  BLAZAR_LOG_MAX_BYTES
} from '../actions';
import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

import Log from './Log';

class BlazarLogTailer extends Component {
  constructor() {
    super();

    this.initializeFile = this.initializeFile.bind(this);
    this.loadLine = this.loadLine.bind(this);
    this.tailLog = this.tailLog.bind(this);
  }

  initializeFile(atOffset) {
    this.props.unloadFile();
    if (atOffset !== undefined) {
      if (atOffset === -1) {
        this.props.fetchTail();
      } else {
        this.props.fetchLength();
        this.fetchSafe(atOffset, atOffset + BLAZAR_LOG_MAX_BYTES);
      }
    } else {
      this.props.fetchLength();
      this.fetchSafe(0, BLAZAR_LOG_MAX_BYTES);
    }
  }

  fetchSafe(byteRangeStart, byteRangeEnd) {
    const { fetchChunk } = this.props;
    // if already in flight, don't request again
    if (!this.props.requests.has(byteRangeStart)) {
      return fetchChunk(byteRangeStart, byteRangeEnd);
    }
    return Promise.resolve();
  }

  loadLine(index, loadUp, lines, chunks) {
    let byteRangeStart;
    let byteRangeEnd;

    if (index < lines.size) {
      const lineToLoad = lines.get(index);

      if (loadUp) {
        byteRangeEnd = lineToLoad.end;
        byteRangeStart = Math.max(0, byteRangeEnd - BLAZAR_LOG_MAX_BYTES);
      } else {
        byteRangeStart = lineToLoad.start;
        // if this is the last line, and not a missing marker
        if (index === lines.size - 1 && !lineToLoad.isMissingMarker) {
          // we have to load from the end of this line instead of the beginning
          byteRangeStart = lineToLoad.end;
        }
        byteRangeEnd = byteRangeStart + BLAZAR_LOG_MAX_BYTES;
      }

      this.fetchSafe(byteRangeStart, byteRangeEnd);

      const MIN_LOADED_LINES_TO_TRIGGER_UNLOAD = 800;
      const MIN_LOADED_CHUNKS_TO_TRIGGER_UNLOAD = 5;
      if (lines.size >= MIN_LOADED_LINES_TO_TRIGGER_UNLOAD && chunks.size >= MIN_LOADED_CHUNKS_TO_TRIGGER_UNLOAD) {
        if (loadUp) {
          // remove bottom
          this.props.unloadFileChunk(-1);
        } else {
          // remove top
          this.props.unloadFileChunk(0);
        }
      }
    }
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
        byteRangeStart + BLAZAR_LOG_MAX_BYTES
      );
    } else {
      byteRangeEnd = byteRangeStart + BLAZAR_LOG_MAX_BYTES;
    }

    this.fetchSafe(byteRangeStart, byteRangeEnd);
  }

  tailLog(lines) {
    if (lines.size) {
      const lastLine = lines.last();
      this.fetchSafe(lastLine.end, lastLine.end + BLAZAR_LOG_MAX_BYTES);
    } else {
      this.fetchSafe(0, BLAZAR_LOG_MAX_BYTES);
    }
  }

  render() {
    return (
      <Log
        tailerId={this.props.tailerId}
        initializeFile={this.initializeFile}
        loadLine={this.loadLine}
        tailLog={this.tailLog}
        goToOffset={this.props.goToOffset}
        lineLinkRenderer={this.props.lineLinkRenderer}
        startTailing={this.props.startTailing}
        stopTailing={this.props.stopTailing}
      />
    );
  }
}

BlazarLogTailer.propTypes = {
  tailerId: PropTypes.string.isRequired,
  buildId: PropTypes.string.isRequired,
  requests: PropTypes.instanceOf(Immutable.Map).isRequired,
  fetchLength: PropTypes.func.isRequired,
  fetchChunk: PropTypes.func.isRequired,
  fetchTail: PropTypes.func.isRequired,
  unloadFile: PropTypes.func.isRequired,
  unloadFileChunk: PropTypes.func.isRequired,
  goToOffset: PropTypes.number,
  lineLinkRenderer: PropTypes.func
};

const mapStateToProps = (state, ownProps) => ({
  requests: Selectors.getRequests(state, ownProps),
  config: Selectors.getConfig(state, ownProps)
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchLength: (config) => dispatch(
    blazarLogFetchLength(
      ownProps.tailerId,
      ownProps.buildId,
      config
    )
  ),
  fetchChunk: (start, end, config) => dispatch(
    blazarLogFetchChunk(
      ownProps.tailerId,
      ownProps.buildId,
      start,
      end,
      config
    )
  ),
  fetchTail: (config) => dispatch(
    blazarLogFetchTail(
      ownProps.tailerId,
      ownProps.buildId,
      config
    )
  ),
  unloadFile: () => dispatch(
    unloadFile(
      ownProps.tailerId
    )
  ),
  unloadFileChunk: (index) => dispatch(
    unloadFileChunk(
      ownProps.tailerId,
      index
    )
  ),
  startTailing: () => dispatch(
    startTailing(ownProps.tailerId)
  ),
  stopTailing: () => dispatch(
    stopTailing(ownProps.tailerId)
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
  fetchTail: () => dispatchProps.fetchTail(stateProps.config),
  unloadFile: () => dispatchProps.unloadFile(),
  unloadFileChunk: (start) => dispatchProps.unloadFileChunk(start)
});

export default connectToTailer(connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(BlazarLogTailer));
