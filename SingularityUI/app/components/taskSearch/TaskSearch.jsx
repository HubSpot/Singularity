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
      displayTasks: []
    }
  }

  componentWillUpdate(nextProps, nextState) {
    if (nextState.filter !== this.state.filter) {
      this.props.fetchTaskHistory(nextState.filter);
    }
    if (nextProps.taskHistory != this.props.taskHistory) {
      this.setState({
        displayTasks: this.state.displayTasks.concat(nextProps.taskHistory)
      });
    }
  }

  handleSearch(filter) {
    let newFilter = _.extend({page: 1}, this.state.filter, filter);
    this.setState({
      filter: newFilter,
      displayTasks: []
    });
  }

  handlePage(page) {
    this.setState({
      filter: _.extend({}, this.state.filter, {page: page})
    });
  }

  render() {
    console.log(this.state.displayTasks.length);
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
              data={this.state.displayTasks}
              paginate={true}
              page={this.state.filter.page}
              pageSize={TaskSearch.TASKS_PER_PAGE}
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
