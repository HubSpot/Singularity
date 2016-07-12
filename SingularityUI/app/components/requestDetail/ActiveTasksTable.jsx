import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

import UITable from '../common/table/UITable';
import {
  TaskId,
  LastTaskState,
  DeployId,
  StartedAt,
  UpdatedAt
} from '../tasks/Columns';

import TaskStateBreakdown from './TaskStateBreakdown';

const ActiveTasksTable = ({requestId, tasks}) => {
  return (
    <div>
      <h2>Running instances</h2>
      { localStorage.enableTaskStateBreakdown ? <TaskStateBreakdown requestId={requestId} /> : null }
      <UITable
        data={tasks}
        keyGetter={(t) => t.taskId.id}
        emptyTableMessage={<p>No active tasks</p>}
      >
        {TaskId}
        {LastTaskState}
        {DeployId}
        {StartedAt}
        {UpdatedAt}
      </UITable>
    </div>
  );
};

ActiveTasksTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  tasks: PropTypes.arrayOf(PropTypes.object).isRequired
};

const mapStateToProps = (state, ownProps) => ({
  tasks: Utils.maybe(
    state.api.activeTasksForRequest,
    [ownProps.requestId, 'data'],
    []
  )
});

export default connect(
  mapStateToProps,
  null
)(ActiveTasksTable);
