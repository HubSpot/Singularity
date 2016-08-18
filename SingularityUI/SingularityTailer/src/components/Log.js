import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable, { Range } from 'immutable';

import * as Actions from '../actions';

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
      return;
    }

    if (this.props.scrollToOffset !== prevProps.scrollToOffset) {
      this.refs.LogLines.refs.VirtualScroll.scrollToIndex()
    }

    if (this.props.scroll.hasOwnProperty('startIndex')) {
      const { fetchOverscan } = this.props.config;

      let start;
      let stop;
      if (fetchOverscan) {
        start = this.props.scroll.overscanStartIndex;
        stop = this.props.scroll.overscanStopIndex;
      } else {
        start = this.props.scroll.startIndex;
        stop = this.props.scroll.stopIndex;
      }
      const unloaded = this.findUnloadedInRange(start, stop);

      // unloaded.forEach((index) => {
      //   this.loadLines(index, index);
      // });
      if (unloaded.last() !== undefined) {
        this.loadLines(unloaded.last(), unloaded.last());
      }
    }

    if (prevProps.lines.size && this.props.lines.size) {
      const prevStart = prevProps.lines.first().start;
      const newStart = this.props.lines.first().start;
      if (prevStart !== newStart) {
        // how many rows were added above it?
        const prevStartIndex = this.props.lines.findIndex(
          (l) => prevStart >= l.start
        );

        // TODO: actually 'measure' heights
        const scrollDelta = prevStartIndex * 14;
        console.log(scrollDelta);

        console.log(this.refs.LogLines.refs.VirtualScroll.scrollTop);
      }
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
    return false; // this is broken right now...
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
    const isTailing = this.isTailing(stopIndex);

    let tailIntervalId = this.state.tailIntervalId;

    if (isTailing && !this.state.isTailing) {
      // start tailing
      tailIntervalId = setInterval(() => this.tailLog(), 10000);
    } else if (!isTailing && this.state.isTailing) {
      // stop tailing
      clearInterval(tailIntervalId);
      tailIntervalId = undefined;
    }

    this.props.renderedLines(
      startIndex,
      stopIndex,
      overscanStartIndex,
      overscanStopIndex
    );
  }

  render() {
    const { props } = this;
    return (
      <section className="log-pane">
        <div className="log-line-wrapper">
          <LogLines
            ref="LogLines"
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
  config: PropTypes.object.isRequired,
  scroll: PropTypes.object.isRequired,
  // actions
  renderedLines: PropTypes.func.isRequired
};

const makeMapStateToProps = () => {
  const getEnhancedLines = Selectors.makeGetEnhancedLines();
  const mapStateToProps = (state, ownProps) => ({
    isLoaded: Selectors.getIsLoaded(state, ownProps),
    fileSize: Selectors.getFileSize(state, ownProps),
    lines: getEnhancedLines(state, ownProps),
    requests: Selectors.getRequests(state, ownProps),
    config: Selectors.getConfig(state, ownProps),
    scroll: Selectors.getScroll(state, ownProps)
  });
  return mapStateToProps;
};

const mapDispatchToProps = (dispatch, ownProps) => {
  return {
    renderedLines: (startIndex, stopIndex, overscanStartIndex, overscanStopIndex) => (
      dispatch(Actions.renderedLines(
        ownProps.tailerId,
        startIndex,
        stopIndex,
        overscanStartIndex,
        overscanStopIndex
      ))
    )
  };
};

export default connectToTailer(connect(
  makeMapStateToProps,
  mapDispatchToProps
)(Log));
