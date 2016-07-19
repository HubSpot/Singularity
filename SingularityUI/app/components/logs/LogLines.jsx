import React, { PropTypes, Component } from 'react';
import LogLine from './LogLine';
import Humanize from 'humanize-plus';
import classNames from 'classnames';

import { connect } from 'react-redux';
import { taskGroupTop, taskGroupBottom } from '../../actions/log';

function sum(numbers) {
  let total = 0;
  for (let i = 0; i < numbers.length; i++) {
    total += numbers[i];
  }
  return total;
}

class LogLines extends Component {
  componentDidMount() {
    window.addEventListener('resize', this.handleScroll.bind(this));
  }

  componentDidUpdate(prevProps) {
    if (prevProps.updatedAt !== this.props.updatedAt) {
      if (this.props.tailing) {
        this.refs.tailContents.scrollTop = this.refs.tailContents.scrollHeight;
      } else if (this.props.prependedLineCount > 0 || this.props.linesRemovedFromTop > 0) {
        this.refs.tailContents.scrollTop += 20 * (this.props.prependedLineCount - this.props.linesRemovedFromTop);
      } else {
        this.handleScroll();
      }
    }
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.handleScroll.bind(this));
  }

  handleScroll() {
    const {scrollTop, scrollHeight, clientHeight} = this.refs.tailContents;

    if (scrollTop < clientHeight) {
      this.props.taskGroupTop(this.props.taskGroupId, true);
    } else {
      this.props.taskGroupTop(this.props.taskGroupId, false);
    }

    if (scrollTop + clientHeight > scrollHeight - clientHeight) {
      this.props.taskGroupBottom(this.props.taskGroupId, true, (scrollTop + clientHeight > scrollHeight - 20));
    } else {
      this.props.taskGroupBottom(this.props.taskGroupId, false);
    }
  }

  renderLoadingMore() {
    if (this.props.terminated) {
      return null;
    } else if (this.props.initialDataLoaded) {
      if (this.props.reachedEndOfFile) {
        if (this.props.search) {
          return <div>Tailing for '{this.props.search}'...</div>;
        }
        return <div>Tailing...</div>;
      }
      if (this.props.search) {
        return <div>Searching for '{this.props.search}'... ({Humanize.filesize(this.props.bytesRemainingAfter)} remaining)</div>;
      }
      return <div>Loading more... ({Humanize.filesize(this.props.bytesRemainingAfter)} remaining)</div>;
    }
    return null;
  }

  renderLogLines() {
    return this.props.logLines.map(({data, offset, taskId, timestamp}) => (
      <LogLine
        content={data}
        key={`${taskId}_${offset}`}
        offset={offset}
        taskId={taskId}
        timestamp={timestamp}
        isHighlighted={offset === this.props.initialOffset}
        color={this.props.colorMap[taskId]}
      />
    ));
  }

  renderLoadingPrevious() {
    if (this.props.initialDataLoaded) {
      if (!this.props.reachedStartOfFile) {
        if (this.props.search) {
          return <div>Searching for '{this.props.search}'... ({Humanize.filesize(this.props.bytesRemainingBefore)} remaining)</div>;
        }
        return <div>Loading previous... ({Humanize.filesize(this.props.bytesRemainingBefore)} remaining)</div>;
      }
    }
    return null;
  }

  render() {
    return (
      <div className="contents-container">
        <div className={classNames(['tail-contents', this.props.activeColor])} ref="tailContents" onScroll={(event) => { this.handleScroll(event); }}>
          {this.renderLoadingPrevious()}
          {this.renderLogLines()}
          {this.renderLoadingMore()}
          {this.props.fileNotFound}
        </div>
      </div>
    );
  }
}

LogLines.propTypes = {
  taskGroupTop: PropTypes.func.isRequired,
  taskGroupBottom: PropTypes.func.isRequired,

  taskGroupId: PropTypes.number.isRequired,
  logLines: PropTypes.array.isRequired,

  initialDataLoaded: PropTypes.bool.isRequired,
  reachedStartOfFile: PropTypes.bool.isRequired,
  reachedEndOfFile: PropTypes.bool.isRequired,
  bytesRemainingBefore: PropTypes.number.isRequired,
  bytesRemainingAfter: PropTypes.number.isRequired,
  activeColor: PropTypes.string.isRequired,
  search: PropTypes.string,
  initialOffset: PropTypes.number,
  colorMap: PropTypes.object,
  terminated: PropTypes.bool,
  prependedLineCount: PropTypes.number,
  linesRemovedFromTop: PropTypes.number,
  tailing: PropTypes.bool,

  fileNotFound: PropTypes.element,
  updatedAt: PropTypes.number
};

function mapStateToProps(state, ownProps) {
  const taskGroup = state.taskGroups[ownProps.taskGroupId];
  const tasks = taskGroup.taskIds.map((taskId) => state.tasks[taskId]);

  const colorMap = {};
  if (taskGroup.taskIds.length > 1) {
    let i = 0;
    for (const taskId of taskGroup.taskIds) {
      colorMap[taskId] = `hsla(${(360 / taskGroup.taskIds.length) * i}, 100%, 50%, 0.1)`;
      i++;
    }
  }

  return {
    logLines: taskGroup.logLines,
    updatedAt: taskGroup.updatedAt,
    tailing: taskGroup.tailing,
    prependedLineCount: taskGroup.prependedLineCount,
    linesRemovedFromTop: taskGroup.linesRemovedFromTop,
    activeColor: state.activeColor,
    top: taskGroup.top,
    bottom: taskGroup.bottom,
    initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded')),
    terminated: _.all(_.pluck(tasks, 'terminated')),
    reachedStartOfFile: _.all(tasks.map(({minOffset}) => minOffset === 0)),
    reachedEndOfFile: _.all(tasks.map(({maxOffset, filesize}) => maxOffset >= filesize)),
    bytesRemainingBefore: sum(_.pluck(tasks, 'minOffset')),
    bytesRemainingAfter: sum(tasks.map(({filesize, maxOffset}) => Math.max(filesize - maxOffset, 0))),
    colorMap,
    search: state.search,
  };
}

const mapDispatchToProps = { taskGroupTop, taskGroupBottom };

export default connect(mapStateToProps, mapDispatchToProps)(LogLines);
