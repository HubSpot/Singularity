import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { Glyphicon } from 'react-bootstrap';
import rootComponent from '../../rootComponent';
import classNames from 'classnames';
import { FetchRequest } from '../../actions/api/requests';
import { FetchTaskSearchParams } from '../../actions/api/history';

import Breadcrumbs from '../common/Breadcrumbs';
import TasksTable from './TasksTable';
import TaskSearchFilters from './TaskSearchFilters';
import JSONButton from '../common/JSONButton';
import Utils from '../../utils';

class TaskSearch extends React.Component {

  static propTypes = {
    requestId: React.PropTypes.string,
    fetchRequest: React.PropTypes.func.isRequired,
    fetchTaskHistory: React.PropTypes.func.isRequired,
    request: React.PropTypes.object,
    taskHistory: React.PropTypes.array,
    params: React.PropTypes.object
  }

  constructor(props) {
    super(props);
    this.state = {
      filter: {
        requestId: props.params.requestId,
        page: 1,
        count: TaskSearch.TASKS_PER_PAGE
      },
      disableNext: false,
      loading: false
    };
  }

  static TASKS_PER_PAGE = 10;

  setCount(count) {
    TaskSearch.TASKS_PER_PAGE = count;
    const newFilter = _.extend({}, this.state.filter, {count, page: 1});
    this.setState({
      filter: newFilter
    });
    this.props.fetchTaskHistory(newFilter);
  }

  handleSearch(filter) {
    const newFilter = _.extend({}, this.state.filter, filter, {page: 1});
    this.setState({
      filter: newFilter,
      disableNext: false
    });
    this.props.fetchTaskHistory(newFilter);
  }

  handlePage(page) {
    const newFilter = _.extend({}, this.state.filter, {page});
    this.setState({
      loading: true
    });

    this.props.fetchTaskHistory(newFilter).then((resp) => {
      if (!resp.data.length) {
        this.props.fetchTaskHistory(_.extend({}, newFilter, {page: page - 1})).then(() => {
          this.setState({
            disableNext: true,
            loading: false
          });
        });
      } else if (resp.data.length < TaskSearch.TASKS_PER_PAGE) {
        this.setState({
          filter: newFilter,
          loading: false,
          disableNext: true
        });
      } else {
        this.setState({
          filter: newFilter,
          loading: false,
          disableNext: false
        });
      }
    });
  }

  renderTableRow(data, i) {
    return (
      <tr key={i}>
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
    if (this.props.params.requestId) {
      return (
        <Breadcrumbs
          items={[
            {
              label: 'Request',
              text: this.props.request.request.id,
              link: `request/${this.props.request.request.id}`
            }
          ]}
        />
      );
    }
    return null;
  }

  renderPageOptions() {
    if (this.props.taskHistory.length) {
      return (
        <div className="row">
          <div className="col-md-12">
            <div className="pull-right count-options">
              Results per page:
              <a className={classNames({inactive: TaskSearch.TASKS_PER_PAGE === 5})} onClick={() => this.setCount(5)}>5</a>
              <a className={classNames({inactive: TaskSearch.TASKS_PER_PAGE === 10})} onClick={() => this.setCount(10)}>10</a>
              <a className={classNames({inactive: TaskSearch.TASKS_PER_PAGE === 25})} onClick={() => this.setCount(25)}>25</a>
            </div>
          </div>
        </div>
      );
    }
    return null;
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
        <div className="row">
          <div className="col-md-12">
            <TasksTable
              emptyMessage={"No matching tasks"}
              headers={['', 'Request ID', 'Deploy ID', 'Host', 'Last Status', 'Started', 'Updated', '']}
              data={this.props.taskHistory}
              paginate={true}
              page={this.state.filter.page}
              pageSize={TaskSearch.TASKS_PER_PAGE + 1}
              disableNext={this.state.disableNext}
              onPage={(page) => this.handlePage(page)}
              renderTableRow={(...args) => this.renderTableRow(...args)}
              loading={this.state.loading}
            />
          </div>
        </div>
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  return {
    request: Utils.maybe(state.api.request, [ownProps.params.requestId, 'data']),
    taskHistory: state.api.taskHistory.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchTaskHistory: (...args) => dispatch(FetchTaskSearchParams.trigger(...args)),
    fetchRequest: (requestId) => dispatch(FetchRequest.trigger(requestId))
  };
}

function refresh(props) {
  const promises = [];
  if (props.params.requestId) {
    promises.push(props.fetchRequest(props.params.requestId));
  }
  promises.push(props.fetchTaskHistory({requestId: props.params.requestId, page: 1, count: TaskSearch.TASKS_PER_PAGE}));
  return Promise.all(promises);
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(TaskSearch, 'Task Search', refresh));
