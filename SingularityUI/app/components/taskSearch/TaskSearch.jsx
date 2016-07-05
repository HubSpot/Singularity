import React from 'react';
import { connect } from 'react-redux';
import { FetchTaskSearchParams } from '../../actions/api/history';

import Breadcrumbs from '../common/Breadcrumbs';
import TasksTable from './TasksTable';
import TaskSearchFilters from './TaskSearchFilters';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import JSONButton from '../common/JSONButton';
import Utils from '../../utils';

class TaskSearch extends React.Component {

  static TASKS_PER_PAGE = 10;

  constructor(props) {
    super(props);
    this.state = {
      filter: {
        requestId: props.requestId,
        page: 1,
        count: TaskSearch.TASKS_PER_PAGE
      },
      disableNext: false,
      loading: false
    }
  }

  handleSearch(filter) {
    let newFilter = _.extend({}, this.state.filter, filter, {page: 1});
    this.setState({
      filter: newFilter,
      disableNext: false
    });
    this.props.fetchTaskHistory(newFilter);
  }

  handlePage(page) {
    let newFilter = _.extend({}, this.state.filter, {page});
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
        <td className="actions-column"><a href={`${config.appRoot}/task/${data.taskId.id}`}><Glyphicon iconClass="link" /></a></td>
        <td><a href={`${config.appRoot}/request/${data.taskId.requestId}`}>{data.taskId.requestId}</a></td>
        <td><a href={`${config.appRoot}/request/${data.taskId.requestId}/deploy/${data.taskId.deployId}`}>{data.taskId.deployId}</a></td>
        <td><a href={`${config.appRoot}/tasks/active/all/${data.taskId.host}`}>{data.taskId.host}</a></td>
        <td>
          <span className={`label label-${Utils.getLabelClassFromTaskState(data.lastTaskState)}`}>
            {Utils.humanizeText(data.lastTaskState)}
          </span>
        </td>
        <td>{Utils.timeStampFromNow(data.taskId.startedAt)}</td>
        <td>{Utils.timeStampFromNow(data.updatedAt)}</td>
        <td className="actions-column">
          <a href={`${config.appRoot}/task/${data.taskId.id}/tail/${config.finishedTaskLogPath}`}>···</a>
          <JSONButton object={data}>{'{ }'}</JSONButton>
        </td>
      </tr>
    );
  }

  renderBreadcrumbs() {
    if (this.props.requestId) {
      return (
        <Breadcrumbs
          items={[
            {
              label: "Request",
              text: this.props.request.request.id,
              link: `${config.appRoot}/request/${this.props.request.request.id}`
            }
          ]}
        />
      )
    }
  }

  render() {
    // console.log(this.props.taskHistory);
    return (
      <div>
        {this.renderBreadcrumbs()}
        <h1 className="inline-header">{!this.props.requestId && 'Global '}Historical Tasks </h1>
        {this.props.requestId && <h3 className="inline-header" style={{marginLeft: '10px'}}>for {this.props.requestId}</h3>}
        <h2>Search Parameters</h2>
        <TaskSearchFilters requestId={this.props.requestId} onSearch={(filter) => this.handleSearch(filter)} />
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

function mapStateToProps(state) {
  return {
    request: state.api.request.data,
    taskHistory: state.api.taskHistory.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchTaskHistory: (...args) => dispatch(FetchTaskSearchParams.trigger(...args))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TaskSearch);
