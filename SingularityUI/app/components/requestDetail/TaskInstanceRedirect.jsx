import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { FetchActiveTasksForRequest } from '../../actions/api/history';
import { withRouter } from 'react-router';

function refresh(props) {
  props.fetchActiveTasksForRequest(props.params.requestId);
}

class TaskInstanceRedirect extends Component {
  componentWillReceiveProps(nextProps) {
    if (nextProps.activeTasksForRequest && nextProps.activeTasksForRequest[nextProps.params.requestId].data.length > 0) {
      let found = false;
      nextProps.activeTasksForRequest[nextProps.params.requestId].data.forEach((task) => {
        if (task.taskId.instanceNo == parseInt(nextProps.params.instanceNo) && !found) {
          found = true;
          this.props.router.replace(`task/${task.taskId.id}`);
        }
      });
      if (!found) {
        this.props.router.replace(`request/${nextProps.params.requestId}`);
      }
    }
  }

  render() {
    return (
      <div className="page-loader-with-message"><div className="page-loader" /><p>Fetching active tasks...</p></div>
    );
  }
}

TaskInstanceRedirect.propTypes = {
  params: PropTypes.object.isRequired,
  activeTasksForRequest: PropTypes.object
};

const mapStateToProps = (state, ownProps) => {
  return {activeTasksForRequest: state.api.activeTasksForRequest}
};

const mapDispatchToProps = (dispatch, ownProps) => {
  return {
    fetchActiveTasksForRequest: (requestId) => dispatch(FetchActiveTasksForRequest.trigger(requestId))
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(rootComponent(withRouter(TaskInstanceRedirect), (props) => props.params.requestId, refresh));
