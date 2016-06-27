import React from 'react';
import { connect } from 'react-redux';
import filterSelector from '../../selectors/tasks/filterSelector';

import TaskFilters from './TaskFilters';
import { FetchAction } from '../../actions/api/tasks';

import UITable from '../common/table/UITable';
import { TaskId, StartedAt, Host, Rack, CPUs, Memory, ActiveActions, NextRun, PendingType, DeployId, ScheduledActions, ScheduledTaskId } from './Columns';

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
    }
  }

  getDefaultSortAttribute(t) {
    switch(this.state.filter.taskStatus) {
      case 'active':
        return t.taskId.startedAt;
      case 'scheduled':
        if (!t.pendingTask) return null;
        return t.pendingTask.pendingTaskId.nextRunAt;
    }
  }

  render() {
    const displayRequestTypeFilters = this.state.filter.taskStatus == 'active';
    const displayTasks = _.sortBy(filterSelector({tasks: this.props.tasks, filter: this.state.filter}), (t) => this.getDefaultSortAttribute(t));
    if (this.state.filter.taskStatus == 'active') displayTasks.reverse();

    const table = !this.state.loading ? (
      <UITable
        data={displayTasks}
        keyGetter={(r) => r.taskId ? r.taskId.id : r.pendingTask.pendingTaskId.id}
      >
        {this.getColumns()}
      </UITable>
    ) : <div className="page-loader fixed"></div>;

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
    tasks: state.api.tasks.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchAction.trigger(state))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TasksPage);
