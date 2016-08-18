import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Section from '../common/Section';

import Utils from '../../utils';

import UITable from '../common/table/UITable';
import {
  ScheduledTaskId,
  NextRun,
  ScheduledActions
} from '../tasks/Columns';

const PendingTasksTable = ({tasksAPI}) => {
  const tasks = tasksAPI ? tasksAPI.data : [];

  if (!tasks.length) {
    return <div></div>;
  }

  return (
    <Section id="pending-tasks" title="Scheduled &amp; pending tasks">
      <UITable
        data={tasks}
        keyGetter={(task) => task.pendingTask.pendingTaskId.id}
      >
        {ScheduledTaskId}
        {NextRun}
        {ScheduledActions}
      </UITable>
    </Section>
  );
};

PendingTasksTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  tasksAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  tasksAPI: Utils.maybe(
    state.api.scheduledTasksForRequest,
    [ownProps.requestId]
  )
});

export default connect(
  mapStateToProps,
  null
)(PendingTasksTable);
