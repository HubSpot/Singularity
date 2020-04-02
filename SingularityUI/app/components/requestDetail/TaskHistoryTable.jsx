import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { Row, Col, Button, Glyphicon, ButtonToolbar, ButtonGroup } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { Link } from 'react-router';
import { withRouter } from 'react-router';

import Utils from '../../utils';
import Loader from '../common/Loader';

import { FetchTaskHistoryForRequest } from '../../actions/api/history';
import RunNowButton from '../common/modalButtons/RunNowButton';
import Section from '../common/Section';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

import {
  UpdatedAt,
  StartedAt,
  RunId
} from '../tasks/Columns';

class TaskHistoryTable extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    requestParent: PropTypes.object,
    tasksAPI: PropTypes.object,
    fetchTaskHistoryForRequest: PropTypes.func.isRequired
  }

  constructor(props) {
    super(props);
    this.state = {
      loading: true,
      tableChunkSize: props.initialPageSize
    };
  }

  componentDidMount() {
    const { requestId, initialPageNumber } = this.props;
    this.props.fetchTaskHistoryForRequest(
      requestId, this.state.tableChunkSize, initialPageNumber
    ).then(() => {
      this.setState({ loading: false });
    });
  }

  handleTableSizeToggle(count) {
    this.setState({
      tableChunkSize: count,
      loading: true
    })
    const { router, location } = this.props
    router.replace({ ...location, query: {...location.query, taskHistoryPageSize: count }})
    this.props.fetchTaskHistoryForRequest(this.props.requestId, count, 1).then(() => {
      this.setState({
        loading: false
      })
    });
  }

  render() {
    const {requestId, requestParent, tasksAPI, fetchTaskHistoryForRequest} = this.props;
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
          <Button bsSize="small" bsStyle="primary">
            <Glyphicon glyph="search" aria-hidden="true" /><span> Search</span>
          </Button>
        </Link>
      );
    }

    const showButtons = (
      <ButtonToolbar>
        <ButtonGroup bsSize="small" className="pull-right">
          <Button disabled={this.state.tableChunkSize == 10} onClick={() => this.handleTableSizeToggle(10)} >Show 10</Button>
          <Button disabled={this.state.tableChunkSize == 20} onClick={() => this.handleTableSizeToggle(20)} >Show 20</Button>
          <Button disabled={this.state.tableChunkSize == 50} onClick={() => this.handleTableSizeToggle(50)} >Show 50</Button>
        </ButtonGroup>
      </ButtonToolbar>
    )

    const title = (
      <Row>
        <Col md={6}>
          <span>Task history </span>
          {maybeSearchButton}
        </Col>
        <Col md={6}>
          {showButtons}
        </Col>
      </Row>
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

    let table;
    if (this.state.loading) {
      table = <Loader />;
    } else {
      table = (
        <UITable
          emptyTableMessage={emptyTableMessage}
          data={tasks}
          keyGetter={(task) => task.taskId.id}
          rowChunkSize={this.state.tableChunkSize}
          paginated={true}
          fetchDataFromApi={(page, numberPerPage) => fetchTaskHistoryForRequest(requestId, numberPerPage, page)}
          isFetching={isFetching}
          initialPageNumber={this.props.initialPageNumber}
          onPageChange={this.props.onPageChange}
          defaultSortBy={'updatedAt'}
        >
          <Column
            label="Instance"
            id="instanceNo"
            key="instanceNo"
            cellData={(task) => (
              <Link to={`task/${task.taskId.id}`}>
                {task.taskId.instanceNo} - {Utils.humanizeSlaveHostName(task.taskId.host)}
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
          {StartedAt}
          {UpdatedAt}
          <Column
            label="Run Time"
            id="run-time"
            key="run-time"
            cellData={(task) => Utils.duration(task.updatedAt - task.taskId.startedAt)}
          />
          {!Utils.request.isLongRunning(requestParent) && RunId}
          <Column
            id="actions-column"
            key="actions-column"
            className="actions-column"
            cellData={(task) => (
              <span>
                <OverlayTrigger placement="top" id="view-log-overlay" overlay={logTooltip}>
                  <Link to={Utils.tailerPath(task.taskId.id, config.finishedTaskLogPath)}>
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
      );
    }

    return (
      <Section id="task-history" title={title}>
        {table}
      </Section>
    );
  }
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

export default withRouter(connect(
  mapStateToProps,
  mapDispatchToProps
)(TaskHistoryTable));
