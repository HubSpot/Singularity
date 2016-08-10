import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable, { Range } from 'immutable';

import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

import LogLines from './LogLines';

class Log extends Component {
  constructor() {
    super();

    this.isLineLoaded = this.isLineLoaded.bind(this);
    this.isTailing = this.isTailing.bind(this);
    this.loadLines = this.loadLines.bind(this);
    this.tailLog = this.tailLog.bind(this);
    this.onRowsRendered = this.onRowsRendered.bind(this);

    this.state = {
      isTailing: false,
      tailIntervalId: undefined
    };
  }

  componentDidMount() {
    this.props.initializeFile();
  }

  componentDidUpdate(prevProps) {
    if (this.props.tailerId !== prevProps.tailerId) {
      this.props.initializeFile();
    }

    if (this.state.hasOwnProperty('startIndex')) {
      const unloaded = this.findUnloadedInRange(
        this.state.startIndex,
        this.state.stopIndex
      );

      unloaded.forEach((index) => {
        this.loadLines(index, index);
      });
    }
  }

  isLineLoaded(index) {
    return (
      index < this.props.lines.size
      && (
        !this.props.lines.get(index).isMissingMarker
      )
    );
  }

  isTailing(stopIndex) {
    return stopIndex === this.props.lines.size - 1;
  }

  loadLines(startIndex, stopIndex) {
    return this.props.loadLines(startIndex, stopIndex, this.props.lines);
  }

  tailLog() {
    return this.props.tailLog(this.props.lines);
  }

  findUnloadedInRange(startIndex, stopIndex) {
    const range = new Range(startIndex, stopIndex + 1);
    return range.filter((index) => !this.isLineLoaded(index));
  }

  onRowsRendered ({ startIndex, stopIndex, overscanStartIndex, overscanStopIndex }) {
    const { fetchOverscan } = this.props.config;

    const isTailing = this.isTailing(stopIndex);

    let tailIntervalId = this.state.tailIntervalId;

    if (isTailing && !this.state.isTailing) {
      // start tailing
      tailIntervalId = setInterval(() => this.tailLog(), 1000);
    } else if (!isTailing && this.state.isTailing) {
      // stop tailing
      clearInterval(tailIntervalId);
      tailIntervalId = undefined;
    }

    this.setState({
      startIndex: fetchOverscan ? overscanStartIndex : startIndex,
      stopIndex: fetchOverscan ? overscanStopIndex : stopIndex,
      isTailing,
      tailIntervalId
    });
  }

  render() {
    const { props } = this;
    return (
      <section className="log-pane">
        <div className="log-line-wrapper">
          <LogLines
            isLoaded={props.isLoaded}
            lines={props.lines}
            isLineLoaded={this.isLineLoaded}
            isTailing={this.isTailing}
            loadLines={this.loadLines}
            tailLog={this.tailLog}
            onRowsRendered={this.onRowsRendered}
          />
        </div>
      </section>
    );
  }
}

Log.propTypes = {
  tailerId: PropTypes.string.isRequired,
  // from connectToTailer HOC
  getTailerState: PropTypes.func.isRequired,
  // from chosen tailer HOC
  // actions
  initializeFile: PropTypes.func.isRequired,
  loadLines: PropTypes.func.isRequired,
  tailLog: PropTypes.func.isRequired,
  // from connect
  isLoaded: PropTypes.bool.isRequired,
  fileSize: PropTypes.number,
  lines: PropTypes.instanceOf(Immutable.List),
  requests: PropTypes.instanceOf(Immutable.Map),
  config: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => {
  const getEnhancedLines = Selectors.makeGetEnhancedLines();

  return {
    isLoaded: Selectors.getIsLoaded(state, ownProps),
    fileSize: Selectors.getFileSize(state, ownProps),
    lines: getEnhancedLines(state, ownProps),
    requests: Selectors.getRequests(state, ownProps),
    config: Selectors.getConfig(state, ownProps)
  };
};

export default connectToTailer(connect(
  mapStateToProps
)(Log));
