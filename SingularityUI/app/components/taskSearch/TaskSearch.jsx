import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { Glyphicon, Label } from 'react-bootstrap';
import rootComponent from '../../rootComponent';
import { FetchTaskSearchParams } from '../../actions/api/history';
import { UpdateFilter, refresh } from '../../actions/ui/taskSearch';

import Breadcrumbs from '../common/Breadcrumbs';
import TaskSearchFilters from './TaskSearchFilters';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import Utils from '../../utils';

const INITIAL_TASKS_PER_PAGE = 10;

class TaskSearch extends React.Component {

  static propTypes = {
    fetchTaskHistory: React.PropTypes.func.isRequired,
    updateFilter: React.PropTypes.func.isRequired,
    taskHistory: React.PropTypes.array,
    isFetching: React.PropTypes.bool,
    filter: React.PropTypes.shape({
      requestId: React.PropTypes.string,
      deployId: React.PropTypes.string,
      runId: React.PropTypes.string,
      host: React.PropTypes.string,
      startedAfter: React.PropTypes.number,
      startedBefore: React.PropTypes.number,
      updatedAfter: React.PropTypes.number,
      updatedBefore: React.PropTypes.number,
      lastTaskStatus: React.PropTypes.string
    }),
    params: React.PropTypes.shape({
      requestId: React.PropTypes.string
    })
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.params.requestId !== this.props.params.requestId) {
      FetchTaskSearchParams.clear();
      nextProps.fetchTaskHistory(INITIAL_TASKS_PER_PAGE, 1, { requestId: nextProps.params.requestId }).then(this.resetTablePageAndCount);
      nextProps.updateFilter({ requestId: nextProps.params.requestId });
    }
  }

  handleSearch(filter) {
    const newFilter = _.extend({}, {requestId: this.props.params.requestId}, _.omit(filter, (value) => !value));
    this.props.updateFilter(newFilter);
    this.props.fetchTaskHistory(INITIAL_TASKS_PER_PAGE, 1, filter);
    this.resetTablePageAndCount();
  }

  fetchDataFromApi(page, numberPerPage, sortBy) {
    return this.props.fetchTaskHistory(numberPerPage, page, _.extend({}, this.props.filter, { sortBy }));
  }

  bindResetPageAndCount(table) {
    if (!table) return;
    this.resetTablePageAndCount = table.resetPageAndChunkSizeWithoutChangingData(table);
  }

  renderTag(field, value) {
    return (
      <Label>
        {field}: <b>{value}</b>
      </Label>
    );
  }

  renderTags() {
    const {requestId, deployId, runId, host, startedAfter, startedBefore, updatedBefore, updatedAfter, lastTaskStatus} = this.props.filter;
    return (
      <div>
        {requestId && !this.props.params.requestId && this.renderTag('Request ID', requestId)}{' '}
        {deployId && this.renderTag('Deploy ID', deployId)}{' '}
        {runId && this.renderTag('Run ID', runId)}{' '}
        {host && this.renderTag('Host', host)}{' '}
        {startedAfter && this.renderTag('Started After', Utils.absoluteTimestamp(parseInt(startedAfter, 10)))}{' '}
        {startedBefore && this.renderTag('Started Before', Utils.absoluteTimestamp(parseInt(startedBefore, 10)))}{' '}
        {updatedAfter && this.renderTag('Updated After', Utils.absoluteTimestamp(parseInt(updatedAfter, 10)))}{' '}
        {updatedBefore && this.renderTag('Updated Before', Utils.absoluteTimestamp(parseInt(updatedBefore, 10)))}{' '}
        {lastTaskStatus && this.renderTag('Last Task Status', Utils.humanizeText(lastTaskStatus))}
      </div>);
  }

  renderBreadcrumbs() {
    return this.props.params.requestId && (
      <Breadcrumbs
        items={[
          {
            label: 'Request',
            text: this.props.params.requestId,
            link: `request/${this.props.params.requestId}`
          }
        ]}
      />
    );
  }

  render() {
    return (
      <div>
        {this.renderBreadcrumbs()}
        <h1 className="inline-header">Task Search</h1>
        {this.props.params.requestId && <h3 className="inline-header" style={{marginLeft: '10px'}}>for {this.props.params.requestId}</h3>}
        <h2>Search Parameters</h2>
        <TaskSearchFilters requestId={this.props.params.requestId} onSearch={(filter) => this.handleSearch(filter)} />
        {this.renderTags()}
        <UITable
          emptyTableMessage="No matching tasks"
          data={this.props.taskHistory}
          keyGetter={(task) => task.taskId.id}
          rowChunkSize={INITIAL_TASKS_PER_PAGE}
          rowChunkSizeChoices={[5, 10, 25]}
          paginated={true}
          fetchDataFromApi={(page, numberPerPage, sortBy) => this.fetchDataFromApi(page, numberPerPage, sortBy)}
          isFetching={this.props.isFetching}
          showPageLoaderWhenFetching={true}
          ref={(table) => this.bindResetPageAndCount(table)}
        >
          <Column
            label=""
            id="url"
            key="url"
            className="actions-column"
            cellData={(task) => (
              <Link to={`task/${task.taskId.id}`}>
                <Glyphicon glyph="link" />
              </Link>
            )}
          />
          <Column
            label="Request ID"
            id="request-id"
            key="request-id"
            cellData={(task) => (
              <Link to={`request/${task.taskId.requestId}`}>
                {task.taskId.requestId}
              </Link>
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
            label="Host"
            id="host"
            key="host"
            cellData={(task) => (
              <Link to={`tasks/active/all/${task.taskId.host}`}>{task.taskId.host}</Link>
            )}
          />
          <Column
            label="Last Status"
            id="status"
            key="status"
            cellData={(task) => (
              <span className={`label label-${Utils.getLabelClassFromTaskState(task.lastTaskState)}`}>
                {Utils.humanizeText(task.lastTaskState)}
              </span>
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
            label="Logs"
            id="actions-column"
            key="actions-column"
            className="actions-column"
            cellData={(task) => (
              <Link to={`task/${task.taskId.id}/tail/${config.finishedTaskLogPath}`}>
                <Glyphicon glyph="file" />
              </Link>
            )}
          />
        </UITable>
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    isFetching: state.api.taskHistory.isFetching,
    taskHistory: state.api.taskHistory.data,
    filter: state.taskSearch
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchTaskHistory: (count, page, ...args) => dispatch(FetchTaskSearchParams.trigger(...args, count, page)),
    updateFilter: (newFilter) => dispatch(UpdateFilter(newFilter))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(TaskSearch, (props) => refresh(Utils.maybe(props, ['params', 'requestId'], undefined), INITIAL_TASKS_PER_PAGE, 1), false));
