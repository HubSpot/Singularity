import React, { Component, PropTypes } from 'react';

import LogLines from './LogLines';

import { connect } from 'react-redux';

import Immutable from 'immutable';

import { sandboxGetLength, sandboxFetchChunk } from '../actions';

import * as Selectors from '../selectors';

import connectToTailer from './connectToTailer';

// import sandboxTailer from './sandboxTailer';

class SandboxTailer extends Component {
  constructor(props, context) {
    super(props, context);

    this.componentDidMount = this.componentDidMount.bind(this);
    this.isRowLoaded = this.isRowLoaded.bind(this);
    this.loadMoreRows = this.loadMoreRows.bind(this);
    this.onScroll = this.onScroll.bind(this);

    this.remoteRowCount = this.remoteRowCount.bind(this);

    this.sandboxMaxBytes = 65535;
  }

  componentDidMount() {
    this.props.fetchLength();
  }

  componentDidUpdate(prevProps) {
    if (this.props.tailerId !== prevProps.tailerId) {
      this.props.fetchLength();
    }
  }

  isRowLoaded({index}) {
    return (
      index < this.props.lines.size
      && !this.props.lines.get(index).isMissingMarker
    );
  }

  loadMoreRows({startIndex, stopIndex}) {
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

    return fetchChunk(byteRangeStart, byteRangeEnd);
  }

  onScroll({clientHeight, scrollHeight, scrollTop}) {
    console.log('onScroll', clientHeight, scrollHeight, scrollTop);
  }

  remoteRowCount() {
    const { isLoaded, fileSize } = this.props;
    return Math.max(
      Math.ceil((isLoaded && fileSize || 0) / 150),
      (isLoaded && fileSize || 0)
    ); // real solid math
  }

  render() {
    return (
      <section className="log-pane">
        <div className="log-line-wrapper" style={{minHeight: this.props.minLines * 14}}>
          <LogLines
            {...this.props}
            onScroll={this.onScroll}
            isRowLoaded={this.isRowLoaded}
            loadMoreRows={this.loadMoreRows}
            remoteRowCount={this.remoteRowCount()}
          />
        </div>
      </section>
    );
  }
}

SandboxTailer.propTypes = {
  tailerId: PropTypes.string.isRequired,
  fetchLength: PropTypes.func.isRequired,
  fetchChunk: PropTypes.func.isRequired,
  isLoaded: PropTypes.bool.isRequired,
  fileSize: PropTypes.number,
  lines: PropTypes.instanceOf(Immutable.List),
  minLines: PropTypes.number
};

SandboxTailer.defaultProps = {
  minLines: 10
};

// const Log = ({type, file}) => {
//   return (
//   )
// };

// Log.propTypes = {
//   type: PropTypes.oneOf([
//     'sandbox'
//   ]).isRequired,
//   file: PropTypes.object.isRequired
// };

const mapStateToProps = (state, ownProps) => {
  const tailerId = `${ownProps.taskId}/${ownProps.path}`;
  const tailerState = ownProps.getTailerState(state);
  const file = tailerState.files[tailerId];

  const getLines = Selectors.makeGetEnhancedLines();

  return {
    tailerId,
    isLoaded: !!file,
    fileSize: file && file.fileSize,
    lines: getLines(state, {
      ...ownProps,
      tailerId
    }),
    config: tailerState.config
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  const tailerId = `${ownProps.taskId}/${ownProps.path}`;
  return {
    fetchLength: (config) => dispatch(sandboxGetLength(tailerId, ownProps.taskId, ownProps.path, config)),
    fetchChunk: (start, end, config) => dispatch(
      sandboxFetchChunk(tailerId, ownProps.taskId, ownProps.path, start, end, config)
    ),
  };
};

const mergeProps = (stateProps, dispatchProps, ownProps) => {
  return {
    ...stateProps,
    ...ownProps,
    fetchLength: () => dispatchProps.fetchLength(stateProps.config),
    fetchChunk: (start, end) => dispatchProps.fetchChunk(start, end, stateProps.config),
  };
};

export default connectToTailer(connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(SandboxTailer));
