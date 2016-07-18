import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { Glyphicon } from 'react-bootstrap';
import rootComponent from '../../rootComponent';
import classNames from 'classnames';
import { FetchTaskSearchParams } from '../../actions/api/history';
import { UpdateFilter } from '../../actions/ui/TaskSearch';

import Breadcrumbs from '../common/Breadcrumbs';
import TaskSearchFilters from './TaskSearchFilters';
import JSONButton from '../common/JSONButton';
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
      page: React.PropTypes.number
    }),
    params: React.PropTypes.shape({
      requestId: React.PropTypes.string
    })
  }

  setPage(page) {
    this.props.updateFilter(_.extend({}, this.props.filter, {page}));
  }

  setCount(count) {
    this.props.updateFilter(_.extend({}, this.props.filter, {count}));
  }

  handleSearch(filter) {
    const count = this.props.filter.count || INITIAL_TASKS_PER_PAGE;
    const page = this.props.filter.page || 1;
    const newFilter = _.extend({}, _.omit(filter, (value) => !value), {count, page});
    this.props.updateFilter(newFilter);
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
          <Link to={`task/${data.taskId.id}/tail/${config.finishedTaskLogPath}`}>···</Link>
          <JSONButton object={data}>{'{ }'}</JSONButton>
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
    return this.props.taskHistory.length && (
      <div className="row">
        <div className="pull-right count-options">
          Results per page:
          <a className={classNames({inactive: this.props.filter.count === 5})} onClick={() => this.setCount(5)}>5</a>
          <a className={classNames({inactive: this.props.filter.count === 10})} onClick={() => this.setCount(10)}>10</a>
          <a className={classNames({inactive: this.props.filter.count === 25})} onClick={() => this.setCount(25)}>25</a>
        </div>
      </div>
    );
  }

  render() {
    return (
      <div>
        {this.renderBreadcrumbs()}
        <h1 className="inline-header">{!this.props.params.requestId && 'Global '}Historical Tasks </h1>
        {this.props.params.requestId && <h3 className="inline-header" style={{marginLeft: '10px'}}>for {this.props.params.requestId}</h3>}
        <h2>Search Parameters</h2>
        <TaskSearchFilters requestId={this.props.params.requestId} onSearch={(filter) => this.handleSearch(filter)} />
        {this.renderPageOptions()}
        <ServerSideTable
          emptyMessage="No matching tasks"
          entries={this.props.taskHistory}
          paginate={true}
          perPage={this.props.filter.count || INITIAL_TASKS_PER_PAGE}
          fetchAction={FetchTaskSearchParams}
          fetchParams={[this.props.filter]}
          headers={['', 'Request ID', 'Deploy ID', 'Host', 'Last Status', 'Started', 'Updated', '']}
          renderTableRow={(...args) => this.renderTableRow(...args)}
        />
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    taskHistory: state.api.taskHistory.data,
    filter: state.taskSearch || { requestId: state.api.taskHistory.data }
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchTaskHistory: (count, page, ...args) => dispatch(FetchTaskSearchParams.trigger(...args, count, page)),
    updateFilter: (newFilter) => dispatch(UpdateFilter(newFilter))
  };
}

function refresh(props) {
  const count = props.filter && props.filter.count || INITIAL_TASKS_PER_PAGE;
  const filter = _.extend({}, {requestId: props.params.requestId}, props.filter);
  return props.fetchTaskHistory(count, 1, filter);
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(TaskSearch, 'Task Search', refresh));
