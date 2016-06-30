import React from 'react';
import { connect } from 'react-redux';

import Breadcrumbs from '../common/Breadcrumbs';
import TaskSearchFilters from './TaskSearchFilters';

class TaskSearch extends React.Component {

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
        <h2>Search Parameters</h2>
        <TaskSearchFilters requestId={this.props.requestId} />
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
  return {};
}

export default connect(mapStateToProps, mapDispatchToProps)(TaskSearch);
