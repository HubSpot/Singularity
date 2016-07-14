import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

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

import TaskStateBreakdown from './TaskStateBreakdown';

const ActiveTasksTable = ({requestId, tasksAPI}) => {
  const tasks = tasksAPI ? tasksAPI.data : [];
  const emptyTableMessage = (Utils.api.isFirstLoad(tasksAPI)
    ? <p>Loading...</p>
    : <p>No active tasks</p>
  );

  let maybeAggregateTailButton;
  if (tasks.length > 1) {
    maybeAggregateTailButton = (
      <Button
        className="pull-right"
        href={`${config.appRoot}/request/${requestId}/tail/$TASK_ID/${config.runningTaskLogPath}`}>
        View Aggregate Logs
      </Button>
    );
  }

  const title = <span>Running instances {maybeAggregateTailButton}</span>;

  return (
    <Section id="running-instances" title={title}>
      { localStorage.enableTaskStateBreakdown ? <TaskStateBreakdown requestId={requestId} /> : null }
      <UITable
        data={tasks}
        keyGetter={(t) => t.taskId.id}
        emptyTableMessage={emptyTableMessage}
      >
        {TaskId}
        {LastTaskState}
        {DeployId}
        {StartedAt}
        {UpdatedAt}
        {LogLinkAndJSON}
      </UITable>
    </Section>
  );
};

ActiveTasksTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  tasksAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  tasksAPI: Utils.maybe(
    state.api.activeTasksForRequest,
    [ownProps.requestId]
  )
});

export default connect(
  mapStateToProps,
  null
)(ActiveTasksTable);
