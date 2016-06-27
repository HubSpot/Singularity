import React from 'react';
import { connect } from 'react-redux';
import filterSelector from '../../selectors/tasks/filterSelector';
import decomSelector from '../../selectors/tasks/decomSelector';

import TaskFilters from './TaskFilters';
import { FetchAction } from '../../actions/api/tasks';

import UITable from '../common/table/UITable';
import { TaskId, StartedAt, Host, Rack, CPUs, Memory, ActiveActions, NextRun, PendingType, DeployId, ScheduledActions, ScheduledTaskId, CleanupType, JSONAction, InstanceNumber } from './Columns';

class TasksPage extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      filter: {
        taskStatus: props.state,
        requestTypes: props.requestsSubFilter == 'all' ? TaskFilters.REQUEST_TYPES : props.requestsSubFilter.split(','),
        filterText: props.searchFilter,
        loading: false
      }
    }
  }

  handleFilterChange(filter) {
    const lastFilterTaskStatus = this.state.filter.taskStatus;
    this.setState({
      loading: lastFilterTaskStatus != filter.taskStatus,
      filter: filter
    });

    const requestTypes = filter.requestTypes.length == TaskFilters.REQUEST_TYPES.length ? 'all' : filter.requestTypes.join(',');
    this.props.updateFilters(filter.taskStatus, requestTypes, filter.filterText);
    app.router.navigate(`/tasks/${filter.taskStatus}/${requestTypes}/${filter.filterText}`);

    if (lastFilterTaskStatus != filter.taskStatus) {
      this.props.fetchFilter(filter.taskStatus).then(() => {
        this.setState({
          loading: false
        });
      });
    }
  }

  getColumns() {
    switch(this.state.filter.taskStatus) {
      case 'active':
        return [TaskId, StartedAt, Host, Rack, CPUs, Memory, ActiveActions];
      case 'scheduled':
        return [ScheduledTaskId, NextRun, PendingType, DeployId, ScheduledActions];
      case 'cleaning':
        return [TaskId, CleanupType, JSONAction];
      case 'lbcleanup':
        return [TaskId, StartedAt, Host, Rack, InstanceNumber, JSONAction];
      case 'decommissioning':
        return [TaskId, StartedAt, Host, Rack, CPUs, Memory, ActiveActions];
    }
  }

  getDefaultSortAttribute(t) {
    switch(this.state.filter.taskStatus) {
      case 'active':
      case 'decommissioning':
        return t.taskId.startedAt;
      case 'scheduled':
        if (!t.pendingTask) return null;
        return t.pendingTask.pendingTaskId.nextRunAt;
    }
  }

  render() {
    const displayRequestTypeFilters = this.state.filter.taskStatus == 'active';
    const displayTasks = this.state.filter.taskStatus != 'decommissioning' ?
      _.sortBy(filterSelector({tasks: this.props.tasks, filter: this.state.filter}), (t) => this.getDefaultSortAttribute(t)) :
      _.sortBy(decomSelector({tasks: this.props.tasks, cleanups: this.props.cleanups}), (t) => this.getDefaultSortAttribute(t));
    if (_.contains(['active', 'decommissioning'], this.state.filter.taskStatus)) displayTasks.reverse();

    let table;
    if (this.state.loading) {
      table = <div className="page-loader fixed"></div>;
    }
    else if (!displayTasks.length) {
      table = <div className="empty-table-message"><p>No matching tasks</p></div>;
    } else {
      table = (
        <UITable
          data={displayTasks}
          keyGetter={(r) => r.taskId ? r.taskId.id : r.pendingTask.pendingTaskId.id}
        >
          {this.getColumns()}
        </UITable>
      );
    }

    return (
      <div>
        <TaskFilters filter={this.state.filter} onFilterChange={this.handleFilterChange.bind(this)} displayRequestTypeFilters={displayRequestTypeFilters} />
        {table}
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  return {
    tasks: state.api.tasks.data,
    cleanups: state.api.taskCleanups.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchAction.trigger(state))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TasksPage);
