import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Immutable from 'immutable';

import { sandboxGetLength, sandboxFetchChunk } from '../actions';

import * as Selectors from '../selectors';

const Nothing = (props) => {
  console.log(props);
  return (
    <div>I am nothing</div>
  );
};

const sandboxTailer = (Wrapped, taskId, path) => {
  const tailerId = `${taskId}/${path}`;

  class SandboxTailer extends Component {
    constructor(props, context) {
      super(props, context);

      this.getTailerState = context.getTailerState;

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
        <Wrapped
          {...this.props}
          isRowLoaded={this.isRowLoaded}
          loadMoreRows={this.loadMoreRows}
          onScroll={this.onScroll}
          remoteRowCount={this.remoteRowCount()}
        />
      );
    }
  }

  SandboxTailer.contextTypes = {
    getTailerState: PropTypes.func
  };

  SandboxTailer.propTypes = {
    fetchLength: PropTypes.func.isRequired,
    fetchChunk: PropTypes.func.isRequired,
    isLoaded: PropTypes.bool.isRequired,
    fileSize: PropTypes.number,
    lines: PropTypes.instanceOf(Immutable.List)
  };

  const mapStateToProps = (state, ownProps) => {
    const tailerState = ownProps.getTailerState(state);
    const file = tailerState.files[tailerId];

    const getLines = Selectors.makeGetEnhancedLines();

    return {
      isLoaded: !!file,
      fileSize: file && file.fileSize,
      lines: getLines(state, ownProps),
      config: tailerState.config
    };
  };

  const mapDispatchToProps = (dispatch) => ({
    fetchLength: (config) => dispatch(sandboxGetLength(tailerId, taskId, path, config)),
    fetchChunk: (start, end, config) => dispatch(
      sandboxFetchChunk(tailerId, taskId, path, start, end, config)
    ),
  });

  const mergeProps = (stateProps, dispatchProps, ownProps) => {
    return {
      ...stateProps,
      ...ownProps,
      fetchLength: () => dispatchProps.fetchLength(stateProps.config),
      fetchChunk: (start, end) => dispatchProps.fetchChunk(start, end, stateProps.config),
    };
  };

  return connect(
    mapStateToProps,
    mapDispatchToProps,
    mergeProps
  )(SandboxTailer);
};

export default sandboxTailer;
