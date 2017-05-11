import React from 'react';
import { Link } from 'react-router';
import classNames from 'classnames';
import NewColorDropdown from './NewColorDropdown';
import NewTasksDropdown from './NewTasksDropdown';

import { connect } from 'react-redux';
import { switchViewMode } from '../../actions/log';
import { jumpAllToTop, jumpAllToBottom, setColor, toggleTailerGroup } from '../../actions/tailer';

class NewHeader extends React.Component {
  renderBreadcrumbs() {
    if (this.props.paths.length > 1) {
      return (<li>(multiple paths)</li>);
    } else if (this.props.paths.length === 1) {
      return this.props.paths[0].split('/').map((subpath, key) => {
        if (subpath === '$TASK_ID') {
          return <li key={key}><span className="label label-info">Task ID</span></li>;
        }
        return <li key={key}>{subpath}</li>;
      });
    }
  }

  renderAnchorButtons() {
    return (this.props.tailerGroupCount > 1) && (
      <span>
        <a className="btn btn-default btn-sm tail-bottom-button" onClick={this.props.jumpAllToBottom} title="Scroll All to Bottom">
          <span className="glyphicon glyphicon-chevron-down"></span>
        </a>
        <a className="btn btn-default btn-sm tail-top-button" onClick={this.props.jumpAllToTop()} title="Scroll All to Top">
          <span className="glyphicon glyphicon-chevron-up"></span>
        </a>
      </span>
    );
  }

  renderRequestLink() {
    if (this.props.requestIds.length > 1) {
      return (<li>
        (multiple requests)
      </li>);
    } else if (this.props.requestIds.length === 1) {
      return (<li>
        Request&nbsp;
        <Link to={`request/${this.props.requestIds[0]}`}>
          {this.props.requestIds[0]}
        </Link>
      </li>);
    }
  }

  renderTasksDropdown() {
    if (this.props.requestIds.length === 1 && this.props.paths.length === 1) {
      return (<NewTasksDropdown
        ready={this.props.ready}
        runningTasks={this.props.runningTasks}
        visibleTasks={this.props.taskIds}
        onToggle={(taskId) => this.props.toggleTailerGroup(taskId, this.props.paths[0], this.props.taskIds)}
        />);
    }
  }

  renderSwitchToOldTailer() {
    if ((this.props.tailerGroupCount === 1)) {
      return (<Link to={`/task/${this.props.taskIds[0]}/old-tail/${this.props.paths[0]}`}>
        <button type="button" className="btn btn-sm btn-default">Back to old tailer</button>
      </Link>);
    } else if ((this.props.tailerGroupCount > 1)) {
      return (<Link to={`/request/${this.props.requestIds[0]}/old-tail/${this.props.paths[0]}`}>
        <button type="button" className="btn btn-sm btn-default">Back to old tailer</button>
      </Link>);
    }
  }

  render() {
    if (!this.props.ready) {
      return (<div>Loading...</div>);
    }

    return (
      <div className="tail-header">
        <div className="row">
          <div className="col-md-3">
            <ul className="breadcrumb breadcrumb-request">
              {this.renderRequestLink()}
            </ul>
          </div>
          <div className="col-md-6">
            <ul className="breadcrumb">
              {this.renderBreadcrumbs()}
            </ul>
          </div>
          <div className="col-md-3 hidden-xs tail-buttons">
            {this.renderSwitchToOldTailer()}
            {this.renderTasksDropdown()}
            <NewColorDropdown activeColor={this.props.activeColor} onSetColor={this.props.setColor} />
            {this.renderAnchorButtons()}
          </div>
        </div>
      </div>
    );
  }
}

NewHeader.propTypes = {
  //requestIds: React.PropTypes.string,
  //paths: React.PropTypes.string.isRequired,
  viewMode: React.PropTypes.string.isRequired,
  tailerGroupCount: React.PropTypes.number.isRequired,
  switchViewMode: React.PropTypes.func.isRequired,
  jumpAllToTop: React.PropTypes.func.isRequired,
  jumpAllToBottom: React.PropTypes.func.isRequired,
};

export default connect((state) => ({
  tailerGroupCount: state.tailerView.tailerGroups.length,
  taskIds: state.tailerView.taskIds,
  requestIds: state.tailerView.requestIds,
  paths: state.tailerView.paths,
  viewMode: state.tailerView.viewMode,
  ready: state.tailerView.ready,
  activeColor: state.tailerView.color,
  runningTasks: state.tailerView.requestIds.length > 0 && state.api.activeTasksForRequest[state.tailerView.requestIds[0]].data
}), {
  switchViewMode,
  jumpAllToTop,
  jumpAllToBottom,
  setColor,
  toggleTailerGroup,
})(NewHeader);
