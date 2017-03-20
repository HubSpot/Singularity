import React from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import rootComponent from '../../rootComponent';

import {
  getDecomissioningTasks,
  getFilteredTasks
} from '../../selectors/tasks';

import TaskFilters from './TaskFilters';

import { FetchTasksInState, FetchTaskCleanups, KillTask } from '../../actions/api/tasks';
import { FetchRequestRun, RunRequest } from '../../actions/api/requests';
import { FetchRequestRunHistory } from '../../actions/api/history';
import { FetchTaskFiles } from '../../actions/api/sandbox';
import { refresh } from '../../actions/ui/tasks';

import UITable from '../common/table/UITable';
import Utils from '../../utils';

import {
  TaskIdShortened,
  StartedAt,
  Host,
  Rack,
  CPUs,
  Memory,
  Disk,
  ActiveActions,
  NextRun,
  PendingType,
  PendingDeployId,
  ScheduledActions,
  ScheduledTaskId,
  CleanupType,
  JSONAction,
  InstanceNumber
} from './Columns';

class TasksPage extends React.Component {
  static propTypes = {
    params: React.PropTypes.object,
    router: React.PropTypes.object,
    fetchFilter: React.PropTypes.func,
    killTask: React.PropTypes.func,
    runRequest: React.PropTypes.func,
    tasks: React.PropTypes.array,
    cleanups: React.PropTypes.array,
    taskRun: React.PropTypes.func,
    taskRunHistory: React.PropTypes.func,
    taskFiles: React.PropTypes.func,
    filter: React.PropTypes.shape({
      taskStatus: React.PropTypes.string,
      requestTypes: React.PropTypes.array,
      filterText: React.PropTypes.string
    }).isRequired
  };

  constructor(props) {
    super(props);
    this.state = {
      loading: false
    };
  }

  handleFilterChange(filter) {
    const lastFilterTaskStatus = this.props.filter.taskStatus;
    this.setState({
      loading: lastFilterTaskStatus !== filter.taskStatus
    });

    const requestTypes = filter.requestTypes.length === TaskFilters.REQUEST_TYPES.length ? 'all' : filter.requestTypes.join(',');
    this.props.router.push(`/tasks/${filter.taskStatus}/${requestTypes}/${filter.filterText}`);

    if (lastFilterTaskStatus !== filter.taskStatus) {
      this.props.fetchFilter(filter.taskStatus).then(() => {
        this.setState({
          loading: false
        });
      });
    }
  }


  getColumns() {
    let columns;
    switch (this.props.filter.taskStatus) {
      case 'active':
        columns = [TaskIdShortened, StartedAt, Host, Rack, CPUs, Memory];
        if (config.showTaskDiskResource) {
          columns.push(Disk);
        }
        columns.push(ActiveActions);
        return columns;
      case 'scheduled':
        return [ScheduledTaskId, NextRun, PendingType, PendingDeployId, ScheduledActions];
      case 'cleaning':
        return [TaskIdShortened, CleanupType, JSONAction];
      case 'lbcleanup':
        return [TaskIdShortened, StartedAt, Host, Rack, InstanceNumber, JSONAction];
      case 'decommissioning':
        columns = [TaskIdShortened, StartedAt, Host, Rack, CPUs, Memory, ActiveActions];
        if (config.showTaskDiskResource) {
          columns.push(Disk);
        }
        columns.push(ActiveActions);
        return columns;
      default:
        return [TaskIdShortened, JSONAction];
    }
  }

  getDefaultSortAttribute(task) {
    switch (this.props.filter.taskStatus) {
      case 'active':
      case 'decommissioning':
        return task.taskId.startedAt;
      case 'scheduled':
        if (!task.pendingTask) return null;
        return task.pendingTask.pendingTaskId.nextRunAt;
      default:
        return null;
    }
  }

  render() {
    const displayRequestTypeFilters = this.props.filter.taskStatus === 'active';
    const displayTasks = this.props.filter.taskStatus !== 'decommissioning' ?
      _.sortBy(getFilteredTasks({tasks: this.props.tasks, filter: this.props.filter}), (task) => this.getDefaultSortAttribute(task)) :
      _.sortBy(getDecomissioningTasks({tasks: this.props.tasks, cleanups: this.props.cleanups}), (task) => this.getDefaultSortAttribute(task));
    if (_.contains(['active', 'decommissioning'], this.props.filter.taskStatus)) displayTasks.reverse();

    let table;
    if (this.state.loading) {
      table = <div className="page-loader fixed"></div>;
    } else if (!displayTasks.length) {
      table = <div className="empty-table-message"><p>No matching tasks</p></div>;
    } else {
      table = (
        <UITable
          data={displayTasks}
          keyGetter={(task) => (Utils.maybe(task, ['taskId', 'id']) || Utils.maybe(task, ['pendingTask', 'pendingTaskId', 'id']) || Utils.maybe(task, ['id']))}
        >
          {this.getColumns()}
        </UITable>
      );
    }

    return (
      <div>
        <TaskFilters filter={this.props.filter} onFilterChange={(...args) => this.handleFilterChange(...args)} displayRequestTypeFilters={displayRequestTypeFilters} />
        {table}
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  const filter = {
    taskStatus: ownProps.params.state || 'active',
    requestTypes: !ownProps.params.requestsSubFilter || ownProps.params.requestsSubFilter === 'all' ? TaskFilters.REQUEST_TYPES : ownProps.params.requestsSubFilter.split(','),
    filterText: ownProps.params.searchFilter || ''
  };
  const statusCode = Utils.maybe(state, ['api', 'tasks', 'statusCode']);
  return {
    pathname: ownProps.location.pathname,
    notFound: statusCode === 404,
    tasks: state.api.tasks.data,
    cleanups: state.api.taskCleanups.data,
    filter
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchTasksInState.trigger(state, true)),
    fetchCleanups: () => dispatch(FetchTaskCleanups.trigger()),
    killTask: (taskId, data) => dispatch(KillTask.trigger(taskId, data)),
    runRequest: (requestId, data) => dispatch(RunRequest.trigger(requestId, data)),
    taskRun: (requestId, runId) => dispatch(FetchRequestRun.trigger(requestId, runId)),
    taskRunHistory: (requestId, runId) => dispatch(FetchRequestRunHistory.trigger(requestId, runId)),
    taskFiles: (taskId, path) => dispatch(FetchTaskFiles.trigger(taskId, path)),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(withRouter(TasksPage), (props) => refresh(props.params.state)));
