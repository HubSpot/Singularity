import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Button, Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { Link } from 'react-router';

import Utils from '../../utils';

import { FetchTaskHistoryForRequest } from '../../actions/api/history';
import RunNowButton from '../common/modalButtons/RunNowButton';
import Section from '../common/Section';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

const TaskHistoryTable = ({requestId, requestParent, tasksAPI, fetchTaskHistoryForRequest}) => {
  const tasks = tasksAPI ? tasksAPI.data : [];
  const isFetching = tasksAPI ? tasksAPI.isFetching : false;
  const emptyTableMessage = (Utils.api.isFirstLoad(tasksAPI)
    ? 'Loading...'
    : 'No tasks'
  );

  let maybeSearchButton;
  if (tasks.length) {
    maybeSearchButton = (
      <Link to={`request/${requestId}/task-search`}>
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

  const logTooltip = (
    <ToolTip id="log">
      Logs
    </ToolTip>
  );

  const runNowTooltip = (
    <ToolTip id="run-now">
      Rerun This Task
    </ToolTip>
  );

  return (
    <Section id="task-history" title={title}>
      <UITable
        emptyTableMessage={emptyTableMessage}
        data={tasks}
        keyGetter={(task) => task.taskId.id}
        rowChunkSize={5}
        paginated={true}
        fetchDataFromApi={(page, numberPerPage) => fetchTaskHistoryForRequest(requestId, numberPerPage, page)}
        isFetching={isFetching}
      >
        <Column
          label="Name"
          id="url"
          key="url"
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
          label="Deploy ID"
          id="deploy-id"
          key="deploy-id"
          cellData={(task) => (
            <Link to={`request/${task.taskId.requestId}/deploy/${task.taskId.deployId}`}>
              {task.taskId.deployId}
            </Link>
          )}
        />
        <Column
          label="Started At"
          id="started"
          key="started"
          cellData={(task) => Utils.timestampFromNow(task.taskId.startedAt)}
        />
        <Column
          label="Updated At"
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
              <OverlayTrigger placement="top" id="view-log-overlay" overlay={logTooltip}>
                <Link to={`task/${task.taskId.id}/tail/${config.finishedTaskLogPath}`}>
                  <Glyphicon glyph="file" />
                </Link>
              </OverlayTrigger>
              {Utils.request.canBeRunNow(requestParent) && (
                <RunNowButton requestId={requestId} taskId={task.taskId.id}>
                  <OverlayTrigger placement="top" id="view-run-now-overlay" overlay={runNowTooltip}>
                    <a title="Rerun This Task">
                      <Glyphicon glyph="repeat" />
                    </a>
                  </OverlayTrigger>
                </RunNowButton>
              )}
              <JSONButton object={task} showOverlay={true}>
                {'{ }'}
              </JSONButton>
            </span>
          )}
        />
      </UITable>
    </Section>
  );
};

TaskHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  requestParent: PropTypes.object,
  tasksAPI: PropTypes.object.isRequired,
  fetchTaskHistoryForRequest: PropTypes.func.isRequired
};

const mapDispatchToProps = (dispatch) => ({
  fetchTaskHistoryForRequest: (requestId, count, page) => dispatch(FetchTaskHistoryForRequest.trigger(requestId, count, page))
});

const mapStateToProps = (state, ownProps) => ({
  requestParent: Utils.maybe(state.api.request, [ownProps.requestId, 'data']),
  tasksAPI: Utils.maybe(
    state.api.taskHistoryForRequest,
    [ownProps.requestId]
  )
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(TaskHistoryTable);
