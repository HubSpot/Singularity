import React from 'react';
import { connect } from 'react-redux';
import {
  getDecomissioningTasks,
  getFilteredTasks
} from '../../selectors/tasks';

import TaskFilters from './TaskFilters';
import { FetchTasksInState, KillTask } from '../../actions/api/tasks';
import { FetchRequestRun, RunRequest } from '../../actions/api/requests';
import { FetchRequestRunHistory } from '../../actions/api/history';
import { FetchTaskFiles } from '../../actions/api/sandbox';

import UITable from '../common/table/UITable';
import KillTaskModal from '../common/KillTaskModal';
import RunNowModal from '../common/RunNowModal';
import TaskLauncher from '../common/TaskLauncher';
import {
  TaskId,
  StartedAt,
  Host,
  Rack,
  CPUs,
  Memory,
  ActiveActions,
  NextRun,
  PendingType,
  DeployId,
  ScheduledActions,
  ScheduledTaskId,
  CleanupType,
  JSONAction,
  InstanceNumber
} from './Columns';

class TasksPage extends React.Component {
  static propTypes = {
  };

  constructor(props) {
    super(props);
    this.state = {
      filter: {
        taskStatus: props.state,
        requestTypes: props.requestsSubFilter === 'all' ? TaskFilters.REQUEST_TYPES : props.requestsSubFilter.split(','),
        filterText: props.searchFilter,
        loading: false
      }
    };
  }

  handleFilterChange(filter) {
    const lastFilterTaskStatus = this.state.filter.taskStatus;
    this.setState({
      loading: lastFilterTaskStatus !== filter.taskStatus,
      filter
    });

    const requestTypes = filter.requestTypes.length === TaskFilters.REQUEST_TYPES.length ? 'all' : filter.requestTypes.join(',');
    this.props.updateFilters(filter.taskStatus, requestTypes, filter.filterText);
    app.router.navigate(`/tasks/${filter.taskStatus}/${requestTypes}/${filter.filterText}`);

    if (lastFilterTaskStatus !== filter.taskStatus) {
      this.props.fetchFilter(filter.taskStatus).then(() => {
        this.setState({
          loading: false
        });
      });
    }
  }

  handleTaskKill(taskId, data) {
    this.props.killTask(taskId, data);
  }

  handleRunNow(requestId, data) {
    this.props.runRequest(requestId, data).then((response) => {
      if (_.contains([RunNowModal.AFTER_TRIGGER.SANDBOX, RunNowModal.AFTER_TRIGGER.TAIL], data.afterTrigger)) {
        this.refs.taskLauncher.startPolling(response.data.request.id, response.data.pendingRequest.runId, data.afterTrigger == RunNowModal.AFTER_TRIGGER.TAIL && data.fileToTail);
      }
    });
  }

  getColumns() {
    switch (this.state.filter.taskStatus) {
      case 'active':
        return [TaskId, StartedAt, Host, Rack, CPUs, Memory, ActiveActions((taskId) => this.refs.killTaskModal.show(taskId))];
      case 'scheduled':
        return [ScheduledTaskId, NextRun, PendingType, DeployId, ScheduledActions((requestId) => this.refs.runModal.show(requestId))];
      case 'cleaning':
        return [TaskId, CleanupType, JSONAction];
      case 'lbcleanup':
        return [TaskId, StartedAt, Host, Rack, InstanceNumber, JSONAction];
      case 'decommissioning':
        return [TaskId, StartedAt, Host, Rack, CPUs, Memory, ActiveActions((taskId) => this.refs.killTaskModal.show(taskId))];
      default:
        return [TaskId, JSONAction];
    }
  }

  getDefaultSortAttribute(t) {
    switch (this.state.filter.taskStatus) {
      case 'active':
      case 'decommissioning':
        return t.taskId.startedAt;
      case 'scheduled':
        if (!t.pendingTask) return null;
        return t.pendingTask.pendingTaskId.nextRunAt;
      default:
        return null;
    }
  }

  render() {
    const displayRequestTypeFilters = this.state.filter.taskStatus === 'active';
    const displayTasks = this.state.filter.taskStatus !== 'decommissioning' ?
      _.sortBy(getFilteredTasks({tasks: this.props.tasks, filter: this.state.filter}), (t) => this.getDefaultSortAttribute(t)) :
      _.sortBy(getDecomissioningTasks({tasks: this.props.tasks, cleanups: this.props.cleanups}), (t) => this.getDefaultSortAttribute(t));
    if (_.contains(['active', 'decommissioning'], this.state.filter.taskStatus)) displayTasks.reverse();

    let table;
    if (this.state.loading) {
      table = <div className="page-loader fixed"></div>;
    } else if (!displayTasks.length) {
      table = <div className="empty-table-message"><p>No matching tasks</p></div>;
    } else {
      table = (
        <UITable
          data={displayTasks}
          keyGetter={(r) => (r.taskId ? r.taskId.id : r.pendingTask.pendingTaskId.id)}
        >
          {this.getColumns()}
        </UITable>
      );
    }

    return (
      <div>
        <TaskFilters filter={this.state.filter} onFilterChange={(...args) => this.handleFilterChange(...args)} displayRequestTypeFilters={displayRequestTypeFilters} />
        {table}
        <RunNowModal ref="runModal" onRunNow={(...args) => this.handleRunNow(...args)} />
        <KillTaskModal ref="killTaskModal" onTaskKill={(...args) => this.handleTaskKill(...args)} />
        <TaskLauncher
          ref="taskLauncher"
          fetchTaskRun={(...args) => this.props.taskRun(...args)}
          fetchTaskRunHistory={(...args) => this.props.taskRunHistory(...args)}
          fetchTaskFiles={(...args) => this.props.taskFiles(...args)}
        />
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    tasks: state.api.tasks.data,
    cleanups: state.api.taskCleanups.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchTasksInState.trigger(state)),
    killTask: (taskId, data) => dispatch(KillTask.trigger(taskId, data)),
    runRequest: (requestId, data) => dispatch(RunRequest.trigger(requestId, data)),
    taskRun: (requestId, runId) => dispatch(FetchRequestRun.trigger(requestId, runId)),
    taskRunHistory: (requestId, runId) => dispatch(FetchRequestRunHistory.trigger(requestId, runId)),
    taskFiles: (taskId, path) => dispatch(FetchTaskFiles.trigger(taskId, path)),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TasksPage);
