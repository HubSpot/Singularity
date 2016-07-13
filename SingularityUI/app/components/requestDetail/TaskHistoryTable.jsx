import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Button, Glyphicon } from 'react-bootstrap';

import Utils from '../../utils';

import { FetchTaskHistoryForRequest } from '../../actions/api/history';

import ServerSideTable from '../common/ServerSideTable';
import JSONButton from '../common/JSONButton';

const TaskHistoryTable = ({requestId, tasksAPI}) => {
  const tasks = tasksAPI ? tasksAPI.data : [];
  const emptyTableMessage = (Utils.api.isFirstLoad(tasksAPI)
    ? 'Loading...'
    : 'No tasks'
  );

  let maybeSearchButton;
  if (tasks.length) {
    maybeSearchButton = (
      <Button bsStyle="primary" href={`${config.appRoot}/request/${requestId}/taskSearch`}>
        <Glyphicon glyph="search" aria-hidden="true" /><span> Search</span>
      </Button>
    );
  }

  return (
    <div>
      <h2>
        <span>Task history </span>
        {maybeSearchButton}
      </h2>
      <ServerSideTable
        emptyMessage={emptyTableMessage}
        entries={tasks}
        paginate={tasks.length >= 5}
        perPage={5}
        fetchAction={FetchTaskHistoryForRequest}
        fetchParams={[requestId]}
        headers={['Name', 'Last State', 'Deploy ID', 'Started At', 'Updated At', '', '']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index}>
              <td><a href={`${config.appRoot}/task/${data.taskId.id}`}>{data.taskId.id}</a></td>
              <td><span className={`label label-${Utils.getLabelClassFromTaskState(data.lastTaskState)}`}>{Utils.humanizeText(data.lastTaskState)}</span></td>
              <td><a href={`${config.appRoot}/request/${data.taskId.requestId}/deploy/${data.taskId.deployId}`}>{data.taskId.deployId}</a></td>
              <td>{Utils.timestampFromNow(data.taskId.startedAt)}</td>
              <td>{Utils.timestampFromNow(data.updatedAt)}</td>
              <td className="actions-column"><a href={`${config.appRoot}/request/${data.taskId.requestId}/tail/${config.finishedTaskLogPath}?taskIds=${data.taskId.id}`} title="Log">&middot;&middot;&middot;</a></td>
              <td className="actions-column"><JSONButton object={data}>{'{ }'}</JSONButton></td>
            </tr>
          );
        }}
      />
    </div>
  );
};

TaskHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  tasksAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state) => ({
  tasksAPI: state.api.taskHistoryForRequest
});

export default connect(
  mapStateToProps,
  null
)(TaskHistoryTable);
