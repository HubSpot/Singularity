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
        filterText: props.searchFilter
      }
    }
  }

  handleFilterChange(filter) {
    this.props.fetchFilter(filter.taskStatus);
    this.setState({
      filter: filter
    });
    const requestTypes = filter.requestTypes.length == TaskFilters.REQUEST_TYPES.length ? 'all' : filter.requestTypes.join(',');
    this.props.updateFilters(filter.taskStatus, requestTypes, filter.filterText);
    app.router.navigate(`/tasks/${filter.taskStatus}/${requestTypes}/${filter.filterText}`);
  }

  getColumns() {
    switch(this.state.filter.taskStatus) {
      case 'active':
        return [TaskId, StartedAt, Host, Rack, CPUs, Memory, ActiveActions];
      case 'scheduled':
        return [ScheduledTaskId, NextRun, PendingType, DeployId, ScheduledActions];
    }
  }

  render() {
    const displayRequestTypeFilters = this.state.filter.taskStatus == 'active';
    const displayTasks = _.sortBy(filterSelector({tasks: this.props.tasks, filter: this.state.filter}), (t) => t.taskId && t.taskId.startedAt).reverse();

    return (
      <div>
        <TaskFilters filter={this.state.filter} onFilterChange={this.handleFilterChange.bind(this)} displayRequestTypeFilters={displayRequestTypeFilters} />
        <UITable
          data={displayTasks}
          keyGetter={(r) => r.taskId ? r.taskId.id : r.pendingTask.pendingTaskId.id}
        >
          {this.getColumns()}
        </UITable>
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
