import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable from 'immutable';

import connectToTailer from './connectToTailer';

import * as Selectors from '../selectors';

import LogLines from './LogLines';

class Log extends Component {
  constructor() {
    super();

    this.isRowLoaded = this.isRowLoaded.bind(this);
    this.loadMoreRows = this.loadMoreRows.bind(this);
    this.onScroll = this.onScroll.bind(this);
  }
  componentDidMount() {
    this.props.initializeFile();
  }

  componentDidUpdate(prevProps) {
    if (this.props.tailerId !== prevProps.tailerId) {
      this.props.initializeFile();
    }
  }

  isRowLoaded({index}) {
    return (
      index < this.props.lines.size
      && (
        !this.props.lines.get(index).isMissingMarker
        || this.props.lines.get(index).isLoading // if loading don't try again
      )
    );
  }

  loadMoreRows({startIndex, stopIndex}) {
    this.props.loadLines(startIndex, stopIndex);
  }

  onScroll({clientHeight, scrollHeight, scrollTop}) {
    console.log('onScroll', clientHeight, scrollHeight, scrollTop, scrollHeight - scrollTop - clientHeight);

    // if at the bottom of the scroll window
    if (scrollHeight - scrollTop - clientHeight === 0) {
      if (!this.props.lines.size || this.props.lines.last().isMissingMarker) {

      }
    }
  }

  render() {
    const { props } = this;
    return (
      <section className="log-pane">
        <div className="log-line-wrapper">
          <LogLines
            isLoaded={props.isLoaded}
            lines={props.lines}
            remoteRowCount={props.isLoaded ? props.lines.size + 1 : 0}
            isRowLoaded={this.isRowLoaded}
            loadMoreRows={this.loadMoreRows}
            onScroll={this.onScroll}
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
