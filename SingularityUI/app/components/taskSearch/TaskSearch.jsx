import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { Glyphicon, Label } from 'react-bootstrap';
import rootComponent from '../../rootComponent';
import classNames from 'classnames';
import { FetchTaskSearchParams } from '../../actions/api/history';
import { UpdateFilter } from '../../actions/ui/TaskSearch';

import Breadcrumbs from '../common/Breadcrumbs';
import TaskSearchFilters from './TaskSearchFilters';
import ServerSideTable from '../common/ServerSideTable';
import Utils from '../../utils';

const INITIAL_TASKS_PER_PAGE = 10;

class TaskSearch extends React.Component {

  static propTypes = {
    fetchTaskHistory: React.PropTypes.func.isRequired,
    updateFilter: React.PropTypes.func.isRequired,
    taskHistory: React.PropTypes.array,
    filter: React.PropTypes.shape({
      count: React.PropTypes.number,
      requestId: React.PropTypes.string,
      deployId: React.PropTypes.string,
      host: React.PropTypes.string,
      startedAfter: React.PropTypes.number,
      startedBefore: React.PropTypes.number,
      lastTaskStatus: React.PropTypes.string
    }),
    params: React.PropTypes.shape({
      requestId: React.PropTypes.string
    })
  }

  setCount(count) {
    this.props.updateFilter(_.extend({}, this.props.filter, {count}));
  }

  isCurrentCount(count) {
    if (this.props.filter.count) {
      return this.props.filter.count === count;
    }
    return INITIAL_TASKS_PER_PAGE === count;
  }

  handleSearch(filter) {
    const count = this.props.filter.count || INITIAL_TASKS_PER_PAGE;
    const page = 1;
    const newFilter = _.extend({}, {requestId: this.props.params.requestId}, _.omit(filter, (value) => !value), {count, page});
    this.props.updateFilter(newFilter);
  }

  renderTag(field, value) {
    return (
      <Label>
        {field}: <b>{value}</b>
      </Label>
    );
  }

  renderTags() {
    const {requestId, deployId, host, startedAfter, startedBefore, lastTaskStatus} = this.props.filter;
    return (
      <div>
        {requestId && !this.props.params.requestId && this.renderTag('Request ID', requestId)}{' '}
        {deployId && this.renderTag('Deploy ID', deployId)}{' '}
        {host && this.renderTag('Host', host)}{' '}
        {startedAfter && this.renderTag('Started After', Utils.absoluteTimestamp(parseInt(startedAfter, 10)))}{' '}
        {startedBefore && this.renderTag('Started Before', Utils.absoluteTimestamp(parseInt(startedBefore, 10)))}{' '}
        {lastTaskStatus && this.renderTag('Last Task Status', Utils.humanizeText(lastTaskStatus))}
      </div>);
  }

  renderTableRow(data, key) {
    return (
      <tr key={key}>
        <td className="actions-column"><Link to={`task/${data.taskId.id}`}><Glyphicon glyph="link" /></Link></td>
        <td><Link to={`request/${data.taskId.requestId}`}>{data.taskId.requestId}</Link></td>
        <td><Link to={`request/${data.taskId.requestId}/deploy/${data.taskId.deployId}`}>{data.taskId.deployId}</Link></td>
        <td><Link to={`tasks/active/all/${data.taskId.host}`}>{data.taskId.host}</Link></td>
        <td>
          <span className={`label label-${Utils.getLabelClassFromTaskState(data.lastTaskState)}`}>
            {Utils.humanizeText(data.lastTaskState)}
          </span>
        </td>
        <td>{Utils.timestampFromNow(data.taskId.startedAt)}</td>
        <td>{Utils.timestampFromNow(data.updatedAt)}</td>
        <td className="actions-column">
          <Link to={`task/${data.taskId.id}/tail/${config.finishedTaskLogPath}`}><Glyphicon glyph="file" /></Link>
        </td>
      </tr>
    );
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

  renderPageOptions() {
    return this.props.taskHistory.length !== 0 && (
      <div className="pull-right count-options">
        Results per page:
        <a className={classNames({inactive: this.isCurrentCount(5)})} onClick={() => this.setCount(5)}>5</a>
        <a className={classNames({inactive: this.isCurrentCount(10)})} onClick={() => this.setCount(10)}>10</a>
        <a className={classNames({inactive: this.isCurrentCount(25)})} onClick={() => this.setCount(25)}>25</a>
      </div>
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
        <div className="row">
          {this.renderTags()}
          {this.renderPageOptions()}
        </div>
        <ServerSideTable
          emptyMessage="No matching tasks"
          entries={this.props.taskHistory}
          paginate={true}
          perPage={this.props.filter.count || INITIAL_TASKS_PER_PAGE}
          fetchAction={FetchTaskSearchParams}
          fetchParams={[this.props.filter]}
          headers={['', 'Request ID', 'Deploy ID', 'Host', 'Last Status', 'Started', 'Updated', 'Logs']}
          renderTableRow={(...args) => this.renderTableRow(...args)}
        />
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
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

let firstLoad = true;

function refresh(props) {
  if (!firstLoad) {
    return null;
  }
  firstLoad = false;
  const promises = [];
  const filter = _.extend({}, { requestId: props.params.requestId }, props.filter);
  promises.push(props.fetchTaskHistory(INITIAL_TASKS_PER_PAGE, 1, filter));
  promises.push(props.updateFilter(filter));
  return Promise.all(promises);
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(TaskSearch, 'Task Search', refresh));
