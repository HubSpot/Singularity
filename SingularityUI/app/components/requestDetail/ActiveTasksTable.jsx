import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Link } from 'react-router';
import { Button } from 'react-bootstrap';

import Section from '../common/Section';

import Utils from '../../utils';

import UITable from '../common/table/UITable';
import {
  TaskId,
  LastTaskState,
  DeployId,
  StartedAt,
  UpdatedAt,
  LogLinkAndJSON
} from '../tasks/Columns';

import { FetchTaskHistoryForRequest } from '../../actions/api/history';

import TaskStateBreakdown from './TaskStateBreakdown';

const ActiveTasksTable = ({requestId, tasksAPI, fetchTaskHistoryForRequest}) => {
  const tasks = tasksAPI ? tasksAPI.data : [];
  const emptyTableMessage = (Utils.api.isFirstLoad(tasksAPI)
    ? <p>Loading...</p>
    : <p>No active tasks</p>
  );

  let maybeAggregateTailButton;
  if (tasks.length > 1) {
    maybeAggregateTailButton = (
      <Link to={`request/${requestId}/tail/${config.runningTaskLogPath}`}>
        <Button className="pull-right">
          View Aggregate Logs
        </Button>
      </Link>
    );
  }

  const title = <span>Running instances {maybeAggregateTailButton}</span>;

  return (
    <Section id="running-instances" title={title}>
      { localStorage.enableTaskStateBreakdown ? <TaskStateBreakdown requestId={requestId} /> : null }
      <UITable
        data={tasks}
        keyGetter={(task) => task.taskId.id}
        emptyTableMessage={emptyTableMessage}
        triggerOnDataSizeChange={fetchTaskHistoryForRequest}
      >
        {TaskId}
        {LastTaskState}
        {DeployId}
        {StartedAt}
        {UpdatedAt}
        {LogLinkAndJSON(config.runningTaskLogPath)}
      </UITable>
    </Section>
  );
};

ActiveTasksTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  tasksAPI: PropTypes.object.isRequired,
  fetchTaskHistoryForRequest: PropTypes.func.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  tasksAPI: Utils.maybe(
    state.api.activeTasksForRequest,
    [ownProps.requestId]
  )
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchTaskHistoryForRequest: () => dispatch(FetchTaskHistoryForRequest.trigger(ownProps.requestId, 5, 1))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ActiveTasksTable);
