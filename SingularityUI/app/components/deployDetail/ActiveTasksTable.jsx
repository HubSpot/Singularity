import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';
import { Link } from 'react-router';

import SimpleTable from '../common/SimpleTable';
import JSONButton from '../common/JSONButton';

const ActiveTasksTable = ({tasks}) => (
  <SimpleTable
    emptyMessage="No tasks"
    entries={tasks}
    perPage={5}
    first={true}
    last={true}
    headers={['Name', 'Last State', 'Started', 'Updated', '', '']}
    renderTableRow={(data, index) => {
      return (
        <tr key={index}>
          <td><Link to={`task/${data.taskId.id}`}>{data.taskId.id}</Link></td>
          <td><span className={`label label-${Utils.getLabelClassFromTaskState(data.lastTaskState)}`}>{Utils.humanizeText(data.lastTaskState)}</span></td>
          <td>{Utils.timestampFromNow(data.taskId.startedAt)}</td>
          <td>{Utils.timestampFromNow(data.updatedAt)}</td>
          <td className="actions-column"><Link to={`$request/${data.taskId.requestId}/tail/${config.finishedTaskLogPath}?taskIds=${data.taskId.id}`} title="Log">&middot;&middot;&middot;</Link></td>
          <td className="actions-column"><JSONButton object={data}>{'{ }'}</JSONButton></td>
        </tr>
      );
    }}
  />
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
