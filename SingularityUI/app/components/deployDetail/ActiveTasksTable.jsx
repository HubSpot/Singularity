import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { Glyphicon } from 'react-bootstrap';

import Utils from '../../utils';
import { Link } from 'react-router';

import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import JSONButton from '../common/JSONButton';

const ActiveTasksTable = ({tasks}) => (
  <UITable
    emptyTableMessage="No tasks"
    data={tasks}
    keyGetter={(task) => task.taskId.id}
    rowChunkSize={5}
    paginated={true}
  >
    <Column
      label="Name"
      id="name"
      key="name"
      cellData={(task) => (
        <Link to={`task/${task.taskId.id}`}>
          {task.taskId.id}
        </Link>
      )}
    />
    <Column
      label="Last State"
      id="state"
      key="state"
      cellData={(task) => (
        <span className={`label label-${Utils.getLabelClassFromTaskState(task.lastTaskState)}`}>
          {Utils.humanizeText(task.lastTaskState)}
        </span>
      )}
    />
    <Column
      label="Started"
      id="started"
      key="started"
      cellData={(task) => Utils.timestampFromNow(task.taskId.startedAt)}
    />
    <Column
      label="Updated"
      id="updated"
      key="updated"
      cellData={(task) => Utils.timestampFromNow(task.updatedAt)}
    />
    <Column
      id="actions-column"
      key="actions-column"
      className="actions-column"
      cellData={(task) => (
        <span>
          <Link to={`request/${task.taskId.requestId}/tail/${config.finishedTaskLogPath}?taskIds=${task.taskId.id}`} title="Log">
            <Glyphicon glyph="file" />
          </Link>
          <JSONButton object={task} showOverlay={true}>
            {'{ }'}
          </JSONButton>
        </span>
      )}
    />
  </UITable>
);

ActiveTasksTable.propTypes = {
  tasks: PropTypes.arrayOf(PropTypes.object).isRequired
};

const mapStateToProps = (state) => ({
  tasks: state.api.activeTasksForDeploy.data
});

export default connect(
  mapStateToProps
)(ActiveTasksTable);
