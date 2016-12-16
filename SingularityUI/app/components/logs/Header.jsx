import React from 'react';
import { Link } from 'react-router';
import classNames from 'classnames';
import ColorDropdown from './ColorDropdown';
import SearchDropdown from './SearchDropdown';
import TasksDropdown from './TasksDropdown';

import { connect } from 'react-redux';
import { switchViewMode, scrollAllToTop, scrollAllToBottom } from '../../actions/log';

class Header extends React.Component {
  renderBreadcrumbs() {
    return this.props.path.split('/').map((subpath, key) => {
      if (subpath === '$TASK_ID') {
        return <li key={key}><span className="label label-info">Task ID</span></li>;
      }
      return <li key={key}>{subpath}</li>;
    });
  }

  renderViewButtons() {
    if (this.props.multipleTasks) {
      return (<div className="btn-group" role="group" title="Select View Type">
        <button type="button" className={classNames({btn: true, 'btn-sm': true, 'btn-default': true, 'no-margin': true, active: this.props.viewMode === 'unified'})} onClick={() => { this.props.switchViewMode('unified'); }}>Unified</button>
        <button type="button" className={classNames({btn: true, 'btn-sm': true, 'btn-default': true, 'no-margin': true, active: this.props.viewMode === 'split'})} onClick={() => { this.props.switchViewMode('split'); }}>Split</button>
      </div>);
    }
  }

  renderAnchorButtons() {
    return (this.props.taskGroupCount > 1) && (
      <span>
        <a className="btn btn-default btn-sm tail-bottom-button" onClick={this.props.scrollAllToBottom} title="Scroll All to Bottom">
          <span className="glyphicon glyphicon-chevron-down"></span>
        </a>
        <a className="btn btn-default btn-sm tail-top-button" onClick={this.props.scrollAllToTop} title="Scroll All to Top">
          <span className="glyphicon glyphicon-chevron-up"></span>
        </a>
      </span>
    );
  }

  renderSwitchToNewTailer() {
    if (!this.props.taskGroupHasMultipleTasks) {
      if ((this.props.taskGroupCount === 1)) {
        return (<Link to={`/task/${this.props.firstTaskId}/new-tail/${this.props.path}`}>
          <button type="button" className="btn btn-sm btn-default">Switch to new tailer</button>
        </Link>);
      } else if ((this.props.taskGroupCount > 1)) {
        return (<Link to={`/request/${this.props.requestId}/new-tail/${this.props.path}`}>
          <button type="button" className="btn btn-sm btn-default">Switch to new tailer</button>
        </Link>);
      }
    }
  }

  render() {
    return (
      <div className="tail-header">
        <div className="row">
          <div className="col-md-3">
            <ul className="breadcrumb breadcrumb-request">
              <li>
                Request&nbsp;
                <Link to={`request/${this.props.requestId}`}>
                  {this.props.requestId}
                </Link>
              </li>
            </ul>
          </div>
          <div className="col-md-6">
            <ul className="breadcrumb">
              {this.renderBreadcrumbs()}
            </ul>
          </div>
          <div className="col-md-3 hidden-xs tail-buttons">
            {this.renderSwitchToNewTailer()}
            <SearchDropdown />
            <TasksDropdown />
            <ColorDropdown />
            {this.renderViewButtons()}
            {this.renderAnchorButtons()}
          </div>
        </div>
      </div>
    );
  }
}

Header.propTypes = {
  requestId: React.PropTypes.string,
  path: React.PropTypes.string.isRequired,
  multipleTasks: React.PropTypes.bool.isRequired,
  viewMode: React.PropTypes.string.isRequired,
  taskGroupCount: React.PropTypes.number.isRequired,
  switchViewMode: React.PropTypes.func.isRequired,
  scrollAllToBottom: React.PropTypes.func.isRequired,
  scrollAllToTop: React.PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  return {
    taskGroupCount: state.taskGroups.length,
    multipleTasks: (state.taskGroups.length > 1) || ((state.taskGroups.length > 0) && (state.taskGroups[0].taskIds.length > 1)),
    taskGroupHasMultipleTasks: _.some(state.taskGroups.map((tg) => tg.taskIds.length > 1)),
    firstTaskId: state.taskGroups[0] && state.taskGroups[0].taskIds[0],
    path: state.path,
    viewMode: state.viewMode,
    requestId: state.activeRequest.requestId
  };
}

const mapDispatchToProps = { switchViewMode, scrollAllToBottom, scrollAllToTop };

export default connect(mapStateToProps, mapDispatchToProps)(Header);
