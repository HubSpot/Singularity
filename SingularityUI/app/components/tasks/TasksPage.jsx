import React from 'react';
import { connect } from 'react-redux';
import filterSelector from '../../selectors/tasks/filterSelector';

import TaskFilters from './TaskFilters';

import { FetchAction } from '../../actions/api/tasks';

class TasksPage extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      filter: {
        taskStatus: props.state,
        requestTypes: props.requestsSubFilter == 'all' ? TaskFilters.REQUEST_TYPES : props.requestsSubFilter.split(','),
        filterText: props.searchFilter
      }
    }
  }

  handleFilterChange(filter) {
    this.props.fetchFilter(filter.taskStatus);
    this.setState({
      filter: filter
    });
    const requestTypes = filter.requestTypes.length == TaskFilters.REQUEST_TYPES.length ? 'all' : filter.requestTypes.join(',');
    this.props.updateFilters(filter.taskStatus, requestTypes, filter.filterText);
    app.router.navigate(`/tasks/${filter.taskStatus}/${requestTypes}/${filter.filterText}`);
  }

  render() {
    const displayTasks = filterSelector({tasks: this.props.tasks, filter: this.state.filter});
    console.log(displayTasks);

    return (
      <TaskFilters filter={this.state.filter} onFilterChange={this.handleFilterChange.bind(this)} />
    );
  }
}

function mapStateToProps(state, ownProps) {
  return {
    tasks: state.api.tasks.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchAction.trigger(state))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TasksPage);
