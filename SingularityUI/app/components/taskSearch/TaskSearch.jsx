import React from 'react';
import { connect } from 'react-redux';
import { FetchAction } from '../../actions/api/taskHistory';

import Breadcrumbs from '../common/Breadcrumbs';
import TasksTable from './TasksTable';
import TaskSearchFilters from './TaskSearchFilters';
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
      disableNext: false
    }
  }

  handleSearch(filter) {
    let newFilter = _.extend({}, this.state.filter, filter, {disableNext: false, page: 1});
    this.setState({
      filter: newFilter
    });
    this.props.fetchTaskHistory(newFilter);
  }

  handlePage(page) {
    let newFilter = _.extend({}, this.state.filter, {page});
    this.props.fetchTaskHistory(newFilter).then((resp) => {
      if (resp.data.length < TaskSearch.TASKS_PER_PAGE) {
        this.setState({
          disableNext: false,
          page
        });
      } else {
        this.setState({
          filter: newFilter
        });
      }
    });
  }

  render() {
    console.log(this.props.taskHistory, this.state.filter.page);
    return (
      <div>
        <Breadcrumbs
          items={[
            {
              label: "Request",
              text: this.props.request.request.id,
              link: `${config.appRoot}/request/${this.props.request.request.id}`
            }
          ]}
        />
        <h1>Historical Tasks</h1>
        <h2>Search Parameters</h2>
        <TaskSearchFilters requestId={this.props.requestId} onSearch={(filter) => this.handleSearch(filter)} />
        <div className="row">
          <div className="col-md-12">
            <TasksTable
              data={this.props.taskHistory}
              paginate={true}
              page={this.state.filter.page}
              pageSize={TaskSearch.TASKS_PER_PAGE + 1}
              disableNext={this.props.taskHistory.length < TaskSearch.TASKS_PER_PAGE}
              onPage={(page) => this.handlePage(page)}
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
    fetchTaskHistory: (...args) => dispatch(FetchAction.trigger(...args))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TaskSearch);
