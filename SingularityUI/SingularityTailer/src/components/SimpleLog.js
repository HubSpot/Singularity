import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable, { Range } from 'immutable';

import * as Actions from '../actions';

import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

import SimpleLogLines from './SimpleLogLines';

class Log extends Component {
  constructor() {
    super();

    this.isLineLoaded = this.isLineLoaded.bind(this);
    this.isTailing = this.isTailing.bind(this);
    this.loadLines = this.loadLines.bind(this);
    this.tailLog = this.tailLog.bind(this);

    this.state = {
      isTailing: false,
      tailIntervalId: undefined
    };
  }

  componentDidMount() {
    this.props.initializeFile(632423);
  }

  componentDidUpdate(prevProps) {
    if (this.props.tailerId !== prevProps.tailerId) {
      let atOffset;
      if (this.props.goToOffset !== prevProps.goToOffset) {
        atOffset = this.props.goToOffset;
      }
      this.props.initializeFile(atOffset);
      return;
    }

    let atOffset;
    if (this.props.goToOffset !== prevProps.goToOffset) {
      atOffset = this.props.goToOffset;

      this.props.initializeFile(atOffset);
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

  render() {
    const { props } = this;
    return (
      <section className="log-pane">
        <div className="log-line-wrapper">
          <SimpleLogLines
            isLoaded={props.isLoaded}
            lines={props.lines}
            isLineLoaded={this.isLineLoaded}
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
  scroll: PropTypes.object.isRequired
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

export default connectToTailer(connect(
  makeMapStateToProps
)(Log));
