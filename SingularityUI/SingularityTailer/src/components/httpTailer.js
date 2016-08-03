import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable from 'immutable';

import { httpFetchChunk } from '../actions';
import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

const httpTailer = (WrappedLog, path) => {
  class HttpTailer extends Component {
    constructor() {
      super();

      this.initializeFile = this.initializeFile.bind(this);
      this.loadLines = this.loadLines.bind(this);
      this.tailLog = this.tailLog.bind(this);

      this.path = path;
    }

    initializeFile() {
      this.props.fetchChunk(0, 1000);
    }

    loadLines(startIndex, stopIndex) {
      const { lines, fetchChunk } = this.props;

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

  HttpTailer.propTypes = {
    tailerId: PropTypes.string.isRequired,
    lines: PropTypes.instanceOf(Immutable.List).isRequired,
    requests: PropTypes.instanceOf(Immutable.Map).isRequired,
    fetchChunk: PropTypes.func.isRequired
  };

  HttpTailer.defaultProps = {
    tailerId: path
  };

  const mapStateToProps = (state, ownProps) => ({
    isLoaded: Selectors.getIsLoaded(state, ownProps),
    fileSize: Selectors.getFileSize(state, ownProps),
    lines: Selectors.getLines(state, ownProps),
    requests: Selectors.getRequests(state, ownProps),
    config: Selectors.getConfig(state, ownProps)
  });

  const mapDispatchToProps = (dispatch, ownProps) => {
    return {
      fetchChunk: (start, end) => dispatch(httpFetchChunk(
        path, // tailerId
        path,
        start,
        end
      ))
    };
  };

  return connectToTailer(connect(
    mapStateToProps,
    mapDispatchToProps
  )(HttpTailer));
};

export default httpTailer;
