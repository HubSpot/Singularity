import React from 'react';
import Interval from 'react-interval';
import Header from './Header';
import TaskGroupContainer from './TaskGroupContainer';

import { connect } from 'react-redux';

import { updateGroups, updateTaskStatuses } from '../../actions/log';

class LogContainer extends React.Component {
  renderTaskGroups() {
    let rows = [];

    let tasksPerRow = this.props.taskGroupsCount === 4 ? 2 : 3;

    let row = [];
    let iterable = __range__(1, Math.min(this.props.taskGroupsCount, tasksPerRow), true);
    for (let j = 0; j < iterable.length; j++) {
      var i = iterable[j];
      row.push(<TaskGroupContainer key={i - 1} taskGroupId={i - 1} taskGroupContainerCount={Math.min(this.props.taskGroupsCount, tasksPerRow)} />);
    }

    rows.push(row);

    if (this.props.taskGroupsCount > tasksPerRow) {
      row = [];
      let iterable1 = __range__(tasksPerRow + 1, Math.min(this.props.taskGroupsCount, 6), true);
      for (let k = 0; k < iterable1.length; k++) {
        var i = iterable1[k];
        row.push(<TaskGroupContainer key={i - 1} taskGroupId={i - 1} taskGroupContainerCount={Math.min(this.props.taskGroupsCount, 6) - tasksPerRow} />);
      }
      rows.push(row);
    }

    let rowClassName = 'row tail-row';

    if (rows.length > 1) {
      rowClassName = 'row tail-row-half';
    }

    return rows.map((row, i) => <div key={i} className={rowClassName}>{row}</div>);
  }

  render() {
    const cb = this.props.updateGroups
    return <div><Interval enabled={this.props.ready} timeout={2000} callback={cb} /><Interval enabled={true} timeout={10000} callback={this.props.updateTaskStatuses} /><Header />{this.renderTaskGroups()}</div>;
  }
}

LogContainer.propTypes = {
  taskGroupsCount: React.PropTypes.number.isRequired,
  ready: React.PropTypes.bool.isRequired,

  updateGroups: React.PropTypes.func.isRequired,
  updateTaskStatuses: React.PropTypes.func.isRequired
};

function mapStateToProps(state) {
  return {
    taskGroupsCount: state.taskGroups.length,
    ready: _.all(_.pluck(state.taskGroups, 'ready'))
  };
}

const mapDispatchToProps = { updateGroups, updateTaskStatuses };

export default connect(mapStateToProps, mapDispatchToProps)(LogContainer);

function __range__(left, right, inclusive) {
  let range = [];
  let ascending = left < right;
  let end = !inclusive ? right : ascending ? right + 1 : right - 1;
  for (let i = left; ascending ? i < end : i > end; ascending ? i++ : i--) {
    range.push(i);
  }
  return range;
}

