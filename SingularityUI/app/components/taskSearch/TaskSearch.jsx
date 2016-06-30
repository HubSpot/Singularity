import React from 'react';
import { connect } from 'react-redux';
import { FetchAction } from '../../actions/api/taskHistory';

import Breadcrumbs from '../common/Breadcrumbs';
import TaskSearchFilters from './TaskSearchFilters';

class TaskSearch extends React.Component {

  handleSearch(filter) {
    console.log(filter);
  }

  render() {
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
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    request: state.api.request.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchTaskHistory: (...args) => dispatch(FetchAction.trigger(...args))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TaskSearch);
