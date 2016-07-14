import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Button, Glyphicon } from 'react-bootstrap';
import { Link } from 'react-router';

import Utils from '../../utils';

import { FetchTaskHistoryForRequest } from '../../actions/api/history';

import Section from '../common/Section';

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
      <Link to={`request/${requestId}/taskSearch`}>
        <Button bsStyle="primary">
          <Glyphicon glyph="search" aria-hidden="true" /><span> Search</span>
        </Button>
      </Link>
    );
  }

  const title = (
    <span>
      <span>Task history </span>
      {maybeSearchButton}
    </span>
  );

  return (
    <Section id="task-history" title={title}>
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
              <td><Link to={`task/${data.taskId.id}`}>{data.taskId.id}</Link></td>
              <td><span className={`label label-${Utils.getLabelClassFromTaskState(data.lastTaskState)}`}>{Utils.humanizeText(data.lastTaskState)}</span></td>
              <td><Link to={`request/${data.taskId.requestId}/deploy/${data.taskId.deployId}`}>{data.taskId.deployId}</Link></td>
              <td>{Utils.timestampFromNow(data.taskId.startedAt)}</td>
              <td>{Utils.timestampFromNow(data.updatedAt)}</td>
              <td className="actions-column"><Link to={`request/${data.taskId.requestId}/tail/${config.finishedTaskLogPath}?taskIds=${data.taskId.id}`} title="Log">&middot;&middot;&middot;</Link></td>
              <td className="actions-column"><JSONButton object={data}>{'{ }'}</JSONButton></td>
            </tr>
          );
        }}
      />
    </Section>
  );
};

TaskHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  tasksAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  tasksAPI: Utils.maybe(
    state.api.taskHistoryForRequest,
    [ownProps.requestId]
  )
});

export default connect(
  mapStateToProps,
  null
)(TaskHistoryTable);
