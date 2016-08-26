import React, { Component, PropTypes } from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux';

import Immutable from 'immutable';

import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

import LogLines, { LOG_LINE_HEIGHT } from './LogLines';

class Log extends Component {
  constructor() {
    super();

    this.isLineLoaded = this.isLineLoaded.bind(this);
    this.loadLine = this.loadLine.bind(this);
    this.tailLog = this.tailLog.bind(this);
    this.pollScroll = this.pollScroll.bind(this);

    this.scrollTop = undefined;
    this.scrollHeight = undefined;
    this.invalidate = false;

    this.fakeLineCount = 0;
    this.scrollDelta = 0;
  }

  componentDidMount() {
    if (!this.props.isLoaded) {
      this.props.initializeFile(this.props.goToOffset);
    }

    this.rafRequestId = window.requestAnimationFrame(this.pollScroll);
  }

  componentWillUpdate(nextProps) {
    if (nextProps.lines !== this.props.lines) {
      this.invalidate = true;
    }

    if (nextProps.lines.size > 1 && this.props.lines.size > 1) {
      const oldLines = this.props.lines;
      const newLines = nextProps.lines;

      const addedToBeginning = newLines.findIndex((l) => { return l.start >= oldLines.get(0).end; }) - 1;
      const removedFromBeginning = oldLines.findIndex((l) => { return l.end >= newLines.get(0).end; });

      if (removedFromBeginning) {
        this.fakeLineCount += removedFromBeginning - 1;
      }

      if (addedToBeginning) {
        if (this.fakeLineCount - addedToBeginning >= 0) {
          this.fakeLineCount -= addedToBeginning;
        } else {
          this.scrollDelta += LOG_LINE_HEIGHT * (addedToBeginning - this.fakeLineCount);
          this.fakeLineCount = 0;
        }
      }
    }
  }

  componentDidUpdate(prevProps) {
    const idMatches = this.props.tailerId === prevProps.tailerId;
    const offsetMatches = this.props.goToOffset === prevProps.goToOffset;

    if (!idMatches || !offsetMatches) {
      this.props.initializeFile(this.props.goToOffset);
    }
  }

  componentWillUnmount() {
    window.cancelAnimationFrame(this.rafRequestId);
  }

  isLineLoaded(index) {
    return (
      index < this.props.lines.size
      && (
        !this.props.lines.get(index).isMissingMarker &&
        !this.props.requests.has(this.props.lines.get(index).start)
      )
    );
  }

  // detect when dom has changed underneath us- either scrollTop or scrollHeight (layout reflow)
  // may have changed.
  pollScroll () {
    const domNode = ReactDOM.findDOMNode(this);

    // let's update the scroll now, in a raf.
    if (this.scrollDelta) {
      domNode.scrollTop += this.scrollDelta;
      this.scrollDelta = 0;
    }

    const { scrollTop, scrollHeight, clientHeight } = domNode;
    if (scrollTop !== this.scrollTop || this.invalidate) {
      this.invalidate = false;
      const scrollLoadThreshold = LOG_LINE_HEIGHT * 300;
      const atTop = (scrollTop - this.fakeLineCount * LOG_LINE_HEIGHT) <= scrollLoadThreshold;
      const atBottom = scrollTop >= (scrollHeight - clientHeight - scrollLoadThreshold);

      const { lines } = this.props;
      if (atTop && atBottom && lines.size === 1) {
        // wait until the first chunk is loaded.
      } else {
        // we are at the top/bottom, load some stuff.
        if (atTop && !this.isLineLoaded(0)) {
          setTimeout(() => this.loadLine(0, true), 0);
        }

        if (atBottom) {
          if (lines.size) {
            setTimeout(() => this.loadLine(lines.size - 1, false), 0);
          }
        }
      }
      // update the scroll position.
      this.scrollTop = domNode.scrollTop;
      this.scrollHeight = domNode.scrollHeight;
    }
    // do another raf.
    this.rafRequestId = window.requestAnimationFrame(this.pollScroll);
  }

  loadLine(index, loadUp) {
    return this.props.loadLine(
      index,
      loadUp,
      this.props.lines,
      this.props.chunks
    );
  }

  tailLog() {
    return this.props.tailLog(this.props.lines);
  }

  render() {
    const { props } = this;

    const hrefFunc = props.hrefFunc
      ? (offset) => props.hrefFunc(props.tailerId, offset)
      : undefined;

    return (
      <section className="log-pane">
        <div className="log-line-wrapper">
          <LogLines
            isLoaded={props.isLoaded}
            lines={props.lines}
            fakeLineCount={this.fakeLineCount}
            isLineLoaded={this.isLineLoaded}
            hrefFunc={hrefFunc}
          />
        </div>
      </section>
    );
  }
}

Log.propTypes = {
  tailerId: PropTypes.string.isRequired,
  goToOffset: PropTypes.number,
  hrefFunc: PropTypes.func,
  // from connectToTailer HOC
  getTailerState: PropTypes.func.isRequired,
  // from tailer implementation
  // actions
  initializeFile: PropTypes.func.isRequired,
  loadLine: PropTypes.func.isRequired,
  tailLog: PropTypes.func.isRequired,
  // from connect
  isLoaded: PropTypes.bool.isRequired,
  fileSize: PropTypes.number,
  lines: PropTypes.instanceOf(Immutable.List),
  chunks: PropTypes.instanceOf(Immutable.List),
  requests: PropTypes.instanceOf(Immutable.Map),
  config: PropTypes.object.isRequired
};

const makeMapStateToProps = () => {
  const getEnhancedLines = Selectors.makeGetEnhancedLines();
  const mapStateToProps = (state, ownProps) => ({
    isLoaded: Selectors.getIsLoaded(state, ownProps),
    fileSize: Selectors.getFileSize(state, ownProps),
    lines: getEnhancedLines(state, ownProps),
    chunks: Selectors.getChunks(state, ownProps),
    requests: Selectors.getRequests(state, ownProps),
    config: Selectors.getConfig(state, ownProps)
  });
  return mapStateToProps;
};

export default connectToTailer(connect(
  makeMapStateToProps
)(Log));
